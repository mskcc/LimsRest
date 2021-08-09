package org.mskcc.limsrest.service.dmp;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@ToString @EqualsAndHashCode
public class TumorType {
    private String code;
    private String tissue_type;
    private String tumor_type;
}