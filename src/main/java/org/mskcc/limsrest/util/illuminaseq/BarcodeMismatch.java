package org.mskcc.limsrest.util.illuminaseq;

import java.util.*;

public class BarcodeMismatch {
    public static Integer[] determineBarcodeMismatches(List<Set<Barcode>> barcodes, boolean isDualBarcode) {
        List<Integer[]> mismatchesToTry = new ArrayList<Integer[]>();
        
        if (isDualBarcode) {
            Integer[] one = {1, 1};
            Integer[] two = {1, 0};
            Integer[] three = {0, 1};
            Integer[] four = {0, 0};
            mismatchesToTry.add(one);
            mismatchesToTry.add(two);
            mismatchesToTry.add(three);
            mismatchesToTry.add(four);
        } else {
            Integer[] one = {1, 1};
            Integer[] four = {0, 0};
            mismatchesToTry.add(one);
            mismatchesToTry.add(four);
        }
        

        for (Integer[] mismatch : mismatchesToTry) { // try the mismatch list from least stringent to 0 mismatches
            System.out.println("Trying:" + Arrays.toString(mismatch));
            boolean success = true;
            if (mismatch[0] == 0 && mismatch[1] == 0) // skip test if already at 0,0
                return mismatch;

            for (Set<Barcode> barcodeSet : barcodes) { // for each set of barcodes on a lane
                Set<Barcode> copy = new HashSet<>(barcodeSet);
                for (Barcode tag : barcodeSet) { // compare every barcode against every other barcode
                    for (Barcode tag2 : copy) {
                        if (tag.equals(tag2))
                            continue;
                        if (tag.indexA.contains("-") || tag2.indexA.contains("-")) // ignore fake 10X barcodes like "SI-GA-A10"
                            continue;
                        if (!validateBarcodes(tag, tag2, mismatch[0], mismatch[1])) {
                            System.out.println("Barcode conflict detected: " + tag + " and " + tag2 + " given mismatches - " + Arrays.toString(mismatch));
                            success = false;
                            break; // could use break with label to break out of both loops
                        }
                    }
                }
            }
            if (success)
                return mismatch;
        }

        return new Integer[]{0, 0};
    }

    // returns true if barcodes are compatible given the possible mismatches
    public static boolean validateBarcodes(Barcode barcode1, Barcode barcode2, int mismatchesPartA, int mismatchesPartB) {
        int numComponentsWithCollision = 0;

        if (2 * mismatchesPartA >= getNumMismatches(barcode1.indexA, barcode2.indexA)) {
            numComponentsWithCollision++;
            System.out.println("Collision:" + barcode1.indexA + " " + barcode2.indexA);
        }

        if (barcode1.size == 2) {
            if (2 * mismatchesPartB >= getNumMismatches(barcode1.indexB, barcode2.indexB)) {
                numComponentsWithCollision++;
                System.out.println("Collision:" + barcode1.indexB + " " + barcode2.indexB);
            }
        }

        if (numComponentsWithCollision == barcode1.size) {
            System.out.println("NumCollisions:" + numComponentsWithCollision);
            return false;
        }

        return true;
    }

    // compare dual barcode strings such as ATCACGATGCAAG to ATCACGATCCACGCGT
    public static int getNumMismatches(String str1, String str2) {
        int i = 0, count = 0;
        while (i < str1.length() && i < str2.length()) {
            if (str1.charAt(i) != str2.charAt(i))
                count++;
            i++;
        }
        return count;
    }
}