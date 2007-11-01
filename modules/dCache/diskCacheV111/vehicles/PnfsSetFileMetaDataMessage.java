// $Id: PnfsSetFileMetaDataMessage.java,v 1.2 2004-11-05 12:07:19 tigran Exp $
package diskCacheV111.vehicles ;
import  diskCacheV111.util.* ;

import diskCacheV111.util.* ;
public class PnfsSetFileMetaDataMessage extends PnfsMessage {

    private FileMetaData _metaData    = null ; 
    private boolean      _resolve     = true ;
    
    private static final long serialVersionUID = -2893526041428172736L;
    
    public PnfsSetFileMetaDataMessage(){ 
       super() ;
       setReplyRequired(false);
    }
    public PnfsSetFileMetaDataMessage( String pnfsId ){
        super( pnfsId ) ;
	setReplyRequired(false);
    }
    public PnfsSetFileMetaDataMessage( PnfsId pnfsId ){
        super( pnfsId ) ;
	setReplyRequired(false);
    }
    public FileMetaData getMetaData(){ return _metaData ; }
    public void setMetaData( FileMetaData metaData ){ _metaData = metaData ; }
    public String toString(){
       return super.toString()+";"+
             (_metaData==null?"[noMetaData]":_metaData.toString()) ;
    }
    public void setResolve( boolean resolve ){ _resolve = resolve ; }
    public boolean resolve(){ return _resolve ; }
}
