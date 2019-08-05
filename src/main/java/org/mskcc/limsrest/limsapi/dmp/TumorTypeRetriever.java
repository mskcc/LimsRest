package org.mskcc.limsrest.limsapi.dmp;

import java.util.Set;

public interface TumorTypeRetriever {
    Set<TumorType> retrieve();
}