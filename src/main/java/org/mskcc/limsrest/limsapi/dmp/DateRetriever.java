package org.mskcc.limsrest.limsapi.dmp;

import java.time.LocalDate;

public interface DateRetriever {
    LocalDate retrieve(String dateString);
}
