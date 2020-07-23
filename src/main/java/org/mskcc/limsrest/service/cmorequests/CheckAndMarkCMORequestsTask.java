package org.mskcc.limsrest.service.cmorequests;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.controller.GetSampleMetadata;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;


/**
 * End point to mark Requests as CMO Projects in LIMS based on following rules:
 *
 * - all WES recapture //TO DO
 * - WES (currently: if pipeline option is checked in ilabs, for near future: if CCS/TEMPO pipeline (Opt-in) option is checked in ilabs or in sample intake) --Done
 * - all IMPACT --Done
 * - all ACCESS --Done
 * - all HEMEPACT --Done
 * - Any request with skicmopm@mskcc.org listed in ilabs --Done
 * - Any request belonging to an existing CMO study (to be added regardless of the platform) --Done
 * - Any request with a CMO analyst listed in ilabs (we can provide list of analyst emails) //TO DO
 */

public class CheckAndMarkCMORequestsTask {

    private final String CMO_PM_EMAIL = "skicmopm@mskcc.org";
    private final String IMPACT_RECIPE_STRING = "impact";
    private final String HEMEMPACT_RECIPE_STRING = "hemepact";
    private final String ACCESS_RECIPE_STRING = "msk-access";
    private Log log = LogFactory.getLog(GetSampleMetadata.class);
    private ConnectionLIMS conn;
    private DataRecordManager dataRecordManager;
    private User user;
    private List<String> cmoRecipes;
    private String response;

    public CheckAndMarkCMORequestsTask(ConnectionLIMS conn) {
        this.conn = conn;
    }

    public String execute() {
        DataMgmtServer dataMgmtServer = conn.getConnection().getDataMgmtServer();
        dataRecordManager = conn.getConnection().getDataRecordManager();
        user = conn.getConnection().getUser();
        try {
            cmoRecipes = Arrays.asList(dataMgmtServer.getPickListManager(user).getPickListConfig("CMO Recipes").getEntryList()
                    .toString()
                    .toLowerCase()
                    .replace("[", "")
                    .replace("]", "")
                    .replaceAll(" ", "")
                    .split(","));

            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "NOT IsCmoRequest", user);
            for (DataRecord request : requests) {
                DataRecord[] samples = request.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);
                if (samples.length == 0) {
                    continue;
                }
                DataRecord sample = samples[0];
                String recipe = getRecipe(sample);
                boolean bicAnalysis = request.getBooleanVal(RequestModel.BICANALYSIS, user);
                if (isCmoRequest(request, recipe, bicAnalysis) || isPartOfExistingCmoStudy(request)) {
                    setIsCmoRequestTrue(request);
                }
            }
            dataRecordManager.storeAndCommit("Finished running CheckAndMarkCMORequests endpoint.", null, user);
        } catch (NotFound | IoError | RemoteException | ServerException e) {
            log.error(String.format("Exception while running CheckAndMarkCMORequests Task:\n%s", ExceptionUtils.getStackTrace(e)));
        }
        return response;
    }

    /**
     * Method to check if request is CMO request.
     *
     * @param request
     * @param recipe
     * @return
     */
    private boolean isCmoRequest(DataRecord request, String recipe, boolean bicAnalysis) {
        try {
            Object contactEmail = request.getValue(RequestModel.MAIL_TO, user);
            if (contactEmail != null && contactEmail.toString().toLowerCase().contains(CMO_PM_EMAIL)) {
                return true;
            }
            if (isCmoRecipe(recipe, bicAnalysis)) {
                return true;
            }
        } catch (NotFound | RemoteException e) {
            log.error(String.format("Exception while validating if request is CMO Request:\n%s", ExceptionUtils.getStackTrace(e)));
        }
        return false;
    }

    /**
     * Method to get recipe from Sample.
     *
     * @param sample
     * @return
     */
    private String getRecipe(DataRecord sample) {
        try {
            Object recipe = sample.getValue(SampleModel.RECIPE, user);
            if (recipe != null) {
                return (String) recipe;
            }
        } catch (NotFound | RemoteException e) {
            log.error(String.format("Error while getting recipe from Request Sample with RecordId %d:\n%s", sample.getRecordId(), ExceptionUtils.getStackTrace(e)));
        }
        return null;
    }

    /**
     * Method to check if request is WES recapture request
     *
     * @param request
     * @return
     */
    private boolean isWesRecapture(DataRecord request) {
        try {
            List<DataRecord> samples = Arrays.asList(request.getChildrenOfType("Sample", user));


        } catch (IoError | RemoteException e) {
            log.error(String.format("Error while validating CMO WES Recapture request:\n%s", ExceptionUtils.getStackTrace(e)));
        }
        return false;
    }

    /**
     * Method to check if recipe on sample is CMO recipe
     *
     * @param recipe
     * @return
     */
    private boolean isCmoRecipe(String recipe, boolean bicAnalysis) {
        if (recipe != null) {
            if (recipe.toLowerCase().contains(IMPACT_RECIPE_STRING) || recipe.toLowerCase().contains(HEMEMPACT_RECIPE_STRING) || recipe.toLowerCase().contains(ACCESS_RECIPE_STRING)) {
                return true;
            }
            return cmoRecipes.contains(recipe.toLowerCase()) && bicAnalysis;
        }
        return false;
    }

    /**
     * Method to set a request and all other request in same project as CMO Requests.
     *
     * @param request
     */
    private void setIsCmoRequestTrue(DataRecord request) {
        try {
            //set the current request and all other requests in the project as IsCmoRequest = true
            String requestId = request.getStringVal(RequestModel.REQUEST_ID, user).split("_")[0];
            String likeVal = requestId + "%";
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", RequestModel.REQUEST_ID + " LIKE '" + likeVal + "'", user);
            for (DataRecord req : requests) {
                req.setDataField("IsCmoRequest", true, user);
            }
        } catch (IoError | NotFound | RemoteException | InvalidValue e) {
            log.error(String.format("Error while setting IsCmoRequest field on Request:\n%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * Methdod to check if request if any other request in parent project is a CMO Request. In case we forgot to check
     * any cmo requests, this method will mark those requests when run.
     *
     * @param request
     * @return
     */
    private boolean isPartOfExistingCmoStudy(DataRecord request) {
        try {
            String requestId = request.getStringVal(RequestModel.REQUEST_ID, user).split("_")[0];
            String likeVal = requestId + "%";
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", RequestModel.REQUEST_ID + " LIKE '" + likeVal + "'", user);
            for (DataRecord req : requests) {
                boolean isCmoRequest = req.getBooleanVal("IsCmoRequest", user);
                if (isCmoRequest) {
                    return true;
                }
            }
        } catch (NotFound | RemoteException | IoError e) {
            log.error(String.format("Error while validating if Request is part of existing CMO Study:\n%s", ExceptionUtils.getStackTrace(e)));
        }
        return false;
    }
}
