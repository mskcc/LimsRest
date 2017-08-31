package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_Sample;
import org.junit.Test;
import org.mskcc.domain.AssignedProcess;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssignedProcessCreatorTest {
    private AssignedProcessCreator assignedProcessCreator = new AssignedProcessCreator();
    private User userMock = mock(User.class);

    @Test
    public void when_should() throws Exception {
        String igoId = "igoId";
        String cmoId = "cmoId";

        AssignedProcess assignedProcess = AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;

        DataRecord sample = mock(DataRecord.class);
        when(sample.getStringVal(DT_Sample.SAMPLE_ID, userMock)).thenReturn(igoId);
        when(sample.getStringVal(DT_Sample.OTHER_SAMPLE_ID, userMock)).thenReturn(cmoId);

        Map<String, Object> assignedProcessMap = assignedProcessCreator.create(sample, assignedProcess, userMock);

        assertThat(assignedProcessMap.containsKey(DT_AssignedProcess.SAMPLE_ID), is(true));
        assertThat(assignedProcessMap.containsKey(DT_AssignedProcess.PROCESS_STEP_NUMBER), is(true));
        assertThat(assignedProcessMap.containsKey(DT_AssignedProcess.PROCESS_NAME), is(true));
        assertThat(assignedProcessMap.containsKey(DT_AssignedProcess.OTHER_SAMPLE_ID), is(true));
        assertThat(assignedProcessMap.containsKey(DT_AssignedProcess.STATUS), is(true));

        assertThat(assignedProcessMap.get(DT_AssignedProcess.SAMPLE_ID), is(igoId));
        assertThat(assignedProcessMap.get(DT_AssignedProcess.PROCESS_STEP_NUMBER), is(assignedProcess.getStepNumber()));
        assertThat(assignedProcessMap.get(DT_AssignedProcess.PROCESS_NAME), is(assignedProcess.getName()));
        assertThat(assignedProcessMap.get(DT_AssignedProcess.OTHER_SAMPLE_ID), is(cmoId));
        assertThat(assignedProcessMap.get(DT_AssignedProcess.STATUS), is(String.format("Ready for - %s", assignedProcess.getWorkflowName())));
    }
}