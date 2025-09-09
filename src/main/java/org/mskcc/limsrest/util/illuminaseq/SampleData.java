package org.mskcc.limsrest.util.illuminaseq;

public class SampleData {
    public String lane;
    public String sampleId;
    public String samplePlate;
    public String sampleWell;
    public String i7IndexId;
    public String index;
    public String index2;
    public String sampleProject;
    public String baitSet;

    @Override
    public String toString() {
        return "SampleData{" +
                "lane='" + lane + '\'' +
                ", sampleId='" + sampleId + '\'' +
                ", samplePlate='" + samplePlate + '\'' +
                ", sampleWell='" + sampleWell + '\'' +
                ", i7IndexId='" + i7IndexId + '\'' +
                ", index='" + index + '\'' +
                ", index2='" + index2 + '\'' +
                ", sampleProject='" + sampleProject + '\'' +
                ", baitSet='" + baitSet + '\'' +
                '}';
    }
}