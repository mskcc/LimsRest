package org.mskcc.limsrest.controller;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class IgoNewRequestMetaDbPublisherTest {

    @Test
    static void formatDeliverPipelineJSON() {
        Date now = new Date();
        IgoNewRequestMetaDbPublisher.formatDeliverPipelineJSON("12345","Curie","WGS", now);
    }
}