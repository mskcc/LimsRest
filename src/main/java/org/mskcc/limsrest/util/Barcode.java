package org.mskcc.limsrest.util;

import java.util.*;

/**
 Allow for single and dual index barcodes
 */
public class Barcode {
    public String indexA;
    public String indexB = "";
    public int size;

    public Barcode(String indexA) {
        this.indexA = indexA;
        this.size = 1;
    }

    public Barcode(String indexA, String indexB) {
        this.indexA = indexA;
        this.indexB = indexB;
        this.size = 2;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Barcode{" +
                "indexA='" + indexA + '\'' +
                ", indexB='" + indexB + '\'' +
                '}';
    }
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Barcode barcode = (Barcode) object;
        return indexA.equals(barcode.indexA) && indexB.equals(barcode.indexB);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), indexA, indexB);
    }
}