package org.mskcc.limsrest.limsapi.integrationtest;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxConnectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.converter.ExternalToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.dmp.DMPSample;
import org.mskcc.limsrest.limsapi.dmp.DMPSamplesRetriever;
import org.mskcc.limsrest.limsapi.dmp.GenerateBankedSamplesFromDMP;
import org.mskcc.limsrest.limsapi.dmp.converter.DMPSampleToCMOBankedSampleConverter;
import org.mskcc.limsrest.limsapi.retriever.LimsDataRetriever;
import org.mskcc.limsrest.limsapi.retriever.VeloxLimsDataRetriever;
import org.mskcc.limsrest.limsapi.store.VeloxRecordSaver;
import org.mskcc.util.tumortype.OncotreeTumorTypeRetriever;
import org.mskcc.util.tumortype.TumorTypeRetriever;

import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerateBankedSamplesFromDMPTest {
    private static final Log LOGGER = LogFactory.getLog(GenerateBankedSamplesFromDMPTest.class);

    private ExternalToBankedSampleConverter externalToBankedSampleConverter;
    private DMPSamplesRetriever dmpSamplesRetriever = mock(DMPSamplesRetriever.class);

    private GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP;
    private VeloxConnection connection;
    private DataRecordManager dataRecordManager;
    private User user;
    private int counter = 0;
    private RecordSaverSpy recordSaverSpy;
    private String patientId = "P-5683956";
    private TumorTypeRetriever tumorTypeRetriever;
    private List<Long> transactionIds = new ArrayList<>();
    private LimsDataRetriever limsDataRetriever = new VeloxLimsDataRetriever();

    @Before
    public void setUp() throws Exception {
        try {
            tumorTypeRetriever = new OncotreeTumorTypeRetriever(getRestUrl());
            externalToBankedSampleConverter = new DMPSampleToCMOBankedSampleConverter(tumorTypeRetriever);
            recordSaverSpy = new RecordSaverSpy();
            addShutdownHook();
            generateBankedSamplesFromDMP = new GenerateBankedSamplesFromDMP(externalToBankedSampleConverter,
                    dmpSamplesRetriever, recordSaverSpy, limsDataRetriever);
            connection = getVeloxConnection();
            openConnection();
        } catch (Exception e) {
            LOGGER.info("Unable to set up test. Closing LIMS connection");
            connection.close();
            throw e;
        }
    }

    private String getRestUrl() throws Exception {
        Properties p = new Properties();
        String appProperties = "/app.properties";

        FileInputStream propFile = new FileInputStream(getResourceFile(appProperties));
        p.load(propFile);

        return p.getProperty("oncotreeRestUrl");
    }

    private VeloxConnection getVeloxConnection() throws Exception {
        Properties p = new Properties();
        String limsConnectionProperties = "/devLimsConnection.properties";

        try {
            FileInputStream propFile = new FileInputStream(getResourceFile(limsConnectionProperties));
            p.load(propFile);
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to load lims connection properties file: %s. System Properties will be " +
                    "used for connection.", limsConnectionProperties));
        }

        String host = System.getProperty("host", p.getProperty("host"));
        int port = Integer.parseInt(System.getProperty("port", p.getProperty("port")));
        String user = System.getProperty("user", p.getProperty("user"));
        String password = System.getProperty("password", p.getProperty("password"));
        String GUID = System.getProperty("GUID", p.getProperty("GUID"));

        return new VeloxConnection(host, port, GUID, user, password);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tearDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private void openConnection() throws VeloxConnectionException {
        if (!connection.isConnected()) {
            connection.open();
            dataRecordManager = connection.getDataRecordManager();
            user = connection.getUser();
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            deleteCreatedBankedRecords();
        } finally {
            LOGGER.info("Closing LIMS connection");
            connection.close();
        }
    }

    private void deleteCreatedBankedRecords() throws VeloxConnectionException, ServerException, RemoteException {
        openConnection();
        LOGGER.info(String.format("Deleting created records: %s", getRecordIds()));

        dataRecordManager.deleteDataRecords(recordSaverSpy.getCreatedBankedSampleRecords(), null,
                false, user);
        dataRecordManager.storeAndCommit("Deleting banked sample records created for test", null, user);

        recordSaverSpy.getCreatedBankedSampleRecords().clear();
    }

    private List<Long> getRecordIds() {
        return recordSaverSpy.getCreatedBankedSampleRecords().stream().map(r -> r.getRecordId())
                .collect(Collectors.toList());
    }

    @Test
    public void whenOneTrackingIdAndOneStudy_shouldCreateOneBankedSample() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        List<DMPSample> tracking1Studies = Arrays.asList(getDMPSample());
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1Studies);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        Multimap<String, DMPSample> dmpSamples = LinkedHashMultimap.create();
        dmpSamples.putAll(trackingId1, tracking1Studies);

        assertBankedSamplesDontExist(dmpSamples.values());

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(dmpSamples);
        assertTransactionIdsAreIncreacing();
    }

    private void assertTransactionIdsAreIncreacing() {
        assertTrue(Ordering.natural().isOrdered(transactionIds));
    }

    @Test
    public void whenOneTrackingIdAndMultipleStudies_shouldCreateBankedSampleForAllOfThem() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        List<DMPSample> tracking1Studies = Arrays.asList(getDMPSample(), getDMPSample(), getDMPSample());
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1Studies);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        Multimap<String, DMPSample> dmpSamples = LinkedHashMultimap.create();
        dmpSamples.putAll(trackingId1, tracking1Studies);

        assertBankedSamplesDontExist(dmpSamples.values());

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(dmpSamples);
    }

    @Test
    public void whenThereAreMultipleTrackingIdsWithMultipleStudies_shouldCreateBankedSampleForAllOfThem() throws
            Exception {
        //given
        String trackingId1 = "1";
        String trackingId2 = "2";
        String trackingId3 = "3";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1, trackingId2, trackingId3);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        List<DMPSample> tracking1DMPSamples = Arrays.asList(getDMPSample());
        when(dmpSamplesRetriever.getDMPSamples(trackingId1)).thenReturn(tracking1DMPSamples);

        List<DMPSample> tracking2DMPSamples = Arrays.asList(getDMPSample(), getDMPSample(), getDMPSample(),
                getDMPSample(), getDMPSample());
        when(dmpSamplesRetriever.getDMPSamples(trackingId2)).thenReturn(tracking2DMPSamples);

        List<DMPSample> tracking3DMPSamples = Arrays.asList(getDMPSample(), getDMPSample(), getDMPSample());
        when(dmpSamplesRetriever.getDMPSamples(trackingId3)).thenReturn(tracking3DMPSamples);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        Multimap<String, DMPSample> dmpSamples = LinkedHashMultimap.create();
        dmpSamples.putAll(trackingId1, tracking1DMPSamples);
        dmpSamples.putAll(trackingId2, tracking2DMPSamples);
        dmpSamples.putAll(trackingId3, tracking3DMPSamples);

        assertBankedSamplesDontExist(dmpSamples.values());

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(dmpSamples);
    }

    private void assertBankedSamples(Multimap<String, DMPSample> studies) throws Exception {
        openConnection();

        Long previousTransactionId = null;
        for (String trackingId : studies.keySet()) {
            Long transactionId = null;
            for (DMPSample dmpSample : studies.get(trackingId)) {
                List<DataRecord> dataRecords = dataRecordManager.queryDataRecords(BankedSample.DATA_TYPE_NAME,
                        BankedSample.USER_SAMPLE_ID + " = '" + dmpSample.getInvestigatorSampleId() + "'", user);
                assertThat(String.format("No Banked Sample with id: %s", dmpSample.getInvestigatorSampleId()),
                        dataRecords
                        .size(), is(1));

                DataRecord bankedRecord = dataRecords.get(0);

                if (transactionId == null) {
                    transactionId = bankedRecord.getLongVal(BankedSample.TRANSACTION_ID, user);
                    transactionIds.add(transactionId);

                    if (previousTransactionId != null) {
                        assertThat(bankedRecord.getLongVal(BankedSample.TRANSACTION_ID, user), is
                                (previousTransactionId + 1));
                    }
                }

                assertThat(bankedRecord.getStringVal(BankedSample.BARCODE_ID, user), is(dmpSample.getIndex()));
                assertThat(bankedRecord.getStringVal(BankedSample.COLLECTION_YEAR, user), is(dmpSample
                        .getCollectionYear()));
                assertThat(bankedRecord.getDoubleVal(BankedSample.CONCENTRATION, user), is(dmpSample.getConcentration
                        ()));
                assertThat(bankedRecord.getStringVal(BankedSample.GENDER, user), is(dmpSample.getSex()));
                assertThat(bankedRecord.getStringVal(BankedSample.INVESTIGATOR, user), is(dmpSample.getPiName()));
                assertThat(bankedRecord.getStringVal(BankedSample.NATO_EXTRACT, user), is(dmpSample
                        .getNucleidAcidType()));
                assertThat(bankedRecord.getDoubleVal(BankedSample.NON_LIMS_LIBRARY_INPUT, user), is(dmpSample
                        .getDnaInputIntoLibrary()));
                assertThat(bankedRecord.getDoubleVal(BankedSample.NON_LIMS_LIBRARY_OUTPUT, user), is(dmpSample
                        .getReceivedDnaMass()));
                assertThat(bankedRecord.getStringVal(BankedSample.OTHER_SAMPLE_ID, user), is(dmpSample
                        .getInvestigatorSampleId()));
                assertThat(bankedRecord.getStringVal(BankedSample.PATIENT_ID, user), is(patientId));
                assertThat(bankedRecord.getStringVal(BankedSample.PLATE_ID, user), is(dmpSample.getBarcodePlateId()));
                assertThat(bankedRecord.getIntegerVal(BankedSample.ROW_INDEX, user), is(9 * 100 + 7));
                assertThat(bankedRecord.getStringVal(BankedSample.PRESERVATION, user), is(dmpSample.getPreservation()));
                assertThat(bankedRecord.getStringVal(BankedSample.SAMPLE_CLASS, user), is(dmpSample.getSampleClass()));
                assertThat(bankedRecord.getStringVal(BankedSample.SPECIMEN_TYPE, user), is(dmpSample.getSpecimenType
                        ()));
                assertThat(bankedRecord.getLongVal(BankedSample.TRANSACTION_ID, user), is(transactionId));


                assertThat(bankedRecord.getStringVal(BankedSample.TUMOR_TYPE, user), is("BLCA"));
                assertThat(bankedRecord.getStringVal(BankedSample.USER_SAMPLE_ID, user), is(dmpSample
                        .getInvestigatorSampleId
                        ()));
                assertThat(bankedRecord.getDoubleVal(BankedSample.VOLUME, user), is(dmpSample.getVolume()));
            }

            previousTransactionId = transactionId;
        }
    }

    private void assertBankedSamplesDontExist(Collection<DMPSample> studies) throws NotFound, IoError, RemoteException {
        for (DMPSample DMPSample : studies) {
            List<DataRecord> dataRecords = dataRecordManager.queryDataRecords(BankedSample.DATA_TYPE_NAME,
                    BankedSample.USER_SAMPLE_ID + " = '" + DMPSample.getInvestigatorSampleId() + "'", user);
            assertThat(dataRecords.size(), is(0));
        }
    }

    private DMPSample getDMPSample() {
        DMPSample dmpSample = new DMPSample("dmpSample" + counter);
        dmpSample.setBarcodePlateId("barcode" + counter);
        dmpSample.setCollectionYear(String.valueOf(1 + counter));
        dmpSample.setConcentration(1.0 + counter);
        dmpSample.setDmpId(patientId + "-" + counter);
        dmpSample.setDnaInputIntoLibrary(1.0 + counter);
        dmpSample.setIndex("index" + counter);
        dmpSample.setIndexSequence("indexSeq" + counter);
        dmpSample.setInvestigatorSampleId("invSampleId" + counter);
        dmpSample.setNucleidAcidType("nuclAcid" + counter);
        dmpSample.setPiName("piName" + counter);
        dmpSample.setPreservation("preservation" + counter);
        dmpSample.setReceivedDnaMass(1.0 + counter);
        dmpSample.setSampleApprovedByCmo("sampleAppCMP" + counter);
        dmpSample.setSampleClass("sampleClass" + counter);
        dmpSample.setSex("sex" + counter);
        dmpSample.setSpecimenType("specimen" + counter);
        dmpSample.setStudyOfTitle("studyTitle" + counter);
        dmpSample.setTrackingId("trackingId" + counter);
        dmpSample.setTumorType("Bladder_Urinary_Tract:Bladder Urothelial Carcinoma");
        dmpSample.setVolume(1.0 + counter);
        dmpSample.setWellPosition("H9");

        counter++;

        return dmpSample;
    }

    private String getResourceFile(String connectionFile) throws Exception {
        return PromoteBankedTest.class.getResource(connectionFile).getPath();
    }

    private class RecordSaverSpy extends VeloxRecordSaver {
        private List<DataRecord> createdBankedSampleRecords = new ArrayList<>();

        @Override
        protected DataRecord addBankedSampleRecord(DataRecordManager dataRecordManager, User user) throws IoError,
                NotFound, AlreadyExists, InvalidValue,
                RemoteException {
            DataRecord bankedSampleRecord = super.addBankedSampleRecord(dataRecordManager, user);
            createdBankedSampleRecords.add(bankedSampleRecord);

            return bankedSampleRecord;
        }

        public List<DataRecord> getCreatedBankedSampleRecords() {
            return createdBankedSampleRecords;
        }
    }
}