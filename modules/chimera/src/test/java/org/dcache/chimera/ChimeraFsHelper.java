package org.dcache.chimera;

import com.jolbox.bonecp.BoneCPDataSource;

public class ChimeraFsHelper {

    private ChimeraFsHelper() {}

    public static FileSystemProvider getFileSystemProvider(String url, String drv, String user,
            String pass, String dialect)
    {

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setDriverClass(drv);
        ds.setMaxConnectionsPerPartition(2);
        ds.setPartitionCount(1);

        return new JdbcFs(ds, dialect);
    }
}
