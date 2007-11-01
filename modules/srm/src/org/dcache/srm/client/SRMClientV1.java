// $Id: SRMClientV1.java,v 1.16 2006-10-19 20:59:09 timur Exp $
// $Log: not supported by cvs2svn $
// Revision 1.15  2006/09/11 22:28:49  timur
// / is a default path in url
//
// Revision 1.14  2006/03/14 17:44:17  timur
// moving toward the axis 1_3
//
// Revision 1.13  2006/02/23 21:46:38  neha
// Changes by Neha- to allow user specified value of command line option 'webservice_path'
// to override the default value.
//
// Revision 1.12  2006/02/03 01:43:38  timur
// make  srm v2 copy work with remote srm v1 and vise versa
//
// Revision 1.11  2006/01/26 04:42:12  timur
// changed error type logs to debug logs
//
// Revision 1.10  2005/10/26 15:00:47  timur
// make delegation work in case srm copy with srm v1 under Axis/Tomcat
//
// Revision 1.9  2005/09/06 21:43:20  timur
// make sure the slash separates port and path to web service
//
// Revision 1.8  2005/08/29 22:52:04  timur
// commiting changes made by Neha needed by OSG
//
// Revision 1.7  2005/08/10 14:32:30  leoheska
// No longer used - SRMLsCaller1 is used instead.
//
// Revision 1.6  2005/08/09 22:04:44  leoheska
// Formatting changes only.
//
// Revision 1.5  2005/06/02 20:48:04  timur
// better error propagation in case of advisory delete, less error printing in client
//
// Revision 1.4  2005/03/24 19:16:18  timur
// made built in client always delegate credentials, which is required by LBL's DRM
//
// Revision 1.3  2005/03/11 21:16:25  timur
// making srm compatible with cern tools again
//
// Revision 1.2  2005/03/11 17:50:33  timur
// find the cannonical host name before connecting to it
//
// Revision 1.1  2005/01/14 23:07:14  timur
// moving general srm code in a separate repository
//
// Revision 1.7  2005/01/11 18:10:39  timur
// do not retry setFileStatus
//
// Revision 1.6  2005/01/07 23:12:16  timur
// make sure we do not throw NullPointerException with cern server, which does not set all the fields
//
// Revision 1.5  2005/01/07 20:55:30  timur
// changed the implementation of the built in client to use apache axis soap toolkit
//
// Revision 1.4  2004/12/14 19:37:56  timur
// fixed advisory delete permissions and client issues
//
// Revision 1.3  2004/09/08 21:27:39  timur
// remote gsiftp transfer manager will now use ftp logger too
//
// Revision 1.2  2004/08/06 19:35:22  timur
// merging branch srm-branch-12_May_2004 into the trunk
//
// Revision 1.1.2.6  2004/08/03 16:37:51  timur
// removing unneeded dependancies on dcache
//
// Revision 1.1.2.5  2004/07/29 22:17:29  timur
// Some functionality for disk srm is working
//
// Revision 1.1.2.4  2004/07/02 20:10:23  timur
// fixed the leak of sql connections, added propogation of srm errors
//
// Revision 1.1.2.3  2004/06/30 20:37:23  timur
// added more monitoring functions, added retries to the srm client part, adapted the srmclientv1 for usage in srmcp
//
// Revision 1.1.2.2  2004/06/15 22:15:41  timur
// added cvs logging tags and fermi copyright headers at the top
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


// generated by GLUE/wsdl2java on Mon Jun 17 15:27:13 CDT 2002
package org.dcache.srm.client;

