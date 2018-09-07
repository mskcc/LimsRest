package org.mskcc.limsrest.limsapi.cmoinfo.cspace;

import org.junit.Test;
import org.mskcc.domain.Recipe;
import org.mskcc.domain.sample.*;
import org.mskcc.util.Constants;
import org.mskcc.util.TestUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mskcc.domain.sample.SampleOrigin.TISSUE;
import static org.mskcc.domain.sample.SpecimenType.CFDNA;

public class CspaceSampleAbbreviationRetrieverTest {
    private CspaceSampleTypeAbbreviationRetriever cspaceSampleAbbreviationRetriever = new
            CspaceSampleTypeAbbreviationRetriever();

    @Test
    public void whenSampleTypeIsPooledLibrary_shouldResolveNucleicAcidByRecipe() throws Exception {
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.RNA_SEQ, Constants.RNA_ABBREV);

        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.WHOLE_EXOME_SEQUENCING, Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.SMARTER_AMP_SEQ, Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.RNA_SEQ_POLY_A, Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.RNA_SEQ_RIBO_DEPLETE, Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.TEN_X_Genomics_RNA, Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.POOLED_LIBRARY, Recipe.SH_RNA_SEQ, Constants.DNA_ABBREV);
    }

    private void assertNucleicAcidAbbrev(SampleType sampleType, Recipe recipe, String nucleicAcidAbbrev) {
        assertNucleicAcidAbbrev(sampleType, recipe, Optional.empty(), nucleicAcidAbbrev);
    }


    private void assertNucleicAcidAbbrev(SampleType sampleType, Recipe recipe, Optional<NucleicAcid> nucleicAcid,
                                         String nucleicAcidAbbrev) {
        //given
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("id");
        correctedCmoSampleView.setSampleType(sampleType);
        correctedCmoSampleView.setRecipe(recipe);

        if (nucleicAcid.isPresent())
            correctedCmoSampleView.setNucleidAcid(nucleicAcid.get());

        //when
        String nucleicAcidAbbr = cspaceSampleAbbreviationRetriever.getNucleicAcidAbbr(correctedCmoSampleView);

        //then
        assertThat(nucleicAcidAbbr, is(nucleicAcidAbbrev));
    }

    @Test
    public void whenSampleTypeIsNotPooledLibraryAndRecipeIsRNASeq_shouldResolveNucleicAcidByNaToExtract() throws
            Exception {
        assertNucleicAcidAbbrev(SampleType.BLOCKS_SLIDES, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants
                .DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.DNA_LIBRARY, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants
                .DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.TISSUE, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.DNA, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.CFDNA, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.CELLS, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants.DNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.BLOOD, Recipe.RNA_SEQ, Optional.of(NucleicAcid.DNA), Constants.DNA_ABBREV);

        assertNucleicAcidAbbrev(SampleType.BLOOD, Recipe.RNA_SEQ, Optional.of(NucleicAcid.RNA), Constants.RNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.BUFFY_COAT, Recipe.RNA_SEQ, Optional.of(NucleicAcid.RNA), Constants
                .RNA_ABBREV);
        assertNucleicAcidAbbrev(SampleType.BLOCKS_SLIDES, Recipe.RNA_SEQ, Optional.of(NucleicAcid.RNA), Constants
                .RNA_ABBREV);
    }

    @Test
    public void whenPropertiesAreCorrectlySet_shouldReturnSampleTypeAbbrev() throws Exception {
        assertSampleTypeAbbrev(SpecimenType.XENOGRAFT, "X");
        assertSampleTypeAbbrev(SpecimenType.XENOGRAFTDERIVEDCELLLINE, "X");
        assertSampleTypeAbbrev(SpecimenType.PDX, "X");
        assertSampleTypeAbbrev(SpecimenType.PDX, SampleOrigin.TISSUE, "X");
        assertSampleTypeAbbrev(SpecimenType.PDX, SampleOrigin.CEREBROSPINAL_FLUID, SampleClass.METASTASIS, "X");

        assertSampleTypeAbbrev(SpecimenType.ORGANOID, "G");
        assertSampleTypeAbbrev(SpecimenType.ORGANOID, SampleOrigin.WHOLE_BLOOD, "G");
        assertSampleTypeAbbrev(SpecimenType.ORGANOID, SampleOrigin.PLASMA, "G");
        assertSampleTypeAbbrev(SpecimenType.ORGANOID, SampleOrigin.WHOLE_BLOOD, SampleClass.LOCAL_RECURRENCE, "G");
        assertSampleTypeAbbrev(SpecimenType.ORGANOID, SampleOrigin.WHOLE_BLOOD, SampleClass.ADJACENT_TISSUE, "G");

        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.URINE, "U");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.URINE, SampleClass.CELL_FREE, "U");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.CEREBROSPINAL_FLUID, "S");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.CEREBROSPINAL_FLUID, SampleClass.PRIMARY, "S");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.PLASMA, "L");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.PLASMA, SampleClass.LOCAL_RECURRENCE, "L");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.WHOLE_BLOOD, "L");
        assertSampleTypeAbbrev(SpecimenType.CFDNA, SampleOrigin.WHOLE_BLOOD, SampleClass.ADJACENT_TISSUE, "L");

        assertSampleTypeAbbrev(SpecimenType.RAPIDAUTOPSY, SampleOrigin.WHOLE_BLOOD, SampleClass.UNKNOWN_TUMOR, "T");
        assertSampleTypeAbbrev(SpecimenType.BLOOD, SampleOrigin.WHOLE_BLOOD, SampleClass.LOCAL_RECURRENCE, "R");
        assertSampleTypeAbbrev(SpecimenType.SALIVA, SampleOrigin.WHOLE_BLOOD, SampleClass.PRIMARY, "P");
        assertSampleTypeAbbrev(SpecimenType.RESECTION, SampleOrigin.WHOLE_BLOOD, SampleClass.RECURRENCE, "R");
        assertSampleTypeAbbrev(SpecimenType.FINGERNAILS, SampleOrigin.WHOLE_BLOOD, SampleClass.METASTASIS, "M");
        assertSampleTypeAbbrev(SpecimenType.BLOOD, SampleOrigin.WHOLE_BLOOD, SampleClass.NORMAL, "N");
        assertSampleTypeAbbrev(SpecimenType.BIOPSY, SampleOrigin.WHOLE_BLOOD, SampleClass.ADJACENT_NORMAL, "N");
        assertSampleTypeAbbrev(SpecimenType.BLOOD, SampleOrigin.WHOLE_BLOOD, SampleClass.ADJACENT_TISSUE, "T");
    }

    @Test
    public void whenSampleisCellFree_shouldThrowAnException() throws Exception {
        Optional<Exception> exception = TestUtils.assertThrown(() -> assertSampleTypeAbbrev(SpecimenType.RAPIDAUTOPSY,
                SampleOrigin.URINE, SampleClass.CELL_FREE, "U"));

        assertThat(exception.isPresent(), is(true));
    }

    private void assertSampleTypeAbbrev(SpecimenType specimenType, SampleOrigin sampleOrigin, SampleClass sampleClass,
                                        String expected) {

        assertSampleTypeAbbrev(specimenType, Optional.of(sampleOrigin), Optional.of(sampleClass), expected);
    }

    private <T> void assertSampleTypeAbbrev(SpecimenType specimenType, SampleOrigin sampleOrigin, String expected) {
        assertSampleTypeAbbrev(specimenType, Optional.of(sampleOrigin), Optional.empty(), expected);
    }

    private void assertSampleTypeAbbrev(SpecimenType specimenType, String expected) {
        assertSampleTypeAbbrev(specimenType, Optional.empty(), Optional.empty(), expected);
    }

    private void assertSampleTypeAbbrev(SpecimenType specimenType, Optional<SampleOrigin> sampleOrigin,
                                        Optional<SampleClass> sampleClass, String abbrev) {
        CorrectedCmoSampleView correctedCmoSampleView = new CorrectedCmoSampleView("s");

        sampleClass.ifPresent((sampleClass1) -> correctedCmoSampleView.setSampleClass(sampleClass1));
        sampleOrigin.ifPresent((sampleOrigin1) -> correctedCmoSampleView.setSampleOrigin(sampleOrigin1));
        correctedCmoSampleView.setSpecimenType(specimenType);

        String typeAbbrev = cspaceSampleAbbreviationRetriever.getSampleTypeAbbr(correctedCmoSampleView);

        assertThat(typeAbbrev, is(abbrev));
    }

    @Test
    public void whenSpeciemnIsCfdnaAndOriginIsTissueAndClassIsCellFree_shouldThrowAnException() throws Exception {
        CorrectedCmoSampleView view = new CorrectedCmoSampleView("sampleId");
        String patientId = "12345";
        view.setPatientId(patientId);
        view.setSpecimenType(CFDNA);
        view.setSampleOrigin(TISSUE);
        view.setSampleClass(SampleClass.CELL_FREE);
        view.setNucleidAcid(NucleicAcid.DNA);

        Optional<Exception> exception = TestUtils.assertThrown(() -> cspaceSampleAbbreviationRetriever
                .getSampleTypeAbbr(view));

        assertThat(exception.isPresent(), is(true));
    }
}