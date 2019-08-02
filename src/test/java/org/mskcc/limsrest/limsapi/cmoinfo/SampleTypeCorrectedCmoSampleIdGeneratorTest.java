package org.mskcc.limsrest.limsapi.cmoinfo;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import org.junit.Ignore;
import org.junit.Test;
import org.mskcc.domain.sample.CorrectedCmoSampleView;
import org.mskcc.limsrest.limsapi.LimsException;
import org.mskcc.limsrest.limsapi.PatientSamplesRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.CmoSampleIdRetrieverFactory;
import org.mskcc.util.notificator.Notificator;
import org.mskcc.util.notificator.SlackNotificator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PropertySource("classpath:application-test.properties")
public class SampleTypeCorrectedCmoSampleIdGeneratorTest {
    private CmoSampleIdRetrieverFactory factory = mock(CmoSampleIdRetrieverFactory.class);
    private PatientSamplesRetriever retriever = mock(PatientSamplesRetriever.class);

    @Value("${slack.webhookUrl}")
    private String webhook;
    private String channel = "cmo-id-autogen";
    private String user = "ja";
    private String icon = ":kingjulien:";
    private Notificator notificator = new SlackNotificator(webhook, channel, user, icon);

    private SampleTypeCorrectedCmoSampleIdGenerator sampleTypeCorrectedCmoSampleIdGenerator = new
            SampleTypeCorrectedCmoSampleIdGenerator();

    @Ignore
    @Test
    public void whenExceptionIsThrown_shouldSendSlackNotification() throws Exception {
        when(retriever.retrieve(any(), any(), any())).thenThrow(new LimsException("Terrible exception"));

        sampleTypeCorrectedCmoSampleIdGenerator.generate(mock(CorrectedCmoSampleView.class), "reqId", mock
                (DataRecordManager.class), mock(User.class));
    }

    @Test
    public void whenCmoSampleIdIsSameAsBefore_shouldNotOverrideIt() throws Exception {
        //given
        when(factory.getCmoSampleIdRetriever(any())).thenReturn(new CmoSampleIdRetriever() {
            @Override
            public String retrieve(CorrectedCmoSampleView correctedCmoSampleView, List<CorrectedCmoSampleView>
                    patientSamples, String requestId) {
                return "C-123456-X002-d";
            }
        });

        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("sampId");
        String currentId = "C-123456-X001-d";
        correctedCmoSampleView.setCorrectedCmoId(currentId);
        correctedCmoSampleView.setPatientId("patId");

        //when
        String id = sampleTypeCorrectedCmoSampleIdGenerator.generate(correctedCmoSampleView, "id", mock
                (DataRecordManager
                        .class), mock(User.class));

        //then
        assertThat(id, is(currentId));

    }
}