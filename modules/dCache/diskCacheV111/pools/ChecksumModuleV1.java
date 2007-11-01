// $Id: ChecksumModuleV1.java,v 1.11 2007-08-30 21:11:02 abaranov Exp $

package diskCacheV111.pools ;

import  diskCacheV111.util.* ;
import  diskCacheV111.vehicles .* ;
import  diskCacheV111.repository.* ;

import  dmg.util.* ;
import  dmg.cells.nucleus.* ;

import  java.util.* ;
import  java.io.* ;
import  java.security.MessageDigest ;
import  java.security.NoSuchAlgorithmException ;

public class ChecksumModuleV1  {

   private final CellAdapter     _cell       ;
   private final CacheRepository _repository ;
   
   private boolean _frequently = false ;
   private boolean _onRead     = false ;
   private boolean _onWrite    = false ;
   private boolean _onTransfer = false ;
   private boolean _onRestore  = false ;
   private boolean _enforceCRC = false ;
   private boolean _updatepnfs = false ;
   
   private long    _delayFile    =  1000L ;
   private long    _scanEvery    =  24L ;

   private ChecksumFactory _defaultChecksumFactory = null;
   
   private final FullScan   _fullScan   = new FullScan() ;
   private final SingleScan _singleScan = new SingleScan();
     
   private final PnfsHandler _pnfs ;
   
   private boolean _fake_checksum_error = false ;
   private boolean _fake_checksum_ftp   = false ;
   
   public ChecksumModuleV1( CellAdapter cell , 
                            CacheRepository repository , 
                            PnfsHandler pnfs){
      _repository = repository ;
      _cell       = cell ;
      _pnfs       = pnfs ;
      try {
        _defaultChecksumFactory = ChecksumFactory.getFactory("adler32");
      } catch ( Throwable ex){
        _cell.esay("Exception building checksum factory for adler32");
        assert(false);
      }
   }
   public void setMoverChecksums( CacheRepositoryEntry entry ,
                                  ChecksumFactory clientChecksumFactory , // can be null
				  Checksum clientChecksum, // can be null
                                  Checksum transferChecksum )
          throws CacheException, 
                 InterruptedException,
                 IOException  {

       if( _fake_checksum_error )
          throw new
          CacheException("Checksum error") ;

       if( _fake_checksum_ftp )clientChecksum = null ;

       _cell.say(entry.getPnfsId()+" client = "+clientChecksum+" transfer "+transferChecksum);
       
        
       Checksum pnfsChecksum = null ;
       //
       // take checksum from client (close mover protocol, or from pnfs)
       //
       ChecksumFactory defaultChecksumFactory = null;

       if ( clientChecksumFactory != null ){
	   defaultChecksumFactory = clientChecksumFactory;
       } else {
	   defaultChecksumFactory = _defaultChecksumFactory;	   
       }

       if ( clientChecksum == null ){
	   pnfsChecksum = defaultChecksumFactory.createFromPersistentState(_cell, entry.getPnfsId());
	   clientChecksum = pnfsChecksum;
       }

       //
       // only if client and transfer is available, do the check.
       //                 
       if( ( clientChecksum   != null ) &&
           ( transferChecksum != null ) &&
           ( ! clientChecksum.equals( transferChecksum ) ) )
          throw new
          CacheException("Checksum error client="+clientChecksum+";transfer="+transferChecksum) ;

       _cell.say(entry.getPnfsId()+" client = "+clientChecksum+" transfer "+transferChecksum);
       
       Checksum  checksum     = clientChecksum == null ? transferChecksum : clientChecksum ;

       Checksum  fileChecksum = defaultChecksumFactory.create();
 
       fileChecksum = _onWrite ? 
	   calculateFileChecksum( entry , defaultChecksumFactory.create() ) : 
                                null;

       _cell.say(entry.getPnfsId()+" filechecksum = "+fileChecksum ) ;

       if( ( checksum     != null ) &&
           ( fileChecksum != null ) &&
           ( ! checksum.equals( fileChecksum ) ) )
          throw new
          CacheException("Checksum error client="+checksum+";file="+fileChecksum) ;
       
       checksum = checksum == null ? fileChecksum : checksum ;
                  
       if( checksum == null ){
            if( ! _enforceCRC )return ;
            checksum = calculateFileChecksum( entry ,  defaultChecksumFactory.create() ) ;
       }
       if( ( checksum != null ) && ( pnfsChecksum == null ) ){
           _cell.say(entry.getPnfsId()+" sending checksum = "+checksum ) ;
           storeChecksumInPnfs(entry.getPnfsId() , checksum) ;
       } 
           
   }
   public Checksum calculateFileChecksum( 
                CacheRepositoryEntry entry , 
                Checksum checksum          )throws IOException , CacheException {
   
       _cell.say("Calculating checksum on "+entry.getDataFile());
       MessageDigest digest = checksum.getMessageDigest() ;
       
       FileInputStream in = new FileInputStream( entry.getDataFile() ) ;
       byte [] buffer = new byte[64*1024] ;
       long sum = 0L ;
       try{ 
          while(true){

              int rc = in.read( buffer , 0 , buffer.length ) ;
              if( rc <=0 )break ;
              sum += rc ;
              digest.update( buffer , 0 , rc ) ;
              
          }
       }finally{
      
          try{ in.close() ; }catch(Exception ee ){}
          
       }
       _cell.say("Calculating checksum on "+entry.getDataFile() + " done, length "+sum + " chsm "+checksum);
       checksum.getDigest();
       return checksum ;
      
   }
   public void storeChecksumInPnfs( PnfsId pnfsId , Checksum checksum ){
      storeChecksumInPnfs( pnfsId , checksum , true ) ;
   }
   public void storeChecksumInPnfs( PnfsId pnfsId , Checksum checksum , boolean overwrite ){
      try{
	  ChecksumPersistence.getPersistenceMgr().store(_cell, pnfsId , checksum,overwrite ) ;
       }catch(Exception eee ){
         _cell.esay("Failed to send crc to PnfsManager : "+eee ) ;
       }
   }
   private Checksum getChecksumFromPnfs( PnfsId pnfsId ){

     try{
          return _defaultChecksumFactory.createFromPersistentState(_cell,pnfsId);
     }catch(Exception ee ){
         ee.printStackTrace();
     }
     return null ;
   }

