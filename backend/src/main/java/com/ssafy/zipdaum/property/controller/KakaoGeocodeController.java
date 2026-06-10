package com.ssafy.zipdaum.property.controller;

import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.service.KakaoGeocodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties/coordinates")
public class KakaoGeocodeController {

  private final KakaoGeocodeService kakaoGeocodeService;

  @GetMapping
  public ResponseEntity<CoordinateDto> getCoordinate(@RequestParam String address) {
    return ResponseEntity.ok(kakaoGeocodeService.getCoordinate(address));
  }
}
