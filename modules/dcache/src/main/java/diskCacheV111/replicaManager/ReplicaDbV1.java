// $Id$

package diskCacheV111.replicaManager;

import diskCacheV111.util.*;
import diskCacheV111.repository.CacheRepositoryEntryInfo;
import dmg.cells.nucleus.*;
import org.dcache.util.JdbcConnectionPool;

import java.util.*;
import java.sql.*;
import java.text.MessageFormat;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.dcache.commons.util.SqlHelper.*;

//import uk.org.primrose.GeneralException;
//import uk.org.primrose.vendor.standalone.*;
//import uk.org.primrose.pool.core.PoolLoader;


/**
 * This class works with replicas database
 */
public class ReplicaDbV1 implements ReplicaDb1 {
    private final static String _cvsId     = "$Id$";

    private final static Logger _log =
        LoggerFactory.getLogger(ReplicaDbV1.class);

    private Connection          _conn      = null;
    private Statement           _stmt      = null;
    private CellAdapter         _cell      = null;
    private static DataSource   DATASOURCE = null;
    private final static String ERRCODE_UNIQUE_VIOLATION = "23505";

    /**
     * Class constructor opens connection to the database and creates a
     * statement for queries
     * @throws SQLException
     */
    public ReplicaDbV1(CellAdapter cell, boolean keep) throws SQLException {

        _cell = cell;

        if (keep) {
            try {
                _conn = DATASOURCE.getConnection();
                _stmt = _conn.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
                _log.warn("Can not create DB connection");
                throw(e);
            }
        }
    }

    /**
     * Class constructor
     * @throws SQLException
     */
    public ReplicaDbV1(CellAdapter cell) throws SQLException {
        this(cell, false);
    }

    /*
     * Report SQL exception ex for the sql statement sql in method m.
     */
    private void reportSQLException(String m, SQLException ex, String sql) {
        int iErr = ex.getErrorCode();
        String sState = ex.getSQLState();

        _log.warn("SQL exception in method " + m + ": '" + ex + "', errCode=" + iErr + " SQLState=" + sState + " SQLStatement=[" + sql
                + "]");
    }

    private void ignoredSQLException(String m, SQLException ex, String sql) {
        int iErr = ex.getErrorCode();
        String sState = ex.getSQLState();
        String exMsg = ex.getMessage().substring(5);

        _log.info("Ignore SQL exception in method " + m + ": '" + exMsg + "', errCode=" + iErr + " SQLState=" + sState
                + " SQLStatement=[" + sql + "]");
    }

