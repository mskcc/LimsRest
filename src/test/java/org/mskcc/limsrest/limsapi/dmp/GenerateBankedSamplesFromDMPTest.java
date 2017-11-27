package org.mskcc.limsrest.limsapi.dmp;

import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.dmp.converter.DMPToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.dmp.converter.StudyToCMOBankedSampleConverter;
import org.mskcc.limsrest.limsapi.store.VeloxRecordSaver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerateBankedSamplesFromDMPTest {
    private GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP;
    private DMPToBankedSampleConverter dmpToBankedSampleConverter = new StudyToCMOBankedSampleConverter();
    private DMPSamplesRetriever dmpSamplesRetriever = mock(DMPSamplesRetriever.class);
    private RecordSaverSpy recordSaverSpy = new RecordSaverSpy();


    @Before
    public void setUp() throws Exception {
        generateBankedSamplesFromDMP = new GenerateBankedSamplesFromDMP(dmpToBankedSampleConverter,
                dmpSamplesRetriever, recordSaverSpy);
    }

    @Test
    public void when_should() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        List<Study> tracking1Studies = Arrays.asList(new Study(""));
        when(dmpSamplesRetriever.getStudies(trackingId1)).thenReturn(tracking1Studies);
        generateBankedSamplesFromDMP.init(date);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.size(), is(1));
    }

    private class RecordSaverSpy extends VeloxRecordSaver {
        private List<BankedSample> createdBankedSamples = new ArrayList<>();

        @Override
        public void save(BankedSample bankedSample, DataRecordManager dataRecordManager, User user) {
            createdBankedSamples.add(bankedSample);
        }
    }

}