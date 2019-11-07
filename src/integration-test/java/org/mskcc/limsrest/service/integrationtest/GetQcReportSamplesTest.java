package org.mskcc.limsrest.service.integrationtest;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.junit.After;
import org.junit.Before;
import org.mskcc.limsrest.controller.GetQcReportSamples;
import org.mskcc.limsrest.service.assignedprocess.QcParentSampleRetriever;
import org.mskcc.limsrest.service.GetQcReportSamplesTask;

import org.mskcc.util.VeloxConstants;

import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mskcc.domain.AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;

public class GetQcReportSamplesIntegrationTest {
    private static DataRecordManager dataRecordManager;
    private static User user;
    private static VeloxConnection veloxConnection;


    private final static String REQUEST_ID = "04592_F";

    private final static String CMO_SAMPLE_ID = "C-666666-Y6-d";
    private final static String CHILD_CMO_SAMPLE_ID = "C-666666-Y61-d";
    private static final String connectionFile = "/lims-tango-dev.properties";

    private GetQcReportSamplesTask getQcReportSamplesTask;
    private GetQcReportSamples getQcReportSamples;
    private DataRecord sample;


    @Before
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileReader(GetQcReportSamplesIntegrationTest.class.getResource(connectionFile).getPath()));
        veloxConnection = new VeloxConnection(
                (String) properties.get("lims.host"),
                Integer.parseInt((String) properties.get("lims.port")),
                (String) properties.get("lims.guid"),
                (String) properties.get("lims.username"),
                (String) properties.get("lims.password")
        );
        veloxConnection.open();
        dataRecordManager = veloxConnection.getDataRecordManager();
        user = veloxConnection.getUser();


//        List<DataRecord> requests = dataRecordManager.queryDataRecords("Request", "RequestId = '" + REQUEST_ID + "'",
//                user);
//
//        DataRecord request = requests.get(0);
//
//        sample = request.addChild(VeloxConstants.SAMPLE, user);
//        sample.setDataField(DT_Sample.REQUEST_RECORD_ID_LIST, REQUEST_ID, user);
//        sample.setDataField(DT_Sample.SAMPLE_ID, IGO_ID, user);
//        sample.setDataField(DT_Sample.OTHER_SAMPLE_ID, CMO_SAMPLE_ID, user);
//        sample.setDataField(DT_Sample.EXEMPLAR_SAMPLE_TYPE, "DNA Library", user);
//
//        childSample = sample.addChild(VeloxConstants.SAMPLE, user);
//        childSample.setDataField(DT_Sample.REQUEST_RECORD_ID_LIST, REQUEST_ID, user);
//        childSample.setDataField(DT_Sample.SAMPLE_ID, CHILD_IGO_ID, user);
//        childSample.setDataField(DT_Sample.OTHER_SAMPLE_ID, CHILD_CMO_SAMPLE_ID, user);
//        childSample.setDataField(DT_Sample.EXEMPLAR_SAMPLE_TYPE, "DNA Library", user);
//
//        seqQc = sample.addChild("SeqAnalysisSampleQC", user);
//
//        dataRecordManager.storeAndCommit("Initiating records for QcStatus assign integration test", null, user);
    }

    @After
    public void tearDown() throws Exception {
        try {
            dataRecordManager.deleteDataRecords(Arrays.asList(seqQc, sample, childSample), null, true, user);

            dataRecordManager.storeAndCommit("Cleaning up after Qc Assign integration test", null, user);
        } finally {
            veloxConnection.close();
        }
    }

    private DataRecord getPool(DataRecord seqQc) throws Exception {
        return initialPoolRetriever.retrieve(seqQc, user);
    }

    private void assertAssignedProcessProperties(DataRecord sample, DataRecord assignedProcess) throws Exception {
        assertAssignedProcessIgoSampleId(sample, assignedProcess);
        assertAssignedProcessSampleRecordId(sample, assignedProcess);
        assertAssignedProcessCmoSampleId(sample, assignedProcess);
        assertAssignedProcessRequestRecordId(sample, assignedProcess);
        assertAssignedProcessStatus(assignedProcess);
        assertAssignedProcessName(assignedProcess);
    }

    private List<DataRecord> getAssignedProcesses(DataRecord sample) throws IoError, RemoteException {
        return sample.getParentsOfType(DT_AssignedProcess.DATA_TYPE, user);
    }

    private DataRecord getParentSample(DataRecord seqQc) throws Exception {
        return qcParentSampleRetriever.retrieve(seqQc, user);
    }

    private DataRecord getSampleLevelQc(String recordId) throws NotFound, IoError, RemoteException {
        List<DataRecord> seqQcs = dataRecordManager.queryDataRecords(VeloxConstants.SEQ_ANALYSIS_SAMPLE_QC, "RecordId = '" + recordId + "'", user);

        assertThat(String.format("There is no sequence level qc for record: %s", recordId), seqQcs.size(), is(1));

        return seqQcs.get(0);
    }

    private void assertSampleState(DataRecord sample) throws Exception {
        assertThat(sample.getStringVal(DT_Sample.EXEMPLAR_SAMPLE_STATUS, user), is(String.format("Ready for - %s", PRE_SEQUENCING_POOLING_OF_LIBRARIES.getWorkflowName())));
    }

    private void assertAssignedProcessChildSample(DataRecord sample, DataRecord assignedProcess) throws Exception {
        DataRecord[] samples = assignedProcess.getChildrenOfType(DT_Sample.DATA_TYPE, user);
        assertThat(samples.length, is(1));
        assertThat(samples[0], is(sample));
    }

    private void assertAssignedProcessName(DataRecord assignedProcess) throws Exception {
        assertThat(assignedProcess.getStringVal(DT_AssignedProcess.PROCESS_NAME, user), is(PRE_SEQUENCING_POOLING_OF_LIBRARIES.getName()));

    }

    private void assertAssignedProcessStatus(DataRecord assignedProcess) throws Exception {
        assertThat(assignedProcess.getStringVal(DT_AssignedProcess.STATUS, user), is(String.format("Ready for - %s", PRE_SEQUENCING_POOLING_OF_LIBRARIES.getWorkflowName())));
    }

    private void assertAssignedProcessRequestRecordId(DataRecord sample, DataRecord assignedProcess) throws Exception {
        assertThat(String.valueOf(assignedProcess.getLongVal(DT_AssignedProcess.REQUEST_RECORD_ID, user)), is(sample.getStringVal(DT_Sample.REQUEST_RECORD_ID_LIST, user)));
    }

    private void assertAssignedProcessCmoSampleId(DataRecord sample, DataRecord assignedProcess) throws Exception {
        assertThat(assignedProcess.getStringVal(DT_AssignedProcess.OTHER_SAMPLE_ID, user), is(sample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, user)));
    }

    private void assertAssignedProcessSampleRecordId(DataRecord sample, DataRecord assignedProcess) throws Exception {
        assertThat(assignedProcess.getLongVal(DT_AssignedProcess.SAMPLE_RECORD_ID, user), is(sample.getRecordId()));
    }

    private void assertOneAssignedProcessWasAdded(List<DataRecord> assignedProcessesAfter) {
        assertThat(assignedProcessesAfter.size(), is(1));
    }

    private void assertAssignedProcessIgoSampleId(DataRecord sample, DataRecord assignedProcess) throws NotFound, RemoteException {
        String assignedProcessSampleId = assignedProcess.getStringVal(DT_AssignedProcess.SAMPLE_ID, user);
        String sampleIgoId = sample.getStringVal(DT_Sample.SAMPLE_ID, user);

        assertThat(assignedProcessSampleId, is(sampleIgoId));
    }
}