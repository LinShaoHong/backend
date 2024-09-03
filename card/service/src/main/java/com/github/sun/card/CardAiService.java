package com.github.sun.card;

import com.github.sun.foundation.ai.Assistant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.inject.Named;
import java.util.List;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardAiService {
    @Value("${qwen.key}")
    private String apiKey;
    @Value("${qwen.model}")
    private String model;
    private final @Named("qwen") Assistant assistant;

    public String chat(List<String> q) {
        return assistant.chat(apiKey, model, q);
    }
}