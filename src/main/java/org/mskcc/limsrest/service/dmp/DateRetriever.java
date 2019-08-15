package org.mskcc.limsrest.service.dmp;

import java.time.LocalDate;

public interface DateRetriever {
    LocalDate retrieve(String dateString);
}
