//______________________________________________________________________________
//
// $Id$
// $Author$
//
// created 06/21 by Neha Sharma (neha@fnal.gov)
//
//______________________________________________________________________________

/*
 * SrmGetPermission
 *
 * Created on 06/21
 */

package org.dcache.srm.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import org.dcache.srm.AbstractStorageElement;
import org.dcache.srm.FileMetaData;
import org.dcache.srm.SRM;
import org.dcache.srm.SRMException;
import org.dcache.srm.SRMUser;
import org.dcache.srm.request.RequestCredential;
import org.dcache.srm.v2_2.ArrayOfAnyURI;
import org.dcache.srm.v2_2.ArrayOfTGroupPermission;
import org.dcache.srm.v2_2.ArrayOfTPermissionReturn;
import org.dcache.srm.v2_2.ArrayOfTUserPermission;
import org.dcache.srm.v2_2.SrmGetPermissionRequest;
import org.dcache.srm.v2_2.SrmGetPermissionResponse;
import org.dcache.srm.v2_2.TGroupPermission;
import org.dcache.srm.v2_2.TPermissionMode;
import org.dcache.srm.v2_2.TPermissionReturn;
import org.dcache.srm.v2_2.TReturnStatus;
import org.dcache.srm.v2_2.TStatusCode;
import org.dcache.srm.v2_2.TUserPermission;

/**
 *
 * @author  litvinse
 */

public class SrmGetPermission {
        private static Logger logger =
                LoggerFactory.getLogger(SrmGetPermission.class);
	AbstractStorageElement storage;
	SrmGetPermissionRequest request;
	SrmGetPermissionResponse response;
	SRMUser user;

	public SrmGetPermission(SRMUser user,
				RequestCredential credential,
				SrmGetPermissionRequest request,
				AbstractStorageElement storage,
				SRM srm,
				String client_host ) {
		this.request = request;
		this.user = user;
		this.storage = storage;
	}

	public SrmGetPermissionResponse getResponse() {
		if(response != null ) {
                    return response;
                }
		try {
			response = srmGetPermission();
        } catch(URISyntaxException e) {
            logger.debug(" malformed uri : "+e.getMessage());
            response = getFailedResponse(" malformed uri : "+e.getMessage(),
                    TStatusCode.SRM_INVALID_REQUEST);
        } catch(SRMException srme) {
            logger.error(srme.toString());
            response = getFailedResponse(srme.toString());
        }
		return response;
	}

	public static final SrmGetPermissionResponse getFailedResponse(String error) {
		return getFailedResponse(error,null);
	}

	public static final SrmGetPermissionResponse getFailedResponse(String error,TStatusCode statusCode) {
		if(statusCode == null) {
			statusCode =TStatusCode.SRM_FAILURE;
		}
                SrmGetPermissionResponse response = new SrmGetPermissionResponse();
		response.setReturnStatus(new TReturnStatus(statusCode, error));
		return response;
	}


	/**
	 * implementation of srm get permission
	 */

	public SrmGetPermissionResponse srmGetPermission()
                throws SRMException, URISyntaxException
        {
		if(request==null) {
			return getFailedResponse(" null request passed to SrmGetPermission()");
		}
		ArrayOfAnyURI anyuriarray = request.getArrayOfSURLs();
		org.apache.axis.types.URI[] uriarray = anyuriarray.getUrlArray();
		int length = uriarray.length;
		if (length==0) {
			return getFailedResponse(" zero length array of URLS");
		}
		ArrayOfTPermissionReturn permissionarray=new ArrayOfTPermissionReturn();
		TPermissionReturn permissionsArray[] =new TPermissionReturn[length];
		permissionarray.setPermissionArray(permissionsArray);
		boolean haveFailure = false;
		int nfailed = 0;
		for(int i=0;i <length;i++){
                        logger.debug("SURL["+i+"]= "+uriarray[i]);
                        URI surl = new URI(uriarray[i].toString());
                        TPermissionReturn p = new TPermissionReturn();
			p.setStatus(new TReturnStatus(TStatusCode.SRM_SUCCESS, null));
			p.setSurl(uriarray[i]);
			try {
                                FileMetaData fmd= storage.getFileMetaData(user,surl, false);
				String owner    = fmd.owner;
				int permissions = fmd.permMode;
				TPermissionMode  upm = PermissionMaskToTPermissionMode.maskToTPermissionMode(((permissions>>6)&0x7));
				TPermissionMode  gpm = PermissionMaskToTPermissionMode.maskToTPermissionMode(((permissions>>3)&0x7));
				TPermissionMode  opm = PermissionMaskToTPermissionMode.maskToTPermissionMode((permissions&0x7));
				ArrayOfTUserPermission arrayOfTUserPermissions = new ArrayOfTUserPermission();
				TUserPermission userPermissionArray[] = new TUserPermission[1];
				for (int j=0;j<userPermissionArray.length;j++) {
					userPermissionArray[j] = new TUserPermission(owner,upm);
				}
				arrayOfTUserPermissions.setUserPermissionArray(userPermissionArray);
				ArrayOfTGroupPermission arrayOfTGroupPermissions = new ArrayOfTGroupPermission();
				TGroupPermission groupPermissionArray[] = new TGroupPermission[1];
				for (int j=0;j<groupPermissionArray.length;j++) {
					groupPermissionArray[j] = new TGroupPermission(fmd.group,gpm);
				}
				arrayOfTGroupPermissions.setGroupPermissionArray(groupPermissionArray);
				p.setOwnerPermission(upm);
				p.setArrayOfUserPermissions(arrayOfTUserPermissions);
				p.setArrayOfGroupPermissions(arrayOfTGroupPermissions);
				p.setOtherPermission(opm);
				p.setOwner(owner);
			}
			catch (SRMException srme) {
				logger.warn(srme.toString());
				p.setStatus(new TReturnStatus(TStatusCode.SRM_FAILURE, uriarray[i] + " " + srme
                                        .getMessage()));
				haveFailure = true;
				nfailed++;
			}
			finally {
				permissionarray.setPermissionArray(i,p);
			}
		}
                TReturnStatus returnStatus;
                if (!haveFailure) {
                    returnStatus = new TReturnStatus(TStatusCode.SRM_SUCCESS, "success");
                } else if (nfailed == length) {
                    returnStatus = new TReturnStatus(TStatusCode.SRM_FAILURE,
                            "failed to get Permission for all requested surls");
                } else {
                    returnStatus = new TReturnStatus(TStatusCode.SRM_PARTIAL_SUCCESS,
                            "failed to get Permission for at least one file");

                }
                return new SrmGetPermissionResponse(returnStatus, permissionarray);
	}
}
