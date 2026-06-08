package com.orderflux.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * PageResponse — Standard wrapper for all paginated API responses.
 *
 * Every paginated endpoint returns this shape:
 * {
 *   "content": [...],        ← actual data items
 *   "pageNumber": 0,         ← current page (0-indexed)
 *   "pageSize": 10,          ← items per page
 *   "totalElements": 100,    ← total items across ALL pages
 *   "totalPages": 10,        ← total number of pages
 *   "isFirst": true,         ← is this the first page?
 *   "isLast": false          ← is this the last page?
 * }
 *
 * Why a generic <T>?
 *   Same wrapper works for Page<ProductResponse>, Page<UserResponse>, etc.
 *   One class — used everywhere.
 *
 * Static factory from(Page<T>):
 *   Converts Spring's Page object to our API response shape.
 *   Hides Spring internals from the API consumer.
 */
@Getter
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

    /**
     * Convert Spring's Page<T> directly to our PageResponse<T>.
     *
     * Usage:
     *   Page<ProductResponse> page = ...;
     *   PageResponse<ProductResponse> response = PageResponse.from(page);
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}