   public ChecksumFactory getDefaultChecksumFactory(){
       return _defaultChecksumFactory;
   }

   public Checksum getDefaultChecksum(){
       return _defaultChecksumFactory.create();
   }

   public Checksum checksumOf(String type) throws java.security.NoSuchAlgorithmException {
       return ChecksumFactory.getFactory(type).create();
   }

   public void dumpSetup( PrintWriter pw ){
      pw.println("csm set checksumtype "+_defaultChecksumFactory.getType());
      if( _frequently ){
         pw.print("csm set policy -frequently=on") ;
         pw.print(" -filedelay="+_delayFile) ;
         pw.println(" -every="+_scanEvery);
      }else{
         pw.println("csm set policy -frequently=off");
      }
      pw.print("csm set policy");
      pw.print(" -onread=") ; pw.print(_onRead?"on":"off");
      pw.print(" -onwrite=") ; pw.print(_onWrite?"on":"off");
      pw.print(" -onrestore=") ; pw.print(_onRestore?"on":"off");
      pw.print(" -ontransfer=") ; pw.print(_onTransfer?"on":"off");
      pw.print(" -enforcecrc=") ; pw.print(_enforceCRC?"on":"off");
      pw.print(" -getcrcfromhsm=") ; pw.print(_updatepnfs?"on":"off");
      pw.println("");
   }
   public boolean checkOnRead(){ return _onRead ; }
   public boolean checkOnRestore(){ return _onRestore ; }
   public boolean checkOnWrite(){ return _onWrite ; }
   public boolean checkOnTransfer(){ return _onTransfer ; }
   public boolean enforceCRC(){ return _enforceCRC ; }
   public void getInfo( PrintWriter pw ){
       pw.println( "ChecksumModuleV1 : $Id: ChecksumModuleV1.java,v 1.11 2007-08-30 21:11:02 abaranov Exp $" ) ;
       pw.println( "          Checksum type : "+_defaultChecksumFactory.getType() ) ;
       pw.println( "                   Fake : ftp="+_fake_checksum_ftp+" error="+_fake_checksum_error);
         pw.print( " Checkum calculation on : " ) ;
       if( _onRead )pw.print("read ") ;
       if( _onWrite)pw.print("write ");
       if( _onRestore)pw.print("restore ") ;
       if( _onTransfer)pw.print("transfer ");
       if( _enforceCRC)pw.print("enforceCRC ");
       if( _updatepnfs)pw.print("getcrcfromhsm ");
       if( _frequently ){
          pw.print("frequently(") ;
          pw.print(_delayFile) ;
          pw.print(",");
          pw.print(_scanEvery);
          pw.print(")");
       }
       pw.println("");
      
       pw.println("  "+_fullScan.toString());
   }
   public String hh_csm_info = "" ;   
   public String ac_csm_info_$_0( Args args ){
      StringBuffer sb = new StringBuffer() ;
      sb = getPolicies(sb);
      sb.append(_fullScan.toString()).append("\n");
      sb.append(_singleScan.toString()).append("\n");
      return sb.toString() ;
   }
   private StringBuffer getPolicies( StringBuffer sb ){
       if( sb == null )sb = new StringBuffer() ;
       
       sb.append( " Policies :\n").
          append( "        on read : ").append(_onRead).append("\n").
          append( "       on write : ").append(_onWrite).append("\n").
          append( "     on restore : ").append(_onRestore).append("\n").
          append( "    on transfer : ").append(_onTransfer).append("\n").
          append( "    enforce crc : ").append(_enforceCRC).append("\n").
          append( "     getcrcfromhsm : ").append(_updatepnfs).append("\n").
          append( "     frequently : ").append(_frequently).append("\n") ;
       if( _frequently ){
          sb.append("         file delay = ").append(_delayFile).append(" millis\n").
             append("             every  = ").append(_scanEvery).append(" hours\n");     
       }
       return sb ;
   }
   public String hh_csm_set_policy = "[-<option>=on|off] ... # see 'help csm set policy" ;
   public String fh_csm_set_policy = 
       "  Syntax : csm set policy [-<option>=on[|off]] ...\n"+
       "\n"+
       "    OPTIONS :\n"+
       "       -frequently      :  run a permanent scan (suboptions :)\n"+
       "\n"+
       "            -filedelay=<millisec's> :  time delay between file scan\n"+
       "            -every=<hours>          :  run full scan every 'n' hours\n"+
       "\n"+
       "       -onread          : run check before each open for reading\n"+
       "       -onwrite         : run check after receiving the file from client (on fs)\n"+
       "       -onrestore       : run check after restore from HSM\n"+
       "       -ontransfer      : run check while receiving data from client (on the fly)\n"+
       "       -enforcecrc      : make sure there is at least one crc stored in pnfs\n"+
       "       -getcrcfromhsm   : read the <pool>/data/<pnfsid>.crcval file and send to pnfs\n"+
       "\n" ;
   public String ac_csm_set_policy_$_0( Args args ){
   
      _frequently = checkBoolean( args , "frequently"  , _frequently ) ;
      _onRead     = checkBoolean( args , "onread"      , _onRead ) ;
      _onWrite    = checkBoolean( args , "onwrite"     , _onWrite ) ;
      _onRestore  = checkBoolean( args , "onrestore"   , _onRestore ) ;
      _onTransfer = checkBoolean( args , "ontransfer"  , _onTransfer ) ;
      _enforceCRC = checkBoolean( args , "enforcecrc"  , _enforceCRC ) ;
      _updatepnfs = checkBoolean( args , "getcrcfromhsm"  , _updatepnfs ) ;
      
      String value = args.getOpt("filedelay") ;
      if( value != null )_delayFile = Long.parseLong(value);
         

      value = args.getOpt("every") ;
      if( value != null )_scanEvery = Integer.parseInt(value) ;
      
      return args.getOpt("v") == null ? "" : getPolicies(null).toString() ;
   }
   public boolean getCrcFromHsm(){ return _updatepnfs ; }
   private Checksum checkFile( File file ) throws Exception {
   
       Checksum      checksum = getDefaultChecksum() ;
       MessageDigest digest   = checksum.getMessageDigest() ;

       FileInputStream in = new FileInputStream( file ) ;
       
       byte [] buffer = new byte[64*1024] ;
       long sum = 0L ;
       
       try{
          while(true){

              int rc = in.read( buffer , 0 , buffer.length ) ;
              if( rc <=0 )break ;
              sum += rc ;
              digest.update( buffer , 0, rc ) ;
          }
       }finally{
          try{  in.close() ; }catch(Exception ee ){};
       }
       return checksum ;
   
   }
   private CRC getCrc( CacheRepositoryEntry entry , boolean repair ) throws CacheException {
   
       StorageInfo info = entry.getStorageInfo() ;
       if( info == null )info = _pnfs.getStorageInfo( entry.getPnfsId().toString() ) ;
       
       return null;
   }
   private class FullScan extends Singleton  {
   
