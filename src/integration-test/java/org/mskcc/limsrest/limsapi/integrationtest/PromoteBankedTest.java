package org.mskcc.limsrest.limsapi.integrationtest;

import com.google.common.collect.ImmutableMap;
import com.velox.api.datamgmtserver.DataMgmtServer;
import com.velox.api.datarecord.*;
import com.velox.api.user.User;
import com.velox.api.util.ServerException;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import com.velox.sapioutils.client.standalone.VeloxStandaloneManagerContext;
import com.velox.sapioutils.shared.managers.DataRecordUtilManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.PatientSamplesRetriever;
import org.mskcc.limsrest.limsapi.PatientSamplesWithCmoInfoRetriever;
import org.mskcc.limsrest.limsapi.PromoteBanked;
import org.mskcc.limsrest.limsapi.cmoinfo.CorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.SampleTypeCorrectedCmoSampleIdGenerator;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.cellline.CellLineCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.BankedSampleToCorrectedCmoSampleIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.CorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.FormatAwareCorrectedCmoIdConverterFactory;
import org.mskcc.limsrest.limsapi.cmoinfo.converter.SampleToCorrectedCmoIdConverter;
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.*;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.util.VeloxConstants;
import org.mskcc.util.notificator.Notificator;
import org.mskcc.util.notificator.SlackNotificator;

import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mskcc.domain.sample.NucleicAcid.DNA;
import static org.mskcc.domain.sample.NucleicAcid.RNA;
import static org.mskcc.domain.sample.SampleClass.*;
import static org.mskcc.domain.sample.SampleOrigin.*;
import static org.mskcc.domain.sample.SpecimenType.*;
import static org.mskcc.domain.sample.SpecimenType.SALIVA;

public class PromoteBankedTest {
    private static final Log LOG = LogFactory.getLog(PromoteBankedTest.class);

    private static final String connectionPromoteBanked = "/Connection-promote-banked.txt";
    private static final String connectionTest = "/Connection-test.txt";

    private static final String USER_SAMP_ID1 = "userSampId1";
    private static final String USER_SAMP_ID2 = "userSampId2";
    private static final String USER_SAMP_ID3 = "userSampId3";
    private static final String USER_SAMP_ID4 = "userSampId4";

    private static final String SAMPLE_ID1 = "promoteBankedTest_sampleId1";
    private static final String SAMPLE_ID2 = "promoteBankedTest_sampleId2";
    private static final String SAMPLE_ID3 = "promoteBankedTest_sampleId3";
    private static final String SAMPLE_ID4 = "promoteBankedTest_sampleId4";

    private static final String OTHER_SAMPLE_ID1 = "promoteBankedTest_otherId_1";
    private static final String OTHER_SAMPLE_ID2 = "promoteBankedTest_otherId_2";
    private static final String OTHER_SAMPLE_ID3 = "promoteBankedTest_otherId_3";
    private static final String OTHER_SAMPLE_ID4 = "promoteBankedTest_otherId_4";

    private static final String userSampleId = "g748";
    private static final String projectId = "PromoteBankedTest";
    private static final String serviceId = "65436546";
    private static final String requestId1 = "PromoteBankedTest_A";
    private static final String normalizedRequestId1 = "PromoteBankedTestA";
    private static final String requestId2 = "PromoteBankedTest_B";
    private static PromoteBanked promoteBanked;
    private final String patientId1 = "promoteBankedTestPatient1";
    private final String patientId2 = "promoteBankedTestPatient2";
    private final String patientId3 = "promoteBankedTestPatient3";
    private List<DataRecord> createdBankedRecords;
    private VeloxConnection connection;
    private DataRecordManager dataRecordManager;
    private User user;
    private DataMgmtServer dataMgmtServer;
    private DataRecordUtilManager drum;
    private VeloxStandaloneManagerContext managerContext;
    private DataRecord projectRecord;

