package org.mskcc.limsrest.limsapi.cmoinfo.retriever;

import java.util.List;
import java.util.Map;

public interface CmoSampleIdRetriever {
    String retrieve(Map<String, Object> fields, List<String> patientCmoSampleIds);
}
