package org.mskcc.limsrest.service.dmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mskcc.domain.sample.NucleicAcid;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;
import org.mskcc.util.Constants;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CMOSampleRequestDetailsResponse {
    private String trackingId;
    private String result;
    private Content content;

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public Map<String, Map<String, String>> getCmoSampleRequestDetails() {
        return content.getCmoSampleRequestDetails();
    }

    public List<DMPSample> getStudies() {
        return content.getStudies();
    }

    @Override
    public String toString() {
        return "CMOSampleRequestDetails{" +
                "trackingId='" + trackingId + '\'' +
                ", result='" + result + '\'' +
                ", content=" + content +
                '}';
    }

    public static class Content {
        public static final String NOT_AVAILABLE = "N/A";
        public static final String CONCENTRATION_UNITS = "ng/ul";
        private static Map<String, String> dmpSampleClassToCMO = new HashMap<>();
        private static Map<String, String> dmpSpecimenTypeToCMO = new HashMap<>();
        private static Map<String, String> dmpNucleidAcidToCMO = new HashMap<>();
        private static Map<String, String> dmpNucleidAcidToCMOSampleType = new HashMap<>();
        private static Map<String, String> dmpToCMO = new HashMap<>();
        private static Map<String, String> dmpPreservationToCMOOrigin = new HashMap<>();

        static {
            dmpToCMO.put(NOT_AVAILABLE, "");

            dmpSpecimenTypeToCMO.put("biopsy", SpecimenType.BIOPSY.getValue());
            dmpSpecimenTypeToCMO.put("resection", SpecimenType.RESECTION.getValue());
            dmpSampleClassToCMO.put("Metastatic", SampleClass.METASTASIS.getValue());

            /**
             * DMP Always returns DNA Nucleid Acid
             */
            dmpNucleidAcidToCMO.put("DNA", NucleicAcid.DNA.getValue());
            dmpNucleidAcidToCMO.put("Library", NucleicAcid.DNA.getValue());

            dmpNucleidAcidToCMOSampleType.put("DNA", NucleicAcid.DNA.getValue());
            dmpNucleidAcidToCMOSampleType.put("Library", Constants.DNA_LIBRARY);

            dmpPreservationToCMOOrigin.put("FFPE", SampleOrigin.BLOCK.getValue());
            dmpPreservationToCMOOrigin.put("Blood", SampleOrigin.WHOLE_BLOOD.getValue());
        }

        @JsonProperty("CMO Sample Request Details")
        private Map<String, Map<String, String>> cmoSampleRequestDetails;
        private List<DMPSample> studies;

        public Content() {
        }

        public static String getCMOValueIfExists(String dmpValue) {
            return dmpToCMO.getOrDefault(dmpValue, dmpValue);
        }

        public static String getCMOSpecimenType(String specimenType) {
            return getCMOValueIfExists(dmpSpecimenTypeToCMO.getOrDefault(specimenType, specimenType));
        }

        public static String getCMOSampleClass(String sampleClass) {
            return getCMOValueIfExists(dmpSampleClassToCMO.getOrDefault(sampleClass, sampleClass));
        }

        public static String getNucleidAcidToExtract(String nucleidAcid) {
            return getCMOValueIfExists(dmpNucleidAcidToCMO.getOrDefault(nucleidAcid, nucleidAcid));
        }

        public static String getCMOOrigin(String dmpPreservation) {
            return getCMOValueIfExists(dmpPreservationToCMOOrigin.getOrDefault(dmpPreservation, dmpPreservation));
        }

        public static String getSampleType(String nucleidAcidType) {
            return null;
        }

        public List<DMPSample> getStudies() {
            if (studies == null) {
                studies = new ArrayList<>();

                for (Map.Entry<String, Map<String, String>> studyIdToProperties : cmoSampleRequestDetails.entrySet()) {
                    DMPSample DMPSample = getStudyFromProperties(studyIdToProperties);
                    studies.add(DMPSample);
                }
            }

            return studies;
        }

        private DMPSample getStudyFromProperties(Map.Entry<String, Map<String, String>> studyIdToProperties) {
            String studyId = studyIdToProperties.getKey();
            Map<String, String> properties = studyIdToProperties.getValue();

            DMPSample DMPSample = new DMPSample(studyId);
            DMPSample.setBarcodePlateId(properties.get(Fields.BARCODE_PLATE_ID));
            DMPSample.setCollectionYear(properties.get(Fields.COLLECTION_YEAR));
            DMPSample.setConcentration(getNumericValue(properties, Fields.CONCENTRATION));
            DMPSample.setDmpId(properties.get(Fields.DMP_ID));
            DMPSample.setDnaInputIntoLibrary(getNumericValue(properties, Fields.DNA_INPUT_INTO_LIBRARY));
            DMPSample.setIndex(properties.get(Fields.INDEX));
            DMPSample.setIndexSequence(properties.get(Fields.INDEX_SEQUENCE));
            DMPSample.setInvestigatorSampleId(properties.get(Fields.INVESTIGATOR_SAMPLE_ID));
            DMPSample.setNucleidAcidType(properties.get(Fields.NUCLEID_ACID_TYPE));
            DMPSample.setPiName(properties.get(Fields.PI_NAME));
            DMPSample.setPreservation(properties.get(Fields.PRESERVATION));
            DMPSample.setReceivedDnaMass(getNumericValue(properties, Fields.RECEIVED_DNA_MASS));
            DMPSample.setSampleApprovedByCmo(properties.get(Fields.SAMPLE_APPROVED_BY_CMO));
            DMPSample.setSampleClass(properties.get(Fields.SAMPLE_CLASS));
            DMPSample.setSex(properties.get(Fields.SEX));
            DMPSample.setSpecimenType(properties.get(Fields.SPECIMEN_TYPE));
            DMPSample.setStudyOfTitle(properties.get(Fields.STUDY_OF_TITLE));
            DMPSample.setTrackingId(properties.get(Fields.TRACKING_ID));
            DMPSample.setTumorType(properties.get(Fields.TUMOR_TYPE));
            DMPSample.setVolume(getNumericValue(properties, Fields.VOLUME));
            DMPSample.setWellPosition(properties.get(Fields.WELL_POSITION));

            return DMPSample;
        }

        private Double getNumericValue(Map<String, String> properties, String key) {
            if (!properties.containsKey(key) || Objects.equals(properties.get(key), ""))
                return Double.valueOf(0);
            return Double.valueOf(properties.get(key));
        }

        public Map<String, Map<String, String>> getCmoSampleRequestDetails() {
            return cmoSampleRequestDetails;
        }

        public static class Fields {
            public static final String BARCODE_PLATE_ID = "Barcode/Plate ID";
            public static final String COLLECTION_YEAR = "Collection Year";
            public static final String CONCENTRATION = "Concentration (ng/ul)";
            public static final String DMP_ID = "DMP ID";
            public static final String DNA_INPUT_INTO_LIBRARY = "DNA Input Into Library";
            public static final String INDEX = "Index";
            public static final String INDEX_SEQUENCE = "Index Sequence";
            public static final String INVESTIGATOR_SAMPLE_ID = "Investigator Sample ID";
            public static final String NUCLEID_ACID_TYPE = "Nucleic Acid Type (Library or DNA)";
            public static final String PI_NAME = "PI Name";
            public static final String PRESERVATION = "Preservation (FFPE or Blood)";
            public static final String RECEIVED_DNA_MASS = "Received DNA Mass (ng)";
            public static final String SAMPLE_APPROVED_BY_CMO = "Sample Approved By CMO";
            public static final String SAMPLE_CLASS = "Sample Class (Primary, Met or Normal)";
            public static final String SEX = "Sex";
            public static final String SPECIMEN_TYPE = "Specimen Type (Resection, Biopsy or Blood)";
            public static final String STUDY_OF_TITLE = "Study of Title";
            public static final String TRACKING_ID = "Tracking ID";
            public static final String TUMOR_TYPE = "Tumor Type";
            public static final String VOLUME = "Volume (ul)";
            public static final String WELL_POSITION = "Well Position";
        }
    }
}
