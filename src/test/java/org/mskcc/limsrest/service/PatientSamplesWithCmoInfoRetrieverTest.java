package org.mskcc.limsrest.service;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.hamcrest.object.IsCompatibleType;
import org.junit.Assert;
import org.junit.Test;
import org.mskcc.limsrest.service.cmoinfo.converter.CorrectedCmoIdConverter;
import org.mskcc.limsrest.service.converter.SampleRecordToSampleConverter;
import org.mskcc.util.CommonUtils;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

public class PatientSamplesWithCmoInfoRetrieverTest {
    private PatientSamplesWithCmoInfoRetriever patientSamplesWithCmoInfoRetriever = new
            PatientSamplesWithCmoInfoRetriever(mock(CorrectedCmoIdConverter.class), mock
            (SampleRecordToSampleConverter.class));

    private DataRecordManager dataRecordManager = mock(DataRecordManager.class);
    private User user = mock(User.class);

    @Test
    public void whenPatientIdIsNull_shouldThrowAnException() throws Exception {
        assertExceptionThrownForPatientId(null);
    }

    @Test
    public void whenPatientIdIsEmpty_shouldThrowAnException() throws Exception {
        assertExceptionThrownForPatientId("");
    }

    private void assertExceptionThrownForPatientId(String patientId) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> {
            try {
                patientSamplesWithCmoInfoRetriever.retrieve
                        (patientId, dataRecordManager, user);
            } catch (LimsException e) {
                e.printStackTrace();
            }
        });

        Assert.assertThat(exception.isPresent(), is(true));
        Assert.assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(CommonUtils
                .NullOrEmptyException.class));
    }
}