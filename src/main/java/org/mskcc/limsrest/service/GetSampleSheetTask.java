package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import org.mskcc.limsrest.ConnectionLIMS;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.shared.exceptions.WorkflowException;
import com.velox.sapioutils.shared.managers.DataRecordUtilManager;
import com.velox.sapioutils.shared.managers.ManagerContext;
import com.velox.sapioutils.shared.managers.SampleUtilManager;
import com.velox.sapioutils.shared.utilities.DataRecordMapPair;
import com.velox.sapioutils.shared.utilities.ExemplarConfig;
import com.velox.sloan.cmo.staticstrings.datatypes.*;
import com.velox.sloan.cmo.utilities.CustomWorkflowException;
import com.velox.sloan.cmo.utilities.FileTemplateUtility;
import com.velox.util.CsvHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.mskcc.limsrest.util.illuminaseq.BarcodeMismatch;
import org.mskcc.limsrest.util.illuminaseq.NovaSeqXSamplesheetGenerator;
import org.mskcc.limsrest.util.illuminaseq.SampleData;
import org.mskcc.limsrest.util.illuminaseq.SampleSheetParser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.lang.ProcessBuilder;
import java.lang.Process;

import org.mskcc.limsrest.util.illuminaseq.Barcode;

public class GetSampleSheetTask {

    private static final String INSTRUMENT_NEXTSEQ = "NextSeq";
    private static final String INSTRUMENT_HISEQ4000 = "HiSeq4000";
    private static final String INSTRUMENT_NOVASEQ = "NovaSeq";
    private static final String DLP_RECIPE = "sc_dlp";
    private static final String SMARTSEQ_RECIPE = "SC_SmartSeq";
    private static final String VALUE_UNAVAILABLE = "UNKNOWN";
    private static final String HI_SEQ_4000_DATE = "Date";
    private static final String HI_SEQ_4000_APPLICATION = "Application";
    private static final String HI_SEQ_4000_READS1 = "Reads1";
    private static final String HI_SEQ_4000_READS2 = "Reads2";
    private static final String HI_SEQ_4000_MISMATCH1 = "BarcodeMismatchesIndex1";
    private static final String HI_SEQ_4000_MISMATCH2 = "BarcodeMismatchesIndex2";

    private static final String HI_SEQ_4000_COL_LANE = "Lane";
    private static final String HI_SEQ_4000_COL_SAMPLE_ID = "Sample_ID";
    private static final String HI_SEQ_4000_COL_SAMPLE_PLATE = "Sample_Plate";
    private static final String HI_SEQ_4000_COL_SAMPLE_WELL = "Sample_Well";
    private static final String HI_SEQ_4000_COL_INDEX_ID = "Index_ID";
    private static final String HI_SEQ_4000_COL_INDEX_TAG = "Index_Tag";
    private static final String HI_SEQ_4000_COL_INDEX_TAG2 = "Index_Tag2";
    private static final String HI_SEQ_4000_COL_SAMPLE_PROJECT = "Sample_Project";
    private static final String HI_SEQ_4000_COL_BAIT_SET = "Bait_Set";
    private static final String HI_SEQ_4000_COL_DESCRIPTION = "Description";
    private static final String IDT_P5_BARCODE_NOT_DUAL_10 = "ACCGAGATCT"; // confirmed with Cassidy 8/15, 2025 //
    private static final String IDT_P5_BARCODE_NOT_DUAL_8 = "CGAGATCT"; // confirmed with Cassidy Sep 13, 2019 //
                                                                        // forward orientation
    private List<DataRecordMapPair> samples;
    private List<DataRecord> assignedLanes;
    private DataRecord experiment;
    private String flowCellId;
    private String instrument;
    private boolean isNova = false;
    private boolean isNovaX = false;
    // allow mixed single & dual barcoded samples to have a sample sheet setup with
    // single indexes
    private boolean ignoreDualBarcodes;
    private String experimentID;
    NovaSeqXSamplesheetGenerator novaSeqXSamplesheetGenerator;
    ConnectionLIMS conn;
    DataRecordManager dataRecordManager;
    DataRecordUtilManager dataRecordUtilManager;
    ManagerContext managerContext;
    User user;

    private void logInfo(String message) {
        System.out.println(message);
    }

    public GetSampleSheetTask(String experimentID, ConnectionLIMS conn) {
        this.experimentID = experimentID;
        this.conn = conn;
        user = conn.getConnection().getUser();
    }

