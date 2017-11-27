package org.mskcc.limsrest.limsapi.integrationtest;

import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxConnectionException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.limsrest.limsapi.dmp.DMPSamplesRetriever;
import org.mskcc.limsrest.limsapi.dmp.GenerateBankedSamplesFromDMP;
import org.mskcc.limsrest.limsapi.dmp.Study;
import org.mskcc.limsrest.limsapi.dmp.converter.DMPToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.dmp.converter.StudyToCMOBankedSampleConverter;
import org.mskcc.limsrest.limsapi.store.VeloxRecordSaver;

import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerateBankedSamplesFromDMPTest {
    private static final Logger LOGGER = Logger.getLogger(GenerateBankedSamplesFromDMPTest.class);

    private DMPToBankedSampleConverter dmpToBankedSampleConverter = new StudyToCMOBankedSampleConverter();
    private DMPSamplesRetriever dmpSamplesRetriever = mock(DMPSamplesRetriever.class);

    private GenerateBankedSamplesFromDMP generateBankedSamplesFromDMP;
    private VeloxConnection connection;
    private DataRecordManager dataRecordManager;
    private User user;
    private int counter = 0;
    private RecordSaverSpy recordSaverSpy = new RecordSaverSpy();
    private String patientId = "P-5683956";

    @Before
    public void setUp() throws Exception {
        try {
            addShutdownHook();
            generateBankedSamplesFromDMP = new GenerateBankedSamplesFromDMP(dmpToBankedSampleConverter,
                    dmpSamplesRetriever, recordSaverSpy);
            connection = getVeloxConnection();
            openConnection();
        } catch (Exception e) {
            LOGGER.info("Unable to set up test. Closing LIMS connection");
            connection.close();
            throw e;
        }
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
        if (!connection.isConnected())
            connection.open();
        dataRecordManager = connection.getDataRecordManager();
        user = connection.getUser();
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

        List<Study> tracking1Studies = Arrays.asList(getStudy());
        when(dmpSamplesRetriever.getStudies(trackingId1)).thenReturn(tracking1Studies);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        List<Study> studies = new ArrayList<>();
        studies.addAll(tracking1Studies);

        assertBankedSamplesDontExist(studies);

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(studies);
    }

    @Test
    public void whenOneTrackingIdAndMultipleStudies_shouldCreateBankedSampleForAllOfThem() throws Exception {
        //given
        String trackingId1 = "1";
        LocalDate date = LocalDate.of(2017, 11, 20);
        List<String> trackingIds = Arrays.asList(trackingId1);
        when(dmpSamplesRetriever.retrieveTrackingIds(date)).thenReturn(trackingIds);

        List<Study> tracking1Studies = Arrays.asList(getStudy(), getStudy(), getStudy());
        when(dmpSamplesRetriever.getStudies(trackingId1)).thenReturn(tracking1Studies);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        List<Study> studies = new ArrayList<>();
        studies.addAll(tracking1Studies);

        assertBankedSamplesDontExist(studies);

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(studies);
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

        List<Study> tracking1Studies = Arrays.asList(getStudy());
        when(dmpSamplesRetriever.getStudies(trackingId1)).thenReturn(tracking1Studies);

        List<Study> tracking2Studies = Arrays.asList(getStudy(), getStudy(), getStudy(), getStudy(), getStudy());
        when(dmpSamplesRetriever.getStudies(trackingId2)).thenReturn(tracking2Studies);

        List<Study> tracking3Studies = Arrays.asList(getStudy(), getStudy(), getStudy());
        when(dmpSamplesRetriever.getStudies(trackingId3)).thenReturn(tracking3Studies);

        generateBankedSamplesFromDMP.setVeloxConnection(connection);
        generateBankedSamplesFromDMP.init(date);

        List<Study> studies = new ArrayList<>();
        studies.addAll(tracking1Studies);
        studies.addAll(tracking2Studies);
        studies.addAll(tracking3Studies);

        assertBankedSamplesDontExist(studies);

        //when
        generateBankedSamplesFromDMP.call();

        //then
        assertBankedSamples(studies);
    }

    private void assertBankedSamples(List<Study> studies) throws Exception {
        openConnection();
        for (Study study : studies) {
            List<DataRecord> dataRecords = dataRecordManager.queryDataRecords(BankedSample.DATA_TYPE_NAME,
                    BankedSample.USER_SAMPLE_ID + " = '" + study.getInvestigatorSampleId() + "'", user);
            assertThat(String.format("No Banked Sample with id: %s", study.getInvestigatorSampleId()), dataRecords
                    .size(), is(1));

            DataRecord bankedRecord = dataRecords.get(0);

            assertThat(bankedRecord.getStringVal(BankedSample.BARCODE_ID, user), is(study.getIndex()));
            assertThat(bankedRecord.getStringVal(BankedSample.COLLECTION_YEAR, user), is(study.getCollectionYear()));
            assertThat(bankedRecord.getDoubleVal(BankedSample.CONCENTRATION, user), is(study.getConcentration()));
            assertThat(bankedRecord.getStringVal(BankedSample.GENDER, user), is(study.getSex()));
            assertThat(bankedRecord.getStringVal(BankedSample.INVESTIGATOR, user), is(study.getPiName()));
            assertThat(bankedRecord.getStringVal(BankedSample.NATO_EXTRACT, user), is(study.getNucleidAcidType()));
            assertThat(bankedRecord.getStringVal(BankedSample.OTHER_SAMPLE_ID, user), is(study
                    .getInvestigatorSampleId()));
            assertThat(bankedRecord.getStringVal(BankedSample.PATIENT_ID, user), is(patientId));
            assertThat(bankedRecord.getStringVal(BankedSample.PLATE_ID, user), is(study.getBarcodePlateId()));
            assertThat(bankedRecord.getStringVal(BankedSample.PRESERVATION, user), is(study.getPreservation()));
            assertThat(bankedRecord.getStringVal(BankedSample.SAMPLE_CLASS, user), is(study.getSampleClass()));
            assertThat(bankedRecord.getStringVal(BankedSample.SPECIMEN_TYPE, user), is(study.getSpecimenType()));
            assertThat(bankedRecord.getStringVal(BankedSample.TUMOR_TYPE, user), is(study.getTumorType()));
            assertThat(bankedRecord.getStringVal(BankedSample.USER_SAMPLE_ID, user), is(study.getInvestigatorSampleId
                    ()));
            assertThat(bankedRecord.getDoubleVal(BankedSample.VOLUME, user), is(study.getVolume()));
        }
    }

    private void assertBankedSamplesDontExist(List<Study> studies) throws NotFound, IoError, RemoteException {
        for (Study study : studies) {
            List<DataRecord> dataRecords = dataRecordManager.queryDataRecords(BankedSample.DATA_TYPE_NAME,
                    BankedSample.USER_SAMPLE_ID + " = '" + study.getInvestigatorSampleId() + "'", user);
            assertThat(dataRecords.size(), is(0));
        }
    }

    private Study getStudy() {
        Study study = new Study("study1");
        study.setBarcodePlateId("barcode" + counter);
        study.setCollectionYear(String.valueOf(1 + counter));
        study.setConcentration(1.0 + counter);
        study.setDmpId(patientId + counter);
        study.setDnaInputIntoLibrary(1.0 + counter);
        study.setIndex("index" + counter);
        study.setIndexSequence("indexSeq" + counter);
        study.setInvestigatorSampleId("invSampleId" + counter);
        study.setNucleidAcidType("nuclAcid" + counter);
        study.setPiName("piName" + counter);
        study.setPreservation("preservation" + counter);
        study.setReceivedDnaMass(1.0 + counter);
        study.setSampleApprovedByCmo("sampleAppCMP" + counter);
        study.setSampleClass("sampleClass" + counter);
        study.setSex("sex" + counter);
        study.setSpecimenType("specimen" + counter);
        study.setStudyOfTitle("studyTitle" + counter);
        study.setTrackingId("trackingId" + counter);
        study.setTumorType("tumTypr" + counter);
        study.setVolume(1.0 + counter);
        study.setWellPosition("wellPos" + counter);

        counter++;

        return study;
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