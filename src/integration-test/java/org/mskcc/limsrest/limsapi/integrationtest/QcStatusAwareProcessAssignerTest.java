package org.mskcc.limsrest.limsapi.integrationtest;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxConnectionException;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mskcc.domain.QCStatus;
import org.mskcc.limsrest.limsapi.assignedprocess.AssignedProcessCreator;
import org.mskcc.limsrest.limsapi.assignedprocess.QcParentSampleRetriever;
import org.mskcc.limsrest.limsapi.assignedprocess.QcStatusAwareProcessAssigner;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;
import org.mskcc.limsrest.limsapi.assignedprocess.resequencepool.InitialPoolRetriever;
import org.mskcc.util.VeloxConstants;

import java.rmi.RemoteException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mskcc.domain.AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;

public class QcStatusAwareProcessAssignerTest {
    private static String connectionFile = "src/integration-test/resources/Connection-test.txt";
    private static DataRecordManager dataRecordManager;
    private static User user;
    private static VeloxConnection veloxConnection;
    private final AssignedProcessConfigFactory configFactory = new AssignedProcessConfigFactory();
    private final AssignedProcessCreator processCreator = new AssignedProcessCreator();
    private final InitialPoolRetriever initialPoolRetriever = new InitialPoolRetriever();
    private final QcParentSampleRetriever qcParentSampleRetriever = new QcParentSampleRetriever();
    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner;

    @BeforeClass
    public static void init() throws VeloxConnectionException {
        veloxConnection = new VeloxConnection(connectionFile);
//        veloxConnection.open();
        dataRecordManager = veloxConnection.getDataRecordManager();
        user = veloxConnection.getUser();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        veloxConnection.close();
    }

    @Before
    public void setUp() throws Exception {
        qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner(configFactory, processCreator);
    }

    @Test
    public void whenStatusIsChangedToRepoolSample_shouldCreateAssignedProcessForParentSampleAssignItAsChildAndChandeSampleStatus() throws Exception {
        String sampleLevelQcRecordId = "258324";
        DataRecord seqQc = getSampleLevelQc(sampleLevelQcRecordId);
        DataRecord sample = getParentSample(seqQc);
        assertAssignedProcess(QCStatus.REPOOL_SAMPLE.getValue(), seqQc, sample);
    }

    @Test
    public void whenStatusIsChangedToResequencePool_shouldCreateAssignedProcessForPoolAssignItAsChildAndChangeSampleStatus() throws Exception {
        String sampleLevelQcRecordId = "258324";
        DataRecord seqQc = getSampleLevelQc(sampleLevelQcRecordId);
        DataRecord sample = getPool(seqQc);
        assertAssignedProcess(QCStatus.RESEQUENCE_POOL.getValue(), seqQc, sample);
    }

    private void assertAssignedProcess(String qcStatus, DataRecord seqQc, DataRecord sample) throws Exception {
        //given
        List<DataRecord> assignedProcessesBefore = getAssignedProcesses(sample);

        //when
        qcStatusAwareProcessAssigner.assign(dataRecordManager, user, seqQc, qcStatus);

        //then
        List<DataRecord> assignedProcessesAfter = getAssignedProcesses(sample);
        assignedProcessesAfter.removeAll(assignedProcessesBefore);
        assertOneAssignedProcessWasAdded(assignedProcessesAfter);

        DataRecord assignedProcess = assignedProcessesAfter.get(0);
        assertAssignedProcessProperties(sample, assignedProcess);
        assertAssignedProcessChildSample(sample, assignedProcess);
        assertSampleState(sample);
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