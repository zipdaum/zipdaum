package com.ssafy.zipdaum.property.controller;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.RealEstateDealItem;
import com.ssafy.zipdaum.property.service.RealEstateDealFetchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/real-estate-deals")
@RequiredArgsConstructor
public class RealEstateDealController {

  private final RealEstateDealFetchService fetchService;

  @GetMapping
  public ResponseEntity<List<RealEstateDealItem>> getRealEstateDeals(
      @RequestParam DealApiType type,
      @RequestParam String lawdCd,
      @RequestParam String dealYmd
  ) {
    return ResponseEntity.ok(fetchService.fetchDeals(type, lawdCd, dealYmd));
  }
}
