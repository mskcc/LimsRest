package org.mskcc.limsrest.controller;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class IgoNewRequestMetaDbPublisherTest {

    @Test
    void formatDeliverPipelineJSON() {
        Date now = new Date();
        String result = IgoNewRequestMetaDbPublisher.formatDeliverPipelineJSON("12345","Curie","WGS", now);
        System.out.println(result);
    }
}