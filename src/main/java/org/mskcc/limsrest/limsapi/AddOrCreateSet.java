package org.mskcc.limsrest.limsapi;


import com.velox.api.datarecord.*;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.util.VeloxConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A queued task that takes requests and samples, and makes a SampleSet record in the lims with this information.
 * If there is a sample set already present with this set name, it will append the information to the existing one.
 *
 * @author Aaron Gabow
 */
@Service
public class AddOrCreateSet extends LimsTask {
    private final static Log log = LogFactory.getLog(GenerateSampleCmoIdTask.class);

    String[] requestIds;
    String[] igoIds;
    String[] pairs;
    String[] categories;
    String igoUser;
    String setName;
    String mapName;
    String baitSet;
    String recipe;
    String primeRequest;
    String[] externalSpecimens;

    public void init(String igoUser, String setName, String mapName, String[] requests, String[] igoIds,
                     String[] pairs, String[] categories, String baitSet, String primeRecipe, String primeRequest,
                     String[] externalSpecimens) {
        this.igoUser = igoUser;
        this.setName = setName;
        this.mapName = mapName;
        this.baitSet = baitSet;
        this.recipe = primeRecipe;
        this.primeRequest = primeRequest;
        if (requests != null)
            this.requestIds = requests.clone();
        if (igoIds != null)
            this.igoIds = igoIds.clone();
        if (externalSpecimens != null)
            this.externalSpecimens = externalSpecimens.clone();
        if (pairs != null)
            this.pairs = pairs.clone();
        if (categories != null)
            this.categories = categories.clone();
    }

    //execute the velox call
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Object execute(VeloxConnection conn) {
        if (requestIds == null && igoIds == null && pairs == null) {
            throw new RuntimeException("FAILURE: You must specify at least one request or sample or have new pairing " +
                    "info");
        }

        StringBuilder errorList = new StringBuilder();
        String recordId;

        try {
            List<DataRecord> allRequests = new LinkedList<>();
            List<DataRecord> allSamples = new LinkedList<>();

            if (pairs == null) {
                pairs = new String[0];
            }

            String[] tumorPairing = new String[pairs.length];
            String[] normalPairing = new String[pairs.length];

            for (int i = 0; i < pairs.length; i++) {
                String[] pairBreak = pairs[i].split(":");
                tumorPairing[i] = pairBreak[0];
                normalPairing[i] = pairBreak[1];
            }

            if (categories == null) {
                categories = new String[0];
            }

            String[] categoryKeys = new String[categories.length];
            String[] categoryVals = new String[categories.length];

            for (int i = 0; i < categories.length; i++) {
                String[] catBreak = categories[i].split(":");
                categoryKeys[i] = catBreak[0];
                categoryVals[i] = catBreak[1];
            }

            DataRecord parent, sampleSet = null;
            //if the service is just adding pairing and category information to an existing request
            if (requestIds != null && requestIds.length == 1 && setName == null) {
                log.info(String.format("Sample set name parameter was not passed. Only pairing and category " +
                        "information will be added to LIMS to request %s", requestIds[0]));

                parent = dataRecordManager.queryDataRecords("Request", "RequestId = '" + requestIds[0] + "'", user)
                        .get(0);
            } else {
                List<DataRecord> matchedSets = dataRecordManager.queryDataRecords("SampleSet", "Name = '" + setName +
                        "'", user);
                if (matchedSets.size() < 1) {
                    log.info(String.format("Sample set %s wasn't found in LIMS. It will be created", setName));
                    sampleSet = dataRecordManager.addDataRecord("SampleSet", user);
                    sampleSet.setDataField("Name", setName, user);
                } else {
                    log.info(String.format("Sample set %s  found in LIMS. It will be used", setName));
                    sampleSet = matchedSets.get(0);
                }

                parent = sampleSet;
            }

            Set<String> nameSet = new HashSet<>();
            addSampleIdsToNameSet(parent, nameSet);
            addPairings(tumorPairing, normalPairing, parent, nameSet);
            addCategories(categoryKeys, categoryVals, parent, nameSet);

            //if updating a request's pair information, we are done so save and return
            if (sampleSet == null) {
                log.info(String.format("Storing pairing and category information for request: %s", requestIds[0]));
                dataRecordManager.storeAndCommit(igoUser + " added pairing and category info to request " +
                        requestIds[0], user);
                return Long.toString(parent.getRecordId());
            }

            addExternalSpecimens(sampleSet, nameSet);
            if (requestIds != null) allRequests.addAll(addOffRequestId(errorList));

            if (igoIds != null && igoIds.length > 0) allSamples.addAll(addOffIgoIds(errorList));

            if (errorList.length() > 0) throw new LimsException(errorList.toString());

            log.info(String.format("Adding requests %s to sample set %s", StringUtils.join(requestIds, ","), setName));
            sampleSet.addChildren(allRequests, user);

            log.info(String.format("Adding samples %s to sample set %s", StringUtils.join(igoIds, ","), setName));
            sampleSet.addChildren(allSamples, user);

            if (baitSet != null) sampleSet.setDataField("BaitSet", baitSet, user);
            if (recipe != null) sampleSet.setDataField("Recipe", recipe, user);
            if (primeRequest != null) sampleSet.setDataField("PrimeRequest", primeRequest, user);

            dataRecordManager.storeAndCommit(igoUser + " added sample set info to sample set " + setName, user);
            recordId = Long.toString(sampleSet.getRecordId());

        } catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            log.error(e.getMessage(), e);

            return String.format("FAILURE: %s TRACE:%s", e.getMessage(), sw.toString());
        }

