package org.mskcc.limsrest.service;

import com.google.common.collect.ImmutableMap;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
import com.velox.sapioutils.client.standalone.VeloxConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mskcc.domain.Recipe;
import org.mskcc.domain.RequestSpecies;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.CmoSampleInfo;
import org.mskcc.limsrest.ConnectionLIMS;
import org.mskcc.limsrest.service.cmoinfo.CorrectedCmoSampleIdGenerator;

import java.rmi.RemoteException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mskcc.domain.Recipe.*;

public class PromoteBankedTest {

    private PromoteBanked promoteBanked;
    private CorrectedCmoSampleIdGenerator correctedCmoSampleIdGenerator;
    private ConnectionLIMS conn;
    private List<DataRecord> readCoverageRefs;
    private DataRecordManager dataRecordManager;
    private User user;

    @Before
    public void setup() {
        correctedCmoSampleIdGenerator = Mockito.mock(CorrectedCmoSampleIdGenerator.class);
            // Connection needed to query the existing tango workflow manager
        this.conn = new ConnectionLIMS("tango.mskcc.org", 1099, "fe74d8e1-c94b-4002-a04c-eb5c492704ba", "test-runner", "password1");
        dataRecordManager= conn.getConnection().getDataRecordManager();
        user = conn.getConnection().getUser();
        try {
            readCoverageRefs = dataRecordManager.queryDataRecords("ApplicationReadCoverageRef", "ReferenceOnly != 1", user);
        } catch (NotFound | IoError | RemoteException e) {
            System.out.println(String.format("%s -> Error while running PromoteBankedTests: %s", ExceptionUtils.getRootCauseMessage(e), ExceptionUtils.getStackTrace(e)));
        }
        List<String> humanRecipes = Arrays.asList(
                IMPACT_341.getValue(),
                IMPACT_410.getValue(),
                IMPACT_410.getValue(),
                IMPACT_410_PLUS.getValue(),
                IMPACT_468.getValue(),
                HEME_PACT_V_3.getValue(),
                HEME_PACT_V_4.getValue(),
                MSK_ACCESS_V1.getValue()
        );
        promoteBanked = new PromoteBanked();
    }

