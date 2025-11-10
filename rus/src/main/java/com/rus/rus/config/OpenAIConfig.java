package com.rus.rus.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

@Configuration
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model.name:gpt-4o-mini}")
    private String modelName;

    @Bean
    public OkHttpClient openAiHttpClient() {
        return new OkHttpClient.Builder().build();
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
