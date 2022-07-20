package org.mskcc.limsrest.service.ilabs;

import java.text.Normalizer;

public class Filter{
    public static String toAscii(String highUnicode){
        String lettersAdded = highUnicode.replaceAll("ß", "ss").replaceAll("æ", "ae").replaceAll("Æ", "Ae");
        return Normalizer.normalize(lettersAdded, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}
