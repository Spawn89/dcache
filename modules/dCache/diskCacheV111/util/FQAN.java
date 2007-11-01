/*
 * FQAN.java
 *
 * Created on October 4, 2007, 2:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package diskCacheV111.util;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * @author timur
 */
public class FQAN implements java.io.Serializable {
    
    private static Pattern p1 = Pattern.compile("(.*)/Role=(.*)/Capability=(.*)");
    private static Pattern p2 = Pattern.compile("(.*)/Role=(.*)(.*)");
    private static Pattern p3 = Pattern.compile("(.*)(.*)(.*)");
    private Matcher m = null;
    
    //immutable
    private final String fqan;
    /** Creates a new instance of FQAN */
    public FQAN(String fqan) {
        this.fqan = fqan;
    }
    
    public String toString() {
        return fqan;
    }
    
    private Matcher getMatcher() {
        if (m!=null) {
            return m;
        }
        
        m = p1.matcher(fqan);
        if(!m.matches()) {
            m = p2.matcher(fqan);
            if(!m.matches()) {
                m = p3.matcher(fqan);
                m.matches();
            }
        }
        return m;
    }
    
    public String getGroup() {
        return getMatcher().group(1);
    }
    
    public String getRole() {
        return getMatcher().group(2);
    }
    
    public String getCapability() {
        return getMatcher().group(3);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        FQAN fqan = new FQAN(args[0]);
        System.out.println("FQAN     = "+fqan);
        System.out.println("Group    = "+fqan.getGroup());
        System.out.println("Role     = "+fqan.getRole());
        System.out.println("Capacity = "+fqan.getGroup());
        
    }
    
}
