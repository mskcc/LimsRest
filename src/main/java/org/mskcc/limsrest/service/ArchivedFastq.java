package org.mskcc.limsrest.service;

import java.util.Date;

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

    @Override
    public String toString() {
        return "ArchivedFastq{" +
                "run='" + run + '\'' +
                ", runBaseDirectory='" + runBaseDirectory + '\'' +
                ", project='" + project + '\'' +
                ", sample='" + sample + '\'' +
                ", fastq='" + fastq + '\'' +
                ", fastqLastModified=" + fastqLastModified +
                ", bytes=" + bytes +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getRunBaseDirectory() {
        return runBaseDirectory;
    }

    public void setRunBaseDirectory(String runBaseDirectory) {
        this.runBaseDirectory = runBaseDirectory;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getFastq() {
        return fastq;
    }

    public void setFastq(String fastq) {
        this.fastq = fastq;
    }

    public Date getFastqLastModified() {
        return fastqLastModified;
    }

    public void setFastqLastModified(Date fastqLastModified) {
        this.fastqLastModified = fastqLastModified;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
