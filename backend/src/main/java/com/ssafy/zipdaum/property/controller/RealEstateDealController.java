package com.ssafy.zipdaum.property.controller;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.RealEstateDealSaveResult;
import com.ssafy.zipdaum.property.service.RealEstateDealFetchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "실거래가", description = "공공데이터 실거래가 조회 및 저장 API")
public class RealEstateDealController {

  private final RealEstateDealFetchService fetchService;

  @PostMapping
  @Operation(
      summary = "실거래가 저장",
      description = "공공데이터 실거래가 API에서 거래 데이터를 조회하고, 카카오 API로 좌표를 가져와 DB에 저장합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "실거래가 저장 성공",
          content = @Content(schema = @Schema(implementation = RealEstateDealSaveResult.class))
      ),
      @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content),
      @ApiResponse(responseCode = "404", description = "주소 좌표 정보 없음", content = @Content),
      @ApiResponse(responseCode = "500", description = "외부 API 키 누락 또는 외부 API 연동 오류", content = @Content)
  })
  public ResponseEntity<RealEstateDealSaveResult> saveRealEstateDeals(
      @Parameter(description = "실거래가 API 유형", example = "APARTMENT_SALE", required = true)
      @RequestParam DealApiType type,
      @Parameter(description = "법정동 코드 앞 5자리", example = "26350", required = true)
      @RequestParam String lawdCd,
      @Parameter(description = "계약년월 6자리", example = "202501", required = true)
      @RequestParam String dealYmd
  ) {
    return ResponseEntity.ok(fetchService.fetchAndSaveDeals(type, lawdCd, dealYmd));
  }
}
