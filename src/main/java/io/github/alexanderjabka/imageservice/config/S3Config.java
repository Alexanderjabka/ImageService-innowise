package io.github.alexanderjabka.imageservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    @Bean
    public String bucketName() {
        return bucketName;
    }
    
    @Bean
    public String s3Endpoint() {
        return endpoint;
    }
}
