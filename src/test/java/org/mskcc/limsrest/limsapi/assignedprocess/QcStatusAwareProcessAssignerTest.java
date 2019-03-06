package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Batch;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class QcStatusAwareProcessAssignerTest {
    private final DataRecordManager drmMock = mock(DataRecordManager.class);
    private final DataRecord qcMock = mock(DataRecord.class);
    private AssignedProcessConfigFactory factory = mock(AssignedProcessConfigFactory.class);
    private AssignedProcessCreator creator = mock(AssignedProcessCreator.class);
    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner(factory,
            creator);
    private Map<String, Object> assProcMap = new HashMap<>();
    private User userMock = mock(User.class);
    private AssignedProcessConfig configMock = mock(AssignedProcessConfig.class);
    private DataRecord sample = mock(DataRecord.class);
    private AssignedProcess processToAssign = AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;

    @Before
    public void setUp() throws Exception {
        when(configMock.getSample()).thenReturn(sample);
        when(configMock.getProcessToAssign()).thenReturn(processToAssign);
        when(factory.getProcessAssignerConfig(any(), any(), any(), any())).thenReturn(configMock);
        assProcMap.put("costam", "costam");
        when(drmMock.addDataRecords(DT_AssignedProcess.DATA_TYPE, Collections.singletonList(assProcMap), userMock))
                .thenReturn(Arrays.asList
                        (mock(DataRecord.class)));

        when(creator.create(any(), any(), any())).thenReturn(assProcMap);
    }

    @Test
    public void whenAssignAndNoBatch_shouldAddAssignProcessRecordWithChildSample() throws Exception {
        //given
        when(sample.getParentsOfType(DT_Batch.DATA_TYPE, userMock)).thenReturn(Collections.emptyList());

        //when
        qcStatusAwareProcessAssigner.assign(drmMock, userMock, qcMock, "");

        //then
        verify(drmMock, times(1)).addDataRecords(DT_AssignedProcess.DATA_TYPE, Arrays.asList(assProcMap), userMock);
        verify(sample, times(1)).setDataField(DT_Sample.EXEMPLAR_SAMPLE_STATUS, processToAssign.getStatus(), userMock);
    }

    @Test
    public void whenAssignAndOneBatch_shouldAddAssignProcessRecordWithChildSampleAndRemoveFromThisBatch() throws
            Exception {
        //given
        DataRecord batchDrMock = mock(DataRecord.class);
        when(sample.getParentsOfType(DT_Batch.DATA_TYPE, userMock)).thenReturn(Arrays.asList(batchDrMock));

        //when
        qcStatusAwareProcessAssigner.assign(drmMock, userMock, qcMock, "");

        //then
        verify(drmMock, times(1)).addDataRecords(DT_AssignedProcess.DATA_TYPE, Arrays.asList(assProcMap), userMock);
        verify(sample, times(1)).setDataField(DT_Sample.EXEMPLAR_SAMPLE_STATUS, processToAssign.getStatus(), userMock);
        verify(batchDrMock, times(1)).removeChild(sample, null, userMock);
    }

    @Test
    public void whenAssignAndMultipleBatches_shouldAddAssignProcessRecordWithChildSampleAndRemoveFromBatch() throws
            Exception {
        //given
        DataRecord batchDrMock1 = mock(DataRecord.class);
        DataRecord batchDrMock2 = mock(DataRecord.class);
        DataRecord batchDrMock3 = mock(DataRecord.class);

        when(sample.getParentsOfType(DT_Batch.DATA_TYPE, userMock)).thenReturn(Arrays.asList(batchDrMock1,
                batchDrMock2, batchDrMock3));

        //when
        qcStatusAwareProcessAssigner.assign(drmMock, userMock, qcMock, "");

        //then
        verify(drmMock, times(1)).addDataRecords(DT_AssignedProcess.DATA_TYPE, Arrays.asList(assProcMap), userMock);
        verify(sample, times(1)).setDataField(DT_Sample.EXEMPLAR_SAMPLE_STATUS, processToAssign.getStatus(), userMock);
        verify(batchDrMock1, times(1)).removeChild(sample, null, userMock);
        verify(batchDrMock2, times(1)).removeChild(sample, null, userMock);
        verify(batchDrMock3, times(1)).removeChild(sample, null, userMock);
    }
}