import org.dcache.srm.security.SslGsiSocketFactory;
import org.dcache.srm.SRMUser;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import electric.registry.Registry;
import electric.registry.RegistryException;
import org.globus.gsi.gssapi.auth.AuthorizationException;
import org.globus.util.GlobusURL;
import org.ietf.jgss.GSSCredential;
import electric.server.http.HTTP;
import electric.net.socket.SocketFactories;
import electric.util.Context;
import electric.net.http.HTTPContext;
import org.dcache.srm.SRMAuthorization;
import org.dcache.srm.Logger;
//import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.globus.gsi.gssapi.auth.GSSAuthorization;
import org.globus.gsi.gssapi.GlobusGSSName;
import org.ietf.jgss.Oid;
//import org.dcache.srm.SRMServerHostAuthorization;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
public class SRMClientV1 implements diskCacheV111.srm.ISRM {
    private final static String SFN_STRING="?SFN=";
    private int retries;
    private long retrytimeout;
    private static final String WSDL_POSTFIX = "/srm/managerv1.wsdl";
    private static final String SERVICE_POSTFIX = "/srm/managerv1";
    
    private diskCacheV111.srm.ISRM glue_isrm;
    private org.dcache.srm.client.axis.ISRM_PortType axis_isrm;
    private SslGsiSocketFactory socket_factory;
    private GSSCredential user_cred;
    private String wsdl_url;
    private String service_url;
    private Logger logger;
    private String host;
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
    
    //this version of constructor will use the soap client from glue
    public SRMClientV1(GlobusURL srmurl,
    SslGsiSocketFactory socket_factory,
    GSSCredential user_cred,
    long retrytimeout,
    int numberofretries,
    Logger logger
    )
    throws IOException,InterruptedException {
        say("constructor: srmurl = "+srmurl+" user_cred= "+ user_cred+
        " retrytimeout="+retrytimeout+" msec numberofretries="+numberofretries);
        this.retrytimeout = retrytimeout;
        this.retries = numberofretries;
        this.user_cred = user_cred;
        this.socket_factory = socket_factory;
        this.logger = logger;
        try {
            if(user_cred.getRemainingLifetime() < 60) {
                throw new IOException("credential remaining lifetime is less then a minute ");
            }
        }
        catch(org.ietf.jgss.GSSException gsse) {
            throw new IOException(gsse.toString());
        }
        
        SocketFactories.addFactory("ssl", socket_factory);
        
        //say("constructor: obtained socket factory");
        host = srmurl.getHost();
        host = InetAddress.getByName(host).getCanonicalHostName();
        int port = srmurl.getPort();
        String path = srmurl.getPath();
        wsdl_url = ((port == 80)?"http://":"https://")+
        host + ":" +
        port;
        int indx=path.indexOf(SFN_STRING);
        
        if(indx >0) {
            wsdl_url += path.substring(0,indx)+".wsdl";
        }
        else {
            wsdl_url += WSDL_POSTFIX;
        }
        
        int i = 0;
        while(true) {
            try {
                
                
                socket_factory.lockCredential(user_cred);
                wsdl_url = unwrapHttpRedirection(wsdl_url);
                //say("constructor: unwrapped wsdl_url="+wsdl_url);
                try {
                    say("constructor: binding to "+wsdl_url);
                    glue_isrm = (diskCacheV111.srm.ISRM) Registry.bind(wsdl_url,diskCacheV111.srm.ISRM.class);
                }
                catch(RegistryException re) {
                    esay("constructor: error while binding : "+re);
                    esay(re.getMessage());
                    
                    socket_factory.unlockCredential();
                    
                    throw new IOException(re.toString());
                    
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }
                return;
            }
            catch(IOException e) {
                esay("CONSTRUCTOR: try # "+i+" failed with error");
                esay(e.getMessage());
                if(i <retries) {
                    i++;
                    esay("CONSTRUCTOR: try again");
                }
                else {
                    throw e;
                }
            }
            try {
                say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                Thread.sleep(retrytimeout*i);
            }
            catch(InterruptedException ie) {
            }
            
        }
    }
    
    //this is the cleint that will use the axis version of the 
    // client underneath
    
