package org.mskcc.limsrest.service.dmp;

public class TumorType {
    private String code;
    private String tissue_type;
    private String tumor_type;

    public TumorType() {
    }

    public TumorType(String code, String tissue_type, String tumor_type) {
        this.code = code;
        this.tissue_type = tissue_type;
        this.tumor_type = tumor_type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTissue_type() {
        return tissue_type;
    }

    public void setTissue_type(String tissue_type) {
        this.tissue_type = tissue_type;
    }

    public String getTumor_type() {
        return tumor_type;
    }

    public void setTumor_type(String tumor_type) {
        this.tumor_type = tumor_type;
    }

    @Override
    public String toString() {
        return "TumorType{" +
                "code='" + code + '\'' +
                ", tissue_type='" + tissue_type + '\'' +
                ", tumor_type='" + tumor_type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TumorType tumorType = (TumorType) o;

        if (code != null ? !code.equals(tumorType.code) : tumorType.code != null) return false;
        if (tissue_type != null ? !tissue_type.equals(tumorType.tissue_type) : tumorType.tissue_type != null)
            return false;
        return tumor_type != null ? tumor_type.equals(tumorType.tumor_type) : tumorType.tumor_type == null;
    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (tissue_type != null ? tissue_type.hashCode() : 0);
        result = 31 * result + (tumor_type != null ? tumor_type.hashCode() : 0);
        return result;
    }
}