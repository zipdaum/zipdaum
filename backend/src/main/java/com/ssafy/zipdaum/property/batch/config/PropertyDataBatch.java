package com.ssafy.zipdaum.property.batch.config;

import com.ssafy.zipdaum.property.api.KakaoGeocodeApiClient;
import com.ssafy.zipdaum.property.api.PropertyApiClient;
import com.ssafy.zipdaum.property.batch.wrapper.PropertyDealWrapper;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.*;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import com.ssafy.zipdaum.property.service.GeocodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PropertyDataBatch {

    private final PropertyApiClient propertyApiClient;
    private final PropertyMapper propertyMapper;
    private final GeocodeService geocodeService;

    private final Map<String, CoordinateDto> geoCache = new ConcurrentHashMap<>();

    @Bean
    @StepScope
    public ItemReader<PropertyItem> propertyApiItemReader(
            @Value("#{jobParameters['apiType']}") String apiTypeStr,
            @Value("#{jobParameters['lawdCd']}") String lawdCd,
            @Value("#{jobParameters['dealYmd']}") String dealYmd) {

        return new AbstractPagingItemReader<PropertyItem>() {
            {
                setPageSize(1000); // API의 numOfRows와 동일하게 설정
            }

            @Override
            protected void doReadPage() {
                if (results == null) {
                    results = new CopyOnWriteArrayList<>();
                } else {
                    results.clear();
                }

                int page = getPage() + 1; // 스프링 배치는 0부터 시작, API는 1부터 시작
                DealApiType apiType = DealApiType.valueOf(apiTypeStr);

                log.info(">>>> [API 조회] 지역: {}, 타입: {}, 계약월: {}, 페이지: {}", lawdCd, apiTypeStr, dealYmd, page);
                List<PropertyItem> response = propertyApiClient.fetch(apiType, lawdCd, dealYmd, page);

                if (response != null && !response.isEmpty()) {
                    results.addAll(response);
                }
            }
        };
    }


    @Bean
    @StepScope
    public ItemProcessor<PropertyItem, PropertyDealWrapper> propertyItemProcessor() {
        return item -> {
            PropertyDealWrapper wrapper = new PropertyDealWrapper();

            PropertySaveCommand property = new PropertySaveCommand();
            property.setPropertyType(item.apiType().getPropertyType());
            property.setName(item.propertyName());
            property.setSggCd(item.sggCd());
            property.setUmdNm(item.umdNm());
            property.setJibun(item.jibun());
            property.setBuildYear(item.buildYear());

            String addressKey = property.getUmdNm() + " " + property.getJibun();
            CoordinateDto coordinate = geoCache.computeIfAbsent(addressKey, k -> geocodeService.getCoordinate(k));

            if (coordinate != null && coordinate.latitude() != null && coordinate.longitude() != null) {
                property.setLatitude(coordinate.latitude());
                property.setLongitude(coordinate.longitude());
            }

            wrapper.setProperty(property);

            if (item.apiType().isSale()) {
                wrapper.setSaleDeal(new SaleDealSaveCommand(
                        null, item.exclusiveArea(), item.landArea(), item.dealAmount(),
                        item.floor(), item.dealDate(), item.buyerGbn(), item.sellerGbn()
                ));
            } else {
                wrapper.setRentDeal(new RentDealSaveCommand(
                        null, item.exclusiveArea(), item.landArea(), item.deposit(),
                        item.monthlyRent(), item.floor(), item.contractTerm(),
                        item.contractType(), item.useRrRight(), item.preDeposit(),
                        item.preMonthlyRent(), item.dealDate()
                ));
            }

            return wrapper;
        };

    }

    @Bean
    public ItemWriter<PropertyDealWrapper> propertyCustomItemWriter() {
        return chunk -> {
            // 💡 2. Chunk 전체 데이터를 담을 바구니 (Bulk Insert용)
            List<SaleDealSaveCommand> saleDealsToSave = new ArrayList<>();
            List<RentDealSaveCommand> rentDealsToSave = new ArrayList<>();

            // Chunk 내 중복 Property DB 조회를 막기 위한 메모리 캐시
            Map<String, Long> propertyIdCache = new HashMap<>();

            for (PropertyDealWrapper wrapper : chunk) {
                PropertySaveCommand property = wrapper.getProperty();

                String propertyKey = property.getUmdNm() + "|" + property.getJibun() + "|" + property.getName();
                Long propertyId = propertyIdCache.get(propertyKey);

                // DB에 있는지 확인 후 ID 확보
                if (propertyId == null) {
                    PropertySaveCommand existingProperty = propertyMapper.findProperty(property);
                    if (existingProperty != null) {
                        propertyId = existingProperty.getId();
                    } else {
                        propertyMapper.insertProperty(property);
                        propertyId = property.getId();
                    }
                    propertyIdCache.put(propertyKey, propertyId);
                }

                // Deal 데이터는 List에 담기만 함 (쿼리 실행 안 함)
                if (wrapper.isSale()) {
                    SaleDealSaveCommand origDeal = wrapper.getSaleDeal();
                    saleDealsToSave.add(new SaleDealSaveCommand(
                            propertyId, origDeal.exclusiveArea(), origDeal.landArea(),
                            origDeal.dealAmount(), origDeal.floor(), origDeal.dealDate(),
                            origDeal.buyerGbn(), origDeal.sellerGbn()
                    ));
                } else {
                    RentDealSaveCommand origDeal = wrapper.getRentDeal();
                    rentDealsToSave.add(new RentDealSaveCommand(
                            propertyId, origDeal.exclusiveArea(), origDeal.landArea(),
                            origDeal.deposit(), origDeal.monthlyRent(), origDeal.floor(),
                            origDeal.contractTerm(), origDeal.contractType(),
                            origDeal.useRrRight(), origDeal.preDeposit(),
                            origDeal.preMonthlyRent(), origDeal.dealDate()
                    ));
                }
            } // end for

            // 💡 2. for문이 끝나면 모아둔 리스트를 단 한 번의 벌크 쿼리로 DB에 전송!
            if (!saleDealsToSave.isEmpty()) {
                propertyMapper.bulkInsertSaleDeals(saleDealsToSave);
                log.info(">> 매매 거래 {}건 벌크 Insert 완료", saleDealsToSave.size());
            }
            if (!rentDealsToSave.isEmpty()) {
                propertyMapper.bulkInsertRentDeals(rentDealsToSave);
                log.info(">> 전월세 거래 {}건 벌크 Insert 완료", rentDealsToSave.size());
            }
        };
    }

    @Bean
    public Step propertyApiStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<PropertyItem> propertyApiItemReader,
            ItemProcessor<PropertyItem, PropertyDealWrapper> propertyItemProcessor,
            ItemWriter<PropertyDealWrapper> propertyCustomItemWriter) {

        return new StepBuilder("propertyApiStep", jobRepository)
                .<PropertyItem, PropertyDealWrapper>chunk(1000, transactionManager)
                .reader(propertyApiItemReader)
                .processor(propertyItemProcessor)
                .writer(propertyCustomItemWriter)
                .build();
    }

    @Bean
    public Step syncPriceStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            log.info(">>>> [가격 동기화 시작] DB 전체 최신 실거래가 일괄 갱신 중...");
            propertyMapper.syncAllLatestSalePrices();
            propertyMapper.syncAllLatestRentPrices();
            log.info("<<<< [가격 동기화 완료]");
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("syncPriceStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }


    @Bean
    public Job propertyApiJob(JobRepository jobRepository, Step propertyApiStep, Step syncPriceStep) {
        return new JobBuilder("propertyApiJob", jobRepository)
                .start(propertyApiStep)
                .next(syncPriceStep)
                .build();
    }
}