    public Map<String, Object> execute() throws Throwable {
        // Initialize result map
        Map<String, Object> result = new HashMap<>();
        managerContext = new VeloxStandaloneManagerContext(user, this.conn.getConnection().getDataMgmtServer());

        dataRecordUtilManager = new DataRecordUtilManager(managerContext);
        DataRecordManager dataRecordManager = managerContext.getDataRecordManager();
        this.dataRecordManager = dataRecordManager;
        List<DataRecord> experiments = dataRecordManager.queryDataRecords("IlluminaSeqExperiment",
                "RecordId = " + experimentID, user);
        experiment = experiments.get(0);

        // get some values from the experiment that are needed for the sample sheet like
        // the flow cell and run type
        flowCellId = experiment.getStringVal("FlowcellId", user);
        System.out.println("Generating sample sheet for flow cell: " + flowCellId);

        DataRecord[] reads = experiment.getChildrenOfType("HiSeqRead", user);
        String isIndexed = "";
        for (DataRecord read : reads) {
            if (read.getBooleanVal("IsIndexedRead", user)) {
                isIndexed = "- Indexed";
                break;
            }
        }

        String runId = getRunId();

        ExemplarConfig exemplarConfig = new ExemplarConfig(managerContext);
        String sampleSheetPath = (String) exemplarConfig.getConfigMap().get("SampleSheetPath");
        String sampleSheetPathDragen;
        if (sampleSheetPath == null || sampleSheetPath.isEmpty()) {
            throw new WorkflowException("Sample Sheet Path is blank on Exemplar Config.");
        }
        sampleSheetPath = sampleSheetPath.replaceAll("\\\\", "/");
        if (!(sampleSheetPath.endsWith("/"))) {
            sampleSheetPath += '/';
        }
        sampleSheetPathDragen = sampleSheetPath + "SampleSheetDRAGEN_" + runId + ".csv";
        sampleSheetPath += "SampleSheet_" + runId + ".csv";
        System.out.println("Sample Sheet output set to: " + sampleSheetPath);

        if (sampleSheetPath == null || sampleSheetPath.isEmpty())
            throw new WorkflowException("Please specify a path to which the Sample Sheet should get written to!");

        PrintWriter outputStream = null;
        try {
            File file = new File(sampleSheetPath);

            // get the flow cell lanes
            // XXX get samples differently, depending on the invocation of this plugin
            assignedLanes = getLanesFromExperiment(experiment).subList(4,5);
            // get the experiments samples
            List<DataRecord> samples_RecList = new ArrayList<DataRecord>();
            //TODO: Remove this once testing is
            //Only Get Third Lane
            

            for (DataRecord assignedLane : assignedLanes) {
                samples_RecList.addAll((assignedLane.getParentsOfType(DT_Sample.DATA_TYPE, user)));
            }

            if (samples_RecList.isEmpty()) {
                throw new WorkflowException("No samples attached to task.");
            }

            samples = DataRecordMapPair.toDataRecordMapPairList(samples_RecList, managerContext);
            instrument = (String) experiment.getDataField("SequencerInstrument", user);

            // *** VERY IMPORTANT STEP BELOW ***
            String sampleSheet = generateSampleSheet(flowCellId, runId);
            if (sampleSheet == null)
                return null;
            String filename = new java.io.File(sampleSheetPath).getName();

            // Store CSV content for API response
            result.put("csvContent", sampleSheet);
            result.put("filename", filename);
            result.put("flowCellId", flowCellId);
            result.put("runId", runId);

            if (isNovaX) {
                System.out.println("Generating sample sheet v2 for DRAGEN and sequencer setup.");
                String referenceGenome = "hg38-alt_masked.cnv.hla.methylated_combined.rna-10-r4.0-2";
                // example read length - "151/8/8/151"
                String readLength = experiment.getStringVal("ReadLength", user);
                String[] cycles = readLength.split("/");
                int read1Cycles = Integer.parseInt(cycles[0]);
                int index1Cycles = Integer.parseInt(cycles[1]);
                int index2Cycles = Integer.parseInt(cycles[2]);
                int read2Cycles = Integer.parseInt(cycles[3]);

                NovaSeqXSamplesheetGenerator x = new NovaSeqXSamplesheetGenerator(flowCellId, read1Cycles, read2Cycles,
                        index1Cycles, index2Cycles);
                List<SampleData> samples = SampleSheetParser.parseSampleSheet(sampleSheet);
                System.out.println("Parsed sample sheet with " + samples.size() + " samples");
                HashSet<String> dragenSamples = new HashSet<>();
                for (SampleData s : samples) {
                    if ("WGS_Deep".equals(s.sampleWell)) {
                        // tell demux to always be 0,0 mismatches so .cram file can always be used
                        x.addBclConvertSample(Integer.parseInt(s.lane), s.sampleId, s.samplePlate, s.sampleWell,
                                s.i7IndexId, s.index, s.index2, s.sampleProject, s.baitSet, 0, 0);
                        // add all WGS samples once to the DRAGEN processing section (no flowcell lanes)
                        if (!dragenSamples.contains(s.sampleId)) {
                            x.addDragenGermlineSample(s.sampleId, referenceGenome, "None");
                            dragenSamples.add(s.sampleId);
                            System.out.println("Adding WGS sample for DRAGEN: " + s);
                        }
                    }
                }
                x.generateSamplesheet(sampleSheetPathDragen);
                System.out.println("DRAGEN Samplesheet generated successfully - " + sampleSheetPathDragen);
            }

            try {
                outputStream = new PrintWriter(new FileWriter(file));
                outputStream.print(sampleSheet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }

        return result;
    }

    protected static boolean is10xGenomics(String indexTag) {
        String[] parts = indexTag.split("-");
        if (parts.length == 3)
            return true;
        return false;
    }

    protected static boolean isDualBarcode(String indexTag) {
        String[] parts = indexTag.split("-");
        if (parts.length == 2)
            return true;
        return false;
    }

    public List<DataRecord> getParentRequests(DataRecord sample)
            throws IoError, RemoteException, NotFound, ServerException {
        List<DataRecord> requests = new ArrayList<>();
        if (sample.getStringVal("SampleId", user).toLowerCase().startsWith("pool-")) {
            return requests;
        }
        if (sample.getParentsOfType("Request", user).size() > 0) {
            return sample.getParentsOfType("Request", user);
        }
        Stack<DataRecord> sampleStack = new Stack<>();
        if (sample.getParentsOfType("Sample", user).size() > 0) {
            sampleStack.push(sample.getParentsOfType("Sample", user).get(0));
        }
        while (!sampleStack.isEmpty()) {
            DataRecord nextSample = sampleStack.pop();
            if (nextSample.getParentsOfType("Request", user).size() > 0) {
                return nextSample.getParentsOfType("Request", user);
            } else if (nextSample.getParentsOfType("Sample", user).size() > 0) {
                sampleStack.push(nextSample.getParentsOfType("Sample", user).get(0));
            }
        }
        return requests;
    }

    /**
     * For a given pool on a lane returns the list of each sample and it's assigned
     * barcode
     */
    public List<HashMap<String, String>> poolSampleBarcodeLookup(DataRecord pool)
            throws NotFound, IoError, RemoteException, ServerException {
        /*
         * There are 3 types of pools: sequencing pools made by the sequencing team,
         * capture pools (IMPACT/HEME, etc) and User Library Pools
         * Therefore a pool can have a pool of pools.
         */
        Queue<DataRecord> sampleQ = new LinkedList<>();
        sampleQ.add(pool);
        List<HashMap<String, String>> resultList = new LinkedList<>();

        while (!sampleQ.isEmpty()) {
            DataRecord sampleToProcess = sampleQ.remove();
            System.out.println(
                    "Barcode look up function is processing sample: " + sampleToProcess.getStringVal("SampleId", user));
            Map<String, Object> sampleFields = sampleToProcess.getFields(user);
            String sampleType = sampleToProcess.getStringVal(DT_Process.EXEMPLAR_SAMPLE_TYPE, user);
            System.out.println("Sample type is: " + sampleType);
            if (sampleType.toLowerCase().contains("pool")
                    || (!sampleToProcess.getParentsOfType("Sample", user).isEmpty()
                            && sampleToProcess.getParentsOfType("Sample", user).size() > 0)) {
                // process all samples in the pool if it is a pool or look for the index barcode
                // if it is a sample
                List<DataRecord> samples = sampleToProcess.getParentsOfType("Sample", user);
                System.out.println("The sample " + sampleToProcess.getStringVal("SampleId", user)
                        + " parents' samples list size  = " + samples.size());
                sampleQ.addAll(samples);
            } else {
                HashMap<String, String> sampleInfo = new HashMap<>();
                DataRecord[] barcodes = sampleToProcess.getChildrenOfType("IndexBarcode", user);
                System.out.println("Barcodes array size = " + barcodes.length);
                if (barcodes.length > 0) {
                    List<DataRecord> requests = null;
                    String recipe = (String) sampleFields.get("Recipe");
                    sampleInfo.put("RECIPE", sampleToProcess.getStringVal("Recipe", user));
                    if (DLP_RECIPE.equals(recipe.toLowerCase())) {
                        requests = getParentRequests(sampleToProcess);
                    } else if ("TCR_IGO-alpha".equals(recipe) || "TCR_IGO-beta".equals(recipe)
                            || SMARTSEQ_RECIPE.equals(recipe)) {
                        // Only get ancestors that are not pools
                        List<DataRecord> allAncestors = sampleToProcess.getAncestorsOfType("Request", user);
                        requests = new ArrayList<>();
                        for (DataRecord ancestor : allAncestors) {
                            String ancestorType = ancestor.getStringVal("SampleType", user);
                            if (ancestorType == null || !ancestorType.toLowerCase().contains("pool")) {
                                requests.add(ancestor);
                            }
                        }
                    } else {
                        requests = sampleToProcess.getAncestorsOfType("Request", user);
                    }
                    if (requests.size() > 0 && !sampleInfo.containsKey("REQUEST_ID")) {
                        Map<String, Object> requestFields = requests.get(0).getFields(user);
                        sampleInfo.put("REQUEST_ID", (String) requestFields.get("RequestId"));
                        sampleInfo.put("PI", (String) requestFields.get(DT_Request.LAB_HEAD_EMAIL));

                        if (requestFields.get(DT_Request.LAB_HEAD_EMAIL) == null
                                || ((String) requestFields.get(DT_Request.LAB_HEAD_EMAIL)).equals("")) {
                            sampleInfo.put("PI", VALUE_UNAVAILABLE);
                        }
                        sampleInfo.put("SPECIES", (String) sampleFields.get("Species"));
                        sampleInfo.put("IGO_ID", baseIgoSampleId((String) sampleFields.get("SampleId")));
                        sampleInfo.put("OTHER_ID", (String) sampleFields.get("OtherSampleId"));
                    }
                    sampleInfo.put("BARCODE_TAG", barcodes[0].getStringVal(DT_IndexBarcode.INDEX_TAG, user));
                    sampleInfo.put("BARCODE_ID", barcodes[0].getStringVal(DT_IndexBarcode.INDEX_ID, user));
                    resultList.add(sampleInfo);
                } else {
                    List<DataRecord> samples = sampleToProcess.getParentsOfType("Sample", user);
                    sampleQ.addAll(samples);
                }
            }
        }

        return resultList;
    }

    // search to assign sample info
    public List<HashMap<String, String>> dfsAssign(DataRecord pool)
            throws NotFound, IoError, RemoteException, ServerException {
        List<HashMap<String, String>> allAdditional = new LinkedList<>();
        Set<DataRecord> blacklist = new HashSet<>();
        LinkedList<DataRecord> next = new LinkedList<>(); // deque/stack
        next.add(pool);
        DataRecord prev = pool;
        // many different key->value pairs such as REQUEST_ID, IGO_ID, PI, SPECIES
        HashMap<String, String> sampleInfo = new HashMap<>();
        HashMap<String, String> prevSampleInfo = new HashMap<>();

        while (!next.isEmpty()) {
            DataRecord sample = next.pop();
            if (blacklist.contains(sample)) {
                continue;
            }
            blacklist.add(sample);

            System.out.println("Starting:" + sample.getStringVal("SampleId", user));
            Map<String, Object> sampleFields = sample.getFields(user);
            List<DataRecord> requests = null;
            // Special logic for control samples only
            if ((Boolean) sampleFields.get("IsControl")) {
                System.out.println("Processing control Sample.");
                if (String.valueOf(sampleFields.get("Recipe")).toLowerCase().equals(DLP_RECIPE)) {
                    List<DataRecord> dlpControlRequests = dataRecordManager.queryDataRecords("Request",
                            "RequestId = '" + String.valueOf(sampleFields.get("RequestId")) + "'", user);
                    if (dlpControlRequests.size() > 0) {
                        sampleInfo.put("REQUEST_ID", dlpControlRequests.get(0).getStringVal("RequestId", user));
                        sampleInfo.put("PI", dlpControlRequests.get(0).getStringVal("LabHeadEmail", user));
                    }
                } else {
                    sampleInfo.put("REQUEST_ID", "");
                    sampleInfo.put("PI", "UNKNOWN");
                }
                sampleInfo.put("SPECIES", (String) sampleFields.get("Species"));
                sampleInfo.put("IGO_ID", baseIgoSampleId((String) sampleFields.get("SampleId")));
                sampleInfo.put("OTHER_ID", (String) sampleFields.get("OtherSampleId"));
                sampleInfo.put("IS_CONTROL", "CONTROL");
                List<DataRecord> parents = prev.getParentsOfType("Sample", user);
                HashSet<String> captureSet = new HashSet<>();
                for (DataRecord parent : parents) {
                    Map<String, Object> parentField = parent.getFields(user);
                    if (!parentField.containsKey("IsControl") || !(Boolean) parentField.get("IsControl")) {
                        if (parentField.containsKey("Recipe") && !((String) parentField.get("Recipe")).contains(",")) {
                            captureSet.add((String) parentField.get("Recipe"));
                        }
                    }
                }
                if (String.valueOf(sampleFields.get("Recipe")).equalsIgnoreCase(DLP_RECIPE)) {
                    sampleInfo.put("CAPTURE", "");
                } else {
                    if (captureSet.size() > 1) {
                        sampleInfo.put("CAPTURE", "AMBIGUOUS");
                    } else if (captureSet.size() == 0
                            && !String.valueOf(sampleFields.get("Recipe")).equalsIgnoreCase(DLP_RECIPE)) {
                        sampleInfo.put("CAPTURE", "MISSING");
                    } else {
                        sampleInfo.put("CAPTURE", (String) captureSet.toArray()[0]);
                        sampleInfo.put("RECIPE", (String) captureSet.toArray()[0]);
                    }
                }
            } // end if for "control" samples
            String recipe = sample.getStringVal("Recipe", user);
            if (DLP_RECIPE.equals(recipe.toLowerCase())) {
                requests = getParentRequests(sample);
            } else if ("TCR_IGO-alpha".equals(recipe) || "TCR_IGO-beta".equals(recipe)
                    || SMARTSEQ_RECIPE.equals(recipe)) {
                requests = sample.getAncestorsOfType("Request", user);
            } else {
                requests = sample.getParentsOfType("Request", user);
            }
            if (requests.size() > 0 && !sampleInfo.containsKey("REQUEST_ID")) {
                Map<String, Object> requestFields = requests.get(0).getFields(user);
                sampleInfo.put("REQUEST_ID", (String) requestFields.get("RequestId"));
                sampleInfo.put("PI", (String) requestFields.get(DT_Request.LAB_HEAD_EMAIL));

                if (requestFields.get(DT_Request.LAB_HEAD_EMAIL) == null
                        || ((String) requestFields.get(DT_Request.LAB_HEAD_EMAIL)).equals("")) {
                    sampleInfo.put("PI", VALUE_UNAVAILABLE);
                }
                sampleInfo.put("SPECIES", (String) sampleFields.get("Species"));
                sampleInfo.put("IGO_ID", baseIgoSampleId((String) sampleFields.get("SampleId")));
                sampleInfo.put("OTHER_ID", (String) sampleFields.get("OtherSampleId"));

                if ("TCR_IGO-alpha".equals(recipe) || "TCR_IGO-beta".equals(recipe) || SMARTSEQ_RECIPE.equals(recipe)) {
                    String otherSampleId = (String) sampleFields.get("OtherSampleId");
                    String igoID = sample.getStringVal("SampleId", user);
                    String baseIgoID = baseIgoSampleId(igoID);

                    String igoId = (String) sampleFields.get("SampleId");
                    sampleInfo.put("REQUEST_ID", requestFromIgoId(igoId));
                    sampleInfo.put("IGO_ID", baseIgoID);
                    System.out.println("Put " + baseIgoID + " for tcrseq sample ID.");

                }

            }

            // lookup bait set for Hybrid Capture Recipes like ACCESS or IMPACT
            if (recipe.startsWith("HC_") && !recipe.contains(",")) { // don't care for pools like HC_ACCESS,User_ERIL
                List<DataRecord> records = sample.getDescendantsOfType("NimbleGenHybProtocol", user);
                if (!records.isEmpty()) {
                    for (DataRecord r : records) {
                        // must check for valid, for example FAUCI2_0050 has "invalid" entries in
                        // NimbleGenHybProtocol
                        if (r.getBooleanVal("Valid", user)) {
                            String baitSet = r.getStringVal("BaitSet", user);
                            sampleInfo.put("BAITSET", baitSet);
                            break;
                        }
                    }
                } else {
                    sampleInfo.put("BAITSET", "");
                }
            } else {
                sampleInfo.put("BAITSET", "");
            }

            String recipeSF = (String) sampleFields.get("Recipe");
            if (!sampleInfo.containsKey("RECIPE") && recipeSF != null && !recipeSF.contains(",") &&
                    !((String) sampleFields.get("SampleId")).startsWith("Pool-")) {
                sampleInfo.put("SPIKE_IN_GENES", (String) sampleFields.get(DT_Sample.SPIKE_IN_GENES));
                sampleInfo.put("RECIPE", recipe);
            }

            DataRecord[] barcodes = sample.getChildrenOfType("IndexBarcode", user);
            if (barcodes.length > 0) {
                sampleInfo.put("BARCODE_TAG", barcodes[0].getStringVal(DT_IndexBarcode.INDEX_TAG, user));
                sampleInfo.put("BARCODE_ID", barcodes[0].getStringVal(DT_IndexBarcode.INDEX_ID, user));
                try {
                    if (!barcodes[0].getStringVal("OtherSampleId", user)
                            .equals((String) sampleFields.get("OtherSampleId"))) {
                        // clientCallback.displayWarning("The barcode of " + (String)
                        // sampleFields.get("SampleId") + " does not seem to match the sample's id");
                    }
                } catch (Exception e) {
                    logInfo("The barcode of " + (String) sampleFields.get("SampleId")
                            + " does not seem to match the sample's id and there was an error in displaying warning");
                }
            }

            String sampleName = sample.getStringVal("OtherSampleId", user);
            String sampleId = (String) sampleFields.get("SampleId");
            boolean tcrseqFinished = false;
            if (sampleId.startsWith("Pool") == false && (sampleName.endsWith("_alpha") || sampleName.endsWith("_beta")))
                tcrseqFinished = true;

            if (!(sampleInfo.containsKey("REQUEST_ID") && sampleInfo.containsKey("BARCODE_ID")
                    && sampleInfo.containsKey("RECIPE"))
                    && tcrseqFinished == false) {
                List<DataRecord> sampleList = sample.getParentsOfType("Sample", user);
                for (DataRecord s : sampleList) {
                    System.out.print("," + s.getStringVal("SampleId", user));
                }
                System.out.println("");


                next.addAll(0, sampleList);

                if (sample.getStringVal("SampleId", user).startsWith("Pool-")) {
                    prev = sample;
                }

            } else {
                System.out.println("-----Finished:" + (String) sampleFields.get("SampleId") + " IGO ID:"
                        + sampleInfo.get("IGO_ID"));

                if (!sampleInfo.isEmpty()) {

                    // This is to reset some fields that can have stale pool data from a previous
                    // loop iteration:
                    // https://mskjira.mskcc.org/browse/IGODATA-98

                    if (sampleInfo.containsKey("REQUEST_ID") && sampleInfo.get("REQUEST_ID").startsWith("Pool-")){
                        if (sampleFields != null && sampleFields.containsKey("SampleId")) {
                            sampleInfo.put("IGO_ID", baseIgoSampleId((String) sampleFields.get("SampleId")));
                        }
    
                        if (requests != null && requests.size() > 0) {
                            Map<String, Object> requestFields = requests.get(0).getFields(user);
                            if (requestFields != null && requestFields.containsKey("RequestId")) {
                                sampleInfo.put("REQUEST_ID", (String) requestFields.get("RequestId"));
                            }
                        }
                    }
                        
                    if (sampleFields != null && sampleFields.containsKey("OtherSampleId")) {
                        String otherSampleID = (String) sampleFields.get("OtherSampleId");
                                                                 // append _alpha or _beta to the sampleID for the sample sheet
                        if ("TCR_IGO-alpha".equals(recipe)) {
                            otherSampleID += "_alpha";
                        }
                        if ("TCR_IGO-beta".equals(recipe))
                            otherSampleID += "_beta";

                        sampleInfo.put("OTHER_ID", otherSampleID);
                    }
                    

                    allAdditional.add(sampleInfo);

                }

                sampleInfo = new HashMap<>();
            }
            blacklist.add(sample);
        }
        return allAdditional;
    }

    public static String requestFromIgoId(String igoId) {
        if (igoId == null)
            return null;
        return igoId.replaceAll("_[0-9]+", "");
    }

    public static boolean isValidIGOSampleId(String igoId) {
        return igoId.matches("\\d\\d\\d\\d\\d(_[A-Z]*)?(_[0-9]*)+");
    }

    public static String baseIgoSampleId(String igoId) {
        if (igoId == null)
            return null;
        if (igoId.startsWith("Pool-")) {
            System.out.println("Error: no IGO ID for Pool " + igoId);
            return null;
        }
        String request = requestFromIgoId(igoId);
        int indexEnd = igoId.indexOf('_', request.length() + 1);
        if (indexEnd == -1)
            return igoId;
        else
            return igoId.substring(0, indexEnd);
    }

    public List<DataRecord> getLanesFromExperiment(DataRecord expRec) throws Exception {
        List<DataRecord> lanes = Arrays
                .asList(expRec.getChildrenOfType("FlowCell", user)[0].getChildrenOfType("FlowCellLane", user));
        return lanes;
    }

    public String reverseComplement(String sequence) throws Exception {
        StringBuilder revCompBuild = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            switch (sequence.charAt(i)) {
                case 'A':
                    revCompBuild.append('T');
                    break;
                case 'C':
                    revCompBuild.append('G');
                    break;
                case 'G':
                    revCompBuild.append('C');
                    break;
                case 'T':
                    revCompBuild.append('A');
                    break;
                default:
                    throw new Exception("Invalid alphabet option to reverse complement sequence " + sequence
                            + ". Only supports ACGT");
            }
        }
        return revCompBuild.toString();
    }

    private String generateSampleSheet(String flowCellId, String runId) throws Throwable {
        LinkedList<String> errorList = new LinkedList<>();
        HashMap<String, String> request2Recipe = new HashMap<>();
        HashMap<String, String> request2Species = new HashMap<>();
        SampleUtilManager sampleUtils = new SampleUtilManager(managerContext);
        List<Set<org.mskcc.limsrest.util.illuminaseq.Barcode>> barcodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) // create empty sets for barcodes for each lane
            barcodes.add(new HashSet<org.mskcc.limsrest.util.illuminaseq.Barcode>());

        boolean isDualBarcoded = false;
        if (samples != null && samples.size() > 0) {
            List<DataRecord> indicesRecList = new ArrayList<DataRecord>(
                    sampleUtils.getIndices(samples.get(0).getDataRecord()));
            for (DataRecord barcode : indicesRecList) {
                if (barcode.getStringVal("IndexTag", user).contains("-")) {
                    DataRecord parentSample = barcode.getParentsOfType("Sample", user).get(0);
                    if (!parentSample.getStringVal("Recipe", user).contains("SC_Chromium")
                            || !parentSample.getStringVal("Recipe", user).contains("Visium")) {
                        isDualBarcoded = true;
                    }
                }
            }
        }

        String machineType = "";
        try {
            List<DataRecord> machineList = dataRecordManager.queryDataRecords("Instrument",
                    "InstrumentName = '" + instrument + "'", user);
            machineType = machineList.get(0).getPickListVal("InstrumentType", user);
            System.out.println("Machine Type: " + machineType);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            System.err.println(
                    "WARNING: Unable to identify the sequencer type being used for creating sample sheet. Machine-specific handling of data will not be possible");
        }

        RunInfoDetails runInfoDetails = null;

        if (isNovaX) {
            logInfo("NovaSeqX, will not parse RunInfo.xml.");
            int runNumber = 0;
            // example read length - "151/8/8/151"
            String readLength = experiment.getStringVal("ReadLength", user);
            logInfo("Read Length is : " + readLength);
            List<Read> reads = readsFromReadLength(readLength);
            String instrument = "";
            runInfoDetails = new RunInfoDetails(runId, runNumber, flowCellId, instrument, new Date(), reads);
            logInfo("Built RunInfo :  " + runInfoDetails);
        } else {
            File runXmlFile = getRunXmlFile();
            runInfoDetails = new RunInfoDetails(runXmlFile);
            logInfo("Finished reading : " + runXmlFile + " - " + runInfoDetails);
        }

        ignoreDualBarcodes = setIgnoreDualBarcodes(runInfoDetails);
        // could have single barcodes that should be padded to dual based on RunInfo.xml
        isDualBarcoded = !ignoreDualBarcodes;
        System.out.println("IGNORE DUAL BARCODES: " + ignoreDualBarcodes + " IS DUAL BARCODES:" + isDualBarcoded);
        int read2Length = 8;
        for (Read read : runInfoDetails.reads) {
            if (read.number == 2)
                read2Length = read.numCycles;
        }

        byte[] template = null;

        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        String templatePath;
        if (isDualBarcoded) {
            templatePath = "/cmo/filetemplates/SampleSheet_HiSeq4000_DUAL_TEMPLATE.csv";
        } else {
            templatePath = "/cmo/filetemplates/SampleSheet_HiSeq4000_TEMPLATE.csv";
        }
        try (InputStream is = this.getClass().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IOException("Template file not found: " + templatePath);
            }
            template = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        }

