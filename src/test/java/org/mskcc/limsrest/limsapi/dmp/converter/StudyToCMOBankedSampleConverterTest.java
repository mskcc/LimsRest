package org.mskcc.limsrest.limsapi.dmp.converter;

import org.junit.Test;
import org.mskcc.domain.sample.BankedSample;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.limsrest.limsapi.dmp.CMOSampleRequestDetailsResponse;
import org.mskcc.limsrest.limsapi.dmp.Study;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class StudyToCMOBankedSampleConverterTest {
    private final Study study = new Study("43543_FD");
    private DMPToBankedSampleConverter dmpToBankedSampleConverter = new StudyToCMOBankedSampleConverter();

    @Test
    public void whenStringFieldsAreNotAvailable_shouldBeLeftBlank() throws Exception {
        //given
        study.setBarcodePlateId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setCollectionYear(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setIndex(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setInvestigatorSampleId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setNucleidAcidType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setDmpId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setPiName(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setPreservation(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setSampleClass(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setSex(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setSpecimenType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setTumorType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        study.setWellPosition(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getBarcodeId(), is(""));
        assertThat(bankedSample.getCollectionYear(), is(""));
        assertThat(bankedSample.getColPosition(), is(""));
        assertThat(bankedSample.getGender(), is(""));
        assertThat(bankedSample.getInvestigator(), is(""));
        assertThat(bankedSample.getNAtoExtract(), is(""));
        assertThat(bankedSample.getOtherSampleId(), is(""));
        assertThat(bankedSample.getPatientId(), is(""));
        assertThat(bankedSample.getPreservation(), is(""));
        assertThat(bankedSample.getRowPosition(), is(""));
        assertThat(bankedSample.getSampleClass(), is(""));
        assertThat(bankedSample.getSpecimenType(), is(""));
        assertThat(bankedSample.getTumorType(), is(""));
        assertThat(bankedSample.getUserSampleID(), is(""));
    }

    @Test
    public void whenNumberFieldsAreNull_shouldBeLeftNull() throws Exception {
        //given

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertNull(bankedSample.getVolume());
        assertNull(bankedSample.getConcentration());
    }

    @Test
    public void whenWellPositionIsFilledIn_shouldFillRowAndColPosition() throws Exception {
        assertWellPosition("A1", "A", "1");
        assertWellPosition("B2", "B", "2");
        assertWellPosition("D14", "D", "14");
        assertWellPosition("H45", "H", "45");
        assertWellPosition("AGH145", "AGH", "145");
    }

    private void assertWellPosition(String wellPosition, String rowPosition, String colPosition) {
        Study study = new Study("123");
        study.setWellPosition(wellPosition);

        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);
        assertThat(bankedSample.getRowPosition(), is(rowPosition));
        assertThat(bankedSample.getColPosition(), is(colPosition));
    }

    @Test
    public void whenPropertiesAreFilled_shouldBeMappedToBankedSample() throws Exception {
        //given
        double concentration = 123.5;
        double volume = 50.0;
        String barcodeOrPlateId = "1232133FDFD";
        String collectionYear = "2017";
        String index = "IDT05";
        String investigatorSampleId = "P-32131-00T-8U";
        String nucleidAcid = "DNA";
        String patientId = "P-1234567";
        String piName = "King Julien";
        String preservation = "FFPE";
        String sampleClass = "Primary";
        String sex = "M";
        String specimenType = "Blood";
        String tumorType = "Germ_Cell_Male:Seminoma";
        String wellPosition = "A12";

        study.setBarcodePlateId(barcodeOrPlateId);
        study.setCollectionYear(collectionYear);
        study.setConcentration(concentration);
        study.setDmpId(patientId + "-something-whatever");
        study.setIndex(index);
        study.setInvestigatorSampleId(investigatorSampleId);
        study.setNucleidAcidType(nucleidAcid);
        study.setPiName(piName);
        study.setPreservation(preservation);
        study.setSampleClass(sampleClass);
        study.setSex(sex);
        study.setSpecimenType(specimenType);
        study.setTumorType(tumorType);
        study.setVolume(volume);
        study.setWellPosition(wellPosition);

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getBarcodeId(), is(index));
        assertThat(bankedSample.getCollectionYear(), is(collectionYear));
        assertThat(bankedSample.getConcentration(), is(concentration));
        assertThat(bankedSample.getConcentrationUnits(), is(CMOSampleRequestDetailsResponse.Content
                .CONCENTRATION_UNITS));
        assertThat(bankedSample.getGender(), is(sex));
        assertThat(bankedSample.getInvestigator(), is(piName));
        assertThat(bankedSample.getNAtoExtract(), is(nucleidAcid));
        assertThat(bankedSample.getOtherSampleId(), is(investigatorSampleId));
        assertThat(bankedSample.getPatientId(), is(patientId));
        assertThat(bankedSample.getPlateId(), is(barcodeOrPlateId));
        assertThat(bankedSample.getPreservation(), is(preservation));
        assertThat(bankedSample.getSampleClass(), is(sampleClass));
        assertThat(bankedSample.getSpecimenType(), is(specimenType));
        assertThat(bankedSample.getTumorType(), is(tumorType));
        assertThat(bankedSample.getUserSampleID(), is(investigatorSampleId));
        assertThat(bankedSample.getVolume(), is(volume));
        assertThat(bankedSample.getRowPosition(), is("A"));
        assertThat(bankedSample.getColPosition(), is("12"));
    }

    @Test
    public void whenSpecimenTypeIsBiopsy_shouldCapitalizeThisValue() throws Exception {
        //given
        study.setSpecimenType("biopsy");

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getSpecimenType(), is(SpecimenType.BIOPSY.getValue()));
    }

    @Test
    public void whenSpecimenTypeIsResection_shouldCapitalizeThisValue() throws Exception {
        //given
        study.setSpecimenType("resection");

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getSpecimenType(), is(SpecimenType.RESECTION.getValue()));
    }

    @Test
    public void whenSampleClassIsMetastatic_shouldChangeItToMetastasis() throws Exception {
        //given
        study.setSampleClass("Metastatic");

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getSampleClass(), is(SampleClass.METASTASIS.getValue()));
    }

    @Test
    public void whenPreservationIsFFPE_shouldSetSampleOriginToBlock() throws Exception {
        //given
        study.setPreservation("FFPE");

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getSampleOrigin(), is(SampleOrigin.BLOCK.getValue()));
    }

    @Test
    public void whenPreservationIsBlood_shouldSetSampleOriginToWholeBlood() throws Exception {
        //given
        study.setPreservation("Blood");

        //when
        BankedSample bankedSample = dmpToBankedSampleConverter.convert(study);

        //then
        assertThat(bankedSample.getSampleOrigin(), is(SampleOrigin.WHOLE_BLOOD.getValue()));
    }

}