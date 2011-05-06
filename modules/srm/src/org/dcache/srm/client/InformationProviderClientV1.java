// $Id$
// $Log: not supported by cvs2svn $
// Revision 1.3  2005/03/23 20:47:03  timur
// making changes in how client delegates
//
// Revision 1.2  2005/03/11 21:16:25  timur
// making srm compatible with cern tools again
//
// Revision 1.1  2005/02/21 21:35:22  timur
// added axis client support for storage info provider
//
// Revision 1.4  2005/01/25 23:20:20  timur
// srmclient now uses srm libraries
//
// Revision 1.3  2004/07/02 20:14:22  timur
// reformated code a bit
//
// Revision 1.2  2004/06/30 21:57:04  timur
//  added retries on each step, added the ability to use srmclient used by srm copy in the server, added srm-get-request-status
//

/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.
 
 
 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.
 
 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).
 
 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.
 
 
 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.
 
 
 
  DISCLAIMER OF LIABILITY (BSD):
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.
 
 
  Liabilities of the Government:
 
  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.
 
 
  Export Control:
 
  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */

// generated by GLUE/wsdl2java on Mon Jun 17 15:36:22 CDT 2002
package org.dcache.srm.client;

import electric.registry.Registry;
import electric.registry.RegistryException;
import diskCacheV111.srm.IInformationProvider;
import org.dcache.srm.SRMException;
import org.dcache.srm.Logger;
import org.dcache.srm.security.SslGsiSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import electric.net.socket.SocketFactories;
import electric.net.socket.ISocketFactory;
import org.ietf.jgss.GSSCredential;


public class InformationProviderClientV1 implements diskCacheV111.srm.IInformationProvider {
    public static final short DEFAULTPORT=8443;
    public static final String DEFAULTPATH="srm/infoProvider1_0.wsdl";
    private IInformationProvider glue_provider;
    private org.dcache.srm.client.axis1.IInformationProvider_PortType axis_provider;
    private Logger logger;
    SslGsiSocketFactory socket_factory;
    boolean glueclient = false;
    GSSCredential user_cred;
    
    public InformationProviderClientV1(String wsdl_url, 
    Logger logger,
    SslGsiSocketFactory socket_factory
    ) throws SRMException {
        glueclient = true;
        this.logger = logger;
        this.socket_factory = socket_factory;
        logger.log(" wsdl url is "+wsdl_url);
        wsdl_url = unwrapHttpRedirection(wsdl_url);
        logger.log("unwrapped wsdl url is "+wsdl_url);
        if(wsdl_url.startsWith("https") || wsdl_url.startsWith("httpg")) {
            try {
                setGsisslFactories();
            }catch(Exception e) {
                logger.elog(e);
                throw new SRMException("failed to set gsissl Facrories "+e);
            }
        }
        if(wsdl_url.startsWith("httpg")) {
            wsdl_url = wsdl_url.replaceFirst("httpg", "https");
        }
        logger.log("connecting to "+wsdl_url);
        
        try {
            glue_provider =(IInformationProvider) Registry.bind(wsdl_url,IInformationProvider.class);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new SRMException(e.toString());
        }
        if(glue_provider == null) {
            throw new SRMException();
        }
    }
    
