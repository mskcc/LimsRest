package org.mskcc.limsrest.limsapi.assignedprocess;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sloan.cmo.staticstrings.datatypes.DT_AssignedProcess;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class QcStatusAwareProcessAssignerTest {
    private final DataRecordManager drmMock = mock(DataRecordManager.class);
    private AssignedProcessCreator creator = mock(AssignedProcessCreator.class);
    private Map<String, Object> assProcMap = new HashMap<>();
    private User userMock = mock(User.class);
    private AssignedProcessConfig configMock = mock(AssignedProcessConfig.class);
    private DataRecord sample = mock(DataRecord.class);
    private AssignedProcess processToAssign = AssignedProcess.PRE_SEQUENCING_POOLING_OF_LIBRARIES;

    @Before
    public void setUp() throws Exception {
        when(configMock.getSample()).thenReturn(sample);
        when(configMock.getProcessToAssign()).thenReturn(processToAssign);
        assProcMap.put("costam", "costam");
        when(drmMock.addDataRecords(DT_AssignedProcess.DATA_TYPE, Collections.singletonList(assProcMap), userMock))
                .thenReturn(Arrays.asList
                        (mock(DataRecord.class)));

        when(creator.create(any(), any(), any())).thenReturn(assProcMap);
    }
}