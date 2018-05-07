package org.mskcc.limsrest.limsapi;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;



public class PromoteBankedUnitTest {

    @Test
    public void whenNothingSpecified_thenNewRequestAndProj() {
        List<Object> empty = new LinkedList<>();
        PromoteBanked.PromotedDestination destination = PromoteBanked.determinePromoteDestination("NULL", "NULL",
                empty);
        assertThat(destination, is(PromoteBanked.PromotedDestination.NEW_PROJECT_AND_REQ));
    }

    @Test
    public void whenNothingSpecifiedButMaps_thenExistingRequest() {
        List<Object> mapped = new LinkedList<>();
        mapped.add("element");
        PromoteBanked.PromotedDestination destination = PromoteBanked.determinePromoteDestination("NULL",
                "NULL", mapped);
        assertThat(destination, is(PromoteBanked.PromotedDestination.EXISTING_REQ));
    }

    @Test
    public void whenReqSpecified_thenExistingRequest() {
        List<Object> empty = new LinkedList<>();
        PromoteBanked.PromotedDestination destination = PromoteBanked.determinePromoteDestination("NULL",
                "REQ1", empty);
        assertThat(destination, is(PromoteBanked.PromotedDestination.EXISTING_REQ));
    }

    @Test
    public void whenProjectSpecified_thenNewRequestInProject() {
        List<Object> empty = new LinkedList<>();
        PromoteBanked.PromotedDestination destination = PromoteBanked.determinePromoteDestination("Proj1",
                "NULL", empty);
        assertThat(destination, is(PromoteBanked.PromotedDestination.EXISTING_PROJECT));
    }
}