package com.lyh.base.agent.observation;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * deep wiki
 */
@Configuration
public class LangfuseConfig {  
      
    @Value("${langfuse.secret-key}")
    private String secretKey;  
      
    @Value("${langfuse.public-key}")  
    private String publicKey;  
      
    @Value("${langfuse.base-url}")
    private String baseUrl;  
      
    @Bean
    public RestTemplate langfuseRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();  
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(
            publicKey, secretKey));  
        return restTemplate;  
    }  
}