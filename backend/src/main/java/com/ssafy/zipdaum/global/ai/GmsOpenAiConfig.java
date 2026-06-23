package com.ssafy.zipdaum.global.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GmsOpenAiProperties.class)
public class GmsOpenAiConfig {
}
