package com.ssafy.zipdaum.global.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,       // 실제 데이터 리스트
        int currentPage,       // 현재 페이지
        int size,              // 페이지당 데이터 개수
        long totalElements,    // 전체 데이터 개수
        int totalPages         // 전체 페이지 수
) {
    public static <T> PageResponse<T> of(List<T> content, int currentPage, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, currentPage, size, totalElements, totalPages);
    }
}
