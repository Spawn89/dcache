package org.dcache.pinmanager;

import java.util.Date;
import java.util.EnumSet;

import diskCacheV111.vehicles.Message;
import diskCacheV111.vehicles.ProtocolInfo;
import diskCacheV111.vehicles.StorageInfo;
import diskCacheV111.util.PnfsId;
import org.dcache.vehicles.FileAttributes;
import org.dcache.namespace.FileAttribute;
import static org.dcache.namespace.FileAttribute.*;

public class PinManagerPinMessage extends Message
{
    static final long serialVersionUID = -146552359952271936L;

    private FileAttributes _fileAttributes;
    private ProtocolInfo _protocolInfo;
    private long _lifetime;
    private long _pinId;
    private String _pool;
    private String _requestId;
    private Date _expirationTime;

    public PinManagerPinMessage(FileAttributes fileAttributes,
                                ProtocolInfo protocolInfo,
                                String requestId,
                                long lifetime)
    {
        _fileAttributes = fileAttributes;
        _protocolInfo = protocolInfo;
        _requestId = requestId;
        _lifetime = lifetime;
    }

    public String getRequestId()
    {
        return _requestId;
    }

    public long getLifetime()
    {
        return _lifetime;
    }

    public PnfsId getPnfsId()
    {
        return _fileAttributes.getPnfsId();
    }

    public StorageInfo getStorageInfo()
    {
        return _fileAttributes.getStorageInfo();
    }

    public FileAttributes getFileAttributes()
    {
        return _fileAttributes;
    }

    public ProtocolInfo getProtocolInfo()
    {
        return _protocolInfo;
    }

    public String getPool()
    {
        return _pool;
    }

    public void setPool(String pool)
    {
        _pool = pool;
    }

    public long getPinId()
    {
        return _pinId;
    }

    public void setPinId(long pinId)
    {
        _pinId = pinId;
    }

    public void setExpirationTime(Date expirationTime)
    {
        _expirationTime = expirationTime;
    }

    public Date getExpirationTime()
    {
        return _expirationTime;
    }

    @Override
    public String toString()
    {
        return "PinManagerPinMessage["+_fileAttributes + "," +
            _protocolInfo + "," + _lifetime + "]";
    }

    public static EnumSet<FileAttribute> getRequiredAttributes()
    {
        return EnumSet.of(PNFSID, STORAGEINFO);
    }
}