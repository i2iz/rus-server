package com.rus.rus.config;

import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model.name:gpt-4o-mini}")
    private String modelName;

    @Bean
    public OkHttpClient openAiHttpClient() {
        return new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(50))
                .build();
    }

    @Bean
    public OpenAIProps openAIProps() {
        return new OpenAIProps(apiKey, modelName);
    }

    @Getter
    public static class OpenAIProps {
        private final String apiKey;
        private final String modelName;

        public OpenAIProps(String apiKey, String modelName) {
            this.apiKey = apiKey;
            this.modelName = modelName;
        }
    }
}
