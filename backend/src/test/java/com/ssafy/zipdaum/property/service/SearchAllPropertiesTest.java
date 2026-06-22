package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class SearchAllPropertiesTest {

    @InjectMocks
    private PropertyServiceImpl propertyService;

    @Mock
    private PropertyMapper propertyMapper;

    @Test
    @DisplayName("부동산 전체 목록 조회 - 성공")
    void searchAllProperties() {
        // Given (준비)
        // 1. 매퍼가 반환할 가짜 데이터를 만듭니다.
        // 주의: PropertySearchResponse의 실제 생성자나 빌더 패턴에 맞게 수정해 주세요.
        PropertySearchResponse mockProperty1 = new PropertySearchResponse();
        PropertySearchResponse mockProperty2 = new PropertySearchResponse();
        List<PropertySearchResponse> expectedList = List.of(mockProperty1, mockProperty2);

        // 2. 가짜 매퍼에게 "selectAllProperties()가 호출되면 expectedList를 반환해라" 라고 지시합니다.
        given(propertyMapper.selectAllProperties()).willReturn(expectedList);

        // When (실행)
        List<PropertySearchResponse> actualList = propertyService.searchAllProperties();

        // Then (검증)
        // 1. 반환된 리스트가 null이 아니고, 크기가 2인지, 그리고 우리가 준비한 리스트와 일치하는지 확인합니다.
        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(2);
        assertThat(actualList).isEqualTo(expectedList);

        // 2. (중요) 실제 서비스 로직 안에서 mapper.selectAllProperties()가 정확히 1번 호출되었는지 검증합니다.
        verify(propertyMapper, times(1)).selectAllProperties();
    }

    @Test
    @DisplayName("부동산 전체 목록 조회 - 없음")
    void searchAllProperties_Empty() {
        // Given (준비)
        // 1. 매퍼가 반환할 가짜 데이터를 만듭니다.
        // 주의: PropertySearchResponse의 실제 생성자나 빌더 패턴에 맞게 수정해 주세요.

        List<PropertySearchResponse> expectedList = List.of();

        // 2. 가짜 매퍼에게 "selectAllProperties()가 호출되면 expectedList를 반환해라" 라고 지시합니다.
        given(propertyMapper.selectAllProperties()).willReturn(expectedList);

        // When (실행)
        List<PropertySearchResponse> actualList = propertyService.searchAllProperties();

        // Then (검증)
        // 1. 반환된 리스트가 null이 아니고, 크기가 2인지, 그리고 우리가 준비한 리스트와 일치하는지 확인합니다.
        assertThat(actualList).isNotNull();
        assertThat(actualList).hasSize(0);
        assertThat(actualList).isEqualTo(expectedList);

        // 2. (중요) 실제 서비스 로직 안에서 mapper.selectAllProperties()가 정확히 1번 호출되었는지 검증합니다.
        verify(propertyMapper, times(1)).selectAllProperties();
    }



}
