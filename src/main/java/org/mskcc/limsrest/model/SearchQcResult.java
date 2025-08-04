package org.mskcc.limsrest.model;

import java.util.Objects;

/**
 * Represents a single search result from the QC search.
 * Contains summary information about a project that matches the search criteria.
 */
public class SearchQcResult {
    private String pi;
    private String type;
    private String recipe;
    private String requestId;
    private String recentRuns;
    private String dateOfLatestStats;

    public SearchQcResult() {}

    public SearchQcResult(String pi, String type, String recipe, String requestId, String recentRuns, String dateOfLatestStats) {
        this.pi = pi;
        this.type = type;
        this.recipe = recipe;
        this.requestId = requestId;
        this.recentRuns = recentRuns;
        this.dateOfLatestStats = dateOfLatestStats;
    }

    public String getPi() {
        return pi;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRecentRuns() {
        return recentRuns;
    }

    public void setRecentRuns(String recentRuns) {
        this.recentRuns = recentRuns;
    }

    public String getDateOfLatestStats() {
        return dateOfLatestStats;
    }

    public void setDateOfLatestStats(String dateOfLatestStats) {
        this.dateOfLatestStats = dateOfLatestStats;
    }

    @Override
    public String toString() {
        return "SearchQcResult{" +
                "pi='" + pi + '\'' +
                ", type='" + type + '\'' +
                ", recipe='" + recipe + '\'' +
                ", requestId='" + requestId + '\'' +
                ", recentRuns='" + recentRuns + '\'' +
                ", dateOfLatestStats='" + dateOfLatestStats + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchQcResult that = (SearchQcResult) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }
}

