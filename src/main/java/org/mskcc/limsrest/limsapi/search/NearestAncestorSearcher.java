package org.mskcc.limsrest.limsapi.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NearestAncestorSearcher {
    public static  Map<String, Map<String, Object>> findMostRecentAncestorFields(List<Long> ancestorSampleDates,
                                                                             List<List<Map<String, Object>>> ancestorBarcodes,
                                                                             List<List<Map<String, Object>>> ancestorPlanningProtocols,
                                                                             List<List<Map<String, Object>>> ancestorSeqRequirements
    ){
        long mostRecentAncestorWithBarcode = Long.MAX_VALUE;
        long mostRecentAncestorWithPlan = Long.MAX_VALUE;
        long mostRecentAncestorWithReq = Long.MAX_VALUE;
        Map<String, Map<String, Object>> resultMaps = new HashMap<>();
        Map<String, Object> emptyMap = Collections.emptyMap();
        resultMaps.put("BarcodeFields", emptyMap);
        resultMaps.put("PlanFields", emptyMap);
        resultMaps.put("ReqFields", emptyMap);
        for(int ancestorIndex = 0; ancestorIndex < ancestorSampleDates.size(); ancestorIndex++){
            long ancestorCreateDate =  Long.MAX_VALUE;
            try {
                ancestorCreateDate = ancestorSampleDates.get(ancestorIndex);
            } catch (Exception e) {}
            if(ancestorBarcodes.get(ancestorIndex).size() > 0 && ancestorCreateDate < mostRecentAncestorWithBarcode){
                resultMaps.put("BarcodeFields", ancestorBarcodes.get(ancestorIndex).get(0));
                mostRecentAncestorWithBarcode = ancestorCreateDate;
            }

            if(ancestorPlanningProtocols.get(ancestorIndex).size() > 0 && ancestorCreateDate < mostRecentAncestorWithPlan){
                resultMaps.put("PlanFields", ancestorPlanningProtocols.get(ancestorIndex).get(0));
                mostRecentAncestorWithPlan = ancestorCreateDate;
            }

            if(ancestorSeqRequirements.get(ancestorIndex).size() > 0 && ancestorCreateDate < mostRecentAncestorWithReq){
                resultMaps.put("ReqFields", ancestorSeqRequirements.get(ancestorIndex).get(0));
                mostRecentAncestorWithReq = ancestorCreateDate;
            }
        }
        return resultMaps;

    }
}