        FileTemplateUtility fileTemplateUtil = new FileTemplateUtility(template);

        String date = new SimpleDateFormat("M/dd/yyyy").format(new Date(System.currentTimeMillis()));
        fileTemplateUtil.addIndividualValue(HI_SEQ_4000_DATE, date);

        List<String> readNums = getReads(runInfoDetails);

        fileTemplateUtil.addIndividualValue(HI_SEQ_4000_APPLICATION, instrument);

        if (readNums.size() >= 1) {
            fileTemplateUtil.addIndividualValue(HI_SEQ_4000_READS1, readNums.get(0));
        }
        if (readNums.size() >= 2) {
            fileTemplateUtil.addIndividualValue(HI_SEQ_4000_READS2, readNums.get(1));
        }

        List<List<DataRecord>> sampleParentRecListList = dataRecordManager
                .getParentsOfType(DataRecordMapPair.getDataRecordList(samples), DT_Sample.DATA_TYPE, user);

        // because the lab sometimes uses mixed length bar codes of 6 and 8 and 10 nt on
        // a flow cell but then wants uniformed length bar codes
        // we have to first loop through all the bar codes, find there are mixed length
        // barcodes on this cell, and if so note that all
        // 6 nt barcodes need padding. The code checks to pad individual bar code tags
        // later, so all it needs to do here is check if there
        // are any length 8 tags
        int maxLengthBarcode = 0;
        for (int sampleIndex = 0; sampleIndex < samples.size(); sampleIndex++) {
            DataRecordMapPair sample = samples.get(sampleIndex);
            List<DataRecord> assignedLanes = Arrays
                    .asList(sample.getDataRecord().getChildrenOfType(DT_FlowCellLane.DATA_TYPE, user));
            List<DataRecord> indicesRecList = new ArrayList<DataRecord>(sampleUtils.getIndices(sample.getDataRecord()));
            for (DataRecord barcode : indicesRecList) {
                int l = barcode.getStringVal("IndexTag", user).length();
                if (l > maxLengthBarcode)
                    maxLengthBarcode = l;
            }
        }

