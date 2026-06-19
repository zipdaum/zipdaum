package com.ssafy.zipdaum.property.batch.controller;

import com.ssafy.zipdaum.property.batch.scheduler.PropertyDataBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchTriggerController {

    private final PropertyDataBatchScheduler propertyDataBatchScheduler;

    @GetMapping("/property")
    public ResponseEntity<String> triggerPropertyBatch() {
        log.info(">>>> 관리자(Admin) 요청으로 부동산 실거래가 수동 배치를 시작합니다.");

        propertyDataBatchScheduler.runMonthlyPropertyDataJob();

        return ResponseEntity.ok("부동산 실거래가 3개월 치 수동 배치 작업이 실행 요청되었습니다.");
    }

}
