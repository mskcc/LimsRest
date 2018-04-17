package org.mskcc.limsrest.limsapi.search;

import java.util.*;

import org.junit.Test;
import org.junit.Before;
import org.mskcc.limsrest.limsapi.GetReadyForIllumina;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;

public class NearestAncestorSearcherTest {
    List<Long> ancestorSampleDates = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorBarcodes = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorPlanningProtocols = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorSeqRequirements = new ArrayList<>();

    @Before
    public void setUpLists(){
        List<Map<String, Object>> emptyList = new LinkedList<>();
        ancestorSampleDates.add(1L);
        ancestorSampleDates.add(3L);
        ancestorSampleDates.add(7L);
        ancestorSampleDates.add(11L);
        for(int i = 0; i < ancestorSampleDates.size(); i++){
           ancestorBarcodes.add(emptyList);
           ancestorPlanningProtocols.add(emptyList);
           ancestorSeqRequirements.add(emptyList);
        }
        Map<String, Object> barcode = new HashMap<>();
        barcode.put("BC1", "BC1");
        List<Map<String, Object>> bcList = new LinkedList<>();
        bcList.add(barcode);
        ancestorBarcodes.add(2, bcList);
        Map<String, Object> proto1 = new HashMap<>();
        proto1.put("P1", "P1");
        List<Map<String, Object>> protoList1 = new LinkedList<>();
        protoList1.add(proto1);
        Map<String, Object> proto2 = new HashMap<>();
        proto2.put("P2", "P2");
        List<Map<String, Object>> protoList2 = new LinkedList<>();
        protoList2.add(proto2);
        ancestorPlanningProtocols.add(1, protoList1);
        ancestorPlanningProtocols.add(2, protoList2);
        Map<String, Object> req1 = new HashMap<>();
        req1.put("R1", "R1");
        List<Map<String, Object>> reqList1 = new LinkedList<>();
        reqList1.add(req1);
        Map<String, Object> req2 = new HashMap<>();
        req2.put("R2", "R2");
        List<Map<String, Object>> reqList2 = new LinkedList<>();
        reqList2.add(req2);
        ancestorSeqRequirements.add(0, reqList1);
        ancestorSeqRequirements.add(3, reqList2);

    }


    @Test
    public void whenDatesAreNotSorted_findsRightAncestor() {
        //given an unordered date list
        ancestorSampleDates.set(2, 2L);
        Map<String, Map<String, Object>>  matches= NearestAncestorSearcher.findMostRecentAncestorFields(
                ancestorSampleDates, ancestorBarcodes,ancestorPlanningProtocols, ancestorSeqRequirements
        );
        assertThat(matches.get("PlanFields"), hasKey("P2"));
        ancestorSampleDates.set(2, 7L);
    }

    @Test
    public void findsRightAncestors() {
        //given an unordered date list
        Map<String, Map<String, Object>>  matches= NearestAncestorSearcher.findMostRecentAncestorFields(
                ancestorSampleDates, ancestorBarcodes,ancestorPlanningProtocols, ancestorSeqRequirements
        );
        assertThat(matches.get("BarcodeFields"), hasKey("BC1"));
        assertThat(matches.get("PlanFields"),hasKey("P1"));
        assertThat(matches.get("ReqFields"), hasKey("R1"));

    }

    @Test
    public void whenNoAncestorPlan_givesEmptyMap(){
        List<List<Map<String, Object>>> emptyProtocols = new ArrayList<>();
        List<Map<String, Object>> emptyList = new LinkedList<>();
        for(int i = 0; i < ancestorSampleDates.size(); i++){
           emptyProtocols.add(emptyList);
        }
        Map<String, Map<String, Object>>  matches= NearestAncestorSearcher.findMostRecentAncestorFields(
                    ancestorSampleDates, ancestorBarcodes, emptyProtocols, ancestorSeqRequirements
        );
        assertThat(matches.get("PlanFields").size(), is(0));
    }
}
