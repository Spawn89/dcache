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

/*
 * SRMStageClient.java
 *
 * Created on January 09, 2006, 10:10 AM
 */

package gov.fnal.srm.util;

import org.globus.util.GlobusURL;
import diskCacheV111.srm.RequestStatus;
import diskCacheV111.srm.RequestFileStatus;
import java.util.HashSet;
import java.util.HashMap;
import java.io.IOException;
import java.util.Iterator;
/**
 *
 * @author  timur
 */
public class SRMStageClientV1 extends SRMClient implements Runnable {
	private String[] protocols;
	GlobusURL from[];
	private HashSet fileIDs = new HashSet();
	private HashMap fileIDsMap = new HashMap();
	private int requestID;
	private Thread hook;

	/** Creates a new instance of SRMStageClient */
	public SRMStageClientV1(Configuration configuration, GlobusURL[] from) {
		super(configuration);
		report = new Report(from,from,configuration.getReport());
		this.protocols = configuration.getProtocols();
		this.from = from;
	}

        @Override
	public void connect() throws Exception {
		connect(from[0]);
	}

	public void setProtocols(String[] protocols) {
		this.protocols = protocols;
	}

        @Override
	public void start() throws Exception {
		try {
			int len = from.length;
			String SURLS[] = new String[len];
			for(int i = 0; i < len; ++i) {
				SURLS[i] = from[i].getURL();
			}
			hook = new Thread(this);
			Runtime.getRuntime().addShutdownHook(hook);
			RequestStatus rs = srm.get(SURLS,protocols);
			if(rs == null) {
				throw new IOException(" null requests status");
			}
			requestID = rs.requestId;
			dsay(" srm returned requestId = "+rs.requestId);
			try {
				if(rs.state.equals("Failed")) {
					esay("rs.state = "+rs.state+" rs.error = "+rs.errorMessage);
					for(int i = 0; i< rs.fileStatuses.length;++i) {
						edsay("      ====> fileStatus state =="+rs.fileStatuses[i].state);
					}
					throw new IOException("rs.state = "+rs.state+" rs.error = "+rs.errorMessage);
				}

				if(rs.fileStatuses.length != len) {
					esay( "incorrect number of RequestFileStatuses"+
					"in RequestStatus expected "+len+" received "+rs.fileStatuses.length);
					throw new IOException("incorrect number of RequestFileStatuses"+
					"in RequestStatus expected "+len+" received "+rs.fileStatuses.length);
				}

				for(int i =0; i<len;++i) {
					Integer fileId = new Integer(rs.fileStatuses[i].fileId);
					fileIDs.add(fileId);
					fileIDsMap.put(fileId,rs.fileStatuses[i]);
				}
				int hashsetSize=fileIDs.size();
				//say("HashSet has : "+hashsetSize+ " elements");
				while(!fileIDs.isEmpty()) {
					Iterator iter = fileIDs.iterator();
					HashSet removeIDs = new HashSet();

					while(iter.hasNext()) {
						Integer nextID = (Integer)iter.next();
						RequestFileStatus frs = getFileRequest(rs,nextID);
						if(frs == null) {
							throw new IOException("request status does not have"+"RequestFileStatus fileID = "+nextID);
						}
						if(frs.state.equals("Failed")) {
							removeIDs.add(nextID);
							GlobusURL surl = new GlobusURL(frs.SURL);
							setReportFailed(surl,surl,  rs.errorMessage);
							esay( "staging of SURL "+frs.SURL+" failed: File Status is \"Failed\"");
							continue;
						}

						if(frs.state.equals("Ready") ) {
							say(frs.SURL+" is staged successfully ");
                                                        GlobusURL surl = new GlobusURL(frs.SURL);
							removeIDs.add(nextID);
                                                        srm.setFileStatus(rs.requestId,nextID.intValue(),"Done");
                                                        setReportSucceeded(surl,surl);
						}
					}

					fileIDs.removeAll(removeIDs);
					removeIDs = null;

					if(fileIDs.isEmpty()) {
						dsay("fileIDs is empty, breaking the loop");
						Runtime.getRuntime().removeShutdownHook(hook);
						break;
					}

					try{
						int retrytime = rs.retryDeltaTime;
						if( retrytime <= 0 ) {
							retrytime = 1;
						}
						say("sleeping "+retrytime+" seconds ...");
						Thread.sleep(retrytime * 1000);
					}catch(InterruptedException ie) {
					}

					rs = srm.getRequestStatus(requestID);
					if(rs == null) {
						throw new IOException(" null requests status");
					}

					if(rs.fileStatuses.length != len) {
						esay( "incorrect number of RequestFileStatuses"+"in RequestStatus expected "+len+" received "+rs.fileStatuses.length);
						throw new IOException("incorrect number of RequestFileStatuses"+"in RequestStatus expected "+len+" received "+rs.fileStatuses.length);
					}

					for(int i =0; i<len;++i) {
						fileIDsMap.put(new Integer(rs.fileStatuses[i].fileId),rs.fileStatuses[i]);
					}

					if(rs.state.equals("Failed")) {
						esay("rs.state = "+rs.state+" rs.error = "+rs.errorMessage);
						for(int i = 0; i< rs.fileStatuses.length;++i) {
							edsay("      ====> fileStatus state =="+rs.fileStatuses[i].state);
						}
						throw new IOException("rs.state = "+rs.state+" rs.error = "+rs.errorMessage);
					}
				}
			}catch(IOException ioe) {
				done(rs,srm);
				throw ioe;
			}
		}finally{
		//say("initialStagingTest: "+initialStagingTest);
			//say("atleastOneFailed: "+atleastOneFailed);
			report.dumpReport();
			if(!report.everythingAllRight()){
				//This means that some failure occured while staging file onto dcache
                                System.err.println("srm stage of at least one file failed or not completed");
				System.exit(1);
			}
		}
	}

        @Override
	public void run() {
		say("setting all remaining file statuses to \"Done\"");
		while(true) {
			if(fileIDs.isEmpty()) {
				break;
			}
			Integer fileId = (Integer)fileIDs.iterator().next();
			fileIDs.remove(fileId);
			say("setting file request "+fileId+" status to Done");
			RequestFileStatus rfs = (RequestFileStatus)fileIDsMap.get(fileId);
			srm.setFileStatus(requestID,rfs.fileId,"Done");
		}
		say("set all file statuses to \"Done\"");
	}
}