        return recordId;
    }

    private void addCategories(String[] categoryKeys, String[] categoryVals, DataRecord parent, Set<String> nameSet)
            throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        for (int i = 0; i < categoryKeys.length; i++) {
            if (!nameSet.contains(categoryKeys[i])) {
                throw new RuntimeException(String.format("FAILURE: Please confirm that %s is a known sample.",
                        categoryKeys[i]));
            }

            DataRecord categoryMap = parent.addChild("CategoryMap", user);
            categoryMap.setDataField("OtherSampleId", categoryKeys[i], user);
            categoryMap.setDataField("Category", categoryVals[i], user);
            categoryMap.setDataField("MapName", mapName, user);

            log.info(String.format("Added CategoryMap record to sample set %s", categoryKeys[i], setName));
        }
    }

    private void addPairings(String[] tumorPairing, String[] normalPairing, DataRecord parent, Set<String> nameSet)
            throws IoError, NotFound, AlreadyExists, InvalidValue, ServerException, RemoteException {
        for (int i = 0; i < normalPairing.length; i++) {
            validateIfPairingSamplesBelongToSet(tumorPairing[i], normalPairing[i], nameSet);

            DataRecord pairInfo = parent.addChild("PairingInfo", user);
            pairInfo.setDataField("TumorId", tumorPairing[i], user);
            pairInfo.setDataField("NormalId", normalPairing[i], user);

            log.info(String.format("Added pairing info for tumor: %s - normal: %s", tumorPairing[i], normalPairing[i]));
        }
    }

    private void validateIfPairingSamplesBelongToSet(String tumor, String normal, Set<String> nameSet) {
        if (!nameSet.contains(tumor)
                || !nameSet.contains(normal)) {
            throw new RuntimeException("FAILURE: Please confirm that " + tumor + " and " +
                    normal + " are known samples.");
        }
    }

    private void addExternalSpecimens(DataRecord sampleSet, Set<String> nameSet) throws NotFound, IoError,
            RemoteException, AlreadyExists, InvalidValue {
        if (externalSpecimens != null) {
            for (String externalSpecId : externalSpecimens) {
                nameSet.add(externalSpecId);
                List<DataRecord> extSpecRecords = dataRecordManager.queryDataRecords(VeloxConstants
                        .EXTERNAL_SPECIMEN, "ExternalId = '" + externalSpecId + "'", user);

                if (extSpecRecords.size() == 0) {
                    createNewExternalSpecimen(sampleSet, externalSpecId);
                } else if (extSpecRecords.size() > 1) {
                    DataRecord recToBeUsed = extSpecRecords.get(0);
                    log.warn(String.format("External Specimen with id %s is ambiguous and matches multiple LIMS " +
                            "Data Records. First one will be used: %d", externalSpecId, recToBeUsed));

                    sampleSet.addChild(recToBeUsed, user);
                } else {
                    log.info(String.format("External specimen %s already exists in LIMS. It will be added as a child " +
                            "to sample set %s", externalSpecId, sampleSet.getRecordId()));
                    sampleSet.addChild(extSpecRecords.get(0), user);
                }
            }
        }
    }

    private void createNewExternalSpecimen(DataRecord sampleSet, String externalSpecId) throws IoError, NotFound,
            AlreadyExists, InvalidValue, RemoteException {
        log.info(String.format("External Specimen with id %s doesn't exist in LIMS. New record will " +
                "be created.", externalSpecId));

        DataRecord newExternalSpecimen = dataRecordManager.addDataRecord(VeloxConstants
                .EXTERNAL_SPECIMEN, user);
        newExternalSpecimen.setDataField("ExternalId", externalSpecId, user);
        newExternalSpecimen.setDataField("DataRecordName", externalSpecId, user);
        sampleSet.addChild(newExternalSpecimen, user);
    }

    private void addSampleIdsToNameSet(DataRecord parent, Set<String> nameSet) throws RemoteException, ServerException {
        if (igoIds != null) {
            List<DataRecord> descSamples = parent.getDescendantsOfType("Sample", user);
            List<Object> names = dataRecordManager.getValueList(descSamples, "SampleId", user);
            for (Object name : names) {
                nameSet.add((String) name);
            }
            nameSet.addAll(Arrays.asList(igoIds));
        }
    }

    List<DataRecord> addOffRequestId(StringBuilder errorList) throws NotFound, IoError, RemoteException {
        String match = Arrays.stream(requestIds).map(r -> String.format("\'%s\'", r)).
                collect(Collectors.joining(",", "(", ")"));
        List<DataRecord> matchedReq = dataRecordManager.queryDataRecords("Request", "RequestId in " +
                match, user);

        validateMatch(errorList, matchedReq, requestIds, "RequestId");

        return matchedReq;
    }

    List<DataRecord> addOffIgoIds(StringBuilder errorList) throws NotFound, IoError, RemoteException {
        String match = Arrays.stream(igoIds).map(s -> String.format("\'%s\'", s)).
                collect(Collectors.joining(",", "(", ")"));
        List<DataRecord> matchedSamples = dataRecordManager.queryDataRecords("Sample", "SampleId in " +
                match, user);
        validateMatch(errorList, matchedSamples, igoIds, "SampleId");
        return matchedSamples;
    }

    private void validateMatch(StringBuilder errorList, List<DataRecord> matchedRecords,
                               String[] recIds, String idField) throws NotFound, RemoteException {
        if (matchedRecords.size() < recIds.length) {
            Set<String> matchedIds = new HashSet<>();
            for (DataRecord matchedSample : matchedRecords) {
                matchedIds.add(matchedSample.getStringVal(idField, user));
            }
            for (String recordId : recIds) {
                if (!matchedIds.contains(recordId)) {
                    errorList.append("FAILURE: There is no record matching requested id ").append(recordId);
                }
            }
        } else if (matchedRecords.size() > recIds.length) {
            errorList.append("FAILURE: There are more records matching those ids than is expected");
        }
    }

}
