package org.mskcc.limsrest.limsapi.dmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TumorType {
    private String code;

    private String tissue;

    private String name;

    public TumorType() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TumorType{" +
                "code='" + code + '\'' +
                ", tissue='" + tissue + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}