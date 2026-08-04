package com.example;

import com.example.service.TechnicalIndicatorService;
import com.example.service.TechnicalIndicatorServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${yahoo.api.connect-timeout-ms:2500}")
    private int connectTimeoutMs;

    @Value("${yahoo.api.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public TechnicalIndicatorService technicalIndicatorService() {
        return new TechnicalIndicatorServiceImpl();
    }
}