      private HashSet  _badSet     = new HashSet() ;
      private int      _totalCount = 0 ;
      private int      _badCount   = 0 ;
      
      public FullScan(){ super( "FullScan" ) ; }

      public void runIt() throws Exception {
      
      }
      public String toString(){
         return super.toString()+" " ;
      }
   }
   private class CRC {
   
      private String _crcString = null ;
      private int    _crcType   = 0 ;
      private long   _crcValue  = 0L ;
      
      private CRC( String string ) throws Exception {
         _crcString = string ;
         StringTokenizer st = new StringTokenizer(string,":") ;
         _crcType = Integer.parseInt(st.nextToken()) ;
         _crcValue = Long.parseLong( st.nextToken() , 16 ) ;
      }
      private int getType(){ return _crcType ; }
      private long getValue(){ return _crcValue ; }
   }
   private class SingleScan extends Singleton {
       
      private PnfsId               _pnfsId        = null ;
      private CacheRepositoryEntry _expectedEntry = null ;
      private CacheRepositoryEntry _entry         = null ;
      private Checksum             _fileCRC       = null ;
      private Checksum             _infoCRC       = null ;
      private Exception            _error         = null ;
      public SingleScan(){ super("SingeScan" ) ; }
      
      public synchronized void go( PnfsId pnfsId ) throws Exception {
         _expectedEntry = _repository.getEntry( pnfsId ) ;
         _error = null ;
         super.go() ;
      }
      public void runIt() throws Exception {
         _entry  = _expectedEntry ;
         _pnfsId = _entry.getPnfsId() ;
         _fileCRC = null ;
         _infoCRC = null ;
         try{
            try{
                File file = _entry.getDataFile() ;
                _fileCRC = checkFile( file ) ;
            }catch(Exception ee ){
                throw ee ;
            }
            try{
                StorageInfo info = _entry.getStorageInfo() ;
                String flags     = info == null ? null : info.getKey("flag-c") ;
                _infoCRC = flags == null ?
                           null :
                           new Checksum( flags );
            }catch(Exception ee ){
                throw ee ;
            }
         }catch(Exception eee ){
            eee.printStackTrace();
            _error = eee ;
         }
      }
      public String toString(){
         StringBuffer sb = new StringBuffer() ;
         sb.append(super.toString());
         if( _pnfsId != null ){
             sb.append("  ").append(_pnfsId).append(" ");
             
             if( _error != null ){
                sb.append(_error.toString()) ;
             }else if( ( _fileCRC != null ) && ( _infoCRC == null ) ){
                sb.append("No StorageInfo , crc = ").append(_fileCRC) ;
             }else if( ( _fileCRC == null ) || ( _infoCRC == null ) ){
                sb.append("BUSY");
             }else if( _fileCRC.equals(_infoCRC) ){
                sb.append("OK ").append(_fileCRC.toString());
             }else{
                sb.append( "BAD File = ").
                   append(_fileCRC.toString()).append(" Expected = ").
                   append(_infoCRC.toString()) ;
             }
         }
         return sb.toString() ;
      }
   }
   abstract private class Singleton {
   
