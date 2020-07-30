package org.mskcc.limsrest.service.cmorequests;

import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.domain.sample.SampleType;
import org.mskcc.limsrest.ConnectionLIMS;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mskcc.limsrest.util.Utils.getValueFromDataRecord;


/**
 * End point to check if a request is CMO request or mark Requests as CMO Projects in LIMS based on following rules:
 *
 * 1. all WES recapture --Done
 * 2. WES (currently: if pipeline option is checked in ilabs, for near future: if CCS/TEMPO pipeline (Opt-in) option is checked in ilabs or in sample intake) --Done
 * 3. all IMPACT --Done
 * 4. all ACCESS --Done
 * 5. all HEMEPACT --Done
 * 6. Any request with skicmopm@mskcc.org listed in ilabs --Done
 * 7. Any request belonging to an existing CMO study (to be added regardless of the platform) --Done
 * 8. Any request with a CMO analyst listed in ilabs (we can provide list of analyst emails) --Done
 *
 * If the projectid is passed to the endpoint and project IsCmoRequest, return text indicating that. No updates are done in this case.
 * If projectid is not passed to the end
 */

public class CheckOrMarkCMORequestsTask {

    private static final String CMO_PM_EMAIL = "skicmopm@mskcc.org";
    private static final String IMPACT_RECIPE_VAL = "impact";
    private static final String HEMEMPACT_RECIPE_VAL = "hemepact";
    private static final String ACCESS_RECIPE_VAL = "msk-access";
    private static final String WHOLE_EXOME_RECIPE = "WholeExomeSequencing";
//    private static final List<String> CMO_ANALYST_EMAILS = Arrays.asList("sharmaa1@mskcc.org", "bolipatc@mskcc.org");
    private Log log = LogFactory.getLog(CheckOrMarkCMORequestsTask.class);
    private String projectId;
    private ConnectionLIMS conn;
    private DataRecordManager dataRecordManager;
    private User user;
    private static List<String> cmoRecipes = Arrays.asList("hemebrainpact_v1","hemepact_v4", "impact410", "impact468",
            "impact505", "msk-access_v1", "wholeexomesequencing");
    private List<String> response = new ArrayList<>();
    private static List<String> cmoAnalysts = Arrays.asList("bergerm1@mskcc.org", "donoghum@mskcc.org", "wonh@mskcc.org"
            , "chavans@mskcc.org", "bandlamc@mskcc.org", "richara4@mskcc.org"); //default emails for unit tests. This will get updated with full list from LIMS when run.
    public CheckOrMarkCMORequestsTask(String projectId, ConnectionLIMS conn) {
        this.projectId = projectId;
        this.conn = conn;
    }

