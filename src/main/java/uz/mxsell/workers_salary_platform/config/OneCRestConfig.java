package uz.mxsell.workers_salary_platform.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OneCRestConfig {

    @Bean
    public RestTemplate oneCRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(5000))
                .readTimeout(Duration.ofMillis(10000))
                .build();
    }
}