    /**
     * Add record (poolname, pnfsid) to the table 'replicas'
     */
    public synchronized void addPool(PnfsId pnfsId, String poolName) {
//1     final String sql = "INSERT INTO replicas VALUES ('" + poolName + "','" + pnfsId.toString() + "',now())";
        Connection conn = null;
//1     Statement  stmt = null;
        PreparedStatement pstmt = null;
        final String sql1 = "INSERT INTO replicas SELECT ?, ?, now(), pools.poolid, ?, ?, ? FROM pools WHERE pools.pool=?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
//1         stmt = (_conn == null) ? conn.createStatement() : _stmt;
//1         stmt.executeUpdate(sql);
            pstmt = conn.prepareStatement(sql1);
            pstmt.setString (1, poolName);
            pstmt.setString (2, pnfsId.toString());
            pstmt.setInt    (3, 1);
            pstmt.setBoolean(4, true);
            pstmt.setBoolean(5, false);
            pstmt.setString (6, poolName);
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            String exState = ex.getSQLState();
            if (exState.equals(ERRCODE_UNIQUE_VIOLATION) ) { // "ERROR: duplicate key value violates unique constraint" - or similar
                String s = ex.getMessage().substring(5);
                _log.info("WARNING" + s + "; caused by duplicate message, ignore for now. pnfsid=" + pnfsId.toString() + " pool="
                        + poolName);
//1             ignoredSQLException("addPool()", (SQLException) ex, sql);
                ignoredSQLException("addPool()", (SQLException) ex, sql1);
            } else {
                ex.printStackTrace();
                _log.warn("Database access error");
            }
        } finally {
            tryToClose(pstmt);
            tryToClose(conn);
        }
    }

    /**
     * Add records (poolname, pnfsid) to the table 'replicas'
     */
    public synchronized void addPnfsToPool(List<CacheRepositoryEntryInfo> fileList, String poolName) {
        Connection conn = null;
        Statement  stmt = null;
        PreparedStatement pstmt = null;
//      final String sql = "INSERT INTO replicas VALUES ('" + poolName + "',?,now())";
        final String sql = MessageFormat.format(
                "INSERT INTO replicas SELECT ''{0}'', ?, now(), pools.poolid, ?, ?, ? FROM pools WHERE pools.pool=''{0}''",
                poolName);
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            pstmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            _log.warn("addPnfsToPool: prepareStatement error");
        }
        try {
//            stmt.execute("BEGIN ISOLATION LEVEL SERIALIZABLE");
//            stmt.execute("BEGIN");
            //
            for (CacheRepositoryEntryInfo info:  fileList) {        // Now put
                                                                    // all
                                                                    // pnfsids
                                                                    // into
                                                                    // replicas
                                                                    // table
                String pnfsId = info.getPnfsId().toString();
                int bitmask = info.getBitMask();
                boolean countable =
                        info.isPrecious() &&
//                        info.isCached() &&
                        !info.isReceivingFromClient() &&
                        !info.isReceivingFromStore() &&
//                        info.isSendingToStore() &&
                        !info.isBad() &&
                        !info.isRemoved() &&
                        !info.isDestroyed();
//                        info.isSticky();

                try {
                    pstmt.setString(1, pnfsId);
                    pstmt.setInt    (2, bitmask);
                    pstmt.setBoolean(3, countable);
                    pstmt.setBoolean(4, false);
                    pstmt.executeUpdate();
                } catch (SQLException ex) {
                    String exState = ex.getSQLState();
                    if (exState.equals(ERRCODE_UNIQUE_VIOLATION) ) { // "ERROR: duplicate key value violates unique constraint" - or similar
                        String s = ex.getMessage().substring(5);
                        _log.info("WARNING" + s + "; caused by duplicate message, ignore for now. pnfsid=" + pnfsId + " pool=" + poolName);
                        ignoredSQLException("addPool()", (SQLException) ex, sql);

                    } else {
                        ex.printStackTrace();
                        _log.warn("Database access error");
                    }
                }
            }
            //
//            stmt.execute("COMMIT");
        } finally {
            tryToClose(pstmt);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Remove record (poolname, pnfsid) from the table 'replicas'
     */
    public void removePool(PnfsId pnfsId, String poolName) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        String sql = "DELETE FROM ONLY replicas WHERE pool = ? and pnfsId = ?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, poolName);
            stmt.setString(2, pnfsId.toString());
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            _log.warn("WARNING: Database access error, can not delete pnfsId='" + pnfsId.toString() + "' " + "at pool = '" + poolName
                    + "' from replicas DB table");
            reportSQLException("removePool()", ex, sql);
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }

    }

    /**
     * Get the number of pools for given pnfsid depreciated - will not work with
     * newer postgres release
     */
    public int countPools(PnfsId pnfsId) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        ResultSet  rset = null;
        String sql = "SELECT pool FROM ONLY replicas WHERE pnfsId = ?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql);
            stmt.setString(1, pnfsId.toString());
            rset = stmt.executeQuery();
            // getFetchSize() is depreciated and will not work with newer
            // postgres release
            return rset.getFetchSize();
        } catch (SQLException ex) {
            ex.printStackTrace();
            _log.warn("Database access error");
            reportSQLException("countPools()", ex, sql);
            return -1;
        } finally {
            tryToClose(rset);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Remove all the records with given pnfsid from the table
     */
    public void clearPools(PnfsId pnfsId) {
        Connection conn = null;
        Statement  stmt = null;
        PreparedStatement statement = null;
        String sqlDeleteFromReplicas = "DELETE FROM replicas WHERE pnfsId = ?";
        String sqlDeleteFromExcluded = "DELETE FROM excluded WHERE pnfsId = ?";
        String sqlDeleteFromFiles = "DELETE FROM files    WHERE pnfsId = ?";
        // If the file has been removed from PNFS, we also have to clean up "files" and "excluded" tables
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.execute("BEGIN ISOLATION LEVEL SERIALIZABLE");
            statement = conn.prepareStatement(sqlDeleteFromReplicas);
            statement.setString(1, pnfsId.toString());
            statement.executeUpdate(sqlDeleteFromReplicas);
            statement = conn.prepareStatement(sqlDeleteFromExcluded);
            statement.setString(1, pnfsId.toString());
            statement.executeUpdate(sqlDeleteFromExcluded);
            statement = conn.prepareStatement(sqlDeleteFromFiles);
            statement.setString(1, pnfsId.toString());
            statement.executeUpdate(sqlDeleteFromFiles);
            stmt.execute("COMMIT");
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Database access error");
        } finally {
            tryToClose(stmt);
            tryToClose(statement);
            tryToClose(conn);
        }
    }

    /**
     * Abstract class for DB access
     */
    protected abstract class DbIterator implements Iterator {

        protected Connection conn = null;
        protected Statement  stmt = null;
        protected ResultSet  rset = null;

        public DbIterator() throws SQLException {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
        }

        public boolean hasNext() {
            try {
                return rset.next();
            } catch (Exception ex) {
                ex.printStackTrace();
                _log.warn("Can't step to the next element of the result set");
            }
            return false;
        }

        public Object next() {
            try {
                return rset.getObject(1);
            } catch (Exception ex) {
                ex.printStackTrace();
                _log.warn("Can't get the next element of the result set");
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("No remove");
        }

        public void close() {
            tryToClose(rset);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Private class for PNFSIDs access
     */
    private class PnfsIdIterator extends DbIterator {

        private PnfsIdIterator() throws SQLException {
            final String sql = "SELECT pnfsId FROM ONLY replicas GROUP BY pnfsid";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sql);
        }

        private PnfsIdIterator(String poolName) throws SQLException {
            String sql = "SELECT pnfsId FROM ONLY replicas WHERE  pool = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, poolName);
            stmt = statement;
            rset = statement.executeQuery(sql);
        }

        private PnfsIdIterator(long timestamp) throws SQLException {
            String sql = "SELECT pnfsId FROM actions WHERE timestamp < ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setLong(1, timestamp);
            stmt = statement;
            rset = statement.executeQuery();
        }
    }

    /*
     * Returns all PNFSIDs from the DB
     *
     * @deprecated
     */
    public Iterator pnfsIds() {
        try {
            return new PnfsIdIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Returns all PNFSIDs from the DB
     */
    public Iterator getPnfsIds() {
        try {
            return new PnfsIdIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /*
     * Returns all PNFSIDs for the given pool from the DB.
     *
     * @deprecated
     */
    public Iterator pnfsIds(String poolName) {
        try {
            return new PnfsIdIterator(poolName);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Returns all PNFSIDs for the given pool from the DB
     */
    public Iterator getPnfsIds(String poolName) {
        try {
            return new PnfsIdIterator(poolName);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Private class for PNFSIDs access
     */
    private class PoolsIterator extends DbIterator {

        /**
         * Returns all pools from the DB
         *
         * @throws SQLException
         */
        private PoolsIterator() throws SQLException {
            final String sql = "SELECT * FROM pools";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sql);
        }

        /**
         * Returns all pools for given pnfsid from the DB
         *
         * @throws SQLException
         */
        private PoolsIterator(PnfsId pnfsId) throws SQLException {
            String sql = "SELECT pool FROM ONLY replicas WHERE pnfsId = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, pnfsId.toString());
            stmt = statement;
            rset = statement.executeQuery();
        }
    }

    /**
     * Returns all pools from DB
     */
    public Iterator getPools() {
        try {
            return new PoolsIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Returns all pools for a given pnfsid
     */
    public Iterator getPools(PnfsId pnfsId) {
        try {
            return new PoolsIterator(pnfsId);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    private class PoolsWritableIterator extends DbIterator {
        /**
         * Returns Writable pools from the DB
         *
         * @throws SQLException
         */
        private PoolsWritableIterator() throws SQLException {
            final String query = "select * from pools WHERE status='"+ ONLINE+"'";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
        }
    }

    /**
     * Returns all writable pools from DB
     */
    public Iterator getPoolsWritable() {
        try {
            return new PoolsWritableIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    private class PoolsReadableIterator extends DbIterator {
        /**
         * Returns Readable pools from the DB
         *
         * @throws SQLException
         */
        private PoolsReadableIterator() throws SQLException {
            String query = "select * from pools WHERE " + "(  status='" + ONLINE + "' " + "OR status='" + DRAINOFF + "' "
                    + "OR status='" + OFFLINE_PREPARE + "')";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
        }
    }

    /**
     * Returns all Readable pools from DB
     */
    public Iterator getPoolsReadable() {
        try {
            return new PoolsReadableIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Clears the tables
     */
    public void clearAll() {
        Connection conn = null;
        Statement  stmt = null;
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.execute("BEGIN ISOLATION LEVEL SERIALIZABLE");
            stmt.executeUpdate("TRUNCATE TABLE replicas, pools, deficient, redundant, excluded");
            stmt.execute("COMMIT");
        } catch (Exception ex) {
            try { conn.rollback(); } catch (SQLException e1) { }
//          ex.printStackTrace();
            _log.warn("Can't clear the tables");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Clears pools and replicas tables for the pool 'pool' in argument
     *
     * @throws SQLException
     *                 if can not remove pool from DB
     */
    public void clearPool(String poolName) {
        Connection conn = null;
        Statement  stmt = null;
        String sqlDeleteReplicas = "DELETE FROM ONLY replicas WHERE pool=?";
        String sqlDeletePools = "DELETE FROM pools    WHERE pool=?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.execute("BEGIN ISOLATION LEVEL SERIALIZABLE");
            PreparedStatement statement = conn.prepareStatement(sqlDeleteReplicas);
            statement.setString(1, poolName);
            statement.executeUpdate(sqlDeleteReplicas);
            statement = conn.prepareStatement(sqlDeletePools);
            statement.setString(1, poolName);
            statement.executeUpdate(sqlDeletePools);
            stmt.execute("COMMIT");
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException e1) { }
            _log.warn("Can't remove pool '" + poolName + "' from the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Private class to get the PNFSIDs with number of replicas > maximum
     */
    private class getRedundantIterator extends DbIterator {

        private getRedundantIterator(int maxcnt) throws SQLException {
/*
            String sql = "SELECT * FROM (SELECT pnfsid, sum(CASE WHEN pools.status='"
                    + ONLINE + "' THEN 1 ELSE 0 END) "
                    + "FROM      replicas, pools WHERE replicas.pool=pools.pool GROUP BY pnfsid) AS tmp "
                    + "WHERE sum > " + maxcnt
                    + " AND pnfsid NOT IN (SELECT pnfsid FROM actions) ORDER BY sum DESC";
*/
            stmt = conn.createStatement();
            stmt.executeUpdate("BEGIN ISOLATION LEVEL SERIALIZABLE");
            stmt.executeUpdate("TRUNCATE TABLE redundant");
            final String sql;
            sql = "INSERT  INTO redundant"
                    +"  SELECT pnfsid, count(*)"
                    +"  FROM ONLY replicas, pools"
                    +"  WHERE"
                    +"        replicas.poolid=pools.poolid"
                    +"        AND pools.status='" + ONLINE + "'"
                    +"       AND replicas.countable"
                    +"  GROUP BY pnfsid"
                    +"  HAVING count(*) > " + maxcnt;
            stmt.executeUpdate(sql);
            stmt.executeUpdate("DELETE FROM redundant WHERE pnfsid IN (SELECT pnfsid FROM actions)");
            stmt.executeUpdate("DELETE FROM redundant WHERE pnfsid IN (SELECT pnfsid FROM excluded)");
            stmt.executeUpdate("COMMIT");
            //
            rset = stmt.executeQuery("SELECT * FROM redundant ORDER BY count DESC");
        }

        public Object next() {
            try {
                return new Object[] { rset.getObject(1), rset.getObject(2) };
            } catch (Exception ex) {
                ex.printStackTrace();
                _log.warn("Can't get the next element of the result set");
            }
            return null;
        }
    }

    /**
     * Returns all pnfsids with counters > 4
     */
    public Iterator getRedundant(int maxcnt) {
        try {
            return new getRedundantIterator(maxcnt);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Private class to get the PNFSIDs with number of replicas < minimum
     */
    private class getDeficientIterator extends DbIterator {

        private getDeficientIterator(int mincnt) throws SQLException {
/*
            final String sql;
            sql = "SELECT * FROM (SELECT pnfsid,"
                    + "sum(CASE WHEN pools.status='" + ONLINE
                    + "' OR pools.status='" + OFFLINE
                    + "' OR pools.status='" + OFFLINE_PREPARE
                    + "' THEN 1 ELSE 0 END) "
                    + "FROM      replicas, pools WHERE replicas.pool=pools.pool GROUP BY pnfsid) AS tmp "
                    + "WHERE sum > 0 and sum < " + mincnt
                    + " AND pnfsid NOT IN (SELECT pnfsid FROM actions) ORDER BY sum ASC";
*/
            stmt = conn.createStatement();
            stmt.executeUpdate("BEGIN ISOLATION LEVEL SERIALIZABLE");
            stmt.executeUpdate("TRUNCATE TABLE deficient");
            final String sql;
            sql = "INSERT INTO deficient"+
                    "  SELECT pnfsid, count(*)"+
                    "  FROM replicas, pools"+
                    "  WHERE"+
            //      "--      replicas.pool=pools.pool"+
                    "        replicas.poolid=pools.poolid"+
                    "        AND pools.status IN ('"+ONLINE+"','"+OFFLINE+"','"+OFFLINE_PREPARE+"')"+
            //      "--      AND pools.countable"+
                    "       AND replicas.countable"+
                    "  GROUP BY pnfsid"+
                    "  HAVING count(*) < " + mincnt;
            stmt.executeUpdate(sql);
            stmt.executeUpdate("DELETE FROM deficient WHERE pnfsid IN (SELECT pnfsid FROM actions)");
            stmt.executeUpdate("DELETE FROM deficient WHERE pnfsid IN (SELECT pnfsid FROM excluded)");
            stmt.executeUpdate("COMMIT");
            //
            rset = stmt.executeQuery("SELECT * FROM deficient ORDER BY count ASC");
        }

        public Object next() {
            try {
                return new Object[] { rset.getObject(1), rset.getObject(2) };
            } catch (Exception ex) {
                ex.printStackTrace();
                _log.warn("Can't get the next element of the result set");
            }
            return null;
        }
    }

    /**
     * Returns all pnfsids with counters = 1
     */
    public Iterator getDeficient(int mincnt) {
        try {
            return new getDeficientIterator(mincnt);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Private class to get the PNFSIDs with number of replicas = 0
     */
    private class getMissingIterator extends DbIterator {

        private getMissingIterator() throws SQLException {
            String sql = "SELECT pnfsid FROM (SELECT pnfsid, " + "sum(CASE " + "WHEN pools.status='" + ONLINE
                    + "' OR pools.status='" + OFFLINE + "' OR pools.status='" + OFFLINE_PREPARE + "' THEN 1 "
                    + "WHEN pools.status='reduce' THEN -1 ELSE 0 "
                    + "END) FROM replicas, pools WHERE replicas.pool=pools.pool GROUP BY pnfsid) AS tmp WHERE sum=0";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sql);
        }
    }

    /**
     * Returns all pnfsids with counters = 0
     */
    public Iterator getMissing() {
        try {
            return new getMissingIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    public void removePoolStatus(String poolName) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        String sql = "DELETE FROM pools WHERE pool=?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql);
            stmt.setString(1, poolName);
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't remove pool '" + poolName + "' from the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Set the value of pool status.
     *
     * @param poolName
     *                Pool name.
     * @param poolStatus
     *                Value to assign to pool status.
     */
    public void setPoolStatus(String poolName, String poolStatus) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        String sql_i = "insert into pools values (?,?,now())";
        String sql_u = "update pools set status=?, datestamp=now() where pool=?";

        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql_i);
            stmt.setString(1, poolName);
            stmt.setString(2, poolStatus);
            stmt.executeUpdate(sql_i);
        } catch (SQLException ex) {
            try {
                stmt = conn.prepareStatement(sql_u);
                stmt.setString(1, poolStatus);
                stmt.setString(2, poolName);
                stmt.executeUpdate(sql_u);
            } catch (SQLException ex2) {
                ex2.printStackTrace();
                _log.warn("setPoolStatus() ERROR: Can't add/update pool '" + poolName + "'" + " status in 'pools' table in DB");
            }
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Get the value of pool status.
     *
     * @return value of pool status.
     */
    public String getPoolStatus(String poolName) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        ResultSet  rset = null;
        String sql = "SELECT status FROM pools WHERE pool=?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql);
            stmt.setString(1, poolName);
            rset = stmt.executeQuery();
            rset.next();
            return rset.getString(1);
        } catch (SQLException ex) {
            reportSQLException("getPoolStatus()", ex, sql);

            _log.warn("DB: Can't get status for pool '" + poolName + "' from pools table, return 'UNKNOWN'");
            return "UNKNOWN";
        } finally {
            tryToClose(rset);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Add transaction into DB
     */
    public void addTransaction(PnfsId pnfsId, long timestamp, int count) {
        Connection conn = null;
        Statement  stmt = null;
        String op;
        if (count > 0)
            op = "replicate";
        else if (count < 0)
            op = "reduce";
        else
            op = "exclude";

        final String sql = MessageFormat.format("INSERT INTO actions VALUES (''{0}'',''{1}'',''{2}'',''{3}'',now(),{4,number,#})",
                op, "s", pnfsId.toString(), "d", timestamp);

        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't add transaction to the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Add transaction into DB
     * @param pnfsId
     * @param timestamp
     * @param errcode
     * @param errmsg
     */
    public void addExcluded(PnfsId pnfsId, long timestamp, String errcode, String errmsg) {
        Connection conn = null;
        Statement  stmt = null;

        final String sql = MessageFormat.format("INSERT INTO excluded VALUES (''{0}'',''{1}'',now(),{2,number,#},10000,{3,number,#},''{4}'',''{5}'')",
                "s", pnfsId.toString(), timestamp, 0xFFFF, errcode, errmsg);

        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't add transaction to the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Remove transaction from DB
     */
    public void removeTransaction(PnfsId pnfsId) {
        Connection conn = null;
        Statement  stmt = null;
        final String sql = MessageFormat.format("DELETE FROM actions WHERE pnfsId = ''{0}''", pnfsId.toString());
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't remove transaction from the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Release excluded files from "excluded" table with timestamp older than "timesatamp"
     */
    public int releaseExcluded(long timestamp) {
        Connection conn = null;
        Statement  stmt = null;
        final String sql = "DELETE FROM excluded WHERE timestamp < " + timestamp;
        int count = 0;
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            count = stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't delete old records from the 'excluded' table");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }

        return count;
    }


    /**
     * Clear transactions from DB
     */
    public void clearTransactions() {
        Connection conn = null;
        Statement  stmt = null;
        final String sql = "DELETE FROM actions WHERE action IN ('replicate', 'reduce')";
//      final String sql = "TRUNCATE actions";

        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't clear transactions from the DB");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Return the timestamp for a given PNFSID
     */
    public long getTimestamp(PnfsId pnfsId) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        ResultSet  rset = null;
        String sql = "SELECT timestamp FROM actions WHERE pnfsId = ?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql);
            stmt.setString(1, pnfsId.toString());
            rset = stmt.executeQuery();
            rset.next();
            return rset.getLong(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Can't get data from the DB");
            return -1;
        } finally {
            tryToClose(rset);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /*
     * Return the list of PNFSIDs which are older than 'timestamp'
     *
     * @deprecated
     */
    public Iterator pnfsIds(long timestamp) {
        try {
            return new PnfsIdIterator(timestamp);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Return the list of PNFSIDs which are older than 'timestamp'
     */
    public Iterator getPnfsIds(long timestamp) {
        try {
            return new PnfsIdIterator(timestamp);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    public void removePool(String poolName) {
        Connection conn = null;
        Statement  stmt = null;
        PreparedStatement statement = null;
      String sql = "DELETE FROM ONLY replicas WHERE pool=?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt = (_conn == null) ? conn.createStatement() : _stmt;
            stmt.execute("BEGIN ISOLATION LEVEL SERIALIZABLE");
            statement =  conn.prepareStatement(sql);
            statement.setString(1, poolName);
            statement.executeUpdate();
            stmt.execute("COMMIT");
        } catch (Exception ex) {
            try { conn.rollback(); } catch (SQLException e1) { }
            _log.warn("Can't remove pool '" + poolName + "' from the DB");
        } finally {
            tryToClose(statement);
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     * Private class to get the PNFSIDs which are in the drainoff pools only
     */
    private class getDrainingIterator extends DbIterator {

        private getDrainingIterator() throws SQLException {
/*
            String sql = "SELECT rd.pnfsid " +
            "FROM ONLY replicas rd, pools pd " +
            "WHERE rd.pool = pd.pool AND pd.status = '" + DRAINOFF + "' " +
            "GROUP BY rd.pnfsid " +
            "EXCEPT " +
            "SELECT r.pnfsid " +
            "FROM (" +
            "       SELECT rr.pnfsid FROM ONLY replicas rr, pools pp " +
            "       WHERE rr.pool = pp.pool  AND pp.status = '" + DRAINOFF + "' " +
            "       GROUP BY rr.pnfsid" +
            "     ) r, " +
            "     ONLY replicas r1, " + "     pools p1 " + "WHERE r.pnfsid  = r1.pnfsid" +
            " AND  p1.pool   = r1.pool" +
            " AND  ( p1.status = '" + ONLINE + "' " +
            "     OR r.pnfsid IN (SELECT pnfsid FROM actions) ) " +
            "GROUP BY r.pnfsid";
*/
            stmt = conn.createStatement();
            stmt.executeUpdate("BEGIN ISOLATION LEVEL SERIALIZABLE");
            stmt.executeUpdate("TRUNCATE TABLE drainoff");
            String sql;
            sql = "INSERT INTO drainoff"
                    +"        SELECT rd.pnfsid"
                    +"        FROM ONLY replicas rd, pools pd"
                    +"            WHERE rd.poolid = pd.poolid AND pd.status = '"+DRAINOFF+"'"
                    +"        GROUP BY rd.pnfsid";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM drainoff WHERE pnfsid IN"
                    +"        (SELECT pnfsid"
                    +"         FROM ONLY replicas rd, pools pd"
                    +"            WHERE rd.poolid = pd.poolid AND pd.status = '"+ONLINE+"'"
                    +"         GROUP BY pnfsid"
                    +"         UNION ALL"
                    +"         SELECT pnfsid FROM actions"
                    +"         UNION ALL"
                    +"         SELECT pnfsid FROM excluded"
                    +"        )";
            stmt.executeUpdate(sql);
            stmt.executeUpdate("COMMIT");
            //
            rset = stmt.executeQuery("SELECT * FROM drainoff ORDER BY pnfsid");
        }
    }

    /**
     * Get the list of PNFSIDs which are in the drainoff pools only
     */
    public Iterator getInDrainoffOnly() {
        try {
            return new getDrainingIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     * Private class to get the PNFSIDs which are in the offline pools only
     */
    private class getOfflineIterator extends DbIterator {

        private getOfflineIterator() throws SQLException {
            String sql = "SELECT ro.pnfsid " + "FROM ONLY replicas ro, pools po "
                    + "WHERE ro.pool = po.pool AND po.status = '"  + OFFLINE_PREPARE + "' "
                    + "GROUP BY ro.pnfsid "
                    + "EXCEPT "
                    + "SELECT r.pnfsid " + "FROM ("
                    + "       SELECT rr.pnfsid FROM ONLY replicas rr, pools pp "
                    + "       WHERE rr.pool = pp.pool  AND pp.status = '" + OFFLINE_PREPARE + "' "
                    + "       GROUP BY rr.pnfsid"
                    + "     ) r, " + "     ONLY replicas r1, " + "     pools p1 "
                    + "WHERE r.pnfsid = r1.pnfsid"
                    + " AND  p1.pool  = r1.pool" + " AND  ( p1.status = '" + ONLINE + "' "
                    + "     OR r.pnfsid IN (SELECT pnfsid FROM actions) ) " + "GROUP BY r.pnfsid";
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sql);
        }
    }

    /**
     * Get the list of PNFSIDs which are in the offline pools only
     */
    public Iterator getInOfflineOnly() {
        try {
            return new getOfflineIterator();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new HashSet().iterator(); // Empty set
    }

    /**
     *
     */
    public void setHeartBeat(String name, String desc) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        final String sql_i = "insert into heartbeat values (?,?,now())";
        final String sql_u = "update heartbeat set description=?, datestamp=now() where process=?";

        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql_i);
            stmt.setString(1, name);
            stmt.setString(2, desc);
            stmt.executeUpdate(sql_i);
        } catch (Exception ex) {
            try {
                stmt = conn.prepareStatement(sql_u);
                stmt.setString(1, desc);
                stmt.setString(2, name);
                stmt.executeUpdate(sql_u);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                _log.warn("setHeartBeat() ERROR: Can't add/update process '" + name + "' status in 'heartbeat' table in DB");
            }
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    /**
     *
     */
    public void removeHeartBeat(String name) {
        Connection conn = null;
        PreparedStatement  stmt = null;
        final String sql = "DELETE FROM heartbeat WHERE process = ?";
        try {
            conn = (_conn == null) ? DATASOURCE.getConnection() : _conn;
            stmt =  conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.warn("Database access error");
        } finally {
            tryToClose(stmt);
            tryToClose(conn);
        }
    }

    //////////////////////////////// End Of Interface ///////////////////////////////////////

    public static void printClassName(Object obj) {
        System.out.println("The class of " + obj + " is " + obj.getClass().getName());
    }

    /**
     * Setup method to create connection to the database and the datasource
     *
     * @param connectURI
     * @param jdbcClass
     * @param user
     * @param password
     */
    public final static void setup(String connectURI, String jdbcClass, String user, String password) {

        try {
            DATASOURCE = JdbcConnectionPool.getDataSource(connectURI, jdbcClass, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws SQLException
    {
        System.out.println("Test ReplicaDbV1, cvsId=" + _cvsId);

        setup("jdbc:postgresql://localhost:5432/replicas", "org.postgresql.Driver", "enstore", "NoPassword");
        ReplicaDbV1 db = new ReplicaDbV1(null);

        System.out.println("List pnfsId's in all pools");
        for (Iterator i = db.pnfsIds(); i.hasNext();) {
            System.out.println(i.next().toString());
        }

        for (Iterator p = db.getPools(); p.hasNext();) {
            String pool = p.next().toString();
            System.out.println("Pool : " + pool);
            for (Iterator j = db.pnfsIds(pool); j.hasNext();) {
                System.out.println(j.next().toString());
            }
        }
//        for ( Iterator j = db.pnfsIds("pool1") ; j.hasNext() ; ) {
//            System.out.println( j.next().toString());
//        }
//        for ( Iterator j = db.pnfsIds("pool2") ; j.hasNext() ; ) {
//            System.out.println( j.next().toString());
//        }
//        for ( Iterator j = db.pnfsIds("pool3") ; j.hasNext() ; ) {
//            System.out.println( j.next().toString());
//        }
//        for ( Iterator j = db.pnfsIds("pool4") ; j.hasNext() ; ) {
//            System.out.println( j.next().toString());
//        }

        PnfsId pnfsId = new PnfsId("1234");

        System.out.println("WARNING: db.countPools(...) is depreciated and will not work with newer postgres release ");

        db.addPool(pnfsId, "pool1");
        db.addPool(pnfsId, "pool2");
        db.addPool(pnfsId, "pool3");
        System.out.println("pools: " + db.countPools(pnfsId));

        db.removePool(pnfsId, "pool1");
        System.out.println("pools: " + db.countPools(pnfsId));

        db.addPool(pnfsId, "pool1");
        System.out.println("pools: " + db.countPools(pnfsId));

        db.clearPools(pnfsId);
        System.out.println("pools: " + db.countPools(pnfsId));

        // This has to generate an error: Cannot insert a duplicate key into
        // unique index replicas_index
//        for (int i = 0; i < 5; i++) {
//            db.addPool( new PnfsId("2000"+i) , "pool1" ) ;
//        }

//        for (int i = 0; i < 5; i++) {
//            db.addPool( new PnfsId("2000"+i) , "pool3" ) ;
//        }

        db.addPool(pnfsId, "pool2");
        // db.addPool( pnfsId , "pool2" ) ; // This has to generate an error:
        // Cannot insert a duplicate key into unique index replicas_index
        System.out.println("pools: " + db.countPools(pnfsId));
        for (Iterator i = db.getPools(pnfsId); i.hasNext();) {
            System.out.println(" pnfsid : " + pnfsId + ", pool : " + i.next());
        }

        db.removePool(pnfsId, "pool2");
        System.out.println("pools: " + db.countPools(pnfsId));
        for (Iterator i = db.getPools(pnfsId); i.hasNext();) {
            System.out.println(" pnfsid : " + pnfsId + ", pool : " + i.next());
        }

        for (Iterator i = db.getMissing(); i.hasNext();) {
            System.out.println(" Missing pnfsid : " + i.next());
        }

        for (Iterator i = db.getDeficient(2); i.hasNext();) {
            Object[] r = (Object[]) (i.next());
            // System.out.println(" Length : "+r.length);
            System.out.println(" Deficient pnfsid : " + r[0] + ": " + r[1]);
            // printClassName(i.next());
            // printClassName(new int[] {1,2,3});
        }

        for (Iterator i = db.getRedundant(3); i.hasNext();) {
            Object[] r = (Object[]) (i.next());
            System.out.println(" Redundant pnfsid : " + r[0] + ": " + r[1]);
        }

        System.out.println("pool1: Status : '" + db.getPoolStatus("pool1") + "'");
        System.out.println("pool11111111111: Status : '" + db.getPoolStatus("pool11111111111") + "'");

        db.setPoolStatus("pool9", "offline");
        System.out.println("pool9: Status : '" + db.getPoolStatus("pool9") + "'");

        db.clearPool("pool9");

        System.exit(0);
    }
}