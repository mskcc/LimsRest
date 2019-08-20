package org.mskcc.limsrest.service.cmoinfo.cellline;

import org.hamcrest.object.IsCompatibleType;
import org.junit.Test;
import org.mskcc.util.CommonUtils;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CellLineCmoSampleIdTest {

    @Test
    public void whenSampleIdOrRequestIsInvalidNull_shouldThrowException() throws Exception {
        assertExceptionThrown(null, null);
        assertExceptionThrown("", "");

        assertExceptionThrown(null, "");
        assertExceptionThrown("", null);

        assertExceptionThrown(null, "12345_P");
        assertExceptionThrown("12344_P_4", null);

        assertExceptionThrown("", "1234_P");
        assertExceptionThrown("12344_P_4", "");
    }

    private void assertExceptionThrown(String sampleId, String requestId) {
        Optional<Exception> exception = TestUtils.assertThrown(() -> new CellLineCmoSampleId(sampleId, requestId));

        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), IsCompatibleType.typeCompatibleWith(CommonUtils.NullOrEmptyException
                .class));
    }
}