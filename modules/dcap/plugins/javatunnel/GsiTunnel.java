/*
 * $Id: GsiTunnel.java,v 1.6 2006-10-11 09:49:58 tigran Exp $
 */

package javatunnel;

import gplazma.authz.AuthorizationException;
import gplazma.authz.util.X509CertUtil;
import java.io.*;
import java.util.Iterator;

//jgss
import javax.security.auth.Subject;
import org.dcache.auth.FQANPrincipal;
import org.ietf.jgss.*;

// globus gsi
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glite.voms.FQAN;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.TrustedCertificates;
import org.globus.gsi.jaas.GlobusPrincipal;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;


class GsiTunnel extends GssTunnel  {

    private final static Logger _log = LoggerFactory.getLogger(GsiTunnel.class);

    private ExtendedGSSContext _e_context = null;

    private static final String service_key           = "/etc/grid-security/hostkey.pem";
    private static final String service_cert          = "/etc/grid-security/hostcert.pem";
    private static final String service_trusted_certs = "/etc/grid-security/certificates";
    private Subject _subject = new Subject();

    // Creates a new instance of GssTunnel
    public GsiTunnel(String dummy) {
        this(dummy, true);
    }


    public GsiTunnel(String dummy, boolean init) {
        if( init ) {
            try {
                GlobusCredential serviceCredential;

                serviceCredential =new GlobusCredential(service_cert, service_key);

                GSSCredential cred = new GlobusGSSCredentialImpl(serviceCredential, GSSCredential.ACCEPT_ONLY);
                TrustedCertificates trusted_certs = TrustedCertificates.load(service_trusted_certs);
                GSSManager manager = ExtendedGSSManager.getInstance();
                _e_context = (ExtendedGSSContext) manager.createContext(cred);
                _e_context.setOption(GSSConstants.GSS_MODE, GSIConstants.MODE_GSI);
                _e_context.setOption(GSSConstants.TRUSTED_CERTIFICATES, trusted_certs);

                _context = _e_context;
                // do not use channel binding with GSIGSS
                super.useChannelBinding(false);

            }catch( Exception e ) {
                _log.error("Failed to initialize GSI context", e);
            }

        }
    }

    @Override
    public boolean verify( InputStream in, OutputStream out, Object addon) {

        try {
        	if( super.verify(in, out, addon) ) {
                    _subject.getPrincipals().add( new GlobusPrincipal(_e_context.getSrcName().toString()) );
        		scanExtendedAttributes(_e_context);
        	}
        } catch( Exception e) {
            _log.error("Failed to verify", e);
        }

        return _context.isEstablished();
    }


    @Override
    public Convertable makeCopy() {
        return new GsiTunnel( null, true  );
    }

    private void scanExtendedAttributes(ExtendedGSSContext gssContext) {

        try {

            Iterator<String> fqans = X509CertUtil.getFQANsFromContext(gssContext).iterator();
            boolean primary = true;
            while (fqans.hasNext()) {
                String fqanValue = fqans.next();
                FQAN fqan = new FQAN(fqanValue);
                String group = fqan.getGroup();
                String role = fqan.getRole();
                String s;
                if(role == null  || role.equals("") ) {
                    s = group;
                }else{
                    s = group + "/Role=" + role;
                }
                _subject.getPrincipals().add( new FQANPrincipal(s, primary));
                primary = false;
            }

        } catch (AuthorizationException e) {
            _log.error("Failed to get users group and role context", e);
        }

    }

    @Override
    public Subject getSubject() {
        return _subject;
    }
}