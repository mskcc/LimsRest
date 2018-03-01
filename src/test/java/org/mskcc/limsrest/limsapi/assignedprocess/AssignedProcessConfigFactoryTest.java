package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.domain.QcStatus;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.config.AssignedProcessConfigFactory;
import org.mskcc.limsrest.limsapi.assignedprocess.repoolsample.RepoolSampleAssignedAssignedProcessConfig;
import org.mskcc.limsrest.limsapi.assignedprocess.resequencepool.ResequencePoolAssignedProcessConfig;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssignedProcessConfigFactoryTest {
    private final DataRecord qcMock = mock(DataRecord.class);
    private AssignedProcessConfigFactory assignedProcessConfigFactory = new AssignedProcessConfigFactory();

    @Test
    public void whenFactoryMethodIsInvoked_shouldReturnProperAssignedProcessConfig() throws Exception {
        assertAssignedProcessConfig(QcStatus.REPOOL_SAMPLE.getValue(), RepoolSampleAssignedAssignedProcessConfig.class);
        assertAssignedProcessConfig(QcStatus.RESEQUENCE_POOL.getValue(), ResequencePoolAssignedProcessConfig.class);
    }

    private void assertAssignedProcessConfig(String qcStatus, Class<? extends AssignedProcessConfig> configClass) throws Exception {
        when(qcMock.getParentsOfType(any(), any())).thenReturn(Arrays.asList(mock(DataRecord.class)));
        AssignedProcessConfig processAssigner = assignedProcessConfigFactory.getProcessAssignerConfig(qcStatus, mock(DataRecordManager.class), qcMock, mock(User.class));
        assertThat(processAssigner.getClass(), IsCompatibleType.typeCompatibleWith(configClass));
    }
}