       private final Object  _lock     = new Object() ;
       private boolean _isActive = false ;
       private final String  _name ;
       
       private Exception _lastException = null ;
       private Thread    _currentThread = null ;
       
       private Singleton( String name ){ _name = name ; }
       
       abstract public void runIt() throws Exception ;
       
       public void kill(){
          synchronized( _lock ){
             if( _currentThread != null )_currentThread.interrupt() ;
          }
       }
       public void go() throws Exception {
          synchronized( _lock ){
             if( _isActive )
                throw new
                IllegalStateException("Still active") ;
              _isActive = true ;
              _lastException = null ;
          
             _currentThread =
             _cell.getNucleus().newThread(
                  new Runnable(){
                     public void run(){
                        try{
                           runIt() ;
                        }catch(Exception ee ){
                           _lastException = ee ;
                        }finally{
                           synchronized( _lock ){
                               _isActive = false ;
                               _currentThread = null ;
                           }           
                        }
                     }
                  }
                  ,
                  _name 
             );
          }
          _currentThread.start() ;
       }
       public String toString(){
          synchronized( _lock ){
             return _name + (_isActive?" Active ":" Idle ") +
                    ( _lastException == null ? "" : _lastException.toString() ) ;
          }
       }
   }
   public String hh_csm_fake_ftp = "on|off" ;
   public String ac_csm_fake_ftp_$_1( Args args ){
      String value = args.argv(0) ;
      if( value.equals("on") )_fake_checksum_ftp = true ;
      else if( value.equals("off") )_fake_checksum_ftp = false ;
      else throw new
           IllegalArgumentException("csm fake ftp on|off")  ;
      return "" ;
   }
   public String hh_csm_fake_checksumerror = "on|off" ;
   public String ac_csm_fake_checksumerror_$_1( Args args ){
      String value = args.argv(0) ;
      if( value.equals("on") )_fake_checksum_error = true ;
      else if( value.equals("off") )_fake_checksum_error = false ;
      else throw new
           IllegalArgumentException("csm fake checksumerror on|off")  ;
      return "" ;
   }
   public String hh_csm_check = " [ * | <pnfsId> ]";
   public String ac_csm_check_$_0_1( Args args )throws Exception {
      if( args.argv(0).equals("*") ){
          throw new
          IllegalArgumentException ("Feature disabled");
         // _fullScan.go() ;
      }else{
          _singleScan.go( new PnfsId(args.argv(0)) ) ;
      }
      return "Started ...; check 'csm info' for results";
   }
   public String hh_csm_set_checksumtype = "adler32|md5" ;
   public String ac_csm_set_checksumtype_$_1( Args args ) throws Exception {
      String newChecksumType = args.argv(0) ;
      //
      // can we do that  ?
      //
      _defaultChecksumFactory  = ChecksumFactory.getFactory(newChecksumType);

      //
      // seems to be ok
      //
      return "New checksumtype : "+ _defaultChecksumFactory.getType();
   }
   private boolean checkBoolean( Args args , String key , boolean currentValue ){
      String value = args.getOpt(key) ;
      if( value == null )return currentValue ;
      if( value.equals("on") || value.equals("") )return true ;
      else if( value.equals("off") )return false ;
       
      throw new 
      IllegalArgumentException("-"+key+"[=on|off]") ;
         
   }
}
