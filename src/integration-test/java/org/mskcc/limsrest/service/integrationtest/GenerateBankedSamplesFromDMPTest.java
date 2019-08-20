package org.mskcc.limsrest.service.integrationtest;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.service.converter.ExternalToBankedSampleConverter;
import org.mskcc.limsrest.service.dmp.DMPSample;
import org.mskcc.limsrest.service.dmp.DMPSamplesRetriever;
import org.mskcc.limsrest.service.dmp.GenerateBankedSamplesFromDMP;
import org.mskcc.limsrest.service.dmp.TumorTypeRetriever;
import org.mskcc.limsrest.service.dmp.converter.DMPSampleToCMOBankedSampleConverter;
import org.mskcc.limsrest.service.retriever.LimsDataRetriever;
import org.mskcc.limsrest.service.store.VeloxRecordSaver;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerateBankedSamplesFromDMPTest {
    private TumorTypeRetriever tumorTypeRetriever = mock(TumorTypeRetriever.class);
    private GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP;
    private ExternalToBankedSampleConverter externalToBankedSampleConverter = new DMPSampleToCMOBankedSampleConverter
            (tumorTypeRetriever);
    private DMPSamplesRetriever dmpSamplesRetriever = mock(DMPSamplesRetriever.class);
    private RecordSaverSpy recordSaverSpy;
    private LimsDataRetriever limsDataRetriever = mock(LimsDataRetriever.class);

    @Before
    public void setUp() throws Exception {
        recordSaverSpy = new RecordSaverSpy();
        generateBankedSamplesFromDMP = new GenerateBankedSamplesFromDMP();
    }

    @Test
    public void whenTrackingIdIsNotInCorrectFormat_shouldNotSaveAnyBankedSamples() throws Exception {
        //given
        String trackingId1 = "someId; incorrect format";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(any(), any(), any())).thenReturn(Collections.emptyList());

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i1"));
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(0));
    }

    @Test
    public void whenOneTrackingIdNotProcessed_shouldSaveOneBankedSample() throws Exception {
        //given
        String trackingId1 = "20170405MS";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(any(), any(), any())).thenReturn(Collections.emptyList());

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i1"));
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(tracking1DMPSamples.size()));
    }


    @Test
    public void whenOneTrackingIdAlreadyProcessed_shouldntProcessNorSaveAnything() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(any(), any(), any())).thenReturn(Arrays.asList(new BankedSample
                ("someId")));

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i1"), getDmpSample("id12", "i2"),
                getDmpSample("id13", "i3"));
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(0));
    }

    @Test
    public void whenCheckingTrackingIdProcessedThrowsException_shouldntProcessNorSaveAnything() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(any(), any(), any())).thenThrow(Exception.class);

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i1"), getDmpSample("id12", "i2"),
                getDmpSample("id13", "i3"));
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(0));
    }

    @Test
    public void whenOneTrackingIdNotProcessedAndOneIsProcessed_shouldSaveBankedSamplesOnlyFromNotProcessedNot()
            throws Exception {
        //given
        String trackingId1 = "1";
        String trackingId2 = "2";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1, trackingId2);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(eq(String.format("%s = '%s'", BankedSample.DMP_TRACKING_ID,
                trackingId1)), any(), any())).thenReturn(Collections.emptyList());
        when(limsDataRetriever.getBankedSamples(eq(String.format("%s = '%s'", BankedSample.DMP_TRACKING_ID,
                trackingId2)), any(), any())).thenReturn(Arrays.asList(new BankedSample("id")));

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i11"), getDmpSample("id12", "i12"),
                getDmpSample("id13", "i13"));
        List<DMPSample> tracking2DMPSamples = Arrays.asList(getDmpSample("id21", "i21"), getDmpSample("id22", "i22"));
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        when(dmpSamplesRetriever.getDMPSamples(trackingId2)).thenReturn(tracking2DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(tracking1DMPSamples.size()));

        for (BankedSample bankedSample : recordSaverSpy.createdBankedSamples.values()) {
            assertTrue(tracking1DMPSamples.stream()
                    .anyMatch(t -> t.getInvestigatorSampleId().equals(bankedSample.getUserSampleID())));
        }
    }

    private DMPSample getDmpSample(String studySampleId, String investigatorSampleId) {
        DMPSample dmpSample = new DMPSample(studySampleId);
        dmpSample.setInvestigatorSampleId(investigatorSampleId);
        return dmpSample;
    }

    @Test
    public void whenMultipleDMPSamples_shouldSaveAllBankedSamplesWithCorrectTransactionId() throws Exception {
        //given
        String trackingId1 = "1";
        String trackingId2 = "2";
        String trackingId3 = "3";
        String trackingId4 = "4";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1, trackingId2, trackingId3, trackingId4);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        when(limsDataRetriever.getBankedSamples(any(), any(), any())).thenReturn(Collections.emptyList());

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDmpSample("id11", "i11"), getDmpSample
                ("id12", "i12"), getDmpSample("id13", "i13"));
        List<DMPSample> tracking2DMPSamples = Arrays.asList(getDmpSample("id21", "i21"));
        List<DMPSample> tracking3DMPSamples = Arrays.asList(getDmpSample("id31", "i21"), getDmpSample("id32", "i32"),
                getDmpSample("id33", "i33"), getDmpSample("id34", "i34"));
        List<DMPSample> tracking4DMPSamples = Arrays.asList();
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);
        when(dmpSamplesRetriever.getDMPSamples(trackingId2)).thenReturn(tracking2DMPSamples);
        when(dmpSamplesRetriever.getDMPSamples(trackingId3)).thenReturn(tracking3DMPSamples);
        when(dmpSamplesRetriever.getDMPSamples(trackingId4)).thenReturn(tracking4DMPSamples);
        generateBankedSamplesFromDMP.setDate(date);
        generateBankedSamplesFromDMP.setLimsDataRetriever(limsDataRetriever);
        generateBankedSamplesFromDMP.setDmpSamplesRetriever(dmpSamplesRetriever);
        generateBankedSamplesFromDMP.setRecordSaver(recordSaverSpy);

        //when
        generateBankedSamplesFromDMP.execute(mock(VeloxConnection.class));

        //then
        assertThat(recordSaverSpy.createdBankedSamples.values().size(), is(tracking1DMPSamples.size() +
                tracking2DMPSamples.size() +
                tracking3DMPSamples.size() + tracking4DMPSamples.size()));


        Long oldtransactionId = null;
        for (Long transactionId : recordSaverSpy.createdBankedSamples.keySet()) {
            Collection<BankedSample> bankedSamples = recordSaverSpy.createdBankedSamples.get(transactionId);

            assertTrue(bankedSamples.stream().allMatch(b -> Objects.equals(b.getTransactionId(), transactionId)));

            if (oldtransactionId != null)
                assertThat(transactionId, is(oldtransactionId + 1));

            oldtransactionId = transactionId;
        }
    }

    private class RecordSaverSpy extends VeloxRecordSaver {
        private Multimap<Long, BankedSample> createdBankedSamples = LinkedHashMultimap.create();

        @Override
        public void save(BankedSample bankedSample, DataRecordManager dataRecordManager, User user) {
            createdBankedSamples.put(bankedSample.getTransactionId(), bankedSample);
        }
    }
}