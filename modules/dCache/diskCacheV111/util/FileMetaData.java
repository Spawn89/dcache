// $Id: FileMetaData.java,v 1.15 2007-07-26 13:42:47 tigran Exp $
package diskCacheV111.util ;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FileMetaData implements Serializable {


   /*
    * each field has value and isSetXXX
    */

   private int     _uid = -1 ;
   private boolean _isUidSet = false;

   private int  _gid = -1 ;
   private boolean _isGidSet = false;

   private long _size = 0L ;
   private boolean _isSizeSet = false;

   private long _created      = 0L;


   private long _lastAccessed = 0L;
   private boolean _isATimeSet = false;

   private long _lastModified = 0L;
   private boolean _isMTimeSet = false;

   private boolean _isRegular     = true ;
   private boolean _isDirectory   = false ;
   private boolean _isLink        = false ;

   private static final SimpleDateFormat __formatter =  new SimpleDateFormat("MM.dd-hh:mm:ss") ;


   private  Permissions _user  = new Permissions() ;
   private  Permissions _group = new Permissions() ;
   private  Permissions _world = new Permissions() ;

   private boolean _isUserPermissionSet = false;
   private boolean _isGroupPermissionSet = false;
   private boolean _isWorldPermissionSet = false;

   private static final long serialVersionUID = -6379734483795645452L;


   /*
    * immutable
    */
   public static class Permissions  implements Serializable {

       private final boolean _canRead;
       private final boolean _canWrite;
       private final boolean _canExecute;

       private static final long serialVersionUID = -1340210599513069884L;

       public Permissions(){ this(0) ; }
       public Permissions( int perm ){
           _canRead    = ( perm & 0x4 ) > 0 ;
           _canWrite   = ( perm & 0x2 ) > 0 ;
           _canExecute = ( perm & 0x1 ) > 0 ;
       }

       public boolean canRead(){ return _canRead ; }
       public boolean canWrite(){ return _canWrite ; }
       public boolean canExecute(){ return _canExecute ; }
       public boolean canLookup(){ return canExecute() ; }
       public String toString(){
         return (_canRead?"r":"-")+
                (_canWrite?"w":"-")+
                (_canExecute?"x":"-") ;
       }
   }

   /**
    * sutable for setFileMetaData methods,
    * where all _isXxxSet fields id false and
    * only one field can be defined and setted
    *
    */
   public FileMetaData(){}

   /**
    *
    * @param uid
    * @param gid
    * @param permissions in unix format
    */
   public FileMetaData( int uid , int gid , int permissions ){
      this( false , uid , gid , permissions ) ;
   }

   public FileMetaData( boolean isDirectory , int uid , int gid , int permission ){
      _uid = uid ;
      _gid = gid ;
      _isDirectory = isDirectory ;
      _user  = new Permissions ( ( permission >> 6 ) & 0x7 ) ;
      _group = new Permissions ( ( permission >> 3 ) & 0x7 ) ;
      _world = new Permissions (  permission & 0x7 ) ;

      _isUidSet = true;
      _isGidSet = true;
      _isUserPermissionSet = true;
      _isGroupPermissionSet = true;
      _isWorldPermissionSet = true;

   }

   public void setFileType( boolean isRegular ,
                            boolean isDirectory ,
                            boolean isLink        ){
      _isRegular   = isRegular ;
      _isDirectory = isDirectory ;
      _isLink      = isLink ;
   }



   public long getFileSize(){ return _size ; }
   public void setSize( long size ){
	   _size = size ;
	   _isSizeSet = true;
   }
   public boolean isSizeSet() {
	   return _isSizeSet;
   }

   public long getCreationTime(){ return _created ; }
   public long getLastModifiedTime(){ return _lastModified ; }
   public long getLastAccessedTime(){ return _lastAccessed ; }
   
   /**
    *    
    * @param accessed in milliseconds
    * @param modified in milliseconds
    * @param created in milliseconds
    */
   public void setTimes( long accessed , long modified , long created ){
	      _created      = created  ;
	      
	      this.setLastAccessedTime(accessed);
	      this.setLastModifiedTime(modified);
   }
   
   /**
    * set files last access time
    * @param newTime in milliseconds
    */
   public void setLastAccessedTime(long newTime) {
	   _lastAccessed = newTime ;
	   _isATimeSet = true;
   }
   public boolean isATimeSet() {
	   return _isATimeSet;
   }
   
   /**
    * set files last modification time
    * @param newTime in milliseconds
    */

   public void setLastModifiedTime(long newTime) {
	   _lastModified = newTime ;
	   _isMTimeSet = true;
   }  
   
   public boolean isMTimeSet() {
	   return _isMTimeSet;
   }


   public boolean isDirectory(){ return _isDirectory ; }
   public boolean isSymbolicLink(){ return _isLink ; }
   public boolean isRegularFile(){ return _isRegular ; }


   public Permissions getUserPermissions(){ return _user ; }
   public void setUserPermissions(Permissions userPermissions) {
	   _user = userPermissions;
	   _isUserPermissionSet = true;
   }
   public boolean isUserPermissionsSet() {
	   return _isUserPermissionSet;
   }

   public Permissions getGroupPermissions(){ return _group ; }
   public void setGroupPermissions(Permissions groupPermissions) {
	   _group = groupPermissions;
	   _isGroupPermissionSet = true;
   }
   public boolean isGroupPermissionsSet() {
	   return _isGroupPermissionSet;
   }

   public Permissions getWorldPermissions(){ return _world ; }
   public void setWorldPermissions(Permissions worldPermissions) {
	   _world = worldPermissions;
	   _isWorldPermissionSet = true;
   }
   public boolean isWorldPermissionsSet() {
	   return _isWorldPermissionSet;
   }

   public int getUid(){ return _uid ; }
   public void setUid(int newUid) {
	   _uid = newUid;
	   _isUidSet = true;
   }
   public boolean isUidSet() {
	   return _isUidSet;
   }

   public int getGid(){ return _gid ; }
   public void setGid(int newGid) {
	   _gid = newGid;
	   _isGidSet = true;
   }
   public boolean isGidSet() {
	   return _isGidSet;
   }

   public String getPermissionString(){
      return (_isDirectory?"d":_isLink?"l":_isRegular?"-":"x")+
             _user+_group+_world ;
   }
   public String toString(){
      return "["+(_isDirectory?"d":_isLink?"l":_isRegular?"-":"x")+
             _user+_group+_world+";"+_uid+";"+_gid+"]"+
             "[c="+__formatter.format(new Date(_created))+
             ";m="+__formatter.format(new Date(_lastModified))+
             ";a="+__formatter.format(new Date(_lastAccessed))+"]" ;
   }
}
