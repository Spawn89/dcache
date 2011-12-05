/*
 * Automatically generated by jrpcgen 1.0.7 on 2/21/09 1:22 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package org.dcache.chimera.nfs.v3.xdr;
import org.dcache.xdr.*;
import java.io.IOException;

public class entry3 implements XdrAble {
    public fileid3 fileid;
    public filename3 name;
    public cookie3 cookie;
    public entry3 nextentry;

    public entry3() {
    }

    public entry3(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        entry3 $this = this;
        do {
            $this.fileid.xdrEncode(xdr);
            $this.name.xdrEncode(xdr);
            $this.cookie.xdrEncode(xdr);
            $this = $this.nextentry;
            xdr.xdrEncodeBoolean($this != null);
        } while ( $this != null );
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        entry3 $this = this;
        entry3 $next;
        do {
            $this.fileid = new fileid3(xdr);
            $this.name = new filename3(xdr);
            $this.cookie = new cookie3(xdr);
            $next = xdr.xdrDecodeBoolean() ? new entry3() : null;
            $this.nextentry = $next;
            $this = $next;
        } while ( $this != null );
    }

}
// End of entry3.java