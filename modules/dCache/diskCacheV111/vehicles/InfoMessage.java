// $Id: InfoMessage.java,v 1.12 2007-07-31 12:22:34 tigran Exp $

package diskCacheV111.vehicles ;

import  java.util.Date ;
import  java.text.SimpleDateFormat ;
import  diskCacheV111.util.Transaction ;

public class InfoMessage implements java.io.Serializable {
   private static final SimpleDateFormat __dateFormat = new SimpleDateFormat("MM.dd HH:mm:ss");
   private final String    _cellType    ;
   private String    _messageType = null ;
   private final String    _cellName    ;
   private long      _timeQueued  = 0 ;
   private int       _resultCode  = 0 ;
   private String    _message     = "" ;
   private   long    _timestamp   = System.currentTimeMillis() ;
   private String    _transaction  = null;
   private long      _transactionID = Transaction.newID();

   private static final long serialVersionUID = -8035876156296337291L;

   public InfoMessage( String messageType ,
                       String cellType ,
                       String cellName  ){
     _cellName    = cellName ;
     _cellType    = cellType ;
     _messageType = messageType ;
   }
   public String getInfoHeader(){
      return __dateFormat.format(new Date(_timestamp))+" ["+_cellType+":"+_cellName+":"+_messageType+"]" ;
   }
   public String getResult(){
      return "{"+_resultCode+":\""+_message+"\"}" ;
   }
   public String toString(){
      return getInfoHeader()+" "+getResult() ;
   }
   public void setResult( int resultCode , String resultMessage ){
     _message    = resultMessage ;
     _resultCode = resultCode ;
   }
   public void   setTimeQueued( long timeQueued ){ _timeQueued = timeQueued ; }
   public long   getTimeQueued(){ return _timeQueued ; }
   public String getCellType(){ return _cellType ; }
   public void   setMessageType( String messageType ){
      _messageType = messageType ;
   }
   public String getMessageType(){ return _messageType ; }
   public String getCellName(){ return _cellName ; }
   public String getMessage(){ return _message ; }
   public int    getResultCode(){ return _resultCode ; }
   public long   getTimestamp(){ return _timestamp ; }
   public synchronized void   setTransaction( String transaction ) {
       _transaction = transaction;
   }
   public synchronized String getTransaction() {

       if(_transaction == null) {
           StringBuffer sb = new StringBuffer();
           sb.append(this.getCellType()).append(":").append(this.getCellName()).append(":");
           sb.append(this.getTimestamp()).append("-").append(_transactionID);
           _transaction = sb.toString();
       }
       return _transaction ;
   }
}