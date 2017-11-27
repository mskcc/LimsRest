package org.mskcc.limsrest.limsapi.dmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mskcc.domain.sample.SampleClass;
import org.mskcc.domain.sample.SampleOrigin;
import org.mskcc.domain.sample.SpecimenType;

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

    public List<Study> getStudies() {
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
        private static Map<String, String> dmpStringToCMO = new HashMap<>();
        private static Map<String, String> dmpPreservationToCMOOrigin = new HashMap<>();

        static {
            dmpStringToCMO.put("biopsy", SpecimenType.BIOPSY.getValue());
            dmpStringToCMO.put("resection", SpecimenType.RESECTION.getValue());
            dmpStringToCMO.put("Metastatic", SampleClass.METASTASIS.getValue());
            dmpStringToCMO.put(NOT_AVAILABLE, "");

            dmpPreservationToCMOOrigin.put("FFPE", SampleOrigin.BLOCK.getValue());
            dmpPreservationToCMOOrigin.put("Blood", SampleOrigin.WHOLE_BLOOD.getValue());
        }

        @JsonProperty("CMO Sample Request Details")
        private Map<String, Map<String, String>> cmoSampleRequestDetails;
        private List<Study> studies;

        public Content() {
        }

        public static String getStringCMOValueIfExists(String dmpValue) {
            return dmpStringToCMO.getOrDefault(dmpValue, dmpValue);
        }

        public static String getCMOOrigin(String dmpPreservation) {
            return dmpPreservationToCMOOrigin.getOrDefault(dmpPreservation, dmpPreservation);
        }

        public List<Study> getStudies() {
            if (studies == null) {
                studies = new ArrayList<>();

                for (Map.Entry<String, Map<String, String>> studyIdToProperties : cmoSampleRequestDetails.entrySet()) {
                    Study study = getStudyFromProperties(studyIdToProperties);
                    studies.add(study);
                }
            }

            return studies;
        }

        private Study getStudyFromProperties(Map.Entry<String, Map<String, String>> studyIdToProperties) {
            String studyId = studyIdToProperties.getKey();
            Map<String, String> properties = studyIdToProperties.getValue();

            Study study = new Study(studyId);
            study.setBarcodePlateId(properties.get(Fields.BARCODE_PLATE_ID));
            study.setCollectionYear(properties.get(Fields.COLLECTION_YEAR));
            study.setConcentration(getNumericValue(properties, Fields.CONCENTRATION));
            study.setDmpId(properties.get(Fields.DMP_ID));
            study.setDnaInputIntoLibrary(getNumericValue(properties, Fields.DNA_INPUT_INTO_LIBRARY));
            study.setIndex(properties.get(Fields.INDEX));
            study.setIndexSequence(properties.get(Fields.INDEX_SEQUENCE));
            study.setInvestigatorSampleId(properties.get(Fields.INVESTIGATOR_SAMPLE_ID));
            study.setNucleidAcidType(properties.get(Fields.NUCLEID_ACID_TYPE));
            study.setPiName(properties.get(Fields.PI_NAME));
            study.setPreservation(properties.get(Fields.PRESERVATION));
            study.setReceivedDnaMass(getNumericValue(properties, Fields.RECEIVED_DNA_MASS));
            study.setSampleApprovedByCmo(properties.get(Fields.SAMPLE_APPROVED_BY_CMO));
            study.setSampleClass(properties.get(Fields.SAMPLE_CLASS));
            study.setSex(properties.get(Fields.SEX));
            study.setSpecimenType(properties.get(Fields.SPECIMEN_TYPE));
            study.setStudyOfTitle(properties.get(Fields.STUDY_OF_TITLE));
            study.setTrackingId(properties.get(Fields.TRACKING_ID));
            study.setTumorType(properties.get(Fields.TUMOR_TYPE));
            study.setVolume(getNumericValue(properties, Fields.VOLUME));
            study.setWellPosition(properties.get(Fields.WELL_POSITION));

            return study;
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
