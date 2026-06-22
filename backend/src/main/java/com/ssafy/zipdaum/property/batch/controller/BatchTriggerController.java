package com.ssafy.zipdaum.property.batch.controller;

import com.ssafy.zipdaum.property.batch.scheduler.PropertyDataBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/property/manual")
    public ResponseEntity<String> triggerPropertyBatchByRange(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {

        if (!startMonth.matches("\\d{6}") || !endMonth.matches("\\d{6}")) {
            return ResponseEntity.badRequest().body("올바른 연월 형식(YYYYMM)이 아닙니다.");
        }

        log.info(">>>> 관리자 요청: {} ~ {} 기간 부동산 실거래가 수동 배치 시작", startMonth, endMonth);

        long startTime = System.currentTimeMillis();

        propertyDataBatchScheduler.runManualPropertyDataJobByRange(startMonth, endMonth);

        long endTime = System.currentTimeMillis();
        log.info("스케줄러 비동기 호출 소요 시간: {} ms", (endTime - startTime));

        return ResponseEntity.ok(startMonth + " ~ " + endMonth + " 기간의 데이터 수집이 백그라운드에서 시작되었습니다.");
    }

}