        // GEt first lane to start
        // this.assignedLanes = this.assignedLanes.subList(0, 1);

        boolean useableSheet = true;
        HashMap<Long, String> sp1Processed2Request = new HashMap<>();
        Set<String> fldSet = null;
        String prevChipNumber = "";
        String chipNumber = "";
        for (DataRecord assignedLane : assignedLanes) {
            HashSet<String> usedControls = new HashSet<>();
            HashSet<String> usedBarcodes = new HashSet<>();
            List<DataRecord> lanePools = assignedLane.getParentsOfType("Sample", user);
            long laneNum = assignedLane.getLongVal(DT_FlowCellLane.LANE_NUM, user);

            for (DataRecord sample : lanePools) {
                List<HashMap<String, String>> allInfos = dfsAssign(sample);
                // List<HashMap<String, String>> allInfos_v2 = poolSampleBarcodeLookup(sample);
                // if (allInfos.size() != allInfos_v2.size()) {
                // System.out.println("The two maps have different sizes!!! + old map size = " +
                // allInfos.size()
                // + " new map size = " + allInfos_v2.size());
                // } else {
                // for (int i = 0; i < allInfos.size(); i++) {
                // // Diff the two maps
                // // Map<String, String> diff = diffMaps(allInfos.get(i), allInfos_v2.get(i));
                // if (diff.isEmpty()) {
                // System.out.println("Maps are identical at sample " +
                // allInfos.get(i).get("IGO_ID"));
                // } else {
                // System.out.println("Number of different records = " + diff.size());
                // System.out.println(
                // String.format("------------------------MAPS DIFFERENCES FOR SAMPLE %s AS
                // FOLLOWS:",
                // allInfos.get(i).get("IGO_ID")));
                // diff.forEach((key, value) -> System.out.println("Key '" + key + "': " +
                // value));
                // }
                // }

                // }
                logInfo("Finished DFS Assign, list size:" + allInfos.size());
                for (HashMap<String, String> additionalInfo : allInfos) {
                    logInfo(additionalInfo.toString());
                    // get the samples assigned lane(s)
                    // clientCallback.displayInfo(additionalInfo.toString());
                    boolean sp1Request = false;
                    String indexTag = additionalInfo.get("BARCODE_TAG"); // displayed as Index Tag in LIMS
                    String indexId = additionalInfo.get("BARCODE_ID"); // displayed as Index ID
                    if (usedBarcodes.contains(indexTag)) {
                        logInfo(indexTag + " is in lane " + laneNum + " more then once");
                    }
                    usedBarcodes.add(indexTag);
                    if (is10xGenomics(indexTag)) {
                        // leave index ID as it is
                    } else if (ignoreDualBarcodes && indexTag.contains("-"))
                        indexTag = indexTag.split("-")[0]; // correct index tag field to remove the 2nd barcode

                    String indexTag2 = "";
                    if (isDualBarcoded && isDualBarcode(indexTag)) {
                        indexTag2 = indexTag.split("-")[1].trim();
                        indexTag = indexTag.split("-")[0];
                        System.out.println("indexTag:" + indexTag + " indexTag2:" + indexTag2);

                        // NovaSeqX does not need reverse NovaSeq 6000 does, machineType table in LIMS
                        // does not have "6000", only "NovaSeq" for RUTH & DIANA
                        if (machineType.equalsIgnoreCase(INSTRUMENT_NOVASEQ)) {
                            System.out.println("Reverse complementing indexTag2: " + indexTag2 + " for machineType: "
                                    + machineType);
                            indexTag2 = reverseComplement(indexTag2);
                            // DLP barcodes are 6 mers, so the padding is different based on the machine
                            // type. For non-NEXTSEQ and non-HISEQ4000 machines, padding is correct in LIMS.
                            // For NEXTSEQ and HISEQ4000, remove the first 2 bases from the reverse
                            // complimented IndexTag2 and add GT in the end to match correct padding.
                            if (DLP_RECIPE.equals(additionalInfo.get("RECIPE").toLowerCase())) {
                                indexTag2 = indexTag2.substring(2) + "GT";
                            }
                        } else {
                            if (DLP_RECIPE.equals(additionalInfo.get("RECIPE").toLowerCase())) {
                                indexTag2 = "AC" + indexTag2.substring(0, 6);
                            }
                        }
                    } else if (is10xGenomics(indexTag)) {
                        indexTag2 = indexTag;
                    } else if (isDualBarcoded) {
                        // If the second read length is 10, determine the appropriate indexTag2 value.
                        // For NovaSeq, reverse complement the 10nt padding barcode, otherwise use as
                        // is.
                        if (read2Length == 10) {
                            indexTag2 = isNova ? reverseComplement(IDT_P5_BARCODE_NOT_DUAL_10)
                                    : IDT_P5_BARCODE_NOT_DUAL_10;
                        } else {
                            // For read2Length not equal to 10
                            if (isNovaX) {
                                // For NovaSeqX, do not reverse complement the 8nt padding barcode.
                                System.out.println("Not Reverse complementing padding for machineType: " + machineType);
                                indexTag2 = IDT_P5_BARCODE_NOT_DUAL_8;
                                
                            } else if (isNova) {
                                // For NovaSeq (not NovaSeqX), reverse complement the 8nt padding barcode.
                                System.out.println("NovaSeqX or NextSeq: index2 padding reverse complemented.");
                                indexTag2 = reverseComplement(IDT_P5_BARCODE_NOT_DUAL_8);
                            }
                        }
                    }

                    // Padding or triming happening here!!
                    if (indexTag.length() > read2Length) { // if LIMS barcode is longer than read length then trim the
                        System.out.println("Trimming barcode to match read length:" + indexTag + "-" + indexTag2 + ":"
                                + read2Length);
                        indexTag = indexTag.substring(0, read2Length);
                        if (isDualBarcoded) {
                            if (machineType.equalsIgnoreCase(INSTRUMENT_NOVASEQ)) {
                                // for NovaSeq6000 since reverse complement above take first 8
                                indexTag2 = indexTag2.substring(0, read2Length);
                            } else {
                                // for 10nt barcodes we want to get the last 8 for i5 for NovaSeqX
                                indexTag2 = indexTag2.substring(indexTag2.length() - read2Length, indexTag2.length());
                            }
                        }
                    }
                    
                    if (indexTag.length() < read2Length) {
                        if (isDualBarcoded) {
                            HashMap<String, String> id2padding = paddingMap(machineType);
                            if (id2padding.containsKey(indexId)) {
                                System.out.println("Adding padding for: " + indexId + ":" + id2padding.get(indexId));
                                indexTag = indexTag + id2padding.get(indexId);
                            } else {
                                System.out.println("NO PADDING FOUND FOR BARCODE: " + indexTag + " using GT");
                                indexTag = indexTag + "AT";
                            }
                        }
                    }

                    if (indexTag2.length() < read2Length) {
                        if (isDualBarcoded) {
                            HashMap<String, String> id2padding = paddingMap(machineType);
                            if (id2padding.containsKey(indexId)) {
                                System.out.println("Adding padding for: " + indexId + ":" + id2padding.get(indexId));
                                if(isNovaX) {
                                    indexTag2 = indexTag2 + id2padding.get(indexId);
                                } else if (isNova) {
                                    indexTag2 = id2padding.get(indexId) + indexTag2;
                                }
                            } else if (isNova) {
                                indexTag2 = indexTag2 + "GT";
                            } else if (isNovaX) {
                                indexTag2 = "AC" + indexTag2;
                            } else {
                                System.out.println("NO PADDING FOUND FOR BARCODE: " + indexTag + " guessing AT");
                                indexTag2 = indexTag2 + "AC";
                            }
                        }
                    }

                    Map<String, String> row = new HashMap<String, String>();
                    String igoId = additionalInfo.get("IGO_ID");
                    logInfo("IGO ID: " + igoId);
                    String recipe = additionalInfo.get("RECIPE");
                    String species = CsvHelper.formatCsvField(additionalInfo.get("SPECIES"));
                    String uniqueId;
                    String sampleProject;
                    String baitSet = additionalInfo.get("BAITSET");

                    // if (additionalInfo.containsKey("IS_CONTROL") &&
                    // !recipe.toLowerCase().equalsIgnoreCase(DLP_RECIPE)) { // not applicable
                    // anymore!
                    // sampleProject = "POOLEDNORMALS";
                    // uniqueId = additionalInfo.get("OTHER_ID") + "_IGO_" +
                    // additionalInfo.get("CAPTURE") + "_" + indexTag;
                    // if (additionalInfo.get("CAPTURE").equals("AMBIGUOUS")) {
                    // clientCallback.displayWarning("There is an ambiguous capture in pooled normal
                    // " + additionalInfo.get("OTHER_ID") + ". Please alert your team that there
                    // could be samples with different recipes pooled together");
                    // } else if (additionalInfo.get("CAPTURE").equals("MISSING")) {
                    // clientCallback.displayWarning("There is no capture to guide control the
                    // recipe for pooled normal " + additionalInfo.get("OTHER_ID") + ". Please alert
                    // your team.");
                    // }
                    // if (usedControls.contains(uniqueId)) {
                    // continue;
                    // }
                    // usedControls.add(uniqueId);
                    // } else {
                    String other_id = additionalInfo.get("OTHER_ID").replace(',', '_');
                    uniqueId = other_id + "_IGO_" + igoId;
                    sampleProject = additionalInfo.get("REQUEST_ID");
                    if (sp1Request) { // sp1Request is always false, never sets to true!
                        uniqueId = "Pool_lane" + Long.toString(laneNum) + "_" + sampleProject;
                        indexTag = "";
                    } else if ("Fingerprinting".equals(additionalInfo.get("RECIPE"))) {
                        uniqueId = other_id + "_FP_IGO_" + igoId;
                    }
                    // }
                    if (!request2Recipe.containsKey(sampleProject)) {
                        request2Recipe.put(sampleProject, additionalInfo.get("RECIPE"));
                    }
                    if (!request2Species.containsKey(sampleProject)) {
                        request2Species.put(sampleProject, additionalInfo.get("SPECIES"));
                    }
                    if (!request2Species.get(sampleProject).equals(additionalInfo.get("SPECIES"))) {
                        errorList.add(sampleProject + " has multiple species: " + request2Recipe.get(sampleProject)
                                + " " + additionalInfo.get("SPECIES"));
                    }
                    if (!additionalInfo.containsKey("IS_CONTROL")) {
                        if (!additionalInfo.containsKey("SPECIES") || additionalInfo.get("SPECIES").equals("")) {
                            errorList.add(uniqueId + " is missing a species");
                        } else if (!request2Species.get(sampleProject).equals(additionalInfo.get("SPECIES"))) {
                            errorList.add(sampleProject + " has multiple species: " + request2Species.get(sampleProject)
                                    + " and " + additionalInfo.get("SPECIES"));
                        }
                        if (!request2Recipe.get(additionalInfo.get("REQUEST_ID"))
                                .equals(additionalInfo.get("RECIPE"))) {
                            errorList.add(additionalInfo.get("REQUEST_ID") + " has multiple recipes: "
                                    + request2Recipe.get(additionalInfo.get("REQUEST_ID")) + " "
                                    + additionalInfo.get("RECIPE"));
                        }
                    }
                    if (uniqueId.contains(" ")) {
                        errorList.add(uniqueId + " has a space in it");
                    }
                    if (additionalInfo.get("RECIPE").equalsIgnoreCase(DLP_RECIPE)) {
                        List<DataRecord> dlpProtocol1Recs = dataRecordManager.queryDataRecords(
                                "DLPLibraryPreparationProtocol1", "SampleId = '" + igoId + "' ", user);
                        if (dlpProtocol1Recs.size() > 0) {
                            chipNumber = (String) dlpProtocol1Recs.get(0).getDataField("ChipID", user);
                        }
                        System.out.println(
                                "prev chip number = " + prevChipNumber + " current chip number = " + chipNumber);
                        if (!prevChipNumber.equals(chipNumber)) {
                            fldSet = ConstructDLPFieldFileSet(chipNumber);
                            System.out.println("fldSet size is = " + fldSet.size());
                            System.out.println(
                                    "Construct fld map CALLED! ... Constructing fld map again since we moved to a new chip number!");
                            prevChipNumber = chipNumber;
                        }
                        String chipCol = indexId.split("_")[1].split("-")[0];
                        if (chipCol.startsWith("0")) {
                            chipCol = chipCol.substring(1).trim();
                        }

                        String chipRow = indexId.split("_")[2];
                        if (chipRow.startsWith("0")) {
                            chipRow = chipRow.substring(1).trim();
                        }
                        logInfo("chipRow = " + chipRow);
                        logInfo("chipCol = " + chipCol);
                        if (fldSet.contains(chipRow + "/" + chipCol)) {
                            logInfo("chip row: " + chipRow + "and column: " + chipCol
                                    + " location has valid sample... putting the row on the sheet!");
                            row.put(HI_SEQ_4000_COL_LANE, Long.toString(laneNum));
                            row.put(HI_SEQ_4000_COL_SAMPLE_ID, CsvHelper.formatCsvField(uniqueId));
                            row.put(HI_SEQ_4000_COL_SAMPLE_WELL, CsvHelper.formatCsvField(recipe));
                            row.put(HI_SEQ_4000_COL_SAMPLE_PLATE, species);
                            row.put(HI_SEQ_4000_COL_INDEX_ID, indexId);
                            row.put(HI_SEQ_4000_COL_INDEX_TAG, indexTag);
                            row.put(HI_SEQ_4000_COL_INDEX_TAG2, indexTag2);
                            row.put(HI_SEQ_4000_COL_SAMPLE_PROJECT,
                                    CsvHelper.formatCsvField("Project_" + sampleProject));
                            row.put(HI_SEQ_4000_COL_BAIT_SET, baitSet);
                            row.put(HI_SEQ_4000_COL_DESCRIPTION,
                                    CsvHelper.formatCsvField(additionalInfo.get("PI").replaceAll(" ", "_")));
                        }
                    } else if (!additionalInfo.get("RECIPE").equalsIgnoreCase(DLP_RECIPE)) {
                        row.put(HI_SEQ_4000_COL_LANE, Long.toString(laneNum));
                        row.put(HI_SEQ_4000_COL_SAMPLE_ID, CsvHelper.formatCsvField(uniqueId));
                        row.put(HI_SEQ_4000_COL_SAMPLE_PLATE, species);
                        row.put(HI_SEQ_4000_COL_INDEX_ID, indexId);
                        row.put(HI_SEQ_4000_COL_INDEX_TAG, indexTag);
                        row.put(HI_SEQ_4000_COL_INDEX_TAG2, indexTag2);
                        row.put(HI_SEQ_4000_COL_SAMPLE_PROJECT, CsvHelper.formatCsvField("Project_" + sampleProject));
                        row.put(HI_SEQ_4000_COL_BAIT_SET, baitSet);
                        row.put(HI_SEQ_4000_COL_DESCRIPTION,
                                CsvHelper.formatCsvField(additionalInfo.get("PI").replaceAll(" ", "_")));
                    }

                    // as of Mar. 2019 pooled normals only exist for humans and mice, species is not
                    // set in LIMS for pooled normals
                    if (additionalInfo.containsKey("IS_CONTROL") && "".equals(species)) {
                        if (igoId.contains("MOUSE"))
                            row.put(HI_SEQ_4000_COL_SAMPLE_PLATE, "Mouse");
                        else
                            row.put(HI_SEQ_4000_COL_SAMPLE_PLATE, "Human");
                    }

                    if (!additionalInfo.get("RECIPE").equalsIgnoreCase(DLP_RECIPE)) {
                        if (additionalInfo.containsKey("SPIKE_IN")) {
                            row.put(HI_SEQ_4000_COL_SAMPLE_WELL,
                                    CsvHelper.formatCsvField(recipe + ":" + additionalInfo.containsKey("SPIKE_IN")));
                        } else {
                            row.put(HI_SEQ_4000_COL_SAMPLE_WELL, CsvHelper.formatCsvField(recipe));
                        }
                        if (isDualBarcoded) {
                            row.put(HI_SEQ_4000_COL_INDEX_TAG2, indexTag2);
                            Barcode b = new Barcode(indexTag, indexTag2);
                            barcodes.get((int) laneNum - 1).add(b);
                        } else {
                            Barcode b = new Barcode(indexTag);
                            barcodes.get((int) laneNum - 1).add(b);
                        }
                    }

                    if (!sp1Request && row.size() > 0) {
                        fileTemplateUtil.addRow(row);
                    } else if (!sp1Processed2Request.containsKey(laneNum) && row.size() > 0) {
                        fileTemplateUtil.addRow(row);
                        sp1Processed2Request.put(laneNum, sampleProject);
                    } else if (!sampleProject.equals(sp1Processed2Request.get(laneNum))) {
                        System.err.println("WARNING: Trying to run two different sp1 requests in the same lane: "
                                + sampleProject + " and " + sp1Processed2Request.get(laneNum));
                    }
                }
            }
        }
        // Set the number of barcode mismatches for the demux to 0 or 1
        Integer[] mismatches = BarcodeMismatch.determineBarcodeMismatches(barcodes, isDualBarcoded);
        fileTemplateUtil.addIndividualValue(HI_SEQ_4000_MISMATCH1, Integer.toString(mismatches[0]));
        if (isDualBarcoded) {
            fileTemplateUtil.addIndividualValue(HI_SEQ_4000_MISMATCH2, Integer.toString(mismatches[1]));
        }

