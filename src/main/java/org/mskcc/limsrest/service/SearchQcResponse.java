package org.mskcc.limsrest.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mskcc.limsrest.model.SearchQcResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for QC search results.
 * Includes search results, pagination metadata, and error handling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchQcResponse {
    private List<SearchQcResult> results;
    private int total;
    private int limit;
    private int offset;
    private String search;
    private boolean hasMore;
    private String error;

    /**
     * Constructor for successful search responses.
     *
     * @param results the search results
     * @param total total number of results found
     * @param limit the requested limit
     * @param offset the requested offset
     * @param search the original search term
     * @param hasMore whether there are more results available
     */
    public SearchQcResponse(List<SearchQcResult> results, int total, int limit, int offset, String search, boolean hasMore) {
        this.results = results;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.search = search;
        this.hasMore = hasMore;
        this.error = null;
    }

    /**
     * Constructor for error responses.
     *
     * @param error the error message
     * @param search the original search term
     * @param limit the requested limit
     * @param offset the requested offset
     */
    public SearchQcResponse(String error, String search, int limit, int offset) {
        this.results = new ArrayList<>();
        this.total = 0;
        this.limit = limit;
        this.offset = offset;
        this.search = search;
        this.hasMore = false;
        this.error = error;
    }

    /**
     * Factory method to create an error response.
     *
     * @param errorMessage the error message to include
     * @param search the original search term
     * @param limit the requested limit
     * @param offset the requested offset
     * @return a SearchQcResponse representing an error state
     */
    public static SearchQcResponse error(String errorMessage, String search, int limit, int offset) {
        return new SearchQcResponse(errorMessage, search, limit, offset);
    }

    /**
     * Factory method to create a successful response.
     *
     * @param results the search results
     * @param total total number of results found
     * @param limit the requested limit
     * @param offset the requested offset
     * @param search the original search term
     * @param hasMore whether there are more results available
     * @return a SearchQcResponse representing a successful search
     */
    public static SearchQcResponse success(List<SearchQcResult> results, int total, int limit, int offset, String search, boolean hasMore) {
        return new SearchQcResponse(results, total, limit, offset, search, hasMore);
    }

    /**
     * Checks if the response represents a successful search operation.
     *
     * @return true if no error occurred, false otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }

    @Override
    public String toString() {
        return "SearchQcResponse{" +
                "results=" + (results != null ? results.size() : 0) + " items" +
                ", total=" + total +
                ", limit=" + limit +
                ", offset=" + offset +
                ", search='" + search + '\'' +
                ", hasMore=" + hasMore +
                ", error='" + error + '\'' +
                '}';
    }
}