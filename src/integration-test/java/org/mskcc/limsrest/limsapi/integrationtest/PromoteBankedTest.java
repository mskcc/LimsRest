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
import org.mskcc.limsrest.limsapi.cmoinfo.cspace.CspaceSampleTypeAbbreviationRetriever;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdFormatter;
import org.mskcc.limsrest.limsapi.cmoinfo.patientsample.PatientCmoSampleIdResolver;
import org.mskcc.limsrest.limsapi.cmoinfo.retriever.*;
import org.mskcc.limsrest.limsapi.converter.SampleRecordToSampleConverter;
import org.mskcc.limsrest.limsapi.promote.BankedSampleToSampleConverter;
import org.mskcc.util.Constants;
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
    private final String patientId1 = "C-promoteBankedTestPatient1";
    private final String patientId2 = "C-promoteBankedTestPatient2";
    private final String patientId3 = "C-promoteBankedTestPatient3";
    private List<DataRecord> createdBankedRecords;
    private VeloxConnection connection;
    private DataRecordManager dataRecordManager;
    private User user;
    private DataMgmtServer dataMgmtServer;
    private DataRecordUtilManager drum;
    private VeloxStandaloneManagerContext managerContext;
    private DataRecord projectRecord;
    private BankedSampleToSampleConverter bankedSampleToSampleConverter = new BankedSampleToSampleConverter();
    private int id = 0;

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
        DataRecord dataRecord = promoteSample(patientId1, CELLLINE, requestId1, userSampleId, "otherId_1",
                requestId1, serviceId, projectId);

        //then
        assertPromoteSample(ImmutableMap.<String, List<BankedWithCorrectedCmoId>>builder()
                .put(
                        requestId1,
                        Collections.singletonList(new BankedWithCorrectedCmoId(dataRecord, String.format("%s-%s",
                                userSampleId, normalizedRequestId1))))
                .build());
    }

    @Test
    public void whenCellFreeSampleIsPromoted_shouldPromoteBankedSampleWithNoCmoId() throws Exception {
        //given
        DataRecord dataRecord = promoteSample(patientId3, SpecimenType.SALIVA, Optional.of(PLASMA), Optional.of
                        (CELL_FREE), requestId1,
                SAMPLE_ID4, OTHER_SAMPLE_ID2, requestId1, serviceId, projectId);

        //then
        assertPromoteSample(ImmutableMap.<String, List<BankedWithCorrectedCmoId>>builder()
                .put(
                        requestId1,
                        Collections.singletonList(new BankedWithCorrectedCmoId(dataRecord, "")))
                .build());
    }

    @Test
    public void whenMultipleCellLineSamplesArePromotedForSamePatient_shouldAssignCorrectedCmoId() throws Exception {
        //given
        DataRecord banked1 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID1, OTHER_SAMPLE_ID1,
                SampleType.DNA);
        DataRecord banked2 = addPromoteBanked(patientId1, CELLLINE, RNA, requestId1, USER_SAMP_ID2, OTHER_SAMPLE_ID2,
                SampleType.DNA);
        DataRecord banked3 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID3, OTHER_SAMPLE_ID3,
                SampleType.DNA);
        DataRecord banked4 = addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, USER_SAMP_ID4, OTHER_SAMPLE_ID4,
                SampleType.DNA);
        initPromoteBanked(Arrays.asList(banked1, banked2, banked3, banked4), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s", USER_SAMP_ID1, normalizedRequestId1)),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s", USER_SAMP_ID2, normalizedRequestId1)),
                new BankedWithCorrectedCmoId(banked3, String.format("%s-%s", USER_SAMP_ID3, normalizedRequestId1)),
                new BankedWithCorrectedCmoId(banked4, String.format("%s-%s", USER_SAMP_ID4, normalizedRequestId1))
        );

        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenSampleIsFirstPatientSampleOfThatType_shouldSetCorrectedCmoIdWithCount1() throws Exception {
        //given
        DataRecord dataRecord = promoteSample(patientId1, ORGANOID, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1,
                requestId1, serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(dataRecord, String.format("%s-%s%s-%s", patientId1, "G", "001", "d"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenSampleIsCfdna_shouldSetIdBasedOnSampleOrigin() throws Exception {
        //given
        DataRecord dataRecord = promoteSample(patientId1, CFDNA, Optional.of(URINE), Optional.empty(), requestId1,
                SAMPLE_ID1, OTHER_SAMPLE_ID1,
                requestId1, serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(dataRecord, String.format("%s-%s%s-%s", patientId1, "U", "001", "d"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenBankedSampleHasRequestIdSet_shouldAsignToRequestProvidedToPromoteBanked() throws Exception {
        //given
        DataRecord dataRecord = promoteSample(patientId1, ORGANOID, "someOtherRequest", SAMPLE_ID1, OTHER_SAMPLE_ID1,
                requestId1, serviceId,
                projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(dataRecord, String.format("%s-%s%s-%s", patientId1, "G", "001", "d")));

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSampleArePromotedSeparatelyOfSameTypeForSameRequest_shouldSetCorrectedCmoIdWithCount1And2()
            throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord banked1 = promoteSample(patientId, ORGANOID, requestId1, "sample_1", "otherId_1", requestId1,
                serviceId, projectId);

        DataRecord banked2 = promoteSample(patientId, ORGANOID, requestId1, "sample_2", "otherId_2", requestId1,
                serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "G", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "G", "002", "d"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
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
        DataRecord banked1 = promoteSample(patientId, CELLLINE, Optional.of(CEREBROSPINAL_FLUID), Optional.of
                        (ADJACENT_NORMAL),
                requestId1, cellLineSampleId, OTHER_SAMPLE_ID1, requestId1, serviceId, projectId);

        //when
        DataRecord banked2 = promoteSample(patientId, PDX, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2, requestId1,
                serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s", cellLineSampleId, normalizedRequestId1)),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "X", "001", "d"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
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
        DataRecord banked1 = promoteSample(patientId1, ORGANOID, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1,
                requestId1, serviceId, projectId);

        //when
        DataRecord banked2 = promoteSample(patientId2, ORGANOID, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2,
                requestId1, serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId1, "G", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId2, "G", "001", "d"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSampleArePromotedSeparatelyOfSameTypeForDifferentRequest_shouldSetCorrectedCmoIdWithCount1And2()
            throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord banked1 = promoteSample(patientId, ORGANOID, "someReqId", "sample_1", "otherId_1", requestId1,
                serviceId, projectId);

        DataRecord banked2 = promoteSample(patientId, ORGANOID, "someReqId", "sample_2", "otherId_2", requestId2,
                serviceId, projectId);

        //then
        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "G", "001", "d")))
        );

        requestToCorrectedCmoIds.put(requestId2, Arrays.asList(
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "G", "002", "d")))
        );

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    whenMultipleSamplesArePromotedSeparatelyOfSameTypeForDifferentRequest_shouldSetCorrectedCmoIdWithIncrementedCount()
            throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord banked1 = promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID1, OTHER_SAMPLE_ID1,
                PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);
        DataRecord banked2 = promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID2, OTHER_SAMPLE_ID2,
                PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);
        DataRecord banked3 = promoteSample(patientId, ORGANOID, "someReqId", SAMPLE_ID3, OTHER_SAMPLE_ID3,
                PromoteBankedTest.requestId1,
                PromoteBankedTest.serviceId, PromoteBankedTest.projectId);

        //when
        DataRecord banked4 = promoteSample(patientId, ORGANOID, "someReqId", "sample_2", "otherId_2", requestId2,
                PromoteBankedTest
                        .serviceId, PromoteBankedTest.projectId);

        //then
        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(PromoteBankedTest.requestId1,
                Arrays.asList(
                        new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "G", "001",
                                "d")),
                        new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "G", "002",
                                "d")),
                        new BankedWithCorrectedCmoId(banked3, String.format("%s-%s%s-%s", patientId, "G", "003",
                                "d")))
        );

        requestToCorrectedCmoIds.put(requestId2, Arrays.asList(
                new BankedWithCorrectedCmoId(banked4, String.format("%s-%s%s-%s", patientId, "G", "004", "d")))
        );

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    when2SamplesArePromotedSeparatelyOfDiffSpecimenButSameAbbrForSameReqSameAndNucleidAcid_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord banked1 = promoteSample(patientId, XENOGRAFT, requestId1, "sample_1", "otherId_1", requestId1,
                serviceId, projectId);

        DataRecord banked2 = promoteSample(patientId, PDX, requestId1, "sample_2", "otherId_2", requestId1,
                serviceId, projectId);

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "X", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "X", "002", "d")));

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void
    when2SamplesArePromotedSeparatelyOfDiffSpecimenButSameAbbrForSameReqAndDiffNucleidAcid_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        String patientId = patientId1;
        DataRecord banked1 = promoteSample(patientId, XENOGRAFT, requestId1, "sample_1", "otherId_1", requestId1,
                serviceId, projectId);

        DataRecord banked2 = addPromoteBanked(patientId, PDX, RNA, requestId1, "sample_2", "otherId_2", SampleType.RNA);
        initPromoteBanked(Arrays.asList(banked2), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "X", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "X", "002", "r")));

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenTwoSamplesArePromotedAtOnceOfSameTypeForSameRequest_shouldSetCorrectedCmoIdWithCount1And2() throws
            Exception {
        //given
        DataRecord banked1 = addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1,
                SampleType.DNA);
        DataRecord banked2 = addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2, SampleType.DNA);
        initPromoteBanked(Arrays.asList(banked1, banked2), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId1, "G", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId1, "G", "002", "d")));

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
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
                , RNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1, SampleType.RNA);
        DataRecord banked1 = addPromoteBanked(patientId, PDX, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2,
                SampleType.DNA);
        DataRecord banked2 = addPromoteBanked(patientId, XENOGRAFT, DNA, requestId1, SAMPLE_ID3, OTHER_SAMPLE_ID3,
                SampleType.DNA);
        DataRecord banked3 = addPromoteBanked(patientId, ORGANOID, RNA, requestId1, SAMPLE_ID4, OTHER_SAMPLE_ID4, SampleType.RNA);
        initPromoteBanked(Arrays.asList(cellLine, banked1, banked2, banked3), requestId1, serviceId, projectId);

        //when
        promoteBanked.call();

        //then
        List<BankedWithCorrectedCmoId> correctedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(cellLine, String.format("%s-%s", SAMPLE_ID1, normalizedRequestId1)),
                new BankedWithCorrectedCmoId(banked1, String.format("%s-%s%s-%s", patientId, "X", "001", "d")),
                new BankedWithCorrectedCmoId(banked2, String.format("%s-%s%s-%s", patientId, "X", "002", "d")),
                new BankedWithCorrectedCmoId(banked3, String.format("%s-%s%s-%s", patientId, "G", "001", "r"))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, correctedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    @Test
    public void whenMultipleSamplesArePromotedAtOnce_shouldSetCorrectedCmoId() throws
            Exception {
        //given
        List<DataRecord> bankedToPromote1 = new ArrayList<DataRecord>() {{
            add(addPromoteBanked(patientId1, CELLLINE, DNA, requestId1, SAMPLE_ID1, OTHER_SAMPLE_ID1, SampleType.DNA));

            add(addPromoteBanked(patientId1, ORGANOID, DNA, requestId1, getNextSampleId(), OTHER_SAMPLE_ID2, SampleType.DNA_LIBRARY));

            add(addPromoteBanked(patientId3, SALIVA, Optional.of(PLASMA), Optional.of(METASTASIS), DNA,
                    requestId1, getNextSampleId(), OTHER_SAMPLE_ID2, SampleType.CFDNA));

            add(addPromoteBanked(patientId3, CFDNA, Optional.of(WHOLE_BLOOD), DNA,
                    requestId1, getNextSampleId(), OTHER_SAMPLE_ID2, SampleType.RNA));

            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(WHOLE_BLOOD), Optional.of(NORMAL), DNA,
                    requestId1, getNextSampleId(), OTHER_SAMPLE_ID2, SampleType.DNA));

            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(WHOLE_BLOOD), Optional.of(ADJACENT_NORMAL), DNA,
                    requestId1, getNextSampleId(), OTHER_SAMPLE_ID2, SampleType.DNA));

            add(addPromoteBanked(patientId3, CFDNA, Optional.of(CEREBROSPINAL_FLUID), Optional.of(LOCAL_RECURRENCE),
                    RNA, requestId1, "sample6", OTHER_SAMPLE_ID2, SampleType.BLOCKS_SLIDES));

            add(addPromoteBanked(patientId3, CELLLINE, DNA, requestId1, SAMPLE_ID2, OTHER_SAMPLE_ID2, SampleType.DNA));
        }};

        initPromoteBanked(bankedToPromote1, requestId1, serviceId, projectId);
        promoteBanked.call();

        //when
        List<DataRecord> bankedToPromote2 = new ArrayList<DataRecord>() {{
            add(addPromoteBanked(patientId2, CFDNA, Optional.of(URINE), DNA, requestId2, getNextSampleId(),
                    OTHER_SAMPLE_ID2, SampleType.DNA));

            add(addPromoteBanked(patientId3, RAPIDAUTOPSY, Optional.of(CEREBROSPINAL_FLUID), Optional.of
                    (ADJACENT_TISSUE), DNA, requestId2, getNextSampleId(), OTHER_SAMPLE_ID3, SampleType.DNA));
        }};

        initPromoteBanked(bankedToPromote2, requestId2, serviceId, projectId);
        promoteBanked.call();

        //then
        List<BankedWithCorrectedCmoId> req1CorrectedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(bankedToPromote1.get(0), String.format("%s-%s", SAMPLE_ID1,
                        normalizedRequestId1)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(1), String.format("%s-%s%s-%s", patientId1, "G",
                        "001", Constants.DNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(2), String.format("%s-%s%s-%s", patientId3, "M",
                        "001", Constants.DNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(3), String.format("%s-%s%s-%s", patientId3, "L",
                        "001", Constants.DNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(4), String.format("%s-%s%s-%s", patientId3, "N",
                        "001", Constants.RNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(5), String.format("%s-%s%s-%s", patientId3, "N",
                        "002", Constants.DNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(6), String.format("%s-%s%s-%s", patientId3, "S",
                        "001", Constants.RNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote1.get(7), String.format("%s-%s", SAMPLE_ID2,
                        normalizedRequestId1))
        );

        List<BankedWithCorrectedCmoId> req2CorrectedCmoIds = Arrays.asList(
                new BankedWithCorrectedCmoId(bankedToPromote2.get(0), String.format("%s-%s%s-%s", patientId2, "U",
                        "001", Constants.DNA_ABBREV)),
                new BankedWithCorrectedCmoId(bankedToPromote2.get(1), String.format("%s-%s%s-%s", patientId3, "T",
                        "001", Constants.DNA_ABBREV))
        );

        Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds = new HashMap<>();
        requestToCorrectedCmoIds.put(requestId1, req1CorrectedCmoIds);
        requestToCorrectedCmoIds.put(requestId2, req2CorrectedCmoIds);

        assertPromoteSample(requestToCorrectedCmoIds);
    }

    private String getNextSampleId() {
        return "SampleId" + (id++);
    }

    private DataRecord promoteSample(String patientId, SpecimenType specimenType, Optional<SampleOrigin>
            sampleOrigin, Optional<SampleClass> sampleClass, String bankedReqId, String sampleId, String
                                             otherSampleId, String promoteRequest, String serviceId, String
                                             projectId) throws Exception {
        DataRecord banked = addPromoteBanked(patientId, specimenType, sampleOrigin, sampleClass, DNA, bankedReqId,
                sampleId, otherSampleId, SampleType.DNA);
        initPromoteBanked(Arrays.asList(banked), promoteRequest, serviceId, projectId);

        promoteBanked.call();

        return banked;
    }

    private DataRecord promoteSample(String patientId, SpecimenType specimenType, String bankedReqId, String sampleId,
                                     String otherSampleId, String promoteRequest, String serviceId, String projectId)
            throws Exception {
        return promoteSample(patientId, specimenType, Optional.empty(), Optional.empty(), bankedReqId, sampleId,
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
                                                otherSampleId, SampleType sampleType) throws Exception {
        DataRecord bankedSampleRecord = dataRecordManager.addDataRecord(BankedSample.DATA_TYPE_NAME, user);
        Map<String, Object> fields = new HashMap<>();

        fields.put(BankedSample.ASSAY, "assay");
        fields.put(BankedSample.CELL_COUNT, 2);
        fields.put(BankedSample.CLINICAL_INFO, "clinicalInfo");
        fields.put(BankedSample.CMO_PATIENT_ID, patientId);
        fields.put(BankedSample.COL_POSITION, "C");
        fields.put(BankedSample.COLLECTION_YEAR, "1998");
        fields.put(BankedSample.CONCENTRATION, 23.5);
        fields.put(BankedSample.CONCENTRATION_UNITS, "ng/l");
        fields.put(BankedSample.ESTIMATED_PURITY, 34.70);
        fields.put(BankedSample.GENDER, "M");
        fields.put(BankedSample.GENETIC_ALTERATIONS, "alterations");
        fields.put(BankedSample.INVESTIGATOR, "investigator");
        fields.put(BankedSample.NATO_EXTRACT, nucleicAcid.getValue());
        fields.put(BankedSample.NON_LIMS_LIBRARY_INPUT, 230);
        fields.put(BankedSample.NON_LIMS_LIBRARY_OUTPUT, 56.07);
        fields.put(BankedSample.ORGANISM, "Human");
        fields.put(BankedSample.OTHER_SAMPLE_ID, otherSampleId);
        fields.put(BankedSample.PATIENT_ID, patientId);
        fields.put(BankedSample.PLATE_ID, "plateId");
        fields.put(BankedSample.PLATFORM, "platform");
        fields.put(BankedSample.PRESERVATION, "preservation");
        fields.put(BankedSample.PROMOTED, "false");
        fields.put(BankedSample.RECIPE, "recipe");
        fields.put(BankedSample.REQUEST_ID, requestId);
        fields.put(BankedSample.REQUESTED_READS, "12345");
        fields.put(BankedSample.ROW_INDEX, 2);
        fields.put(BankedSample.RUN_TYPE, "runTYpe");
        fields.put(BankedSample.SAMPLE_TYPE, sampleType.toString());
        fields.put(BankedSample.SERVICE_ID, serviceId);
        fields.put(BankedSample.SPECIES, "Human");
        fields.put(BankedSample.SPECIMEN_TYPE, specimenType.getValue());
        fields.put(BankedSample.SPIKE_IN_GENES, "spikeInGenes");
        fields.put(BankedSample.TISSUE_SITE, "tissueSite");
        fields.put(BankedSample.TRANSACTION_ID, 12376534);
        fields.put(BankedSample.TUBE_BARCODE, "tybeBarcode");
        fields.put(BankedSample.TUMOR_OR_NORMAL, "Normals");
        fields.put(BankedSample.TUMOR_TYPE, "tumorType");
        fields.put(BankedSample.USER_SAMPLE_ID, sampleId);
        fields.put(BankedSample.VOLUME, 456.76);

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
                                                otherSampleId, SampleType sampleType) throws Exception {
        return addPromoteBanked(patientId, specimenType, sampleOrigin, Optional.empty(), nucleicAcid, requestId,
                sampleId, otherSampleId, sampleType);
    }

    private DataRecord addPromoteBanked(String patientId, SpecimenType specimenType, NucleicAcid nucleicAcid, String
            requestId, String sampleId, String otherSampleId, SampleType sampleType) throws
            Exception {
        return addPromoteBanked(patientId, specimenType, Optional.empty(), nucleicAcid, requestId, sampleId,
                otherSampleId, sampleType);

    }

    private void assertPromoteSample(Map<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoIds) throws
            Exception {

        for (Map.Entry<String, List<BankedWithCorrectedCmoId>> requestToCorrectedCmoId : requestToCorrectedCmoIds
                .entrySet()) {
            String assignedRequestId = requestToCorrectedCmoId.getKey();

            List<DataRecord> requestRecords = dataRecordManager.queryDataRecords(VeloxConstants.REQUEST, VeloxConstants
                    .REQUEST_ID + " = '" + assignedRequestId + "'", user);
            assertThat(requestRecords.size(), is(1));

            DataRecord[] samples = requestRecords.get(0).getChildrenOfType(VeloxConstants.SAMPLE, user);

            List<DataRecord> sampleList = Arrays.asList(samples);
            List<BankedWithCorrectedCmoId> bankedWithCorrectedCmoIds = requestToCorrectedCmoId.getValue();
            assertThat(sampleList.size(), is(bankedWithCorrectedCmoIds.size()));

            int i = 0;
            for (DataRecord promotedSample : sampleList) {
                assertThat(promotedSample.getStringVal(VeloxConstants.SAMPLE_ID, user), is(assignedRequestId + "_" +
                        (i + 1)));

                assertThat(promotedSample.getDataTypeName(), is(Sample.DATA_TYPE_NAME));
                DataRecord bankedSample = bankedWithCorrectedCmoIds.get(i).bankedDataRecord;

                assertThat(promotedSample.getStringVal(Sample.ASSAY, user), is(bankedSample.getStringVal(BankedSample
                        .ASSAY, user)));

                assertThat(promotedSample.getStringVal(Sample.CELL_COUNT, user), is(bankedSample.getStringVal
                        (BankedSample.CELL_COUNT, user)));
                assertThat(promotedSample.getStringVal(Sample.CLINICAL_INFO, user), is(bankedSample.getStringVal
                        (BankedSample.CLINICAL_INFO, user)));
                assertThat(promotedSample.getStringVal(Sample.CMOSAMPLE_CLASS, user), is(bankedSample.getStringVal
                        (BankedSample.SAMPLE_CLASS, user)));
                assertThat(promotedSample.getStringVal(Sample.COL_POSITION, user), is(bankedSample.getStringVal
                        (BankedSample.COL_POSITION, user)));
                assertThat(promotedSample.getStringVal(Sample.COLLECTION_YEAR, user), is(bankedSample.getStringVal
                        (BankedSample.COLLECTION_YEAR, user)));
                assertThat(promotedSample.getDoubleVal(Sample.CONCENTRATION, user), is(bankedSample.getDoubleVal
                        (BankedSample.CONCENTRATION, user)));
                assertThat(promotedSample.getStringVal(Sample.CONCENTRATION_UNITS, user), is(bankedSample
                        .getStringVal(BankedSample.CONCENTRATION_UNITS, user)));

                assertThat(promotedSample.getDoubleVal(Sample.ESTIMATED_PURITY, user), is(bankedSample
                        .getDoubleVal(BankedSample.ESTIMATED_PURITY, user)));
                assertThat(promotedSample.getStringVal(Sample.EXEMPLAR_SAMPLE_TYPE, user), is(bankedSample
                        .getStringVal(BankedSample.SAMPLE_TYPE, user)));
                assertThat(promotedSample.getStringVal(Sample.EXEMPLAR_SAMPLE_STATUS, user), is(Constants.RECEIVED));

                assertThat(promotedSample.getStringVal(Sample.GENDER, user), is(bankedSample.getStringVal
                        (BankedSample.GENDER, user)));
                assertThat(promotedSample.getStringVal(Sample.GENETIC_ALTERATIONS, user), is(bankedSample
                        .getStringVal(BankedSample.GENETIC_ALTERATIONS, user)));

                assertThat(promotedSample.getStringVal(Sample.NATO_EXTRACT, user), is(bankedSample
                        .getStringVal(BankedSample.NATO_EXTRACT, user)));

                assertThat(promotedSample.getStringVal(Sample.ORGANISM, user), is(bankedSample
                        .getStringVal(BankedSample.ORGANISM, user)));
                assertThat(promotedSample.getStringVal(Sample.OTHER_SAMPLE_ID, user), is(bankedSample
                        .getStringVal(BankedSample.OTHER_SAMPLE_ID, user)));

                assertThat(promotedSample.getStringVal(Sample.PATIENT_ID, user), is(bankedSample.getStringVal
                        (BankedSample.PATIENT_ID, user)));
                assertThat(promotedSample.getStringVal(Sample.PLATFORM, user), is(bankedSample.getStringVal
                        (BankedSample.PLATFORM, user)));
                assertThat(promotedSample.getStringVal(Sample.PRESERVATION, user), is(bankedSample.getStringVal
                        (BankedSample.PRESERVATION, user)));

                assertThat(promotedSample.getDoubleVal(Sample.RECEIVED_QUANTITY, user), is(bankedSample.getDoubleVal
                        (BankedSample.VOLUME, user)));
                assertThat(promotedSample.getStringVal(Sample.RECIPE, user), is(bankedSample.getStringVal
                        (BankedSample.RECIPE, user)));
                assertThat(promotedSample.getStringVal(Sample.REQUEST_ID, user), is(assignedRequestId));
                assertThat(promotedSample.getStringVal(Sample.ROW_POSITION, user), is(bankedSample.getStringVal
                        (BankedSample.ROW_POSITION, user)));

                assertThat(promotedSample.getStringVal(Sample.SAMPLE_ORIGIN, user), is(bankedSample.getStringVal
                        (BankedSample.SAMPLE_ORIGIN, user)));
                assertThat(promotedSample.getStringVal(Sample.SPECIES, user), is(bankedSample.getStringVal
                        (BankedSample.SPECIES, user)));
                assertThat(promotedSample.getStringVal(Sample.SPECIMEN_TYPE, user), is(bankedSample.getStringVal
                        (BankedSample.SPECIMEN_TYPE, user)));
                assertThat(promotedSample.getStringVal(Sample.SPIKE_IN_GENES, user), is(bankedSample.getStringVal
                        (BankedSample.SPIKE_IN_GENES, user)));
                assertThat(promotedSample.getStringVal(Sample.SPECIMEN_TYPE, user), is(bankedSample.getStringVal
                        (BankedSample.SPECIMEN_TYPE, user)));

                assertThat(promotedSample.getStringVal(Sample.TISSUE_LOCATION, user), is(bankedSample.getStringVal
                        (BankedSample.TISSUE_SITE, user)));
                assertThat(promotedSample.getStringVal(Sample.TUBE_BARCODE, user), is(bankedSample.getStringVal
                        (BankedSample.TUBE_BARCODE, user)));
                assertThat(promotedSample.getStringVal(Sample.TUMOR_OR_NORMAL, user), is(bankedSample
                        .getStringVal(BankedSample.TUMOR_OR_NORMAL, user)));
                assertThat(promotedSample.getStringVal(Sample.TUMOR_TYPE, user), is(bankedSample.getStringVal
                        (BankedSample.TUMOR_TYPE, user)));

                assertCmoInfoRecordExists(promotedSample, bankedWithCorrectedCmoIds.get(i), promotedSample);
                i++;
            }
        }
    }

    private void assertCmoInfoRecordExists(DataRecord promotedSample, BankedWithCorrectedCmoId
            bankedWithCorrectedCmoId, DataRecord
                                                   bankedRecord) throws NotFound, IoError, RemoteException {
        DataRecord[] sampleCMOInfoRecords = promotedSample.getChildrenOfType(VeloxConstants.SAMPLE_CMO_INFO_RECORDS,
                user);
        assertThat(sampleCMOInfoRecords.length, is(1));
        assertThat(sampleCMOInfoRecords[0].getStringVal("UserSampleID", user), is(bankedRecord.getStringVal
                ("UserSampleID", user)));
        assertThat(sampleCMOInfoRecords[0].getStringVal("CorrectedCMOID", user), is(bankedWithCorrectedCmoId.cmoId));
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
        SampleTypeAbbreviationRetriever sampleAbbrRetr = new CspaceSampleTypeAbbreviationRetriever();

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
                getCorrectedCmoSampleIdGenerator(), bankedSampleToSampleConverter);
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
        DataRecord[] requests = record.getChildrenOfType(VeloxConstants.REQUEST, user);

        for (DataRecord request : requests) {
            DataRecord[] samples = request.getChildrenOfType(Sample.DATA_TYPE_NAME, user);
            drum.deleteRecords(Arrays.asList(samples), true);
        }

        drum.deleteRecords(Arrays.asList(requests), true);

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

    class BankedWithCorrectedCmoId {
        private final DataRecord bankedDataRecord;
        private final String cmoId;

        BankedWithCorrectedCmoId(DataRecord bankedDataRecord, String cmoId) {
            this.bankedDataRecord = bankedDataRecord;
            this.cmoId = cmoId;
        }
    }
}