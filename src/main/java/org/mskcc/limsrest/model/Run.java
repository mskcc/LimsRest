package org.mskcc.limsrest.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
public class Run implements Serializable {
    private String runMode;
    private String runId;
    private String flowCellId;
    private String readLength;
    private String runDate;
    private List<Integer> flowCellLanes;
    private List<String> fastqs;

    public Run(){}

    /**
     * Run constructor.
     * @param runId
     * @param flowCellId
     * @param runDate
     */
    public Run(String runId, String flowCellId, String runDate) {
        this.runId = runId;
        this.flowCellId = flowCellId;
        this.runDate = runDate;
    }

    /**
     * Run constructor.
     * @param runMode
     * @param runId
     * @param flowCellId
     * @param readLength
     * @param runDate
     */
    public Run(String runMode, String runId, String flowCellId, String readLength, String runDate) {
        this.runMode = runMode;
        this.runId = runId;
        this.flowCellId = flowCellId;
        this.readLength = readLength;
        this.runDate = runDate;
    }

    /**
     * Run constructor.
     * @param fastqs
     */
    public Run(List<String> fastqs) {
        this.fastqs = fastqs;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getFlowCellId() {
        return flowCellId;
    }

    public void setFlowCellId(String flowCellId) {
        this.flowCellId = flowCellId;
    }

    public String getReadLength() {
        return readLength;
    }

    public void setReadLength(String readLength) {
        this.readLength = readLength;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<Integer> getFlowCellLanes() {
        if (flowCellLanes ==  null) {
            this.flowCellLanes = new ArrayList<>();
        }
        return flowCellLanes;
    }

    public void setFlowCellLanes(List<Integer> flowCellLanes) {
        this.flowCellLanes = flowCellLanes;
    }

    /**
     * Adds lane to flow cell lanes and sorts.
     * @param lane
     */
    public void addLane(Integer lane) {
        if (flowCellLanes == null) {
            this.flowCellLanes = new ArrayList<>();
        }
        flowCellLanes.add(lane);
        Collections.sort(flowCellLanes);
    }

    public List<String> getFastqs() {
        return fastqs;
    }

    public void setFastqs(List<String> fastqs) {
        this.fastqs = fastqs;
    }

    /**
     * Adds FastQ to list.
     * @param fastq
     */
    public void addFastq(String fastq) {
        if (fastqs == null) {
            this.fastqs = new ArrayList<>();
        }
        fastqs.add(fastq);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
