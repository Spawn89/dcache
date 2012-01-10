package dmg.util.edb ;

import java.io.* ;

public class JdbmBucket implements JdbmSerializable {
    private final static int KEY_START_SIZE = 4 ;
    int _size  = 0 ;
    int _count = 0 ;
    int _bits  = 0 ;
    JdbmBucketElement [] _list = null ;
    public JdbmBucket(){}
    public JdbmBucket( int size ){
       _size  = size ;
       _count = 0 ;
       _bits  = 0 ;
       _list  = new JdbmBucketElement[_size] ;
       for( int i = 0 ; i < _list.length ; i++ )
             _list[i] = new JdbmBucketElement() ;
    }
    public void writeObject( ObjectOutput out )
           throws java.io.IOException {
       out.writeInt(_size);
       out.writeInt(_count) ;
       out.writeInt(_bits) ;
       for( int i = 0 ; i < _size ; i++ )
          out.writeObject( _list[i] ) ;
       return ;   
    }
    public void readObject( ObjectInput in )
           throws java.io.IOException, ClassNotFoundException {
       _size  = in.readInt() ;
       _count = in.readInt() ;
       _bits  = in.readInt() ;
       _list  = new JdbmBucketElement[_size] ;
       for( int i = 0 ; i < _size ; i++ )
           _list[i] = (JdbmBucketElement)in.readObject() ;
       return ;
    }
    public int getPersistentSize() { 
       return 3 * 4 + _size * ( new JdbmBucketElement() ).getPersistentSize() ; 
    }

}