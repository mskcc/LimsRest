package org.mskcc.limsrest.limsapi.search;

import java.util.*;

import org.junit.Test;
import org.mskcc.limsrest.limsapi.GetReadyForIllumina;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class NearestAncestorSearcherTest {
    List<Long> ancestorSampleDates = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorBarcodes = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorPlanningProtocols = new ArrayList<>();
    List<List<Map<String, Object>>> ancestorSeqRequirements = new ArrayList<>();

    @Before
    public void setUpLists(){

        ancestorSampleDates.add(1);
        ancestorSampleDates.add(3);
        ancestorSampleDates.add(7);
        ancestorSampleDates.add(11);
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
        protoList1.add(req1);
        Map<String, Object> req2 = new HashMap<>();
        req2.put("R2", "R2");
        List<Map<String, Object>> reqList2 = new LinkedList<>();
        reqList2.add(req2);
        ancestorSeqRequirements.add(0, protoList1);
        ancestorSeqRequirements.add(3, protoList2);

    }


    @Test
    public void whenDatesAreNotSorted_findsRighAncestor() {
        //given an unordered date list
        ancestorSampleDates.add(2, 2);
        Map<String, Map<String, Object>>  matches= NearestAncestorSearcher.findMostRecentAncestorFields(
                ancestorSampleDates, ancestorBarcodes,ancestorPlanningProtocols, ancestorSeqRequirements
        );
        assertThat(matches.get("PlanFields"), is("P2"));

    }
}