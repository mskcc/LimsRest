package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.AssignedProcess;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class QcStatusAwareProcessAssignerTest {
    private AssignedProcessConfigFactory factory = mock(AssignedProcessConfigFactory.class);
    private AssignedProcessCreator creator = mock(AssignedProcessCreator.class);
    private QcStatusAwareProcessAssigner qcStatusAwareProcessAssigner;
    private Map<String, Object> map = new HashMap<>();
    private User userMock = mock(User.class);
    private AssignedProcessConfig configMock = mock(AssignedProcessConfig.class);
    private DataRecord sample = mock(DataRecord.class);

    @Before
    public void setUp() throws Exception {
        when(configMock.getSample()).thenReturn(sample);
        when(configMock.getProcessToAssign()).thenReturn(AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES);
        when(factory.getProcessAssignerConfig(any(), any(), any(), any())).thenReturn(configMock);

        map.put("costam", "costam");
        when(creator.create(any(), any(), any())).thenReturn(map);

        qcStatusAwareProcessAssigner = new QcStatusAwareProcessAssigner(factory, creator);
    }

    @Test
    public void whenAssign_shouldAddAssignProcessRecordWithChildSample() throws Exception {
        DataRecordManager drmMock = mock(DataRecordManager.class);
        DataRecord qcMock = mock(DataRecord.class);

        qcStatusAwareProcessAssigner.assign(drmMock, userMock, qcMock, "");

        verify(drmMock, times(1)).addDataRecords(DT_AssignedProcess.DATA_TYPE, Arrays.asList(map), userMock);
    }
}