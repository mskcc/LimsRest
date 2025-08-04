
package org.mskcc.limsrest.service;

import org.mskcc.limsrest.model.SearchQcResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for QC search results.
 * Includes search results, pagination metadata, and error handling.
 */
public class SearchQcResponse {
    private List<SearchQcResult> results;
    private int total;
    private int limit;
    private int offset;
    private String search;
    private boolean hasMore;
    private String error;

    public SearchQcResponse() {}

    public SearchQcResponse(List<SearchQcResult> results, int total, int limit, int offset, String search, boolean hasMore) {
        this.results = results;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.search = search;
        this.hasMore = hasMore;
        this.error = null;
    }

    public SearchQcResponse(String error, String search, int limit, int offset) {
        this.results = new ArrayList<>();
        this.total = 0;
        this.limit = limit;
        this.offset = offset;
        this.search = search;
        this.hasMore = false;
        this.error = error;
    }

    public static SearchQcResponse error(String errorMessage, String search, int limit, int offset) {
        return new SearchQcResponse(errorMessage, search, limit, offset);
    }

    public static SearchQcResponse success(List<SearchQcResult> results, int total, int limit, int offset, String search, boolean hasMore) {
        return new SearchQcResponse(results, total, limit, offset, search, hasMore);
    }

    public List<SearchQcResult> getResults() {
        return results;
    }

    public void setResults(List<SearchQcResult> results) {
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

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

