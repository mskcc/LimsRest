package org.mskcc.limsrest.limsapi.cmoinfo;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.junit.Ignore;
import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.PatientSamplesRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetrieverFactory;
import org.mskcc.util.notificator.Notificator;
import org.mskcc.util.notificator.SlackNotificator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SampleTypeCorrectedCmoSampleIdGeneratorTest {
    private CmoSampleIdRetrieverFactory factory = mock(CmoSampleIdRetrieverFactory.class);
    private PatientSamplesRetriever retriever = mock(PatientSamplesRetriever.class);
    private String webhook = "https://hooks.slack.com/services/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";
    private String channel = "cmo-id-autogen";
    private String user = "ja";
    private String icon = ":kingjulien:";
    private Notificator notificator = new SlackNotificator(webhook, channel, user, icon);

    private SampleTypeCorrectedCmoSampleIdGenerator sampleTypeCorrectedCmoSampleIdGenerator = new
            SampleTypeCorrectedCmoSampleIdGenerator(factory, retriever, notificator);

    @Ignore
    @Test
    public void whenExceptionIsThrown_shouldSendSlackNotification() throws Exception {
        when(retriever.retrieve(any(), any(), any())).thenThrow(new LimsException("Terrible exception"));

        sampleTypeCorrectedCmoSampleIdGenerator.generate(mock(CorrectedCmoSampleView.class), "reqId", mock
                (DataRecordManager.class), mock(User.class));
    }
}