    public SRMClientV1(GlobusURL srmurl,
            GSSCredential user_cred,
            long retrytimeout,
            int numberofretries,
            Logger logger,
            boolean do_delegation,
            boolean full_delegation,
            String gss_expected_name,
            String webservice_path) throws IOException,InterruptedException,  javax.xml.rpc.ServiceException {
	
	say("In Server Side: webservice_path= "+webservice_path);
	
	esay("constructor: srmurl = "+srmurl+" user_cred= "+ user_cred+" retrytimeout="+retrytimeout+" msec numberofretries="+numberofretries);
        this.retrytimeout = retrytimeout;
        this.retries = numberofretries;
        this.user_cred = user_cred;
        this.logger = logger;
        if(user_cred == null) {
            throw new NullPointerException("user credential is null");
        }
        try {
	    say("user credentials are: "+user_cred.getName());
            if(user_cred.getRemainingLifetime() < 60) {
                throw new IOException("credential remaining lifetime is less then a minute ");
            }
        }
        catch(org.ietf.jgss.GSSException gsse) {
            throw new IOException(gsse.toString());
        }
        
        
        //say("constructor: obtained socket factory");
        host = srmurl.getHost();
        host = InetAddress.getByName(host).getCanonicalHostName();
        int port = srmurl.getPort();
        String path = srmurl.getPath();
        if(path==null) {
            path="/";
        }
        service_url = ((port == 80)?"http://":"httpg://")+host + ":" +port ;
        int indx=path.indexOf(SFN_STRING);
        
        if(indx >0) {
            String service_postfix = path.substring(0,indx);
            if(!service_postfix.startsWith("/")){
                service_url += "/";
            }
            service_url += service_postfix;
	    //say("SFN Exists: service_url: "+service_url);
        }
        else {
	    
	    service_url += "/"+webservice_path;
	    //say("SFN doesnot Exist: service_url: "+service_url);	
        }
        say("SRMClientV1 calling org.globus.axis.util.Util.registerTransport() ");
        org.globus.axis.util.Util.registerTransport();
        org.apache.axis.configuration.SimpleProvider provider = 
            new org.apache.axis.configuration.SimpleProvider();

        org.apache.axis.SimpleTargetedChain c = null;

        c = new org.apache.axis.SimpleTargetedChain(new org.globus.axis.transport.GSIHTTPSender());
        provider.deployTransport("httpg", c);

        c = new org.apache.axis.SimpleTargetedChain(new  org.apache.axis.transport.http.HTTPSender());
        provider.deployTransport("http", c);
        org.dcache.srm.client.axis.SRMServerV1Locator sl = new org.dcache.srm.client.axis.SRMServerV1Locator(provider);
        java.net.URL url = new java.net.URL(service_url);
        say("connecting to srm at "+service_url);
        axis_isrm = sl.getISRM(url);
        if(axis_isrm instanceof org.apache.axis.client.Stub) {
            org.apache.axis.client.Stub axis_isrm_as_stub = (org.apache.axis.client.Stub)axis_isrm;
       		axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_CREDENTIALS,user_cred);
	        // sets authorization type
                axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
                        new PromiscuousHostAuthorization());//HostAuthorization(gss_expected_name));
        	//axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,org.globus.gsi.gssapi.auth.HostAuthorization.getInstance());
                if (do_delegation) {
                    if(full_delegation) {
                        // sets gsi mode
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
                    } else {
                        axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_LIMITED_DELEG);
                    }                    
                    
                } else {
                    // sets gsi mode
                    axis_isrm_as_stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_NO_DELEG);
                }
        }
        else {
            throw new java.io.IOException("can't set properties to the axis_isrm");
        }
    }

    public diskCacheV111.srm.RequestStatus put( String[] sources,
    String[] dests,
    long[] sizes,
    boolean[] wantPerm,
    String[] protocols ) {
        for(int i = 0 ; i<sources.length;++i) {
            say("\tput, sources["+i+"]=\""+sources[i]+"\"");
        }
        for(int i = 0 ; i<dests.length;++i) {
            say("\tput, dests["+i+"]=\""+dests[i]+"\"");
        }
        for(int i = 0 ; i<protocols.length;++i) {
            say("\tput, protocols["+i+"]=\""+protocols[i]+"\"");
        }
        if(glue_isrm != null) {
            say(" put, contacting wsdl "+wsdl_url);

            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    throw new RuntimeException(ie);
                }

                try {
                    diskCacheV111.srm.RequestStatus rs = 
                       glue_isrm.put(sources, dests, sizes, wantPerm,
                                     protocols);
                    return rs;
                }
                catch(RuntimeException e) {
                    esay("put: try # "+i+" failed with error");
                    esay(e.getMessage());
                    /*if(i <retries) {
                        i++;
                        esay("put: try again");
                    }
                    else */
                    // do not retry on the request establishing functions
                    {
                        throw e;
                    }
                }
                finally {           
                    // say(" put is calling socket_factory.unlockCredentialAndCloseSocket");
                    socket_factory.unlockCredentialAndCloseSocket();
                }
                /*
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }*/

            }
        }
        else
        {
            if (axis_isrm == null) 
               throw new NullPointerException ("Both isrms are null.");
            
            say(" put, contacting service " + service_url);
            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException(
                           "credential remaining lifetime is less " +
                           "than one minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {
                    try {
                        org.dcache.srm.client.axis.RequestStatus rs = 
                           axis_isrm.put(sources, dests,
                                         sizes, wantPerm, protocols);
                        return ConvertUtil.axisRS2RS(rs);
                    }
                    catch(java.rmi.RemoteException re) {
                        throw new RuntimeException(re);
                    }
                }
                catch(RuntimeException e) {
                    esay("put: try # "+i+" failed with error");
                    esay(e.getMessage());
                    /*if(i <retries) {
                        i++;
                        esay("put: try again");
                    }
                    else 
                     */
                    // do not retry on the request establishing functions
                    {
                        throw e;
                    }
                }
                /*
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */

            }
            
        }
    }
    
    public diskCacheV111.srm.RequestStatus get( String[] surls,String[] protocols ) {

        for(int i = 0 ; i<surls.length;++i) {
            say("\tget: surls["+i+"]=\""+surls[i]+"\"");
        }
        for(int i = 0 ; i<protocols.length;++i) {
            say("\tget: protocols["+i+"]=\""+protocols[i]+"\"");
        }
        if(glue_isrm != null) {
            esay(" get, contacting wsld "+wsdl_url);
            int i = 0;
            while(true) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
		    say("User Credentials : "+user_cred.getName());
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);

                }
                try {
                    diskCacheV111.srm.RequestStatus rs = glue_isrm.get(surls,protocols);
		    say("Request Status: "+rs);
                    return rs;
                }
                catch(RuntimeException e) {
                    esay("get : try #"+i+" failed with error");
                    esay(e.getMessage());
                    /*
                    if(i <retries) {
                        i++;
                        esay("get : try again");
                    }
                    else */
                    {
                        throw e;
                    }
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }
                /*
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */

            }
        }
        else
        {
	    if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    try
                    {
			org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.get(surls,protocols);
                        return ConvertUtil.axisRS2RS(rs);
                    }catch(java.rmi.RemoteException re) {
                        esay(re.toString());
                        throw new RuntimeException (re.toString());
                    }
                    
                }
                catch(RuntimeException e) {
                    esay("get : try # "+i+" failed with error");
                    esay(e.getMessage());
                    /*
                    if(i <retries) {
                        i++;
                        esay("get : try again");
                    }
                    else 
                     */
                    // do not retry on the request establishing functions
                    {
                        throw e;
                    }
                }
                /*
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */

            }
            
        }
    }
    
    public diskCacheV111.srm.RequestStatus copy( String[] srcSURLS,
    String[] destSURLS,
    boolean[] wantPerm ) {
        for(int i = 0 ; i<srcSURLS.length;++i) {
            say("\tcopy, srcSURLS["+i+"]=\""+srcSURLS[i]+"\"");
        }
        for(int i = 0 ; i<destSURLS.length;++i) {
            say("\tcopy, destSURLS["+i+"]=\""+destSURLS[i]+"\"");
        }
        if(glue_isrm != null) {
            say(" copy, contacting wsld "+wsdl_url);
            int i = 0;
            while(true) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);
                }

                try {
                    diskCacheV111.srm.RequestStatus rs = glue_isrm.copy(srcSURLS,destSURLS,wantPerm);
                    //say("copy returned rs with id="+rs.requestId+ " returning rs");
                    return rs;
                }
                catch(RuntimeException e) {
                    esay("copy: try #"+i+" failed with error");
                    esay(e.getMessage());
                    /*
                    if(i <retries) {
                        i++;
                        esay("copy: try again");
                    }
                    else 
                     */
                    // do not retry on the request establishing functions
                     {
                        throw e;
                    }
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }

                /*
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */
            }
        }
        else {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            say(" copy, contacting service "+service_url);
            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {
                    try
                    {
                        org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.copy(srcSURLS,destSURLS,wantPerm);
                        return ConvertUtil.axisRS2RS(rs);
                    }catch(java.rmi.RemoteException re) {
                        //esay(re);
                        throw new RuntimeException (re.toString());
                    }
                    
                }
                catch(RuntimeException e) {
                    esay("copy: try # "+i+" failed with error");
                    esay(e.getMessage());
                    /*
                    if(i <retries) {
                        i++;
                        esay("copy: try again");
                    }
                    else 
                     */
                    // do not retry on the request establishing functions
                     {
                        throw e;
                    }
                }
                
                /*
                 try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */

            }
            
        }
    }
    
    public diskCacheV111.srm.RequestStatus getRequestStatus( int requestId ) {
        if(glue_isrm != null) 
        {
            //say(" getRequestStatus , contacting wsdl "+wsdl_url);
            int i = 0;
            while(true) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);
                }

                try {
                    diskCacheV111.srm.RequestStatus rs = glue_isrm.getRequestStatus(requestId);
                    //say("getRequestStatus returned");
                    return rs;

                }
                catch(RuntimeException e) {
                    esay("getRequestStatus: try #"+i+" failed with error");
                    esay(e.getMessage());
                    if(i <retries) {
                        i++;
                        esay("getRequestStatus: try again");
                    }
                    else {
                        throw e;
                    }
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }

                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }

            }

        }
        else
        {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            int i = 0;
            while(true) 
            {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {

                    try
                    {
                        org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.getRequestStatus(requestId);
                        return ConvertUtil.axisRS2RS(rs);
                    }catch(java.rmi.RemoteException re) {
                        //esay(re);
                        throw new RuntimeException (re.toString());
                    }
                }
                catch(RuntimeException e) {
                    esay("getRequestStatus: try #"+i+" failed with error");
                    esay(e.getMessage());
                    if(i <retries) {
                        i++;
                        esay("getRequestStatus: try again");
                    }
                    else {
                        throw e;
                    }
                }

                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
            }
        }
    }
    public boolean ping() {
        return true;
    }
    
    public diskCacheV111.srm.RequestStatus mkPermanent( String[] SURLS ) {
        return  null;
    }
    
    public diskCacheV111.srm.RequestStatus pin( String[] TURLS ) {
        try {
            socket_factory.lockCredential(user_cred);
        }
        catch(InterruptedException ie) {
            //esay(ie);
            throw new RuntimeException(ie);
        }
        try {
            diskCacheV111.srm.RequestStatus rs = glue_isrm.pin(TURLS);
            return rs;
        }
        finally {
            socket_factory.unlockCredentialAndCloseSocket();
        }
    }
    
    public diskCacheV111.srm.RequestStatus unPin( String[] TURLS ,int requestID) {
        try {
            socket_factory.lockCredential(user_cred);
        }
        catch(InterruptedException ie) {
            //esay(ie);
            throw new RuntimeException(ie);
        }
        try {
            diskCacheV111.srm.RequestStatus rs = glue_isrm.unPin(TURLS, requestID);
            return rs;
        }
        finally {
            socket_factory.unlockCredentialAndCloseSocket();
        }
    }
    
    public diskCacheV111.srm.RequestStatus getEstGetTime( String[] SURLS ,String[] protocols) {
        try {
            socket_factory.lockCredential(user_cred);
        }
        catch(InterruptedException ie) {
            //esay(ie);
            throw new RuntimeException(ie);
        }
        try {
            diskCacheV111.srm.RequestStatus rs = glue_isrm.getEstGetTime(SURLS, protocols);
            return rs;
        }
        finally {
            socket_factory.unlockCredentialAndCloseSocket();
        }
    }
    
    public diskCacheV111.srm.RequestStatus getEstPutTime( String[] src_names,
    String[] dest_names,
    long[] sizes,
    boolean[] wantPermanent,
    String[] protocols) {
        try {
            socket_factory.lockCredential(user_cred);
        }
        catch(InterruptedException ie) {
            //esay(ie);
            throw new RuntimeException(ie);
        }
        try {
            diskCacheV111.srm.RequestStatus rs = glue_isrm.getEstPutTime(src_names, dest_names, sizes, wantPermanent, protocols);
            return rs;
        }
        finally {
            socket_factory.unlockCredentialAndCloseSocket();
        }
    }
    
    public diskCacheV111.srm.FileMetaData[] getFileMetaData( String[] SURLS ) {
        if(glue_isrm != null) {
            int i = 0;
            while(true) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);
                }
                try {
                    diskCacheV111.srm.FileMetaData[] fmd = glue_isrm.getFileMetaData(SURLS);
                    return fmd;
                }
                catch(RuntimeException e) {
                    esay("getFileMetaData: try #"+i+" failed with error");
                    esay(e.toString());
                    if(i <retries) {
                        i++;
                        esay("getFileMetaData: try again");
                    }
                    else {
                        throw e;
                    }
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }

                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
            }
        }
        else {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            say(" getFileMetaData, contacting service "+service_url);
            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {
                    try
                    {
                        org.dcache.srm.client.axis.FileMetaData[] fmd = axis_isrm.getFileMetaData(SURLS);
                         return ConvertUtil.axisFMDs2FMDs(fmd);
                    }catch(java.rmi.RemoteException re) {
                        //esay(re);
                        throw new RuntimeException (re.toString());
                    }
                    
                }
                catch(RuntimeException e) {
                    esay("copy: try # "+i+" failed with error");
                    esay(e.getMessage());
                    if(i <retries) {
                        i++;
                        esay("copy: try again");
                    }
                    else {
                        throw e;
                    }
                }
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }

            }
            
        }
    }
    
    public diskCacheV111.srm.RequestStatus setFileStatus( int requestId,
    int fileId,
    String state ) {
        //say(" setFileStatus , contacting wsdl "+wsdl_url);
        if(glue_isrm != null) {
            int i = 0;
            while(true) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);
                }
                try {
                    diskCacheV111.srm.RequestStatus rs = glue_isrm.setFileStatus(requestId,fileId,state);
                    return rs;
                }
                catch(RuntimeException e) {
                    esay("setFileStatus: try #"+i+" failed with error");
                    esay(e.getMessage());
                    /*
                     * we do not retry in case of setFileStatus for reasons of performanse
                     * and because the setFileStatus fails too often for castor implementation
                     if(i <retries) 
                     */
                      if(false)
                     {
                        i++;
                        esay("setFileStatus: try again");
                    }
                    else 
                    
                    {
                        throw e;
                    }
                }
                finally {
                    socket_factory.unlockCredentialAndCloseSocket();
                }

                /*
                 * Do not retry on the setFileStatus, it fails too often for cern srm
                 try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
                 */


            }
        }        
        else
        {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            int i = 0;
            while(true) 
            {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {

                    try
                    {
                        org.dcache.srm.client.axis.RequestStatus rs = axis_isrm.setFileStatus(requestId,fileId,state);
                        return ConvertUtil.axisRS2RS(rs);
                    }catch(java.rmi.RemoteException re) {
                        //esay(re);
                        throw new RuntimeException (re.toString());
                    }
                    //say("getRequestStatus returned");
                }
                catch(RuntimeException e) {
                    esay("getRequestStatus: try #"+i+" failed with error");
                    esay(e.getMessage());
                    /*
                     * we do not retry in case of setFileStatus for reasons of performanse
                     * and because the setFileStatus fails too often for castor implementation
                     *
                    if(i <retries) 
                     */
                     if(false)
                     {
                        i++;
                        esay("getRequestStatus: try again");
                    }
                    else 
                     
                     {
                        throw e;
                    }
                }
 
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }
            }
        }
    }
    
    public void advisoryDelete( String[] SURLS) {
        for(int i = 0 ; i<SURLS.length;++i) {
            say("\tadvisoryDelete SURLS["+i+"]=\""+SURLS[i]+"\"");
        }
        if(glue_isrm != null) {
            say(" advisoryDelete, contacting wsld "+wsdl_url);
            int i = 0;
            try {
                if(user_cred.getRemainingLifetime() < 60) {
                    throw new RuntimeException("credential remaining lifetime is less then a minute ");
                }
            }
            catch(org.ietf.jgss.GSSException gsse) {
                throw new RuntimeException(gsse);
            }

            try {
                socket_factory.lockCredential(user_cred);
            }
            catch(InterruptedException ie) {
                //esay(ie);
                throw new RuntimeException(ie);
            }

            try {
                glue_isrm.advisoryDelete(SURLS);
                return ;
            }
            finally { 
                socket_factory.unlockCredentialAndCloseSocket();
            }

        }
        else {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            say(" advisoryDelete, contacting service "+service_url);
            int i = 0;

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
                 axis_isrm.advisoryDelete(SURLS);
                 return;
            }catch(java.rmi.RemoteException re) {
                //esay(re);
                String message = re.getMessage();
                if(message != null)  throw new RuntimeException (message);
                throw new RuntimeException (re);
            }

        }
            
   }
    
    public String[] getProtocols() {
        if(glue_isrm != null) {
                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }

                try {
                    socket_factory.lockCredential(user_cred);
                }
                catch(InterruptedException ie) {
                    //esay(ie);
                    throw new RuntimeException(ie);
                }
                int i = 0;
                while(true) {
                    try {
                        String protocols[] = glue_isrm.getProtocols();
                        return protocols;
                    }
                    catch(RuntimeException e) {
                        esay("getProtocols: try #"+i+" failed with error");
                        esay(e.getMessage());
                        if(i <retries) {
                            i++;
                            esay("getProtocols: try again");
                        }
                        else {
                            throw e;
                        }
                    }
                    finally {
                        socket_factory.unlockCredentialAndCloseSocket();
                    }

                    try {
                        say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                        Thread.sleep(retrytimeout*i);
                    }
                    catch(InterruptedException ie) {
                    }
                }
            
        }
        else {
            if (axis_isrm == null) { throw new NullPointerException ("both isrms are null!!!!");}
            say(" getProtocols, contacting service "+service_url);
            int i = 0;
            while(true) {

                try {
                    if(user_cred.getRemainingLifetime() < 60) {
                        throw new RuntimeException("credential remaining lifetime is less then a minute ");
                    }
                }
                catch(org.ietf.jgss.GSSException gsse) {
                    throw new RuntimeException(gsse);
                }


                try {
                    try
                    {
                          String protocols[] =axis_isrm.getProtocols();
                          return protocols;
                    }catch(java.rmi.RemoteException re) {
                        //esay(re);
                        throw new RuntimeException (re.toString());
                    }
                    
                }
                catch(RuntimeException e) {
                    esay("getProtocols: try # "+i+" failed with error");
                    esay(e.getMessage());
                    if(i <retries) {
                        i++;
                        esay("getProtocols: try again");
                    }
                    else {
                        throw e;
                    }
                }
                try {
                    say("sleeping for "+(retrytimeout*i)+ " milliseconds before retrying");
                    Thread.sleep(retrytimeout*i);
                }
                catch(InterruptedException ie) {
                }

            }
            
        }
    }
}
