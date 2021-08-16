package org.mskcc.limsrest.service;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
@Getter @Setter @ToString
public class ArchivedFastq {
    public String run;
    public String runBaseDirectory;
    public String project;
    public String sample; // from Jan. 2016 format example: A123456_V3_IGO_07973_8 ie sampleId_IGO_igoId
    public String fastq;
    public Date fastqLastModified;
    public Long bytes;
    public Date lastUpdated;

    public ArchivedFastq() {}

    public String getRunId() {
        return run.substring(0, run.lastIndexOf("_"));
    }

    /**
     * Extract flowcell from run - JAX_0454_AHHWKVBBXY, TOMS_5394_000000000-J2FY2, JOHNSAWYERS_0239_000000000-G5TVF
     * @return
     */
    public String getFlowCellId() {
        if (run == null || run.length() < 10)
            return "";

        if (run.contains("_000000000")) {
            return run.substring(run.length() - 5,run.length());
        } else {
            int lastUnderscore = run.lastIndexOf('_');
            return run.substring(lastUnderscore + 2,run.length());
        }
    }
}