        if (!useableSheet) {
            // sample sheet has mixed length barcodes that we don't know how to handle throw
            // a warning
            System.err.println(
                    "WARNING: Mixed length barcodes where we don't know the padding for short barcode. Sample sheet generated but will not automatically work.");
        }

        if (errorList.size() != 0) {
            StringBuilder sb = new StringBuilder();
            for (String error : errorList) {
                sb.append(error);
                sb.append("\n");
            }
            System.err.println(sb.toString());
        }
        return fileTemplateUtil.generateOutput();
    }

    public static <K, V> Map<K, String> diffMaps(Map<K, V> map1, Map<K, V> map2) {
        Map<K, String> differences = new HashMap<>();

        Set<K> allKeys = new java.util.HashSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (K key : allKeys) {
            if (!map1.containsKey(key)) {
                differences.put(key, "only in NEW map!");
            } else if (!map2.containsKey(key)) {
                differences.put(key, "only in FORMER map!");
            } else if (!map1.get(key).equals(map2.get(key))) {
                differences.put(key, "different values for " + key + " FORMER map value = " + map1.get(key)
                        + " NEW map value = " + map2.get(key));
            }
        }
        return differences;
    }

    /**
     * For example this is a runinfo.xml for a single indexed run:
     * <Reads>
     * <Read Number="1" NumCycles="27" IsIndexedRead="N" />
     * <Read Number="2" NumCycles="8" IsIndexedRead="Y" />
     * <Read Number="3" NumCycles="99" IsIndexedRead="N" />
     * </Reads>
     *
     * @param runInfoDetails
     * @return
     */
    private boolean setIgnoreDualBarcodes(RunInfoDetails runInfoDetails) {
        if (runInfoDetails.reads.size() >= 3) {
            Read read3 = runInfoDetails.reads.get(2);
            System.out.println("READ3:" + read3);
            if (read3.isIndexed)
                return false; // this is a dual barcoded runInfo.xml, do not ignore the 2nd barcode
            return true;
        }
        return true;
    }

    private List<String> getReads(RunInfoDetails runInfoDetails) {
        List<String> reads = new ArrayList<>();
        for (Read read : runInfoDetails.reads) {
            if (!read.isIndexed)
                reads.add(Integer.toString(read.numCycles));
        }

        return reads;
    }

    private HashMap<String, String> paddingMap(String machineType) throws Exception {
        HashMap<String, String> id2padding = new HashMap<>();
        // Illumina TruSeq adapter sequences
        id2padding.put("TS1", "AT");
        id2padding.put("TS2", "AT");
        id2padding.put("TS3", "AT");
        id2padding.put("TS4", "AT");
        id2padding.put("TS5", "AT");
        id2padding.put("TS6", "AT");
        id2padding.put("TS7", "AT");
        id2padding.put("TS8", "AT");
        id2padding.put("TS9", "AT");
        id2padding.put("TS10", "AT");
        id2padding.put("TS11", "AT");
        id2padding.put("TS12", "AT");
        id2padding.put("TS13", "CA");
        id2padding.put("TS14", "GT");
        id2padding.put("TS15", "GA");
        id2padding.put("TS16", "CG");
        id2padding.put("TS18", "AC");
        id2padding.put("TS19", "CG");
        id2padding.put("TS20", "TT");
        id2padding.put("TS21", "GA");
        id2padding.put("TS22", "TA");
        id2padding.put("TS23", "AT");
        id2padding.put("TS25", "AT");
        id2padding.put("TS27", "TT");

        for (int i = 1; i < 49; i++) {
            id2padding.put("RPI" + Integer.toString(i), "AT");
            id2padding.put("IDT-TS" + Integer.toString(i), "AT");
        }
        for (int i = 1; i < 10; i++) {
            id2padding.put("NF0" + Integer.toString(i), "AT");
        }
        for (int i = 1; i < 49; i++) {
            id2padding.put("NF" + Integer.toString(i), "AT");
        }

        for (int i = 1; i < 49; i++) {
            id2padding.put("DMP" + Integer.toString(i), "AT");
        }
        for (int i = 1; i < 125; i++) {
            id2padding.put("Ptashne_" + Integer.toString(i), "A");
        }
        for (int i = 1; i < 13; i++) {
            id2padding.put("NEBNext" + Integer.toString(i), "AT");
        }
        id2padding.put("NEBNext13", "CA");
        id2padding.put("NEBNext14", "GT");
        id2padding.put("NEBNext15", "GA");
        id2padding.put("NEBNext16", "AT");
        id2padding.put("NEBNext18", "AC");
        id2padding.put("NEBNext19", "CG");
        id2padding.put("NEBNext20", "TT");
        id2padding.put("NEBNext21", "GA");
        id2padding.put("NEBNext22", "TA");
        id2padding.put("NEBNext23", "AT");
        id2padding.put("NEBNext25", "AT");
        id2padding.put("NEBNext27", "TT");

        // 10X
        id2padding.put("CITE_1", "AT");
        id2padding.put("CITE_2", "AT");
        id2padding.put("CITE_3", "AT");
        id2padding.put("CITE_4", "AT");
        id2padding.put("CITE_5", "AT");
        id2padding.put("CITE_6", "AT");
        id2padding.put("CITE_7", "AT");
        id2padding.put("CITE_8", "AT");
        id2padding.put("CITE_9", "AT");

        if (machineType.equals(INSTRUMENT_NEXTSEQ) || machineType.equals(INSTRUMENT_HISEQ4000)
                || (machineType.equalsIgnoreCase(INSTRUMENT_NOVASEQ)))
            return id2padding;
        else {
            for (String key : id2padding.keySet()) {
                String value = id2padding.get(key);
                System.out.println("Reverse complementing: " + key + ":" + reverseComplement(value));
                id2padding.put(key, reverseComplement(value));
            }
        }

        return id2padding;
    }

    public String getRunId() throws RemoteException, NotFound, IoError, ServerException {
        // First try to find the RunID from the IlluminaHiSeqRunParameter, which should
        // exist for HiSeq runs,
        // then if there are no IlluminaHiSeqRunParameters, check to see if MiSeq
        // information exists
        // if that isn't present, just use the flow cell id as the name

        DataRecord[] hiRunParameters = experiment.getChildrenOfType("IlluminaHiSeqRunParameters", user);
        DataRecord[] nextRunParameters = experiment.getChildrenOfType("IlluminaNextSeqRunParameters", user);
        DataRecord[] next2kRunParameters = experiment.getChildrenOfType("NextSeq2KRunParameter", user);
        DataRecord[] miseqRunParameters = experiment.getChildrenOfType("IlluminaMiSeqRunParameters", user);
        DataRecord[] novaRunParameters = experiment.getChildrenOfType("IlluminaNovaSeqRunParameters", user);
        DataRecord[] novaSeqXRunParameters = experiment.getChildrenOfType("IlluminaNovaSeqXRunParameters", user);

        String runId = experiment.getStringVal("FlowcellId", user);
        ;
        if (hiRunParameters.length > 0) {
            runId = hiRunParameters[0].getStringVal("RunID", user);
        } else if (nextRunParameters.length > 0) {
            runId = nextRunParameters[0].getStringVal("RunID", user);
        } else if (next2kRunParameters.length > 0) {
            runId = next2kRunParameters[0].getStringVal("RunId", user);
        } else if (miseqRunParameters.length > 0) {
            runId = miseqRunParameters[0].getStringVal("RunID", user);
        } else if (novaRunParameters.length > 0) {
            runId = novaRunParameters[0].getStringVal("RunId", user);
            System.out.println("This is a NovaSeq run.");
            isNova = true;
        } else if (novaSeqXRunParameters.length > 0) {
            System.out.println("This is a NovaSeqX run.");
            isNovaX = true;
            runId = novaSeqXRunParameters[0].getStringVal("RunId", user);
        }
        String sequencer = experiment.getStringVal(DT_IlluminaSeqExperiment.SEQUENCER_INSTRUMENT, user);
        if ("FAUCI2".equals(sequencer) || "BONO".equals(sequencer)) {
            isNovaX = true;
            System.out.println("This is a NovaSeqX run on FAUCI2 or BONO");
        }

        return runId;
    }

    /**
     * Returns the path to the RunInfo.xml file or the RunParameters.xml file.
     * 
     * @return
     * @throws RemoteException
     * @throws ServerException
     * @throws NotFound
     * @throws IoError
     * @throws CustomWorkflowException
     */
    private File getRunXmlFile() throws RemoteException, ServerException, NotFound, IoError, CustomWorkflowException {
        // for example /igo/sequencers/fauci/231006_FAUCI_0050_A22C3WKLT3/
        String sequencerRunFolder_Loc = (String) experiment.getDataField(DT_IlluminaSeqExperiment.SEQUENCER_RUN_FOLDER,
                user);
        File sequencerRunFolder = new File(sequencerRunFolder_Loc);

        File xmlFile = null;

        // First try local access
        if (sequencerRunFolder.exists()) {
            for (File file : sequencerRunFolder.listFiles()) {
                if ("RunInfo.xml".equalsIgnoreCase(file.getName())) {
                    xmlFile = file;
                    break;
                }
            }
        } else {
            // If local path doesn't exist, try SSH to igo-ln01
            logInfo("Local path not found, SSH functionality temporarily disabled for debugging: "
                    + sequencerRunFolder_Loc);
            xmlFile = getRunXmlFileViaSSH(sequencerRunFolder_Loc);
        }

        return xmlFile;
    }

    /**
     * Retrieves RunInfo.xml file via SSH from igo-ln01 when running locally
     */
    private File getRunXmlFileViaSSH(String remotePath) {
        try {
            System.out.println("Local path not accessible, attempting SSH to igo-ln01 for: " + remotePath);

            String remoteFilePath = remotePath + "RunInfo.xml";
            String tempDir = System.getProperty("java.io.tmpdir");
            String localTempFile = tempDir + "RunInfo_" + System.currentTimeMillis() + ".xml";

            // Use scp to copy the file from igo-ln01
            ProcessBuilder pb = new ProcessBuilder(
                    "scp",
                    "-v",
                    "-i", "~/.ssh/id_rsa",
                    "timalr@igo-ln01:" + remoteFilePath,
                    localTempFile);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File tempFile = new File(localTempFile);
                if (tempFile.exists()) {
                    System.out.println("Successfully retrieved RunInfo.xml via SSH: " + localTempFile);
                    // Mark for deletion on exit
                    tempFile.deleteOnExit();
                    return tempFile;
                }
            } else {
                System.out.println("SCP failed with exit code: " + exitCode);
                // Try alternative: RunParameters.xml
                System.out.println("SCP process did not complete successfully. Exit code: " + exitCode);
            }

        } catch (Exception e) {
            System.out.println("SSH retrieval failed: " + e.getMessage());
            // Try RunParameters.xml as fallback
            return getRunParametersXmlViaSSH(remotePath);
        }

        return null;
    }

    /**
     * Fallback: try to get RunParameters.xml via SSH
     */
    private File getRunParametersXmlViaSSH(String remotePath) {
        try {
            String remoteFilePath = remotePath + "/RunParameters.xml";
            String tempDir = System.getProperty("java.io.tmpdir");
            String localTempFile = tempDir + "/RunParameters_" + System.currentTimeMillis() + ".xml";

            ProcessBuilder pb = new ProcessBuilder(
                    "scp",
                    "-v",
                    "-i", "~/.ssh/id_rsa",
                    "igo@igo-ln01:" + remoteFilePath,
                    localTempFile);

            pb.environment().put("SSH_AUTH_SOCK", System.getenv("SSH_AUTH_SOCK"));
            pb.environment().put("SSH_AGENT_PID", System.getenv("SSH_AGENT_PID"));

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File tempFile = new File(localTempFile);
                if (tempFile.exists()) {
                    logInfo("Successfully retrieved RunParameters.xml via SSH: " + localTempFile);
                    tempFile.deleteOnExit();
                    return tempFile;
                }
            }

        } catch (Exception e) {
            logInfo("Failed to retrieve RunParameters.xml via SSH: " + e.getMessage());
        }

        return null;
    }

    private HashMap<String, Object> getDLPFieldFileData(String chipNumber) {
        HashMap<String, Object> file = new HashMap<>();
        String recordId = null;
        try {
            System.out.println("Searching for RecordId ='" + recordId + "'" + " file name contains: " + chipNumber);
            // search ExemplarSDMSFile table first where QC files are after August 12, 2023
            List<DataRecord> matched = dataRecordManager.queryDataRecords("ExemplarSDMSFile", "RecordId =" + recordId,
                    user);
            if (matched.size() > 0) {
                System.out.println("Files found: " + matched.size());
                String fileName = (String) matched.get(0).getDataField("FilePath", user);
                InputStream is = matched.get(0).openAttachmentDataInputStream(user);
                byte[] data = is.readAllBytes();
                System.out.println("Bytes read:" + data.length);
                file.put("fileName", fileName);
                file.put("data", data);
                is.close();
            } else if (matched.size() == 0) {
                if (recordId != null)
                    matched = dataRecordManager.queryDataRecords("Attachment", "RecordId =" + recordId, user);
                else {
                    matched = dataRecordManager.queryDataRecords("Attachment",
                            "FILEPATH LIKE '%" + chipNumber + "%.fld'", user);
                    System.out
                            .println("Matched size is zero! Looking for file which contains chipNumber: " + chipNumber);
                    if (matched == null || matched.size() == 0) {
                        System.err.println("No DLP field file found with chip number: " + chipNumber);
                        return null;
                    }
                }

                String fileName = (String) matched.get(0).getDataField("FilePath", user);
                System.out.println("Found fileName: " + fileName);
                byte[] data = matched.get(0).getAttachmentData(user);
                System.out.println("Found FLD data size = " + data.length);
                file.put("fileName", fileName);
                file.put("data", data);
            }
            return file;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This function filters DLP sample sheet records based on the field file.
     * 
     * @param DLPSampleSheet
     * @param fldFile
     * @author mirhajf
     */
    private Set<String> ConstructDLPFieldFileSet(String chipNumber) {
        /*
         * 5th field in the sample sheet contains the chip location
         * Lane,Sample_ID,Sample_Plate,Sample_Well,I7_Index_ID,index,index2,
         * Sample_Project,Description
         * 1,Lee_DLP_BC_02_Breast_130092A_1_1_IGO_15607_B_1,Human,DLP,DLPi7_01-i5_01,
         * ACAGTGAT,ACCGTGAT,Project_15607_B,shahs3@mskcc.org
         * DLPi7_01-i5_01: split by "_", take indexes 1 (split by "-" and take index 0)
         * and 3
         * on the fld file find 1/1. On the filtered sample sheet only copy the lines
         * with chip spots that has a value on its corresponding fld line
         **/
        try {
            byte[] bytes = (byte[]) getDLPFieldFileData(chipNumber).get("data");
            File fldFile = File.createTempFile("fieldFile", ".fld");
            FileOutputStream fos = new FileOutputStream(fldFile);
            fos.write(bytes);
            fos.close();
            FileInputStream fis = new FileInputStream(fldFile);
            BufferedReader fieldFileReader = new BufferedReader(new InputStreamReader(fis));
            String fieldLine, sampleSheetLine;
            Set<String> fieldRowCol = new HashSet<>();
            String regex = "\\d+/\\d+";
            Pattern pattern = Pattern.compile(regex);
            fieldLine = fieldFileReader.readLine();
            while (fieldLine != null && !fieldLine.isEmpty()) {
                Matcher matcher = pattern.matcher(fieldLine);
                if (matcher.find()) {
                    String fieldRow = fieldLine.split("/")[0];
                    String fieldCol = fieldLine.split("/")[1].split("\\s")[0];
                    // Populate a hashSet for the fields with value then check the sample sheet row
                    // and col for those fields
                    if (fieldLine.split(",").length == 2) {
                        fieldRowCol.add(fieldRow.trim() + "/" + fieldCol.trim());
                        System.out
                                .println("Added row/column to the fld set: " + fieldRow.trim() + "/" + fieldCol.trim());
                    }
                } else { // skip
                    System.out.println("The fld file line doesn't match the pattern!");
                }
                fieldLine = fieldFileReader.readLine();
            }
            return fieldRowCol;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * <p>
     * This is a container used to hold values from the RunInfo.xml file associated
     * with the current Illumina Sequencing
     * experiment.
     * </p>
     *
     * <p>
     * Upon initiation, the constructor of this class will parse the provided xml
     * file and populate its own fields.
     * Most of the document fields should be referenced by this object. Fields in
     * accordance to the document are as
     * follows:
     * 
     * <pre>
     * &#60?xml version="1.0"?>
     * &#60RunInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" Version="2">
     *   &#60Run Id="<font color="blue">runId</font>" Number="<font color=
    "blue">runNumber</font>">
     *     &#60Flowcell><font color="blue">flowCellId</font>&#60/Flowcell>
     *     &#60Instrument><font color="blue">instrument</font>&#60/Instrument>
     *     &#60Date><font color="blue">date</font>&#60/Date>
     *     &#60Reads>
     *       &#60Read Number="<font color=
    "blue">Read.number</font>" NumCycles="<font color=
    "blue">Read.numCycles</font>" IsIndexedRead="Read.isIndexed" />
     *     &#60/Reads>
     *     &#60FlowcellLayout LaneCount="<font color=
    "blue">FlowcellLayout.laneCount</font>" SurfaceCount="<font color=
    "blue">FlowcellLayout.surfaceCount</font>" SwathCount="<font color=
    "blue">FlowcellLayout.swathCount</font>" TileCount="<font color=
    "blue">FlowcellLayout.tileCount</font>" />
     *     &#60AlignToPhiX>
     *       &#60Lane><font color="blue">lanes.get(0)</font>&#60/Lane>
     *       &#60Lane><font color="blue">lanes.get(1)</font>&#60/Lane>
     *     &#60/AlignToPhiX>
     *   &#60/Run>
     * &#60/RunInfo>
     * </pre>
     *
     * @author Dmitri Petanov
     */
    // [FR-15528 - Provide a RunInfo.xml file parsing feature to retrieve sequencing
    // details during sample sheet creation.]
    private class RunInfoDetails {
        // -- Variables containing RunInfo.xml file values
        String runId;
        int runNumber;
        String flowCellId;
        String instrument;
        Date date;
        List<Read> reads = new ArrayList<>();
        FlowcellLayout flowcellLayout;
        List<Integer> lanes = new ArrayList<>();

        public RunInfoDetails(String runId, int runNumber, String flowCellId, String instrument, Date date,
                List<Read> reads) {
            this.runId = runId;
            this.runNumber = runNumber;
            this.flowCellId = flowCellId;
            this.instrument = instrument;
            this.date = date;
            this.reads = reads;
        }

        /**
         * This constructor will both instantiate the RunInfoDetails object and populate
         * it with values from the RunInfo.xml
         * file provided via a parameter.
         *
         * @param runInfoXmlFile
         * @throws ParserConfigurationException
         * @throws SAXException
         * @throws IOException
         * @throws ParseException
         */
        RunInfoDetails(File runInfoXmlFile)
                throws ParserConfigurationException, SAXException, IOException, ParseException {

            if (runInfoXmlFile.getName().contains("RunInfo")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(runInfoXmlFile);

                doc.getDocumentElement().normalize();

                // -- Begin breaking the document down into its various components
                Element runDetails = (Element) doc.getElementsByTagName("Run").item(0);

                this.runId = runDetails.getAttribute("Id");
                this.runNumber = Integer.parseInt(runDetails.getAttribute("Number"));
                this.flowCellId = ((Element) runDetails.getElementsByTagName("Flowcell").item(0)).getTextContent();
                this.instrument = ((Element) runDetails.getElementsByTagName("Instrument").item(0)).getTextContent();

                // -- The date in the file is numeric, but it is in a format that lists a year,
                // month and day. Not a long value
                String dateText = ((Element) runDetails.getElementsByTagName("Date").item(0)).getTextContent();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMdd");
                if (isNova) {
                    simpleDateFormat = new SimpleDateFormat("d/M/yyyy H:mm:ss a");
                }
                this.date = simpleDateFormat.parse(dateText);

                // Reads are defined as a list of elements. Break them up and keep them in
                // individual classes
                NodeList readNodes = ((Element) runDetails.getElementsByTagName("Reads").item(0))
                        .getElementsByTagName("Read");

                for (int readCounter = 0; readCounter < readNodes.getLength(); readCounter++) {
                    Element readElement = (Element) readNodes.item(readCounter);
                    int number = Integer.parseInt(readElement.getAttribute("Number"));
                    int numCycles = Integer.parseInt(readElement.getAttribute("NumCycles"));
                    boolean isIndexed = "Y".equalsIgnoreCase(readElement.getAttribute("IsIndexedRead"));

                    reads.add(new Read(number, numCycles, isIndexed));
                }

                // Because a flow cell layout element contains a number of various values, it
                // would make more sense
                // to break it out into its own class
                Element flowCellLayoutElement = (Element) runDetails.getElementsByTagName("FlowcellLayout").item(0);

                int laneCount = Integer.parseInt(flowCellLayoutElement.getAttribute("LaneCount"));
                int surfaceCount = Integer.parseInt(flowCellLayoutElement.getAttribute("SurfaceCount"));
                int swathCount = Integer.parseInt(flowCellLayoutElement.getAttribute("SwathCount"));
                int tileCount = Integer.parseInt(flowCellLayoutElement.getAttribute("TileCount"));

                this.flowcellLayout = new FlowcellLayout(laneCount, surfaceCount, swathCount, tileCount);

                // Lanes are just numeric values that indicate a lane number. This may not be a
                // useful peice of data, but
                // might as well grab it since we're here
                // But AlignToPhiX not part of miseqs so first see if it exists
                NodeList phixes = runDetails.getElementsByTagName("AlignToPhiX");
                if (phixes.getLength() > 0) {
                    NodeList laneNodes = ((Element) phixes.item(0)).getElementsByTagName("Lane");

                    for (int laneCounter = 0; laneCounter < laneNodes.getLength(); laneCounter++) {
                        lanes.add(Integer.parseInt(((Element) laneNodes.item(laneCounter)).getTextContent()));
                    }
                }
            }

            if (runInfoXmlFile.getName().contains("RunParameters.xml")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(runInfoXmlFile);

                doc.getDocumentElement().normalize();

                // -- Begin breaking the document down into its various components
                Element runDetails = (Element) doc.getElementsByTagName("RunParameters").item(0);
                this.runId = runInfoXmlFile.getParent();
                this.runNumber = Integer
                        .parseInt(((Element) runDetails.getElementsByTagName("RunCounter").item(0)).getTextContent());
                String runPath = runInfoXmlFile.getParent();
                this.flowCellId = runPath.substring(runPath.length() - 9, runPath.length());
                this.instrument = "FAUCI2";

                // Reads are defined as a list of elements. Break them up and keep them in
                // individual classes
                NodeList readNodes = ((Element) runDetails.getElementsByTagName("PlannedReads").item(0))
                        .getElementsByTagName("Read");
                for (int readCounter = 0; readCounter < readNodes.getLength(); readCounter++) {
                    Element readElement = (Element) readNodes.item(readCounter);
                    int number = readCounter;
                    int numCycles = Integer.parseInt(readElement.getAttribute("Cycles"));
                    boolean isIndexed = Boolean.FALSE;
                    if (readElement.getAttribute("ReadName").contains("Index"))
                        isIndexed = Boolean.TRUE;

                    reads.add(new Read(number, numCycles, isIndexed));
                }
            }
        }

        @Override
        public String toString() {
            return "RunInfoDetails{" +
                    "runId='" + runId + '\'' +
                    ", runNumber=" + runNumber +
                    ", flowCellId='" + flowCellId + '\'' +
                    ", instrument='" + instrument + '\'' +
                    ", date=" + date +
                    ", reads=" + reads +
                    ", flowcellLayout=" + flowcellLayout +
                    ", lanes=" + lanes +
                    '}';
        }
    }

    public List<Read> readsFromReadLength(String input) {
        if (input == null) {
            ArrayList x = new ArrayList<Read>(4);
            return x;
        }

        // 29/10/10/89
        String[] reads = input.split("/");
        ArrayList<Read> readsList = new ArrayList<Read>();
        int i = 1;
        for (String read : reads) {
            Integer cycles = Integer.parseInt(read);
            boolean isIndexed = false;
            if (cycles < 15)
                isIndexed = true;
            Read x = new Read(i++, cycles.intValue(), isIndexed);
            readsList.add(x);
        }
        return readsList;
    }

    /**
     * This is a container for Read details obtained from the RunInfo.xml file.
     *
     * @author Dmitri Petanov
     */
    // [FR-15528 - Provide a RunInfo.xml file parsing feature to retrieve sequencing
    // details during sample sheet creation.]
    private class Read {
        int number;
        int numCycles;
        boolean isIndexed;

        public Read(int number, int numCycles, boolean isIndexed) {
            this.number = number;
            this.numCycles = numCycles;
            this.isIndexed = isIndexed;
        }

        @Override
        public String toString() {
            return "Read{" +
                    "number=" + number +
                    ", numCycles=" + numCycles +
                    ", isIndexed=" + isIndexed +
                    '}';
        }
    }

    /**
     * This is a container for the flowcell layout details obtained from the
     * RunInfo.xml file.
     *
     * @author Dmitri Petanov
     */
    // [FR-15528 - Provide a RunInfo.xml file parsing feature to retrieve sequencing
    // details during sample sheet creation.]
    private class FlowcellLayout {
        int laneCount;
        int surfaceCount;
        int swathCount;
        int tileCount;

        FlowcellLayout(int laneCount, int surfaceCount, int swathCount, int tileCount) {
            this.laneCount = laneCount;
            this.surfaceCount = surfaceCount;
            this.swathCount = swathCount;
            this.tileCount = tileCount;
        }
    }
}