    public InformationProviderClientV1(
    String service_url, 
    GSSCredential user_cred,
    Logger logger, 
    boolean delegate, boolean full_delegation
    ) throws IOException,InterruptedException,  javax.xml.rpc.ServiceException {
        glueclient = false;
        this.user_cred  = user_cred;
        this.logger = logger;
        try {
            if(user_cred.getRemainingLifetime() < 60) {
                throw new IOException("credential remaining lifetime is less then a minute ");
            }
        }
        catch(org.ietf.jgss.GSSException gsse) {
            throw new IOException(gsse.toString());
        }
        org.globus.axis.util.Util.registerTransport();
        org.apache.axis.configuration.SimpleProvider simpleprovider = 
            new org.apache.axis.configuration.SimpleProvider();

        org.apache.axis.SimpleTargetedChain c = null;

        c = new org.apache.axis.SimpleTargetedChain(new org.globus.axis.transport.GSIHTTPSender());
        simpleprovider.deployTransport("httpg", c);
        simpleprovider.deployTransport("https", c);

        c = new org.apache.axis.SimpleTargetedChain(new  org.apache.axis.transport.http.HTTPSender());
        simpleprovider.deployTransport("http", c);
        org.dcache.srm.client.axis1.SRMServerV1Locator sl = 
        new org.dcache.srm.client.axis1.SRMServerV1Locator(simpleprovider);
        java.net.URL url = new java.net.URL(service_url);
        axis_provider = sl.getIInformationProvider(url);
        if(axis_provider instanceof org.apache.axis.client.Stub) {
            org.apache.axis.client.Stub axis_isrm_as_stub = 
                (org.apache.axis.client.Stub)axis_provider;
       
       		axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_CREDENTIALS,
                         user_cred);

	        // sets authorization type
        	axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
                         org.globus.gsi.gssapi.auth.HostAuthorization.getInstance());
                if(delegate) {
                    if(full_delegation) {
                        // sets gsi mode
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
                                 org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
                    } else {
                        
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
                                 org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_LIMITED_DELEG);
                    }
                } else {
                    // sets gsi mode
                    axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
                             org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_NO_DELEG);
                }
        }
        else {
            throw new java.io.IOException("can't set properties to the axis_privder");
        }
    }
    
    private void say(String s) {
        if(logger != null) {
            logger.log("SRMClientV1 : "+s);
        }
    }
    
    private void esay(String s) {
        if(logger != null) {
            logger.elog("SRMClientV1 : "+s);
        }
    }
    
    private void esay(Throwable t) {
        if(logger != null) {
            logger.elog(t);
        }
    }

    public void setGsisslFactories() throws Exception {
        logger.log(" install SslGsiSocketFactory as ssl "+
        "factory");
        //factory = new SslGsiSocketFactory (configuration);
        //SocketFactories.addFactory ("tcp", factory);
        
        
        socket_factory.setDoDelegation(false);
        SocketFactories.addFactory("ssl",socket_factory);
        
    }
    
    public IInformationProvider getProviderConnection() {
        return glue_provider;
    }
    
    public static IInformationProvider bind( String url ) throws RegistryException {
        return (IInformationProvider) Registry.bind( url, IInformationProvider.class );
    }
    
    public static String unwrapHttpRedirection(String http_url) {
        if(http_url == null || !http_url.startsWith("http://")) {
            return http_url;
        }
        HttpURLConnection http_connection  = null;
        InputStream in = null;
        try {
            URL http_URL = new URL(http_url);
            URLConnection connection = http_URL.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(false);
            if(connection instanceof  HttpURLConnection) {
                http_connection = (HttpURLConnection) connection;
                http_connection.setInstanceFollowRedirects(true);
                in = http_connection.getInputStream(); //this force the reading of the header
                URL new_url = http_connection.getURL();
                return new_url.toString();
                
            }
            
        }
        catch(IOException ioe) {
            
        }
        finally {
            try {
                if(in != null) {
                    in.close();
                }
                if(http_connection != null) {
                    http_connection.disconnect();
                }
            }
            catch(IOException ioe) {
            }
        }
        return http_url;
    }
    
    public diskCacheV111.srm.StorageElementInfo getStorageElementInfo() {
        if(glueclient) {
            return glue_provider.getStorageElementInfo();
        }
        else
        {
            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }
            try
            {
                return ConvertUtil.axisSEI2SEI(axis_provider.getStorageElementInfo());
            }catch(java.rmi.RemoteException re) {
                esay(re);
             throw new RuntimeException(re);
            }
        }
    }
    
}