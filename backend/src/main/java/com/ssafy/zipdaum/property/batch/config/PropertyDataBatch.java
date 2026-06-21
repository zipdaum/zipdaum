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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PropertyDataBatch {

    private final PropertyApiClient propertyApiClient;
    private final PropertyMapper propertyMapper;
    private final GeocodeService geocodeService;

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


            // 부모 객체 (Property) 매핑
            PropertySaveCommand property = new PropertySaveCommand();
            property.setPropertyType(item.apiType().getPropertyType());
            property.setName(item.propertyName());
            property.setSggCd(item.sggCd());
            property.setUmdNm(item.umdNm());
            property.setJibun(item.jibun());
            property.setBuildYear(item.buildYear());

            CoordinateDto coordinate = geocodeService.getCoordinate(property.getUmdNm() + " " + property.getJibun());

            if (coordinate.latitude() != null && coordinate.longitude() != null) {
                property.setLatitude(coordinate.latitude());
                property.setLongitude(coordinate.longitude());
            }

            wrapper.setProperty(property);

            // 자식 객체 (Deal) 매핑 (매매 vs 전월세 분기)
            if (item.apiType().isSale()) {
                SaleDealSaveCommand saleDeal = new SaleDealSaveCommand(
                        null,
                        item.exclusiveArea(),
                        item.landArea(),
                        item.dealAmount(),
                        item.floor(),
                        item.dealDate(),
                        item.buyerGbn(),
                        item.sellerGbn()
                );
                wrapper.setSaleDeal(saleDeal);
            } else {
                RentDealSaveCommand rentDeal = new RentDealSaveCommand(
                        null,
                        item.exclusiveArea(),
                        item.landArea(),
                        item.deposit(),
                        item.monthlyRent(),
                        item.floor(),
                        item.contractTerm(),
                        item.contractType(),
                        item.useRrRight(),
                        item.preDeposit(),
                        item.preMonthlyRent(),
                        item.dealDate()
                );
                wrapper.setRentDeal(rentDeal);
            }

            return wrapper;
        };

    }

    @Bean
    public ItemWriter<PropertyDealWrapper> propertyCustomItemWriter() {
        return chunk -> {
            for (PropertyDealWrapper wrapper : chunk) {
                PropertySaveCommand property = wrapper.getProperty();
                PropertySaveCommand existingProperty = propertyMapper.findProperty(property);

                Long propertyId;
                if (existingProperty != null) {
                    propertyId = existingProperty.getId();
                } else {
                    propertyMapper.insertProperty(property);
                    propertyId = property.getId();
                }

                // 2. 자식(Deal) 데이터 외래키 세팅 및 저장
                if (wrapper.isSale()) {
                    SaleDealSaveCommand origDeal = wrapper.getSaleDeal();
                    SaleDealSaveCommand dealToSave = new SaleDealSaveCommand(
                            propertyId, // 💡 여기서 외래키 삽입!
                            origDeal.exclusiveArea(),
                            origDeal.landArea(),
                            origDeal.dealAmount(),
                            origDeal.floor(),
                            origDeal.dealDate(),
                            origDeal.buyerGbn(),
                            origDeal.sellerGbn()
                    );

                    propertyMapper.insertSaleDeal(dealToSave);
                    propertyMapper.updateLatestSalePrice(propertyId, dealToSave.dealAmount(), dealToSave.dealDate());

                } else {
                    RentDealSaveCommand origDeal = wrapper.getRentDeal();
                    RentDealSaveCommand dealToSave = new RentDealSaveCommand(
                            propertyId, // 💡 여기서 외래키 삽입!
                            origDeal.exclusiveArea(),
                            origDeal.landArea(),
                            origDeal.deposit(),
                            origDeal.monthlyRent(),
                            origDeal.floor(),
                            origDeal.contractTerm(),
                            origDeal.contractType(),
                            origDeal.useRrRight(),
                            origDeal.preDeposit(),
                            origDeal.preMonthlyRent(),
                            origDeal.dealDate()
                    );

                    propertyMapper.insertRentDeal(dealToSave);
                    propertyMapper.updateLatestRentPrice(propertyId, dealToSave.deposit(), dealToSave.monthlyRent(), dealToSave.dealDate());
                }
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
    public Job propertyApiJob(JobRepository jobRepository, Step propertyApiStep) {
        return new JobBuilder("propertyApiJob", jobRepository)
                .start(propertyApiStep)
                .build();
    }
}
