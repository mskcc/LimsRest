package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.servermanager.PickListManager;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.recmodels.RequestModel;
import com.velox.sloan.cmo.recmodels.SampleCMOInfoRecordsModel;
import com.velox.sloan.cmo.recmodels.SeqAnalysisSampleQCModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.sampletracker.WESSampleData;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mskcc.limsrest.util.Utils.*;

/**
 * Task to compile data for the delphi-sample-tracker app. This endpoint currently collects data for all the entries in DMPSampleTracker for "WholeExomeSequencing" and retrieves information from
 * CVR db, oncotree db and LIMS.
 *
 * @author sharmaa1
 */
public class GetWESSampleDataTask {
    private final List<String> HISEQ_2000_MACHINE_NAMES = Arrays.asList("LIZ", "LOLA");
    private final List<String> HISEQ_2500_MACHINE_NAMES = Arrays.asList("KIM", "MOMO");
    private final List<String> HISEQ_4000_MACHINE_NAMES = Arrays.asList("PITT", "JAX", "BRAD");
    private final List<String> MISEQ_MACHINE_NAMES = Arrays.asList("AYYAN", "JOHNSAWYERS", "TOMS", "VIC");
    private final List<String> NOVASEQ_MACHINE_NAMES = Arrays.asList("MICHELLE", "DIANA", "A00227", "A00333");
    private final List<String> NEXTSEQ_MACHINE_NAMES = Collections.singletonList("SCOTT");

    private Log log = LogFactory.getLog(GetWESSampleDataTask.class);
    private String timestamp;
    private String timestamp2;
    private ConnectionLIMS conn;
    private User user;
    DataRecordManager dataRecordManager;

    private List<String> VALID_RECIPES;
    private List<String> VALID_REQUEST_TYPES;

    public GetWESSampleDataTask(String timestamp, String timestamp2, ConnectionLIMS conn) {
        this.timestamp = timestamp;
        this.timestamp2 = timestamp2;
        this.conn = conn;
    }

