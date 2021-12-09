package org.mskcc.limsrest.service;

import lombok.*;

/**
 * Additional fields necessary to generate a CMO Sample ID per the specification described:<BR>
 * http://plvpipetrack1.mskcc.org:8099/display/IDT/CMO+Patient+ID+and+Sample+ID+generation
 */
@Setter @Getter @ToString
@NoArgsConstructor @AllArgsConstructor
public class CMOSampleIdFields {
    private String naToExtract = "";
    private String normalizedPatientId = "";
    private String sampleType = "";
}