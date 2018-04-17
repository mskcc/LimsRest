package org.mskcc.limsrest.limsapi.dmp.converter;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.domain.sample.*;
import org.mskcc.limsrest.limsapi.converter.ExternalToBankedSampleConverter;
import org.mskcc.limsrest.limsapi.dmp.CMOSampleRequestDetailsResponse;
import org.mskcc.limsrest.limsapi.dmp.DMPSample;
import org.mskcc.util.Constants;
import org.mskcc.util.tumortype.TumorTypeRetriever;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DMPSampleToCMOBankedSampleConverterTest {
    private static final String TUMOR_TYPE = "Some tumor type";
    private static final String TUMOR_CODE = "CODE";
    private static final String TISSUE_TYPE = "Tissue";
    private final DMPSample dmpSample = new DMPSample("43543_FD");
    private TumorTypeRetriever tumorTypeRetriever = mock(TumorTypeRetriever.class);
    private ExternalToBankedSampleConverter externalToBankedSampleConverter;
    private long transactionId = 1234565436;

    @Before
    public void setUp() throws Exception {
        when(tumorTypeRetriever.retrieve()).thenReturn(getTumorTypes());
        externalToBankedSampleConverter = new DMPSampleToCMOBankedSampleConverter(tumorTypeRetriever);
    }

    @Test
    public void whenStringFieldsAreNotAvailable_shouldBeLeftBlank() throws Exception {
        //given
        dmpSample.setBarcodePlateId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setCollectionYear(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setIndex(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setInvestigatorSampleId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setNucleidAcidType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setDmpId(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setPiName(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setPreservation(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setSampleClass(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setSex(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setSpecimenType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setTumorType(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);
        dmpSample.setWellPosition(CMOSampleRequestDetailsResponse.Content.NOT_AVAILABLE);

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getBarcodeId(), is(""));
        assertThat(bankedSample.getCollectionYear(), is(""));
        assertNull(bankedSample.getColPosition());
        assertThat(bankedSample.getGender(), is(""));
        assertThat(bankedSample.getInvestigator(), is(""));
        assertThat(bankedSample.getNAtoExtract(), is(""));
        assertThat(bankedSample.getOtherSampleId(), is(""));
        assertThat(bankedSample.getPatientId(), is(""));
        assertThat(bankedSample.getPreservation(), is(""));
        assertNull(bankedSample.getRowPosition());
        assertThat(bankedSample.getSampleClass(), is(""));
        assertThat(bankedSample.getSpecimenType(), is(""));
        assertThat(bankedSample.getTumorOrNormal(), is(""));
        assertThat(bankedSample.getTumorType(), is(""));
        assertThat(bankedSample.getUserSampleID(), is(""));
    }

    @Test
    public void whenNumberFieldsAreNull_shouldBeLeftNull() throws Exception {
        //given

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertNull(bankedSample.getVolume());
        assertNull(bankedSample.getConcentration());
    }

    @Test
    public void whenWellPositionIsFilledIn_shouldFillRowAndColPositionAndRowIndex() throws Exception {
        assertWellPosition("A1", "A", "1", 100 * 1 + 0);
        assertWellPosition("B2", "B", "2", 100 * 2 + 1);
        assertWellPosition("D14", "D", "14", 100 * 14 + 3);
        assertWellPosition("H45", "H", "45", 100 * 45 + 7);
        assertWellPosition("Z145", "Z", "145", 100 * 145 + 25);
        assertWellPosition("a1", "a", "1", 100 * 1 + 0);
        assertWellPosition("g56", "g", "56", 100 * 56 + 6);
    }

    @Test
    public void whenSampleClassIsNormal_shouldSetTumorOrNormalsToNormal() throws Exception {
        assertTumorOrNormal(SampleClass.NORMAL, Constants.NORMAL);
    }

    @Test
    public void whenSampleClassIsNotNormal_shouldSetTumorOrNormalsToTumor() throws Exception {
        assertTumorOrNormal(SampleClass.PRIMARY, Constants.TUMOR);
        assertTumorOrNormal(SampleClass.METASTASIS, Constants.TUMOR);
        assertTumorOrNormal(SampleClass.CELL_FREE, Constants.TUMOR);
        assertTumorOrNormal(SampleClass.RECURRENCE, Constants.TUMOR);
        assertTumorOrNormal(SampleClass.LOCAL_RECURRENCE, Constants.TUMOR);
        assertTumorOrNormal(SampleClass.ADJACENT_TISSUE, Constants.TUMOR);
    }

    @Test
    public void whenIndexContainsPadding0_shouldRemovePadding0() throws Exception {
        assertBarcodeHasNoPadding0("IDT0", "IDT0");
        assertBarcodeHasNoPadding0("IDT01", "IDT1");
        assertBarcodeHasNoPadding0("IDT03", "IDT3");
        assertBarcodeHasNoPadding0("IDT10", "IDT10");
        assertBarcodeHasNoPadding0("IDT44", "IDT44");
        assertBarcodeHasNoPadding0("IDT036", "IDT36");
        assertBarcodeHasNoPadding0("IDT006", "IDT6");
        assertBarcodeHasNoPadding0("IDT004", "IDT4");
        assertBarcodeHasNoPadding0("IDT836", "IDT836");
        assertBarcodeHasNoPadding0("IDT999", "IDT999");
    }

    private void assertBarcodeHasNoPadding0(String index, String expectedBarcode) {
        DMPSample DMPSample = new DMPSample("123");
        DMPSample.setIndex(index);

        BankedSample bankedSample = externalToBankedSampleConverter.convert(DMPSample, transactionId);

        assertThat(bankedSample.getBarcodeId(), is(expectedBarcode));
    }

    private void assertTumorOrNormal(SampleClass sampleClass, String expected) {
        DMPSample DMPSample = new DMPSample("123");
        DMPSample.setSampleClass(sampleClass.getValue());

        BankedSample bankedSample = externalToBankedSampleConverter.convert(DMPSample, transactionId);

        assertThat(bankedSample.getTumorOrNormal(), is(expected));
    }

    private void assertWellPosition(String wellPosition, String rowPosition, String colPosition, int rowIndex) {
        DMPSample DMPSample = new DMPSample("123");
        DMPSample.setWellPosition(wellPosition);

        BankedSample bankedSample = externalToBankedSampleConverter.convert(DMPSample, transactionId);
        assertThat(bankedSample.getRowPosition(), is(rowPosition));
        assertThat(bankedSample.getColPosition(), is(colPosition));
        assertThat(bankedSample.getRowIndex(), is(rowIndex));
    }

    @Test
    public void whenPropertiesAreFilled_shouldBeMappedToBankedSample() throws Exception {
        //given
        double concentration = 123.5;
        double volume = 50.0;
        String barcodeOrPlateId = "1232133FDFD";
        String collectionYear = "2017";
        double dnaInputIntoLib = 5.7;
        String index = "IDT05";
        String investigatorSampleId = "P-32131-00T-8U";
        String nucleidAcid = "DNA";
        String patientId = "P-1234567";
        String piName = "King Julien";
        String preservation = "FFPE";
        double receivedDnaMass = 109.34;
        String sampleClass = "Primary";
        String sex = "M";
        String specimenType = "Blood";
        String trackingId = "6435gfdgd";
        String wellPosition = "A12";

        dmpSample.setBarcodePlateId(barcodeOrPlateId);
        dmpSample.setCollectionYear(collectionYear);
        dmpSample.setConcentration(concentration);
        dmpSample.setDmpId(patientId + "-something-whatever");
        dmpSample.setDnaInputIntoLibrary(dnaInputIntoLib);
        dmpSample.setIndex(index);
        dmpSample.setInvestigatorSampleId(investigatorSampleId);
        dmpSample.setNucleidAcidType(nucleidAcid);
        dmpSample.setPiName(piName);
        dmpSample.setPreservation(preservation);
        dmpSample.setReceivedDnaMass(receivedDnaMass);
        dmpSample.setSampleClass(sampleClass);
        dmpSample.setSex(sex);
        dmpSample.setSpecimenType(specimenType);
        dmpSample.setTumorType(String.format("%s:%s", TISSUE_TYPE, TUMOR_TYPE));
        dmpSample.setTrackingId(trackingId);
        dmpSample.setVolume(volume);
        dmpSample.setWellPosition(wellPosition);

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getBarcodeId(), is("IDT5"));
        assertThat(bankedSample.getCollectionYear(), is(collectionYear));
        assertThat(bankedSample.getColPosition(), is("12"));
        assertThat(bankedSample.getConcentration(), is(concentration));
        assertThat(bankedSample.getConcentrationUnits(), is(CMOSampleRequestDetailsResponse.Content
                .CONCENTRATION_UNITS));
        assertThat(bankedSample.getDMPTrackingId(), is(trackingId));
        assertThat(bankedSample.getGender(), is(sex));
        assertThat(bankedSample.getInvestigator(), is(piName));
        assertThat(bankedSample.getNAtoExtract(), is(nucleidAcid));
        assertThat(bankedSample.getNonLimsLibraryInput(), is(dnaInputIntoLib));
        assertThat(bankedSample.getNonLimsLibraryOutput(), is(receivedDnaMass));
        assertThat(bankedSample.getOtherSampleId(), is(investigatorSampleId));
        assertThat(bankedSample.getPatientId(), is(patientId));
        assertThat(bankedSample.getPlateId(), is(barcodeOrPlateId));
        assertThat(bankedSample.getPreservation(), is(preservation));
        assertThat(bankedSample.getRowIndex(), is(1200));
        assertThat(bankedSample.getRowPosition(), is("A"));
        assertThat(bankedSample.getSampleClass(), is(sampleClass));
        assertThat(bankedSample.getServiceId(), is(trackingId));
        assertThat(bankedSample.getSpecimenType(), is(specimenType));
        assertThat(bankedSample.getTumorOrNormal(), is(Constants.TUMOR));
        assertThat(bankedSample.getTumorType(), is(TUMOR_CODE));
        assertThat(bankedSample.getUserSampleID(), is(investigatorSampleId));
        assertThat(bankedSample.getVolume(), is(volume));
    }

    @Test
    public void whenSpecimenTypeIsBiopsy_shouldCapitalizeThisValue() throws Exception {
        //given
        dmpSample.setSpecimenType("biopsy");

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getSpecimenType(), is(SpecimenType.BIOPSY.getValue()));
    }

    @Test
    public void whenSpecimenTypeIsResection_shouldCapitalizeThisValue() throws Exception {
        //given
        dmpSample.setSpecimenType("resection");

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getSpecimenType(), is(SpecimenType.RESECTION.getValue()));
    }

    @Test
    public void whenSampleClassIsMetastatic_shouldChangeItToMetastasis() throws Exception {
        //given
        dmpSample.setSampleClass("Metastatic");

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getSampleClass(), is(SampleClass.METASTASIS.getValue()));
    }

    @Test
    public void whenPreservationIsFFPE_shouldSetSampleOriginToBlock() throws Exception {
        //given
        dmpSample.setPreservation("FFPE");

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getSampleOrigin(), is(SampleOrigin.BLOCK.getValue()));
    }

    @Test
    public void whenPreservationIsBlood_shouldSetSampleOriginToWholeBlood() throws Exception {
        //given
        dmpSample.setPreservation("Blood");

        //when
        BankedSample bankedSample = externalToBankedSampleConverter.convert(dmpSample, transactionId);

        //then
        assertThat(bankedSample.getSampleOrigin(), is(SampleOrigin.WHOLE_BLOOD.getValue()));
    }

    private List<TumorType> getTumorTypes() {
        List<TumorType> tumorTypes = new ArrayList<>();

        TumorType tumorType = new TumorType();
        tumorType.setCode(TUMOR_CODE);
        tumorType.setTissueType(TISSUE_TYPE);
        tumorType.setTumorType(TUMOR_TYPE);
        tumorTypes.add(tumorType);

        return tumorTypes;
    }

}