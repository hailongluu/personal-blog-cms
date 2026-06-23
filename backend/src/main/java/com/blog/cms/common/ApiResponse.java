package com.blog.cms.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic API response wrapper matching SPEC format: {data, error, meta}
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private String error;

    @Builder.Default
    private MetaDto meta = new MetaDto();

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MetaDto {
        private int page;
        private int pageSize;
        private long totalItems;
        private int totalPages;
    }

    // --- Factory helpers ---

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().data(data).build();
    }

    public static <T> ApiResponse<List<T>> paged(List<T> data, int page, int pageSize, long totalItems) {
        MetaDto meta = MetaDto.builder()
            .page(page)
            .pageSize(pageSize)
            .totalItems(totalItems)
            .totalPages((int) Math.ceil((double) totalItems / pageSize))
            .build();
        return ApiResponse.<List<T>>builder().data(data).meta(meta).build();
    }

    public static <T> ApiResponse<T> error(String errorMessage) {
        return ApiResponse.<T>builder().error(errorMessage).build();
    }
}