    public String execute() {
        DataMgmtServer dataMgmtServer = conn.getConnection().getDataMgmtServer();
        dataRecordManager = conn.getConnection().getDataRecordManager();
        user = conn.getConnection().getUser();
        try {
            cmoRecipes = Arrays.asList(dataMgmtServer.getPickListManager(user).getPickListConfig("CMO Request Recipes").getEntryList()
                    .toString()
                    .toLowerCase()
                    .replace("[", "")
                    .replace("]", "")
                    .replaceAll(" ", "")
                    .split(","));
            cmoAnalysts = Arrays.asList(dataMgmtServer.getPickListManager(user).getPickListConfig("CMO Analysts").getEntryList()
                    .toString()
                    .toLowerCase()
                    .replace("[", "")
                    .replace("]", "")
                    .replaceAll(" ", "")
                    .split(","));

            //if projectid is passed to the endpoint then check if project is CMO Request and return response.
            if (!projectId.equalsIgnoreCase("NULL")){
                List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "RequestId = '" + projectId + "'", user);
                assert requests.size()==1;
                DataRecord request = requests.get(0);
                String requestId = (String)getValueFromDataRecord(request, RequestModel.REQUEST_ID, "String", user);
                //check if the request is already marked true
                if (request.getBooleanVal("IsCmoRequest", user)){
                    return String.format("%s is cmo request", requestId);
                }
                DataRecord[] samples = request.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);
                assert samples.length >0;
                String projectId = requestId.split("_")[0];
                boolean bicAnalysis = false;
                if(request.getValue(RequestModel.BICANALYSIS, user) != null){
                    bicAnalysis = request.getBooleanVal(RequestModel.BICANALYSIS, user);
                }
                String contactEmail = (String)getValueFromDataRecord(request, RequestModel.MAIL_TO, "String", user);
                DataRecord sample = samples[0];
                String recipe = (String) getValueFromDataRecord(sample, SampleModel.RECIPE, "String", user);
                if (isCmoRequest(contactEmail, recipe, bicAnalysis) || isPartOfExistingCmoStudy(projectId)) {
                    return String.format("%s is cmo request", requestId);
                }
                return  String.format("%s is not cmo request", requestId);
            }
            // if projectid is not passed to the endpoint, run for all requests in LIMS.
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "IsCmoRequest <> 0", user);
            log.info("Total Requests: " +  requests.size());
            for (DataRecord request : requests) {
                DataRecord[] samples = request.getChildrenOfType(SampleModel.DATA_TYPE_NAME, user);
                if (samples.length == 0) {
                    continue;
                }
                //get parent project's projectid for request.
                String requestId = (String)getValueFromDataRecord(request, RequestModel.REQUEST_ID, "String", user);
                String projectId = requestId.split("_")[0];
                boolean bicAnalysis = false;
                if(request.getValue(RequestModel.BICANALYSIS, user) != null){
                    bicAnalysis = request.getBooleanVal(RequestModel.BICANALYSIS, user);
                }
                String contactEmail = (String)getValueFromDataRecord(request, RequestModel.MAIL_TO, "String", user);
                DataRecord sample = samples[0];
                String sampleName = (String)getValueFromDataRecord(sample, SampleModel.OTHER_SAMPLE_ID, "String", user);
                String recipe = (String) getValueFromDataRecord(sample, SampleModel.RECIPE, "String", user);
                String sampleType = (String) getValueFromDataRecord(sample, SampleModel.EXEMPLAR_SAMPLE_TYPE, "String", user);
                log.info("projectId: " + projectId);
                log.info("bicAnalysis: " + bicAnalysis);
                log.info("contactEnail: " + contactEmail);
                log.info("sampleName: "+ sampleName);
                log.info("recipe: " + recipe);
                log.info("sampleType: " + sampleType);
                if (isCmoRequest(contactEmail, recipe, bicAnalysis) || isWesRecapture(recipe, sampleType, sampleName) || isPartOfExistingCmoStudy(projectId)) {
                    setIsCmoRequestTrue(projectId);
                }
            }
            dataRecordManager.storeAndCommit("Finished running CheckAndMarkCMORequests endpoint.", null, user);
        } catch (NotFound nf) {
            log.error(String.format("NotFound Exception while running CheckAndMarkCMORequests Task:\n%s", ExceptionUtils.getStackTrace(nf)));
        } catch (RemoteException re) {
            log.error(String.format("Remote Exception while running CheckAndMarkCMORequests Task:\n%s", ExceptionUtils.getStackTrace(re)));
        } catch (IoError ioe) {
            log.error(String.format("IoError while running CheckAndMarkCMORequests Task:\n%s", ExceptionUtils.getStackTrace(ioe)));
        } catch (ServerException se) {
            log.error(String.format("Server Exception while running CheckAndMarkCMORequests Task:\n%s", ExceptionUtils.getStackTrace(se)));
        }
        return String.format("IsCmoRequest set true for : %s", response);
    }

    /**
     * Method to check if request is CMO request.
     *
     * @param recipe
     * @param bicAnalysis
     * @return
     */
    protected static boolean isCmoRequest(String contactEmail, String recipe, boolean bicAnalysis) {
        //check if cmopm email is in contactEmail
        if (!StringUtils.isBlank(contactEmail) && contactEmail.toLowerCase().contains(CMO_PM_EMAIL)) {
            return true;
        }
        //check if recipe is cmo recipe and pipeline option is also checked.
        if (isCmoRecipe(recipe, bicAnalysis)) {
            return true;
        }
        return false;
    }

    /**
     * Method to check if request is WES recapture request
     *
     * @param sampleName
     * @param recipe
     * @param sampleType
     * @return
     */
    protected static boolean isWesRecapture(String recipe, String sampleType, String sampleName) {
        /**
         * CMO recapture should meet the following criteria as per IGO PM's:
         * 1. Sample submitted as DNA Library.
         * 2. Sample recipe is WholeExomeCapture.
         * 3. Sample samplename starts with "P-"
         */
       return recipe.equalsIgnoreCase(WHOLE_EXOME_RECIPE) && sampleType.equalsIgnoreCase(SampleType.DNA_LIBRARY.toString()) && sampleName.startsWith("P-");
    }

    /**
     * Method to check if analyst email is cmo analyst.
     * @param analystEmail
     */

    protected static boolean isCmoAnalystEmail(String analystEmail){
        List<String> emails = Arrays.asList(analystEmail.split(","));
        for (String email : emails){
            if (cmoAnalysts.contains(email.toLowerCase().trim())){
                return true;
            }
        }
        return false;
    }

    /**
     * Method to check if recipe on sample is CMO recipe
     *
     * @param recipe
     * @return
     */
    protected static boolean isCmoRecipe(String recipe, boolean bicAnalysis) {
        //check if cmo recipe, impact, hemepact or msk-access. The recipe validation is enough to call the request cmo request.
        if (!StringUtils.isBlank(recipe)) {
            recipe = recipe.toLowerCase();
            if (recipe.contains(IMPACT_RECIPE_VAL) || recipe.contains(HEMEMPACT_RECIPE_VAL) || recipe.contains(ACCESS_RECIPE_VAL)) {
                return true;
            }
            return cmoRecipes.contains(recipe) && bicAnalysis;
        }
        return false;
    }

    /**
     * Method to set a request and all other request in same project as CMO Requests.
     *
     * @param projectId
     */
    private void setIsCmoRequestTrue(String projectId) {
        try {
            String likeVal = projectId + "%";
            //get all requests for the parent project of the request.
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", RequestModel.REQUEST_ID + " LIKE '" + likeVal + "'", user);
            //set all requests as IsCmoRequest = true
            for (DataRecord req : requests) {
                String requestId = req.getStringVal(RequestModel.REQUEST_ID, user);
                boolean isCmoRequest = false;
                if(req.getValue("IsCmoRequest", user)!=null){
                    isCmoRequest = req.getBooleanVal("IsCmoRequest", user);
                }
                if(!isCmoRequest) {
                    req.setDataField("IsCmoRequest", true, user);
                    log.info("Setting Is CMO Request: " + requestId);
                }
            }
        } catch (InvalidValue invalidValue) {
            log.error(String.format("Invalid value Error while setting IsCmoRequest field on Request:\n%s", ExceptionUtils.getStackTrace(invalidValue)));
        } catch (RemoteException re) {
            log.error(String.format("Remote Exception error while setting IsCmoRequest field on Request:\n%s", ExceptionUtils.getStackTrace(re)));
        } catch (IoError ioError) {
            log.error(String.format("IoError Exception while setting IsCmoRequest field on Request:\n%s", ExceptionUtils.getStackTrace(ioError)));
        } catch (NotFound notFound) {
            log.error(String.format("NotFound Exception while setting IsCmoRequest field on Request:\n%s", ExceptionUtils.getStackTrace(notFound)));
        }
    }

    /**
     * Methdod to check if request if any other request in parent project is a CMO Request. In case we forgot to check
     * any cmo requests, this method will mark those requests when run.
     *
     * @param projectId
     * @return
     */
    private boolean isPartOfExistingCmoStudy(String projectId) {
        try {
            String likeVal = projectId + "%";
            //get all requests for the parent project of the request.
            List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", RequestModel.REQUEST_ID + " LIKE '" + likeVal + "'", user);
            // check if any other request under project is a cmo request.
            for (DataRecord req : requests) {
                boolean isCmoRequest = false;
                if(req.getValue("IsCmoRequest", user)!=null){
                    isCmoRequest = req.getBooleanVal("IsCmoRequest", user);
                }
                if (isCmoRequest) {
                    return true;
                }
            }
        } catch (IoError ioError) {
            log.error(String.format("IoError while validating if Request is part of existing CMO Study:\n%s", ExceptionUtils.getStackTrace(ioError)));
        } catch (RemoteException re) {
            log.error(String.format("Remote Exception while validating if Request is part of existing CMO Study:\n%s", ExceptionUtils.getStackTrace(re)));
        } catch (NotFound notFound) {
            log.error(String.format("NotFound Exception while validating if Request is part of existing CMO Study:\n%s", ExceptionUtils.getStackTrace(notFound)));
        }
        return false;
    }
}