    @Before
    public void setUp() throws Exception {
        try {
            promoteBanked = getPromoteBanked();
            connection = new VeloxConnection(getResourceFile(connectionTest));
            reopenConnection();
            promoteBanked.setVeloxConnection(new VeloxConnection(getResourceFile(connectionPromoteBanked)));

            addProjectWithRequests();

            createdBankedRecords = new ArrayList<>();
            dataRecordManager.storeAndCommit("Added records for promoted banked test", user);
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException("unable to configure integration test", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            reopenConnection();
            deleteRecord(projectRecord);
            deleteBankedSampleRecords();
            dataRecordManager.storeAndCommit("Deleted records for promoted banked test", user);
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            connection.close();
            LOG.info("Closed LIMS connection");
        }
    }

    @Test
    public void whenCellLineSampleIsPromoted_shouldAssignCorrectedCmoId() throws
            Exception {
        //given
        promoteSample(patientId1, CELLLINE, requestId1, userSampleId, "otherId_1", requestId1, serviceId, projectId);

        //then
        assertPromoteSample(ImmutableMap.<String, List<String>>builder()
                .put(
                        requestId1,
                        Collections.singletonList(String.format("%s-%s", userSampleId, normalizedRequestId1)))
                .build());
    }

    @Test
    public void whenMultipleCellLineSamplesArePromotedForSamePatient_shouldAssignCorrectedCmoId() throws Exception {
        //given
        DataRecord banked1 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID1, OTHER_SAMPLE_ID1);
        DataRecord banked2 = addPromoteBanked(patientId1, CELLLINE, RNA, requestId1, USER_SAMP_ID2, OTHER_SAMPLE_ID2);
        DataRecord banked3 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID3, OTHER_SAMPLE_ID3);
        DataRecord banked4 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID4, OTHER_SAMPLE_ID4);
        initPromoteBanked(Arrays.asList(banked1, banked2, banked3, banked4), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        List<String> correctedCmoIds = Arrays.asList(
                String.format("%s-%s", USER_SAMP_ID1, normalizedRequestId1),
                String.format("%s-%s", USER_SAMP_ID2, normalizedRequestId1),
                String.format("%s-%s", USER_SAMP_ID3, normalizedRequestId1),
                String.format("%s-%s", USER_SAMP_ID4, normalizedRequestId1)
        );

        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenSampleIsFirstPatientSampleOfThatType_shouldSetCorrectedCmoIdWithCount1() throws Exception {
        //given
        promoteSample(patientId1, ORGANOID, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1, requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId1, "G", "001", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenSampleIsCfdna_shouldSetIdBasedOnSampleOrigin() throws Exception {
        //given
        promoteSample(patientId1, CFDNA, Optional.of(URINE), Optional.empty(), requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1,
                requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId1, "U", "001", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenBankedSampleHasRequestIdSet_shouldAsignToRequestProvidedToPromoteBanked() throws Exception {
        //given
        promoteSample(patientId1, ORGANOID, "someOtherRequest", SAMPLE_ID1, OTHER_SAMPLE_ID1, requestId1, serviceId,
                projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId1, "G", "001", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSampleArePromotedSeparatelyOfSameTypeForSameRequest_shouldSetCorrectedCmoIdWithCount1And2()
            throws
            Exception {
        //given
        String patientId = patientId1;
        promoteSample(patientId, ORGANOID, requestId1, "sample_1", "otherId_1", requestId1, serviceId, projectId);

        promoteSample(patientId, ORGANOID, requestId1, "sample_2", "otherId_2", requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "G", "001",
                "d"), String.format("C-%s-%s%s-%s", patientId, "G", "002", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    whenCellLineAndPatientSamplesArePromotedSeparatelyOfSameTypeForSameRequest_shouldSetCorrectedCmoIdWithCount1()
            throws
            Exception {
        //given
        String patientId = patientId1;
        String cellLineSampleId = SAMPLE_ID1;
        promoteSample(patientId, CELLLINE, Optional.of(CEREBROSPINAL_FLUID), Optional.of(ADJACENT_NORMAL),
                requestId1, cellLineSampleId, OTHER_SAMPLE_ID1, requestId1, serviceId, projectId);

        //when
        promoteSample(patientId, PDX, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2, requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(
                String.format("%s-%s", cellLineSampleId, normalizedRequestId1),
                String.format("C-%s-%s%s-%s", patientId, "X", "001", "d")
        );

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSampleArePromotedSeparatelyOfSameTypeForDifferentPatient_shouldSetCorrectedCmoIdWithCount1And1()
            throws
            Exception {
        //given
        String patientId1 = "patient1";
        String patientId2 = "patient2";
        promoteSample(patientId1, ORGANOID, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1, requestId1, serviceId, projectId);

        //when
        promoteSample(patientId2, ORGANOID, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2, requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(
                String.format("C-%s-%s%s-%s", patientId1, "G", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId2, "G", "001", "d")
        );

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSampleArePromotedSeparatelyOfSameTypeForDifferentRequest_shouldSetCorrectedCmoIdWithCount1And2()
            throws
            Exception {
        //given
        String patientId = patientId1;
        promoteSample(patientId, ORGANOID, "someReqId", "sample_1", "otherId_1", requestId1, serviceId, projectId);

        promoteSample(patientId, ORGANOID, "someReqId", "sample_2", "otherId_2", requestId2, serviceId, projectId);

        //then
        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "G", "001",
                "d")));
        requestToCorrectedCmoIds.put(requestId2, Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "G", "002",
                "d")));

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    whenMultipleSamplesArePromotedSeparatelyOfSameTypeForDifferentRequest_shouldSetCorrectedCmoIdWithIncrementedCount()
            throws
            Exception {
        //given
        String patientId = patientId1;
        promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID1, OTHER_SAMPLE_ID1, PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);
        promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID2, OTHER_SAMPLE_ID2, PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);
        promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID3, OTHER_SAMPLE_ID3, PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);

        //when
        promoteSample(patientId, ORGANOID, "someReqId", "sample_2", "otherId_2", requestId2, PromoteBankedTest
                .serviceId, PromoteBankedTest.projectId);

        //then
        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(PromoteBankedTest.requestId1,
                Arrays.asList(
                        String.format("C-%s-%s%s-%s", patientId, "G", "001", "d"),
                        String.format("C-%s-%s%s-%s", patientId, "G", "002", "d"),
                        String.format("C-%s-%s%s-%s", patientId, "G", "003", "d")
                ));

        requestToCorrectedCmoIds.put(requestId2, Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "G", "004",
                "d")));

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    when2SamplesArePromotedSeparatelyOfDiffSpecimenButSameAbbrForSameReqSameAndNucleidAcid_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        String patientId = patientId1;
        promoteSample(patientId, XENOGRAFT, requestId1, "sample_1", "otherId_1", requestId1, serviceId, projectId);

        promoteSample(patientId, PDX, requestId1, "sample_2", "otherId_2", requestId1, serviceId, projectId);

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "X", "001",
                "d"), String.format("C-%s-%s%s-%s", patientId, "X", "002", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    when2SamplesArePromotedSeparatelyOfDiffSpecimenButSameAbbrForSameReqAndDiffNucleidAcid_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        String patientId = patientId1;
        promoteSample(patientId, XENOGRAFT, requestId1, "sample_1", "otherId_1", requestId1, serviceId, projectId);

        DataRecord banked2 = addPromoteBanked(patientId, PDX, RNA, requestId1, "sample_2", "otherId_2");
        initPromoteBanked(Arrays.asList(banked2), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId, "X", "001",
                "d"), String.format("C-%s-%s%s-%s", patientId, "X", "002", "r"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSamplesArePromotedAtOnceOfSameTypeForSameRequest_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        DataRecord banked1 = addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1);
        DataRecord banked2 = addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2);
        initPromoteBanked(Arrays.asList(banked1, banked2), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<String> correctedCmoIds = Arrays.asList(String.format("C-%s-%s%s-%s", patientId1, "G", "001",
                "d"), String.format("C-%s-%s%s-%s", patientId1, "G", "002", "d"));

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    whenCellLineAndPatientSamplesArePromotedAtOnceOfSameTypeForSameRequest_shouldNotCountCellLineToCounter() throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord cellLine = addPromoteBanked(patientId, CELLLINE, Optional.of(URINE), Optional.of(LOCAL_RECURRENCE)
                , RNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1);
        DataRecord banked1 = addPromoteBanked(patientId, PDX, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2);
        DataRecord banked2 = addPromoteBanked(patientId, XENOGRAFT, DNA, requestId1, SAMPLE_ID3, OTHER_SAMPLE_ID3);
        DataRecord banked3 = addPromoteBanked(patientId, ORGANOID, RNA, requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID4);
        initPromoteBanked(Arrays.asList(cellLine, banked1, banked2, banked3), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<String> correctedCmoIds = Arrays.asList(
                String.format("%s-%s", SAMPLE_ID1, normalizedRequestId1),
                String.format("C-%s-%s%s-%s", patientId, "X", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId, "X", "002", "d"),
                String.format("C-%s-%s%s-%s", patientId, "G", "001", "r")
        );

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenMultipleSamplesArePromotedAtOnce_shouldSetCorrectedCmoId() throws
            Exception {
        //given
        List<DataRecord> bankedToPromote1 = new ArrayList<DataRecord>() {{
            add(addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1));
            add(addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, SALIVA, Optional.of(PLASMA), Optional.of(CELL_FREE), DNA,
                    requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, CFDNA, Optional.of(WHOLE_BLOOD), DNA,
                    requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(WHOLE_BLOOD), Optional.of(NORMAL), DNA,
                    requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(WHOLE_BLOOD), Optional.of(ADJACENT_NORMAL), DNA,
                    requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, CFDNA, Optional.of(CEREBROSPINAL_FLUID), Optional.of(LOCAL_RECURRENCE),
                    RNA,
                    requestId1, "sample6", OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, CELLLINE, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2));
        }};

        initPromoteBanked(bankedToPromote1, requestId1, serviceId, projectId);
        promoteBanked.call();

        //when
        List<DataRecord> bankedToPromote2 = new ArrayList<DataRecord>() {{
            add(addPromoteBanked(patientId2, CFDNA, Optional.of(URINE), DNA, requestId2, SAMPLE_ID3,
                    OTHER_SAMPLE_ID2));
            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(CEREBROSPINAL_FLUID), Optional.of
                    (ADJACENT_TISSUE), DNA, requestId2, "sample5", OTHER_SAMPLE_ID3));
        }};

        initPromoteBanked(bankedToPromote2, requestId2, serviceId, projectId);
        promoteBanked.call();

        //then
        List<String> req1CorrectedCmoIds = Arrays.asList(
                String.format("%s-%s", SAMPLE_ID1, normalizedRequestId1),
                String.format("C-%s-%s%s-%s", patientId1, "G", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "L", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "L", "002", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "N", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "N", "002", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "S", "001", "r"),
                String.format("%s-%s", SAMPLE_ID2, normalizedRequestId1)
        );

        List<String> req2CorrectedCmoIds = Arrays.asList(
                String.format("C-%s-%s%s-%s", patientId2, "U", "001", "d"),
                String.format("C-%s-%s%s-%s", patientId3, "T", "001", "d")
        );

        Map<String, List<String>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, req1CorrectedCmoIds);
        requestToCorrectedCmoIds.put(requestId2, req2CorrectedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    private void promoteSample(String patientId, SpecimenType specimenType, Optional<SampleOrigin> sampleOrigin,
                               Optional<SampleClass>
                                       sampleClass, String bankedReqId, String sampleId, String otherSampleId, String
                                       promoteRequest, String
                                       serviceId, String projectId) throws Exception {

        DataRecord banked1 = addPromoteBanked(patientId, specimenType, sampleOrigin, sampleClass, DNA, bankedReqId,
                sampleId, otherSampleId);
        initPromoteBanked(Arrays.asList(banked1), promoteRequest, serviceId, projectId);

        promoteBanked.call();
    }

    private void promoteSample(String patientId, SpecimenType specimenType, String bankedReqId, String sampleId,
                               String otherSampleId, String promoteRequest, String serviceId, String projectId)
            throws Exception {
        promoteSample(patientId, specimenType, Optional.empty(), Optional.empty(), bankedReqId, sampleId,
                otherSampleId, promoteRequest, serviceId, projectId);
    }

    private void initPromoteBanked(List<DataRecord> bankedRecords, String requestId, String serviceId, String
            projectId) {
        String[] bankedIds = new String[bankedRecords.size()];

        for (int i = 0; i < bankedRecords.size(); i++) {
            bankedIds[i] = String.valueOf(bankedRecords.get(i).getRecordId());
        }

        promoteBanked.init(bankedIds, projectId, requestId, serviceId, "promoteBankedTest", "false");
    }

    private DataRecord addPromoteBanked(String patientId, SpecimenType specimenType, Optional<SampleOrigin>
            sampleOrigin, Optional<SampleClass> sampleClass,
                                        NucleicAcid nucleicAcid, String requestId, String sampleId, String
                                                otherSampleId) throws Exception {
        DataRecord bankedSampleRecord = dataRecordManager.addDataRecord(BankedSample.DATA_TYPE_NAME, user);
        Map<String, Object> fields = new HashMap<>();

        fields.put(BankedSample.SERVICE_ID, serviceId);
        fields.put(BankedSample.SPECIMEN_TYPE, specimenType.getValue());
        fields.put(BankedSample.REQUEST_ID, requestId);
        fields.put(BankedSample.PATIENT_ID, patientId);
        fields.put(BankedSample.NATO_EXTRACT, nucleicAcid.getValue());
        fields.put(BankedSample.USER_SAMPLE_ID, sampleId);
        fields.put(BankedSample.OTHER_SAMPLE_ID, otherSampleId);
        fields.put(BankedSample.SPECIES, "Human");

        sampleOrigin.ifPresent(sampleOrigin1 -> fields.put(BankedSample.SAMPLE_ORIGIN, sampleOrigin1.getValue()));
        sampleClass.ifPresent(sampleClass1 -> fields.put(BankedSample.SAMPLE_CLASS, sampleClass1.getValue()));

        bankedSampleRecord.setFields(fields, user);

        dataRecordManager.storeAndCommit("Added banked sample: " + userSampleId, user);

        createdBankedRecords.add(bankedSampleRecord);
        return bankedSampleRecord;
    }

    private DataRecord addPromoteBanked(String patientId, SpecimenType specimenType, Optional<SampleOrigin>
            sampleOrigin,
                                        NucleicAcid nucleicAcid, String requestId, String sampleId, String
                                                otherSampleId) throws Exception {
        return addPromoteBanked(patientId, specimenType, sampleOrigin, Optional.empty(), nucleicAcid, requestId,
                sampleId, otherSampleId);
    }

    private DataRecord addPromoteBanked(String patientId, SpecimenType specimenType, NucleicAcid nucleicAcid, String
            requestId, String sampleId, String otherSampleId) throws
            Exception {
        return addPromoteBanked(patientId, specimenType, Optional.empty(), nucleicAcid, requestId, sampleId,
                otherSampleId);

    }

    private void assertPromoteSample(Map<String, List<String>> requestToCorrectedCmoIds) throws
            Exception {

        for (Map.Entry<String, List<String>> requestToCorrectedCmoId : requestToCorrectedCmoIds.entrySet()) {
            String assignedRequestId = requestToCorrectedCmoId.getKey();

            List<DataRecord> requestRecords = dataRecordManager.queryDataRecords(VeloxConstants.REQUEST, VeloxConstants
                    .REQUEST_ID + " = '" + assignedRequestId + "'", user);
            assertThat(requestRecords.size(), is(1));

            DataRecord[] samples = requestRecords.get(0).getChildrenOfType(VeloxConstants.SAMPLE, user);

            List<DataRecord> sampleList = Arrays.asList(samples);
            List<String> samplesToCorrectedIds = requestToCorrectedCmoId.getValue();
            assertThat(sampleList.size(), is(samplesToCorrectedIds.size()));

            int i = 0;
            for (DataRecord promotedSample : sampleList) {
                assertThat(promotedSample.getStringVal(VeloxConstants.SAMPLE_ID, user), is(assignedRequestId + "_" +
                        (i + 1)));

                assertCmoInfoRecordExists(promotedSample, samplesToCorrectedIds.get(i), promotedSample);
                i++;
            }
        }
    }

    private void assertCmoInfoRecordExists(DataRecord promotedSample, String expectedCorrectedId, DataRecord
            bankedRecord) throws NotFound, IoError, RemoteException {
        DataRecord[] sampleCMOInfoRecords = promotedSample.getChildrenOfType(VeloxConstants.SAMPLE_CMO_INFO_RECORDS,
                user);
        assertThat(sampleCMOInfoRecords.length, is(1));
        assertThat(sampleCMOInfoRecords[0].getStringVal("UserSampleID", user), is(bankedRecord.getStringVal
                ("UserSampleID", user)));
        assertThat(sampleCMOInfoRecords[0].getStringVal("CorrectedCMOID", user), is(expectedCorrectedId));
    }

    private void reopenConnection() throws Exception {
        if (connection.isConnected())
            connection.close();

        if (!connection.isConnected())
            connection.open();

        dataRecordManager = connection.getDataRecordManager();
        dataMgmtServer = connection.getDataMgmtServer();
        user = connection.getUser();
        managerContext = new VeloxStandaloneManagerContext(user, dataMgmtServer);
        drum = new DataRecordUtilManager(managerContext);
    }

    private CorrectedCmoSampleIdGenerator getCorrectedCmoSampleIdGenerator() throws Exception {
        SampleAbbreviationRetriever sampleAbbrRetr = new CspaceSampleAbbreviationRetriever();

        SampleToCorrectedCmoIdConverter sampleToCorrectedCmoIdConv = new SampleToCorrectedCmoIdConverter();

        CorrectedCmoIdConverterFactory converterFactory = new FormatAwareCorrectedCmoIdConverterFactory
                (sampleAbbrRetr);

        SampleCounterRetriever sampleCountRetr = new IncrementalSampleCounterRetriever(converterFactory);

        CmoSampleIdResolver patientCmoResolver = new PatientCmoSampleIdResolver(sampleCountRetr, sampleAbbrRetr);

        PatientCmoSampleIdFormatter patientCmoSampleIdFormatter = new PatientCmoSampleIdFormatter();

        CellLineCmoSampleIdFormatter cellLineCmoSampleIdFormatter = new CellLineCmoSampleIdFormatter();

        CmoSampleIdResolver cellLineSmoSampleResolver = new CellLineCmoSampleIdResolver();

        CmoSampleIdRetriever cellLineCmoSampleRetriever = new FormattedCmoSampleIdRetriever
                (cellLineSmoSampleResolver, cellLineCmoSampleIdFormatter);

        CmoSampleIdRetriever patientCmoSampleRetriever = new FormattedCmoSampleIdRetriever(patientCmoResolver,
                patientCmoSampleIdFormatter);

        CmoSampleIdRetrieverFactory cmoSampleIdRetrieverFactory = new CmoSampleIdRetrieverFactory
                (patientCmoSampleRetriever, cellLineCmoSampleRetriever);

        SampleRecordToSampleConverter sampleRecordToSampleConverter = new SampleRecordToSampleConverter();
        PatientSamplesRetriever patientSamplesRetriever = new PatientSamplesWithCmoInfoRetriever
                (sampleToCorrectedCmoIdConv, sampleRecordToSampleConverter);

        Properties properties = new Properties();
        properties.load(new FileReader(getResourceFile("/slack.properties")));

        Notificator notificator = new SlackNotificator(properties.getProperty("webhookUrl"), properties.getProperty
                ("channel"), properties.getProperty("user"), properties.getProperty("icon"));

        return new SampleTypeCorrectedCmoSampleIdGenerator
                (cmoSampleIdRetrieverFactory, patientSamplesRetriever, notificator);
    }

    private PromoteBanked getPromoteBanked() throws Exception {
        BankedSampleToCorrectedCmoSampleIdConverter bankedSampleToCorrectedCmoSampleIdConverter = new
                BankedSampleToCorrectedCmoSampleIdConverter();
        return new PromoteBanked(bankedSampleToCorrectedCmoSampleIdConverter,
                getCorrectedCmoSampleIdGenerator());
    }

    private String getResourceFile(String connectionFile) throws Exception {
        return PromoteBankedTest.class.getResource(connectionFile).getPath();
    }

    private void addProjectWithRequests() throws RemoteException, ServerException, AlreadyExists, NotFound, IoError {
        Map<String, Object> projectFields = new HashMap<>();
        projectFields.put("ProjectId", projectId);
        projectRecord = drum.addDataRecord(VeloxConstants.PROJECT, projectFields);

        Map<String, Object> request1Fields = new HashMap<>();
        request1Fields.put(VeloxConstants.REQUEST_ID, requestId1);
        DataRecord requestRecord1 = drum.addDataRecord(VeloxConstants.REQUEST, request1Fields);

        Map<String, Object> request2Fields = new HashMap<>();
        request2Fields.put(VeloxConstants.REQUEST_ID, requestId2);
        DataRecord requestRecord2 = drum.addDataRecord(VeloxConstants.REQUEST, request2Fields);

        projectRecord.addChild(requestRecord1, user);
        projectRecord.addChild(requestRecord2, user);
    }

    private void deleteRecord(DataRecord record) throws IoError, RemoteException, NotFound {
        drum.deleteRecords(Arrays.asList(record), true);
        LOG.info(String.format("Deleted record of type: %s, with record id: %s", record.getDataTypeName(), record
                .getRecordId()));
    }

    private void deleteBankedSampleRecords() throws Exception {
        for (DataRecord createdBankedRecord : createdBankedRecords) {
            dataRecordManager.deleteDataRecords(Arrays.asList(createdBankedRecord), null, true, user);
            LOG.info(String.format("Deleted banked sample record: %s", createdBankedRecord.getRecordId()));
        }
    }
}