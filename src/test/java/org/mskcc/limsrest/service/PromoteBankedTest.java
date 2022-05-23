package org.mskcc.limsrest.service;

import com.google.common.collect.ImmutableMap;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.DataRecordManager;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.user.User;
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

import java.rmi.RemoteException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mskcc.domain.Recipe.*;

public class PromoteBankedTest {

    private PromoteBanked promoteBanked;
    private ConnectionLIMS conn;
    private List<DataRecord> readCoverageRefs;
    private DataRecordManager dataRecordManager;
    private User user;

    @Before
    public void setup() {
        this.conn = new ConnectionLIMS("igo-lims04.mskcc.org", 1088, "fe74d8e1-c94b-4002-a04c-eb5c492704ba", "test-runner", "password1");
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

        Map<String, Object> cmoFields = promoteBanked.getCmoFields(bankedFields, "123_S", "igo-2", "UIFDF");
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

    @After
    public void destroy(){
        conn.close();
    }
}