    @Test
    public void setSeqRequirementsIMPACT() {
        HashMap<String, Object> map = new HashMap<>();
        PromoteBanked.setSeqReq("IMPACT468", "Tumor", map);
        assertEquals("PE100", map.get("SequencingRunType"));
        assertEquals(14.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("M-IMPACT_v1", "Tumor", map);
        assertEquals("PE100", map.get("SequencingRunType"));
        assertEquals(14.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("IMPACT468", "Normal", map);
        assertEquals("PE100", map.get("SequencingRunType"));
        assertEquals(7.0, map.get("RequestedReads"));
        assertEquals(250, map.get("CoverageTarget"));
    }

    @Test
    public void setSeqRequirementsHemePACT() {
        HashMap<String, Object> map = new HashMap<>();
        PromoteBanked.setSeqReq("HemePACT", "Tumor", map);
        assertEquals("PE100", map.get("SequencingRunType"));
        assertEquals(20.0, map.get("RequestedReads"));
        assertEquals(500, map.get("CoverageTarget"));

        PromoteBanked.setSeqReq("HemePACT", "Normal", map);
        assertEquals("PE100", map.get("SequencingRunType"));
        assertEquals(10.0, map.get("RequestedReads"));
        assertEquals(250, map.get("CoverageTarget"));
    }

    @Test
    public void setSeqRequirementsWES_whenRequestReadsIsValid() {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("100X", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(3);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType", "CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 100);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", 60.0);
    }

    @Test
    public void setSeqRequirementWES_whenCoverageTargetIsNotInMap() {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("1000X", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(3);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType", "CoverageTarget", "RequestedReads");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
        Assertions.assertThat(seqRequirementMap).containsEntry("CoverageTarget", 1000);
        Assertions.assertThat(seqRequirementMap).containsEntry("RequestedReads", null);
    }

    @Test
    public void setSeqRequirementWES_whenRequestedReadsIsEmpty() {
        Map<String, Object> seqRequirementMap = new HashMap<>();
        promoteBanked.setSeqReqForWES("", seqRequirementMap);
        Assertions.assertThat(seqRequirementMap).hasSize(1);
        Assertions.assertThat(seqRequirementMap).containsKeys("SequencingRunType");
        Assertions.assertThat(seqRequirementMap).containsEntry("SequencingRunType", "PE100");
    }

    @Test
    public void unaliquotname_whenSampleNameIsNotAliquot() {
        String sampleName = "1234_S_1";
        Assertions.assertThat(promoteBanked.unaliquotName(sampleName)).isEqualTo(sampleName);
    }

    @Test
    public void unaliquotname_whenSampleNameIsAliquot() {
        String sampleName = "1234_S_1_1_1_1";
        Assertions.assertThat(promoteBanked.unaliquotName(sampleName)).isEqualTo("1234_S_1");
    }

    @Test
    public void getCorrectedCmoSampleId_whenSpeciesIsNotHuman() {
        //given
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Mouse")
                .build();
        BankedSample bankedSample = new BankedSample("123", fields);

        //when
        String id = promoteBanked.getCorrectedCmoSampleId(bankedSample, "123_S");

        //then
        Assertions.assertThat(id).isEmpty();
    }

    @Test
    public void whenSpeciesIsHumanOrRecipeIsTreatedAsHuman_shouldGenerateCmoSampleId() throws Exception {
        assertShouldGenerateCmoId("Mouse", IMPACT_341.getValue(), true);
        assertShouldGenerateCmoId("Mouse", HEME_PACT_V_3.getValue(), true);
        assertShouldGenerateCmoId("Human", HEME_PACT_V_3.getValue(), true);
        assertShouldGenerateCmoId("Human", MSK_ACCESS_V1.getValue(), true);
        assertShouldGenerateCmoId("", MSK_ACCESS_V1.getValue(), true);
    }

    @Test
    public void whenRecipeIsNotTreatedAsHumanOrEmpty_shouldNotGenerateCmoSampleId() throws Exception {
        assertShouldGenerateCmoId("Mouse", RNA_SEQ.getValue(), false);
        assertShouldGenerateCmoId(RequestSpecies.BACTERIA.getValue(), Recipe.SMARTER_AMP_SEQ.getValue(), false);
        assertShouldGenerateCmoId("", "", false);
    }

    private void assertShouldGenerateCmoId(String species, String recipe, boolean expected) {
        //given
        Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", species)
                .put("Recipe", recipe)
                .put("UserSampleID", "U2343")
                .put("OtherSampleId", "O34234")
                .put("SpecimenType", "Resection")
                .build();
        BankedSample bankedSample = new BankedSample("123", fields);

        //when
        boolean result = PromoteBanked.shouldGenerateCmoId(bankedSample);

        //then
        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getCmoFields() {
        Map<String, Object> bankedFields = ImmutableMap.<String, Object>builder()
                .put("CMOPatientId", "pid343")
                .put("SampleType", "DNA")
                .put("Species", "Human")
                .put("UserSampleID", "U2343")
                .put("OtherSampleId", "O34234")
                .put(BankedSample.CLINICAL_INFO, "23432")
                .put(BankedSample.SAMPLE_CLASS, "Tumor")
                .put(BankedSample.COLLECTION_YEAR, 1999)
                .put(BankedSample.PATIENT_ID, "2354")
                .put(BankedSample.ESTIMATED_PURITY, "0.9")
                .put(BankedSample.GENDER, "F")
                .put(BankedSample.GENETIC_ALTERATIONS, "DDF")
                .put(BankedSample.NORMALIZED_PATIENT_ID, "23534")
                .put(BankedSample.PRESERVATION, "OK")
                .put(BankedSample.TUMOR_TYPE, "TUO")
                .put(BankedSample.TUMOR_OR_NORMAL, "TU")
                .put(BankedSample.TISSUE_SITE, "TH")
                .put(BankedSample.SPECIMEN_TYPE, "")
                .put(BankedSample.SAMPLE_ORIGIN, "")
                .build();

        Map<String, Object> cmoFields = promoteBanked.getCmoFields(bankedFields, "C-AAAAA1-V001-d", "123_S", "igo-2",
                "UIFDF");
        Assertions.assertThat(cmoFields).containsKeys(
                CmoSampleInfo.ALT_ID, CmoSampleInfo.CLINICAL_INFO, CmoSampleInfo.CMO_PATIENT_ID,
                CmoSampleInfo.CMOSAMPLE_CLASS, CmoSampleInfo.COLLECTION_YEAR, CmoSampleInfo
                        .CORRECTED_INVEST_PATIENT_ID, CmoSampleInfo.DMPLIBRARY_INPUT, CmoSampleInfo.DMPLIBRARY_OUTPUT,
                CmoSampleInfo.ESTIMATED_PURITY, CmoSampleInfo.GENDER, CmoSampleInfo.GENETIC_ALTERATIONS,
                CmoSampleInfo.NORMALIZED_PATIENT_ID, CmoSampleInfo.OTHER_SAMPLE_ID, CmoSampleInfo.PATIENT_ID,
                CmoSampleInfo.PRESERVATION, CmoSampleInfo.REQUEST_ID, CmoSampleInfo.SAMPLE_ID,
                CmoSampleInfo.SAMPLE_ORIGIN, CmoSampleInfo.SPECIES, CmoSampleInfo.SPECIMEN_TYPE,
                CmoSampleInfo.TISSUE_LOCATION, CmoSampleInfo.TUMOR_OR_NORMAL,
                CmoSampleInfo.TUMOR_TYPE, CmoSampleInfo.USER_SAMPLE_ID
        );
    }

    @Test
    public void selectLarger() {
        double result = PromoteBanked.selectLarger("30-40 million");
        assertEquals(40.0, result, 0.001);
    }

    @Test
    public void needReadCoverageReferenceTest() throws RemoteException {
        assertTrue(promoteBanked.needReadCoverageReference("MSK-ACCESS_v1", readCoverageRefs, user));
        assertTrue(promoteBanked.needReadCoverageReference("WholeExomeSequencing", readCoverageRefs, user));
        assertTrue(promoteBanked.needReadCoverageReference("IMPACT505", readCoverageRefs, user));
        assertFalse(promoteBanked.needReadCoverageReference("10X_Genomics_GeneExpression-31", readCoverageRefs, user));
        assertFalse(promoteBanked.needReadCoverageReference("10X_Genomics_GeneExpression-51", readCoverageRefs, user));
    }

    @Test
    public void getRequestedReadsForCoverageTest(){
        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("MSK-ACCESS_v1", "Tumor", "MSK-ACCESS_v1", "PE100", "Human", "1000", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 60.0);
        assertEquals(seqReq.get("CoverageTarget"), "1000");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq0 = promoteBanked.getRequestedReadsForCoverage("MSK-ACCESS_v1", "Normal", "MSK-ACCESS_v1", "PE100", "Human", "1000", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 6.0);
        assertEquals(seqReq.get("CoverageTarget"), "1000");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");
        // Ref values need to be updated in LIMS
        Map<String, Object> seqReq1 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Tumor", "Poirier_RB1_intron_V2", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq1.get("RequestedReads"), 33.0);
        assertEquals(seqReq1.get("CoverageTarget"), "100");
        assertEquals(seqReq1.get("SequencingRunType"), "PE100");

        // Ref values need to be updated in LIMS
        Map<String, Object> seqReq2 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Normal", "Poirier_RB1_intron_V2", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq2.get("RequestedReads"), 33.0);
        assertEquals(seqReq2.get("CoverageTarget"), "100");
        assertEquals(seqReq2.get("SequencingRunType"), "PE100");


        Map<String, Object> seqReq3 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Tumor", "ADCC1_v3", "PE100", "Human", "500", readCoverageRefs, user);
        assertEquals(seqReq3.get("RequestedReads"), 10.0);
        assertEquals(seqReq3.get("CoverageTarget"), "500");
        assertEquals(seqReq3.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq4 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Normal", "ADCC1_v3", "PE100", "Human", "250", readCoverageRefs, user);
        assertEquals(seqReq4.get("RequestedReads"), 5.0);
        assertEquals(seqReq4.get("CoverageTarget"), "250");
        assertEquals(seqReq4.get("SequencingRunType"), "PE100");


        Map<String, Object> seqReq5 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Tumor", "myTYPE_V1", "PE100", "Human", "600", readCoverageRefs, user);
        assertEquals(seqReq5.get("RequestedReads"), 12.0);
        assertEquals(seqReq5.get("CoverageTarget"), "600");
        assertEquals(seqReq5.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq6 = promoteBanked.getRequestedReadsForCoverage("CustomCapture", "Normal", "myTYPE_V1", "PE100", "Human", "600", readCoverageRefs, user);
        assertEquals(seqReq6.get("RequestedReads"), 12.0);
        assertEquals(seqReq6.get("CoverageTarget"), "600");
        assertEquals(seqReq6.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");

        Map<String, Object> seqReq = promoteBanked.getRequestedReadsForCoverage("WholeExomeSequencing", "Tumor", "EXOME_human_IDT_FP_v4", "PE100", "Human", "100", readCoverageRefs, user);
        assertEquals(seqReq.get("RequestedReads"), 33.0);
        assertEquals(seqReq.get("CoverageTarget"), "100");
        assertEquals(seqReq.get("SequencingRunType"), "PE100");
    }

    @After
    public void destroy(){
        conn.close();
    }
}