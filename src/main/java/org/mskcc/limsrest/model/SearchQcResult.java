package org.mskcc.limsrest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a single search result from the QC search.
 * Contains summary information about a project that matches the search criteria.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SearchQcResult {
    private String pi;
    private String type;
    private String recipe;
    
    @EqualsAndHashCode.Include
    private String requestId;
    
    private String recentRuns;
    private String dateOfLatestStats;
}