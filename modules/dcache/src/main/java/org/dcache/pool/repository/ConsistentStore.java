package org.dcache.pool.repository;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dcache.pool.classic.ChecksumModuleV1;
import org.dcache.pool.classic.ReplicaStatePolicy;
import org.dcache.pool.repository.meta.EmptyMetaDataStore;
import org.dcache.vehicles.FileAttributes;

import diskCacheV111.util.CacheException;
import diskCacheV111.util.DiskErrorCacheException;
import diskCacheV111.util.PnfsHandler;
import diskCacheV111.util.PnfsId;
import diskCacheV111.util.ChecksumFactory;
import diskCacheV111.util.FileInCacheException;
import diskCacheV111.vehicles.StorageInfo;

/**
 * Wrapper for a MetaDataStore which encapsulates the logic for
 * recovering MetaDataRecord objects from PnfsManager in case they are
 * missing or broken in a MetaDataStore.
 *
 * Warning: The class is only thread-safe as long as its methods are
 * not invoked concurrently on the same PNFS ID.
 */
public class ConsistentStore
    implements MetaDataStore
{
    private final static Logger _log = LoggerFactory.getLogger(ConsistentStore.class);

    private final static String RECOVERING_MSG =
        "Recovering %1$s...";
    private final static String MISSING_MSG =
        "Recovering: Reconstructing meta data for %1$s";
    private final static String PARTIAL_FROM_TAPE_MSG =
        "Recovering: Removed %1$s because it was not fully staged";
    private final static String MISSING_SI_MSG =
        "Recovering: Fetched storage info for %1$s from PNFS";
    private final static String FILE_NOT_FOUND_MSG =
        "Recovering: Removed %1$s because name space entry was deleted";
    private final static String UPDATE_SIZE_MSG =
        "Recovering: Setting size of %1$s in PNFS to %2$d";
    private final static String MARKED_MSG =
        "Recovering: Marked %1$s as %2$s";
    private final static String REMOVING_REDUNDANT_META_DATA =
        "Removing redundant meta data for %s";

    private final static String BAD_MSG =
        "Marked %1$s bad: %2$s";
    private final static String BAD_SIZE_MSG =
        "File size mismatch for %1$s. Expected %2$d bytes, but found %3$d bytes.";

    private final PnfsHandler _pnfsHandler;
    private final MetaDataStore _metaDataStore;
    private final FileStore _fileStore;
    private final MetaDataStore _importStore;
    private final ChecksumModuleV1 _checksumModule;
    private final ReplicaStatePolicy _replicaStatePolicy;
    private String _poolName;

    public ConsistentStore(PnfsHandler pnfsHandler,
                           ChecksumModuleV1 checksumModule,
                           FileStore fileStore,
                           MetaDataStore metaDataStore,
                           MetaDataStore importStore,
                           ReplicaStatePolicy replicaStatePolicy)
    {
        _pnfsHandler = pnfsHandler;
        _checksumModule = checksumModule;
        _fileStore = fileStore;
        _metaDataStore = metaDataStore;
        _importStore = importStore;
        _replicaStatePolicy = replicaStatePolicy;

        if (!(_importStore instanceof EmptyMetaDataStore)) {
            _log.warn(String.format("NOTICE: Importing any missing meta data from %s. This should only be used to convert an existing repository and never as a permanent setup.", _importStore));
        }
    }

    public void setPoolName(String poolName)
    {
        if (poolName == null || poolName.isEmpty()) {
            throw new IllegalArgumentException("Invalid pool name");
        }
        _poolName = poolName;
    }

    public String getPoolName()
    {
        return _poolName;
    }

    /**
     * Returns a collection of IDs of entries in the store. Removes
     * redundant meta data entries in the process.
     */
    @Override
    public synchronized Collection<PnfsId> list()
    {
        Collection<PnfsId> files = _fileStore.list();
        Collection<PnfsId> records = _metaDataStore.list();
        records.removeAll(new HashSet(files));
        for (PnfsId id: records) {
            _log.warn(String.format(REMOVING_REDUNDANT_META_DATA, id));
            _metaDataStore.remove(id);
        }
        return files;
    }

    /**
     * Retrieves a CacheRepositoryEntry from the wrapped meta data
     * store. If the entry is missing or fails consistency checks, the
     * entry is reconstructed with information from PNFS.
     */
    @Override
    public MetaDataRecord get(PnfsId id)
        throws IllegalArgumentException, CacheException, InterruptedException
    {
        File file = _fileStore.get(id);
        if (!file.isFile()) {
            return null;
        }

        MetaDataRecord entry = _metaDataStore.get(id);

        if (entry == null) {
            /* Import from old repository.
             */
            entry = _importStore.get(id);
            if (entry != null) {
                entry = _metaDataStore.create(entry);
                _log.warn("Imported meta data for " + id
                          + " from " + _importStore.toString());
            }
        }

        if (isBroken(entry)) {
            _log.warn(String.format(RECOVERING_MSG, id));

            if (entry == null) {
                entry = _metaDataStore.create(id);
                _log.warn(String.format(MISSING_MSG, id));
            }

            try {
                EntryState state = entry.getState();
                if (isDeletable(state)) {
                    handleDeletable(id, file);
                    return null;
                }

                /* We try to replay file registration for BROKEN files.
                 */
                if (state == EntryState.BROKEN) {
                    state = EntryState.FROM_CLIENT;
                }
                entry = rebuildEntry(entry);

            } catch (IOException e) {
                throw new DiskErrorCacheException("I/O error in healer: " + e.getMessage());
            } catch (CacheException e) {
                switch (e.getRc()) {
                case CacheException.FILE_NOT_FOUND:
                    _metaDataStore.remove(id);
                    file.delete();
                    _pnfsHandler.clearCacheLocation(id);
                    _log.warn(String.format(FILE_NOT_FOUND_MSG, id));
                    return null;

                case CacheException.TIMEOUT:
                    throw e;

                default:
                    entry.setState(EntryState.BROKEN);
                    _log.error(String.format(BAD_MSG, id, e.getMessage()));
                    break;
                }
            }
        }

        return entry;
    }

    private boolean isBroken(MetaDataRecord entry) throws CacheException {
        return (entry == null)
                || (entry.getStorageInfo() == null)
                || (entry.getStorageInfo().getFileSize() != entry.getSize())
                || (entry.getState() != EntryState.CACHED && entry.getState() != EntryState.PRECIOUS);
    }



    private MetaDataRecord rebuildEntry(MetaDataRecord entry)
            throws CacheException, InterruptedException, IOException {

                EntryState state = entry.getState();
                PnfsId id = entry.getPnfsId();
                if (entry.getStorageInfo() == null
                    || (isStateTransient(state))) {
                    entry = rebuildMissingStorageInfo(entry, id);
                    _log.warn(String.format(MISSING_SI_MSG, id));
                }

                /* If the intended file size is known, then compare it
                 * to the actual file size on disk. Fail in case of a
                 * mismatch. Notice we do this before the checksum
                 * check: First of all it is a lot cheaper than the
                 * checksum check and we may thus safe some time for
                 * incomplete files. Second, if file size is known but
                 * checksum is not, then we want to fail before the
                 * checksum is updated in PNFS.
                 */
                StorageInfo info = entry.getStorageInfo();
                long length = entry.getDataFile().length();
                if (!(state == EntryState.FROM_CLIENT && info.getFileSize() == 0)
                    && info.getFileSize() != length) {
                    throw new CacheException(String.format(BAD_SIZE_MSG, id, info.getFileSize(), length));
                }

                /* Compute and update checksum. May fail if there is a
                 * mismatch.
                 */
                if (_checksumModule != null) {
                    ChecksumFactory factory =
                        _checksumModule.getDefaultChecksumFactory();
                    _checksumModule.setMoverChecksums(id, entry.getDataFile(), factory,
                                                      null, null);
                }

                /* We always register the file location.
                 */
                FileAttributes fileAttributes = new FileAttributes();
                fileAttributes.setLocations(Collections.singleton(_poolName));

                /* Update the size in the storage info and in PNFS if
                 * file size is unknown. We initialize access latency
                 * and retention policy at the same time.
                 */
                if (state == EntryState.FROM_CLIENT && info.getFileSize() == 0) {
                    fileAttributes.setSize(length);
                    fileAttributes.setAccessLatency(info.getAccessLatency());
                    fileAttributes.setRetentionPolicy(info.getRetentionPolicy());
                    info.setFileSize(length);
                    entry.setStorageInfo(info);
                    _log.warn(String.format(UPDATE_SIZE_MSG, id, length));
                }

                /* Update file size, location, access_latency and
                 * retention_policy within namespace (pnfs or chimera).
                 */
                _pnfsHandler.setFileAttributes(id, fileAttributes);

                /* If not already precious or cached, we move the entry to
                 * the target state of a newly uploaded file.
                 */
                if (state != EntryState.CACHED && state != EntryState.PRECIOUS) {
                    EntryState targetState =
                        _replicaStatePolicy.getTargetState(info);
                    List<StickyRecord> stickyRecords =
                        _replicaStatePolicy.getStickyRecords(info);

                    for (StickyRecord record: stickyRecords) {
                        entry.setSticky(record.owner(), record.expire(), false);
                    }

                    entry.setState(targetState);
                    _log.warn(String.format(MARKED_MSG, id, targetState));
                }
                return entry;
    }

    /**
     * Creates a new entry. Fails if file already exists in the file
     * store. If the entry already exists in the meta data store, then
     * it is overwritten.
     */
    @Override
    public MetaDataRecord create(PnfsId id)
        throws DuplicateEntryException, CacheException
    {
        if (_log.isInfoEnabled()) {
            _log.info("Creating new entry for " + id);
        }

        /* Fail if file already exists.
         */
        File dataFile = _fileStore.get(id);
        if (dataFile.exists()) {
            _log.warn("Entry already exists: " + id);
            throw new FileInCacheException("Entry already exists: " + id);
        }

        /* Create meta data record. Recreate if it already exists.
         */
        MetaDataRecord entry;
        try {
            entry = _metaDataStore.create(id);
        } catch (DuplicateEntryException e) {
            _log.warn("Deleting orphaned meta data entry for " + id);
            _metaDataStore.remove(id);
            try {
                entry = _metaDataStore.create(id);
            } catch (DuplicateEntryException f) {
                throw
                    new RuntimeException("Unexpected repository error", e);
            }
        }

        return entry;
    }

    /**
     * Calls through to the wrapped meta data store.
     */
    @Override
    public MetaDataRecord create(MetaDataRecord entry)
        throws DuplicateEntryException, CacheException
    {
        return _metaDataStore.create(entry);
    }

    /**
     * Calls through to the wrapped meta data store.
     */
    @Override
    public void remove(PnfsId id)
    {
        File f = _fileStore.get(id);
        if(!f.delete()) {
            /*
             * restore scriprs may fail to create a disk file,
             * but repository still will get request to destroy it.
             */
            if( f.exists() && !f.delete() ) {
                String msg = "Failed to delete file " + id + " on pool";
                _log.error(msg);
                throw new RuntimeException(msg);
            }
        }
        _metaDataStore.remove(id);
    }

    /**
     * Calls through to the wrapped meta data store.
     */
    @Override
    public boolean isOk()
    {
        return _fileStore.isOk() && _metaDataStore.isOk();
    }

    @Override
    public void close()
    {
        _metaDataStore.close();
    }

    @Override
    public String toString()
    {
        return String.format("[data=%s;meta=%s]", _fileStore, _metaDataStore);
    }

    /**
     * Provides the amount of free space on the file system containing
     * the data files.
     */
    @Override
    public long getFreeSpace()
    {
        return _metaDataStore.getFreeSpace();
    }

    /**
     * Provides the total amount of space on the file system
     * containing the data files.
     */
    @Override
    public long getTotalSpace()
    {
        return _metaDataStore.getTotalSpace();
    }

    private void handleDeletable(PnfsId id, File file) {
        _metaDataStore.remove(id);
        file.delete();
        _pnfsHandler.clearCacheLocation(id);
        _log.info(String.format(PARTIAL_FROM_TAPE_MSG, id));
    }

    private boolean isDeletable(EntryState state) {
        /* It is safe to remove FROM_STORE files: We have a
         * copy on HSM anyway. Files in REMOVED or DESTROYED
         * where about to be deleted, so we can finish the
         * job.
         */
        return state == EntryState.FROM_STORE
                || state == EntryState.REMOVED
                || state == EntryState.DESTROYED;
    }

    private boolean isStateTransient(EntryState state){
//      a file must be eigther Cached or Precious as an endstate
        return (state != EntryState.CACHED
                        && state != EntryState.PRECIOUS);
    }

    private MetaDataRecord rebuildMissingStorageInfo(MetaDataRecord entry, PnfsId id) throws CacheException {
        entry.setStorageInfo(_pnfsHandler.getStorageInfoByPnfsId(id).getStorageInfo());
        return entry;
    }
}