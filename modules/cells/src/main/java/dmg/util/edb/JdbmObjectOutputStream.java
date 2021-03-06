package dmg.util.edb ;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;

public class      JdbmObjectOutputStream
       extends    DataOutputStream
       implements ObjectOutput{
   public JdbmObjectOutputStream( DataOutputStream out ){
      super( out ) ;
   }
   @Override
   public void writeObject( Object obj ) throws IOException {

      if( obj instanceof JdbmBasic ){
          writeShort( JdbmSerializable.BASIC ) ;
          ((JdbmSerializable)obj).writeObject( this ) ;
      }else if( obj instanceof long [] ){
          for (long l : (long[])obj) {
              writeLong(l);
          }
      }else if( obj instanceof JdbmFileHeader ){
          writeShort( JdbmSerializable.FILE_HEADER ) ;
          ((JdbmSerializable)obj).writeObject( this ) ;
      }else{
         throw new
         IllegalArgumentException("PANIC : Unknown object" ) ;
      }
   }
}