    public List<WESSampleData> execute() {
        long start = System.currentTimeMillis();
        List<WESSampleData> resultList = new ArrayList<>();
        try {
            VeloxConnection vConn = conn.getConnection();
            user = vConn.getUser();
            dataRecordManager = vConn.getDataRecordManager();
            PickListManager pickListManager = vConn.getDataMgmtServer().getPickListManager(user);
            VALID_RECIPES = pickListManager.getPickListConfig("Whole-Exome Recipes for Sample Tracker").getEntryList();
            log.info(VALID_RECIPES);
            VALID_REQUEST_TYPES = pickListManager.getPickListConfig("Whole-Exome Request Types").getEntryList();
            if(timestamp2.equals("NULL")){
                timestamp2 = "" + System.currentTimeMillis();
            }
            try {
                if (Long.parseLong(timestamp) > Long.parseLong(timestamp2)){
                    String timestamp_cp = timestamp2;
                    timestamp2 = timestamp;
                    timestamp = timestamp_cp;
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                return null;
            }
            log.info(" Starting GetWesSample task using timestamp " + timestamp + " and timestamp2 " + timestamp2);
            List<DataRecord> dmpTrackerRecords = new ArrayList<>();
            try {
                dmpTrackerRecords = dataRecordManager.queryDataRecords("DMPSampleTracker", "i_SampleTypeTumororNormal='Tumor' AND DateCreated > " + Long.parseLong(timestamp) + "AND DateCreated < " + Long.parseLong(timestamp2) + " AND i_SampleDownstreamApplication LIKE '%Exome%' COLLATE utf8_general_ci", user);
//                dmpTrackerRecords = dataRecordManager.queryDataRecords("DMPSampleTracker", "i_SampleTypeTumororNormal='Tumor' AND DateCreated > " + Long.parseLong(timestamp) + " AND i_SampleDownstreamApplication LIKE '%Exome%' AND i_StudySampleIdentifierInvesti LIKE 'P-0002976-T01-WES%' COLLATE utf8_general_ci", user);
                //dmpTrackerRecords = dataRecordManager.queryDataRecords("DMPSampleTracker", "i_StudySampleIdentifierInvesti IN ('P-0013536-T02-WES','P-0025596-T01-WES','P-0028625-T01-WES') AND i_SampleTypeTumororNormal='Tumor'" + " AND i_SampleDownstreamApplication LIKE '%Exome%' COLLATE utf8_general_ci", user);
                log.info("Num dmpTracker Records: " + dmpTrackerRecords.size());
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                return null;
            }
            JSONObject consentAList = getConsentStatusDataValues("parta");
            JSONObject consentCList = getConsentStatusDataValues("partc");
            if (!dmpTrackerRecords.isEmpty()) {
                int totalProcessed = 0;
                for (DataRecord dmpTrackRec : dmpTrackerRecords) {
                    List<DataRecord> sampleCmoInfoRecs = new ArrayList<>();
                    if (dmpTrackRec.getValue("i_StudySampleIdentifierInvesti", user) != null) {
                        sampleCmoInfoRecs = dataRecordManager.queryDataRecords("SampleCMOInfoRecords", "UserSampleID = '" + dmpTrackRec.getStringVal("i_StudySampleIdentifierInvesti", user) + "'", user);
                        log.info("sample cmo info query end");
                    }
                    if (sampleCmoInfoRecs.size() > 0) {
                        for (DataRecord cmoInfoRec : sampleCmoInfoRecs) {
                            List<DataRecord> parentSamps = cmoInfoRec.getParentsOfType("Sample", user);
                            if (parentSamps.isEmpty()){
                                log.info(String.format("%s record with recordid %d not linked to any parent Sample.", SampleCMOInfoRecordsModel.DATA_TYPE_NAME, cmoInfoRec.getRecordId()));
                            }
                            List<DataRecord> allSamplesSharingCmoInfoRec = !parentSamps.isEmpty() ? getChildSamplesWithRequestAsParent(parentSamps.get(0)): new ArrayList<>();
                            log.info("Total Wes Samples for shared CmoInfo Rec: " + allSamplesSharingCmoInfoRec.size());
                            if (allSamplesSharingCmoInfoRec.size()>0){
                                for (DataRecord sample: allSamplesSharingCmoInfoRec){
                                    log.info("processing sample: " + sample.getStringVal("SampleId", user)+ ", recipe: "+ sample.getStringVal("Recipe", user));
                                    DataRecord request = getRelatedRequest(sample);
                                    if (isValidRecipeToProcess(sample)) {
                                        String sampleId = sample.getStringVal("SampleId", user);
                                        log.info("DMP Record with related IGO Samples.");
                                        log.info("Sample ID: " + sampleId);
                                        String userSampleId = dmpTrackRec.getStringVal("i_StudySampleIdentifierInvesti", user);
                                        log.info("User Sample ID:" + userSampleId);
                                        String userSampleidHistorical = (String) getValueFromDataRecord(dmpTrackRec, "InvestigatorSampleIdHistorical", "String", user);
                                        String altId = (String) getValueFromDataRecord(sample, "AltId", "String", user);
                                        log.info("AltId: " + altId);
                                        String duplicateSample = (String) getValueFromDataRecord(dmpTrackRec, "DuplicateSample", "String", user);
                                        String wesSampleid = (String) getValueFromDataRecord(dmpTrackRec, "WesId", "String", user);
                                        String cmoSampleId = cmoInfoRec.getStringVal("CorrectedCMOID", user);
                                        String cmoPatientId = cmoInfoRec.getStringVal("CmoPatientId", user);
                                        String dmpSampleId = dmpTrackRec.getStringVal("i_DMPSampleID", user);
                                        JSONObject cvrData = getCvrData(dmpSampleId);
                                        String dmpPatientId = getCvrDataValue(cvrData, "dmp_patient_lbl");
                                        String mrn = getCvrDataValue(cvrData, "mrn");
                                        String sex = getCvrDataValue(cvrData, "gender");
                                        String sampleClass = getCvrDataValue(cvrData, "sample_type");
                                        String tumorType = getCvrDataValue(cvrData, "tumor_type");
                                        String parentalTumorType = getOncotreeTumorType(tumorType);
                                        String tissueSite = getCvrDataValue(cvrData, "primary_site");
                                        String sourceDnaType = (String) getValueFromDataRecord(dmpTrackRec, "i_SpecimenType", "String", user);
                                        String molAccessionNum = getCvrDataValue(cvrData, "molecular_accession_num");
                                        String dateDmpRequest = (String) getValueFromDataRecord(dmpTrackRec, "i_DateSubmittedtoDMP", "Date", user);
                                        String dmpRequestId = dmpTrackRec.getStringVal("i_RequestReference", user);
                                        String igoRequestId = (String) getValueFromDataRecord(request, "RequestId", "String", user);
                                        String collectionYear = (String) getValueFromDataRecord(cmoInfoRec, "CollectionYear", "String", user);
                                        String dateIgoReceived = (String) getValueFromDataRecord(request, "ReceivedDate", "Date", user);
                                        String igoCompleteDate = (String) getValueFromDataRecord(request, "CompletedDate", "Date", user);
                                        String applicationRequested = (String) getValueFromDataRecord(request, "RequestName", "String", user);
                                        String sequencerType = getSequencerTypeUsed(sample);
                                        String projectTitle = (String) getValueFromDataRecord(dmpTrackRec, "i_Studyname", "String", user);
                                        String labHead = (String) getValueFromDataRecord(request, "LaboratoryHead", "String", user);
                                        String ccFund = (String) getValueFromDataRecord(dmpTrackRec, "i_FundCostCenter", "String", user);
                                        String scientificPi = (String)getValueFromDataRecord(dmpTrackRec, "i_PrimaryInvestigator", "String", user);
                                        Boolean consentPartAStatus = getConsentStatus(consentAList, dmpPatientId);
                                        Boolean consentPartCStatus = getConsentStatus(consentCList, dmpPatientId);
                                        String sampleStatus = getMostAdvancedLimsStage(sample, igoRequestId, conn);
                                        log.info("sample status: " + sampleStatus);
                                        List<DataRecord> seqQcRecords = getChildDataRecordsOfType(sample, SeqAnalysisSampleQCModel.DATA_TYPE_NAME, user);
                                        String baitsetUsed = getBaitSet(sample, seqQcRecords, user);
                                        log.info("baitset: " + baitsetUsed);
                                        String accessLevel = "";
                                        String sequencingSite = "";
                                        String piRequestDate = "";
                                        String tempoPipelineQcStatus = (String) getValueFromDataRecord(dmpTrackRec, "TempoPipelineStatus", "String", user);
                                        log.info("Tempo Pipeline Status: " + tempoPipelineQcStatus);
                                        String tempoOutputDeliveryDate =(String) getValueFromDataRecord(dmpTrackRec, "TempoOutputDeliveryDate", "Date", user);
                                        String dataCustodian = (String) getValueFromDataRecord(dmpTrackRec, "DataCustodian", "String", user);
                                        String tissueType = "";
                                        String limsSampleRecordId = String.valueOf(sample.getLongVal("RecordId", user));
                                        String limsTrackerRecordId = String.valueOf(dmpTrackRec.getLongVal("RecordId", user));
                                        log.info("Lims Tracker Rec ID: " + limsTrackerRecordId);
                                        resultList.add(new WESSampleData(sampleId, userSampleId, userSampleidHistorical, altId, duplicateSample, wesSampleid,
                                                cmoSampleId, cmoPatientId, dmpSampleId, dmpPatientId, mrn, sex, sampleClass, tumorType,
                                                parentalTumorType, tissueSite, sourceDnaType, molAccessionNum, collectionYear, dateDmpRequest,
                                                dmpRequestId, igoRequestId, dateIgoReceived, igoCompleteDate, applicationRequested, baitsetUsed,
                                                sequencerType, projectTitle, labHead, ccFund, scientificPi, consentPartAStatus, consentPartCStatus,
                                                sampleStatus, accessLevel, sequencingSite, piRequestDate, tempoPipelineQcStatus,
                                                tempoOutputDeliveryDate, dataCustodian, tissueType, limsSampleRecordId,
                                                limsTrackerRecordId));
                                    }
                                }
                            } else {
                                WESSampleData nonIgoTrackingRecord = createNonIgoTrackingRecord(dmpTrackRec, consentAList, consentCList);
                                resultList.add(nonIgoTrackingRecord);
                            }
                        }
                    } else {
                        WESSampleData nonIgoTrackingRecord = createNonIgoTrackingRecord(dmpTrackRec, consentAList, consentCList);
                        resultList.add(nonIgoTrackingRecord);
                    }
                    totalProcessed ++;
                    log.info("Total DMPTracker records processed: " + totalProcessed);
                }
            }
            log.info("Results found: " + resultList.size() + " Elapsed time (ms): " + (System.currentTimeMillis() - start));
            return resultList;
        } catch (RemoteException e) {
            log.info(String.format("RemoteException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        }
        catch (NoSuchAlgorithmException e) {
            log.info(String.format("NoSuchAlgorithmException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        } catch (KeyManagementException e) {
            log.info(String.format("KeyManagementException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        }
        catch (MalformedURLException e) {
            log.info(String.format("MalformedURLException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        }
        catch (NotFound notFound) {
            log.info(String.format("NotFound while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(notFound)));;
        }
        catch (IOException e) {
            log.info(String.format("IOException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        }
        catch (IoError ioError) {
            log.info(String.format("ioError while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(ioError)));
        }
        catch (ServerException e) {
            log.info(String.format("ServerException while running GetWESSampleDataTask with timestamp %s and timestamp2 %s:\n%s", timestamp, timestamp2, getStackTrace(e)));
        }
        return resultList;
    }

    /**
     * Create the WESSampleData record for entries in DMPSampleTracker that are not processed in IGO.
     *
     * @param dmpTrackRec
     * @param consentAList
     * @param consentCList
     * @return WESSampleData
     * @throws NotFound
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private WESSampleData createNonIgoTrackingRecord(DataRecord dmpTrackRec, JSONObject consentAList, JSONObject consentCList) throws NotFound, IOException, KeyManagementException, NoSuchAlgorithmException {
        String sampleId = "";
        log.info("DMP Record without related IGO Samples.");
        log.info("Sample ID: " + sampleId);
        String userSampleId = dmpTrackRec.getStringVal("i_StudySampleIdentifierInvesti", user);
        log.info("User Sample ID:" + userSampleId);
        String userSampleidHistorical = (String) getValueFromDataRecord(dmpTrackRec, "InvestigatorSampleIdHistorical", "String", user);
        String altId = "";
        log.info("AltId: " + altId);
        String duplicateSample = (String) getValueFromDataRecord(dmpTrackRec,"DuplicateSample", "String", user);
        String wesSampleid = (String) getValueFromDataRecord(dmpTrackRec,"WesId", "String", user);
        String cmoSampleId = "";
        String cmoPatientId = "";
        String dmpSampleId = (String) getValueFromDataRecord(dmpTrackRec,"i_DMPSampleID", "String", user);;
        if (dmpTrackRec.getValue("i_DMPSampleID", user) != null) {
            dmpSampleId = dmpTrackRec.getStringVal("i_DMPSampleID", user);
        }
        JSONObject cvrData = getCvrData(dmpSampleId);
        String dmpPatientId = getCvrDataValue(cvrData, "dmp_patient_lbl");
        String mrn = getCvrDataValue(cvrData, "mrn");
        String sex = getCvrDataValue(cvrData, "gender");
        String sampleClass = getCvrDataValue(cvrData, "sample_type");
        String tumorType = getCvrDataValue(cvrData, "tumor_type");
        String parentalTumorType = getOncotreeTumorType(tumorType);
        String tissueSite = getCvrDataValue(cvrData, "primary_site");
        String sourceDnaType = (String) getValueFromDataRecord(dmpTrackRec, "i_SpecimenType", "String", user);
        String molAccessionNum = getCvrDataValue(cvrData, "molecular_accession_num");
        String dateDmpRequest = (String) getValueFromDataRecord(dmpTrackRec, "i_DateSubmittedtoDMP", "Date", user);
        String dmpRequestId = dmpTrackRec.getStringVal("i_RequestReference", user);
        String igoRequestId = "";
        String collectionYear = "";
        String dateIgoReceived = "";
        String igoCompleteDate = "";
        String applicationRequested = (String) getValueFromDataRecord(dmpTrackRec, "i_SampleDownstreamApplication", "String", user);
        String baitsetUsed = "";
        log.info("baitset: " + baitsetUsed);
        String sequencerType = "";
        String projectTitle = (String) getValueFromDataRecord(dmpTrackRec, "i_Studyname", "String", user);
        String labHead = (String) getValueFromDataRecord(dmpTrackRec, "i_PrimaryInvestigator", "String", user);
        String ccFund = (String) getValueFromDataRecord(dmpTrackRec, "i_FundCostCenter", "String", user);
        String scientificPi = (String)getValueFromDataRecord(dmpTrackRec, "i_PrimaryInvestigator", "String", user);
        Boolean consentPartAStatus = getConsentStatus(consentAList, dmpPatientId);
        Boolean consentPartCStatus = getConsentStatus(consentCList, dmpPatientId);
        String sampleStatus = "";
        log.info("sample status: " + sampleStatus);
        String accessLevel = "";
        String sequencingSite = "";
        String piRequestDate = "";
        String tempoPipelineQcStatus = (String) getValueFromDataRecord(dmpTrackRec, "TempoPipelineStatus", "String", user);
        log.info("Tempo Pipeline Status: " + tempoPipelineQcStatus);
        String tempoOutputDeliveryDate =(String) getValueFromDataRecord(dmpTrackRec, "TempoOutputDeliveryDate", "Date", user);
        String dataCustodian = (String) getValueFromDataRecord(dmpTrackRec, "DataCustodian", "String", user);
        String tissueType = "";
        String limsSampleRecordId = "";
        String limsTrackerRecordId = String.valueOf(dmpTrackRec.getLongVal("RecordId", user));
        log.info("Lims Tracker Rec ID: " + limsTrackerRecordId);
        return new WESSampleData(sampleId, userSampleId, userSampleidHistorical, altId, duplicateSample, wesSampleid,
                cmoSampleId, cmoPatientId, dmpSampleId, dmpPatientId, mrn, sex, sampleClass, tumorType,
                parentalTumorType, tissueSite, sourceDnaType, molAccessionNum, collectionYear, dateDmpRequest,
                dmpRequestId, igoRequestId, dateIgoReceived, igoCompleteDate, applicationRequested, baitsetUsed,
                sequencerType, projectTitle, labHead, ccFund, scientificPi, consentPartAStatus, consentPartCStatus,
                sampleStatus, accessLevel, sequencingSite, piRequestDate, tempoPipelineQcStatus,
                tempoOutputDeliveryDate, dataCustodian, tissueType, limsSampleRecordId,
                limsTrackerRecordId);
    }

    /**
     * Method to get all the child Samples directly under request as child and having a valid Whole Exome recipe.
     * @param sample
     * @return
     * @throws NotFound
     * @throws RemoteException
     * @throws IoError
     */
    private List<DataRecord> getChildSamplesWithRequestAsParent(DataRecord sample) throws NotFound, RemoteException, IoError {
        List<DataRecord> descendantSamples = sample.getDescendantsOfType("Sample", user);
        log.info("Total Descendant Samples: " + descendantSamples.size());
        List<DataRecord> sampleList = new ArrayList<>();
        if (sample.getParentsOfType("Request", user).size()>0){
            sampleList.add(sample);
        }
            if (descendantSamples.size()>0){
                for (DataRecord rec : descendantSamples){
                    boolean sampleHasRequestAsParent = rec.getParentsOfType("Request", user).size() > 0;
                    if (sampleHasRequestAsParent){
                        sampleList.add(rec);
                    }
                }
            }
        return sampleList;
    }
    /**
     * Get Request DataRecord for a Sample.
     *
     * @param sample
     * @return DataRecord
     * @throws RemoteException
     * @throws NotFound
     */
    private DataRecord getRelatedRequest(DataRecord sample) throws RemoteException, NotFound {
        try {
            if (sample.getParentsOfType("Request", user).size() > 0) {
                return sample.getParentsOfType("Request", user).get(0);
            }
            Stack<DataRecord> sampleStack = new Stack<>();
            if (sample.getParentsOfType("Sample", user).size() > 0) {
                sampleStack.push(sample.getParentsOfType("Sample", user).get(0));
            }
            do {
                DataRecord startSample = sampleStack.pop();
                if (startSample.getParentsOfType("Request", user).size() > 0) {
                    return startSample.getParentsOfType("Request", user).get(0);
                } else if (startSample.getParentsOfType("Sample", user).size() > 0) {
                    sampleStack.push(startSample.getParentsOfType("Sample", user).get(0));
                }
            } while (!sampleStack.isEmpty());
        } catch (Exception e) {
            log.error(String.format("Error occured while finding parent Request for Sample %s\n%s", sample.getStringVal("SampleId", user), Arrays.toString(e.getStackTrace())));
        }
        return null;
    }

    /**
     * Check if the recipe on the Sample is valid to create WESSampleData Object.
     *
     * @param sample
     * @return Boolean
     * @throws NotFound
     * @throws RemoteException
     */
    private Boolean isValidRecipeToProcess(DataRecord sample) throws NotFound, RemoteException {
        String sampleId = sample.getStringVal("SampleId", user);
        try {
            Object recipe = sample.getValue("Recipe", user);
            if (recipe != null) {
                log.info("Recipe: " + recipe);
                for (String rec : VALID_RECIPES) {
                    if (String.valueOf(recipe).toLowerCase().trim().equals(rec.toLowerCase().trim())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(String.format("Error occured while validating Recipe for Sample %s\n%s", sampleId, Arrays.toString(e.getStackTrace())));
        }
        return false;
    }

    /**
     * Check if the recipe on the Sample is valid to create WESSampleData Object.
     *
     * @param request
     * @return Boolean
     * @throws NotFound
     * @throws RemoteException
     */
    private boolean isValidRequestToProcess(DataRecord request){
        try {
            Object requestName = request.getValue(RequestModel.REQUEST_NAME, user);
            log.info("Request Type: " + requestName);
            if (requestName == null){
                throw new NullPointerException(String.format("%s field is missing value for %s", RequestModel.DATA_TYPE_NAME, request.getDataTypeName()));
            }
            if (VALID_REQUEST_TYPES.contains(requestName.toString())) {
                return true;
            }
        }
        catch (RemoteException e) {
            log.error(String.format("Error occured while validating Request Type for request with Record ID %d\n%s", request.getRecordId(), Arrays.toString(e.getStackTrace())));
        } catch (NotFound notFound) {
            log.error(String.format("Error occured while validating Request Type for request with Record ID %d\n%s", request.getRecordId(), Arrays.toString(notFound.getStackTrace())));
        }
        return false;
    }

    /**
     * Method to get data from cvr endpoint using dmpSampleId.
     *
     * @param dmpSampleId
     * @return JSONObject
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     */
    private JSONObject getCvrData(String dmpSampleId) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        StringBuilder response = new StringBuilder();
        JSONObject cvrResponseData = new JSONObject();
        // Install the all-trusting host verifier
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            URL url = new URL("https://draco.mskcc.org:9898/get_cmo_metadata/" + dmpSampleId);
            log.info(url.toString());
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            cvrResponseData = new JSONObject(response.toString());
        } catch (Exception e) {
            log.error(String.format("Error occured while querying CVR end point for DMP Sample ID %s\n%s", dmpSampleId, Arrays.toString(e.getStackTrace())));
        }
        return cvrResponseData;
    }

    /**
     * Get value from CVR data using Json Key.
     *
     * @param cvrResponseData
     * @param key
     * @return String
     */
    private String getCvrDataValue(JSONObject cvrResponseData, String key) {
        if (cvrResponseData.length() > 0) {
            if (cvrResponseData.has(key) && cvrResponseData.get(key) != null) {
                return (String) cvrResponseData.get(key);
            }
        }
        return "Not found";
    }

    /**
     * Get the sequencer types that were used to process the samples.
     *
     * @param sample
     * @return String
     * @throws RemoteException
     * @throws NotFound
     */
    private String getSequencerTypeUsed(DataRecord sample) throws RemoteException, NotFound {
        List<DataRecord> sampleLevelSeqQcRecs = sample.getDescendantsOfType("SeqAnalysisSampleQC", user);
        Set<String> sequencerTypes = new HashSet<>();
        if (sampleLevelSeqQcRecs.size() > 0) {
            for (DataRecord record : sampleLevelSeqQcRecs) {
                if (record.getValue("SequencerRunFolder", user) != null) {
                    sequencerTypes.add(getSequencerType(record.getStringVal("SequencerRunFolder", user).split("_")[0]));
                }
            }
        }
        return String.join(",", sequencerTypes);
    }

    /**
     * Get Sequencer type based on Sequencing machine name.
     *
     * @param machineName
     * @return String
     */
    private String getSequencerType(String machineName) {
        if (HISEQ_2000_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "HiSeq 2000";
        }
        if (HISEQ_2500_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "HiSeq 2500";
        }
        if (HISEQ_4000_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "HiSeq 4000";
        }
        if (MISEQ_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "MiSeq";
        }
        if (NOVASEQ_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "NovaSeq";
        }
        if (NEXTSEQ_MACHINE_NAMES.contains(machineName.toUpperCase())) {
            return "NextSeq";
        }
        return "NA";
    }

    /**
     * Get all the patients consent statuses for a consent type(PartA or PartC).
     *
     * @param consentType
     * @return JSONObject
     * @throws MalformedURLException
     * @throws JSONException
     */
    private JSONObject getConsentStatusDataValues(String consentType) throws MalformedURLException, JSONException {
        StringBuilder response = new StringBuilder();
        JSONObject cvrResponseData = new JSONObject();
        URL url;
        if (consentType.toLowerCase().equals("parta")) {
            url = new URL("http://draco.mskcc.org:9890/get_12245_list_parta");
        } else {
            url = new URL("http://draco.mskcc.org:9890/get_12245_list_partc");
        }
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            cvrResponseData = new JSONObject(response.toString());
        } catch (Exception e) {
            log.error(String.format("Error occured while querying consent '%s' end point.\n", consentType));
        }
        return cvrResponseData.getJSONObject("cases");
    }

    /**
     * Get consent status for a patient.
     *
     * @param consentData
     * @param patientId
     * @return Boolean
     */
    private Boolean getConsentStatus(JSONObject consentData, String patientId) {
        if (consentData.has(patientId)) {
            return (Boolean) consentData.get(patientId);
        }
        return false;
    }
}
