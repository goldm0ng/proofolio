package com.proofolio.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.proofolio.ai.entity.AiCall;
import com.proofolio.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AnthropicClaudeGateway implements ClaudeGateway {

    private final AnthropicClient client;
    private final String model;
    private final AiCallLogger logger;

    public AnthropicClaudeGateway(@Value("${app.claude.api-key:}") String apiKey,
                                  @Value("${app.claude.model}") String model,
                                  AiCallLogger logger) {
        this.model = model;
        this.logger = logger;
        this.client = (apiKey == null || apiKey.isBlank())
                ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        if (client == null) {
            log.warn("ANTHROPIC_API_KEY is not set; AI endpoints will return 503");
        }
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public <T> Result<T> structured(String task, String systemPrompt, String userContent, Class<T> schema) {
        if (client == null) {
            throw ApiException.ai(HttpStatus.SERVICE_UNAVAILABLE, "ANTHROPIC_API_KEY가 설정되지 않아 AI 기능을 쓸 수 없습니다");
        }
        long started = System.currentTimeMillis();
        try {
            StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(16000L)
                    .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                            .text(systemPrompt)
                            .cacheControl(CacheControlEphemeral.builder().build())
                            .build()))
                    .outputConfig(schema)
                    .addUserMessage(userContent)
                    .build();

            StructuredMessage<T> response = client.messages().create(params);
            long duration = System.currentTimeMillis() - started;

            Optional<T> value = response.content().stream()
                    .flatMap(cb -> cb.text().stream())
                    .map(t -> t.text())
                    .findFirst();

            long in = response.usage().inputTokens();
            long out = response.usage().outputTokens();
            long cacheRead = response.usage().cacheReadInputTokens().orElse(0L);

            if (value.isEmpty()) {
                AiCall call = logger.error(task, model, duration, "empty structured response (stop_reason=" + response.stopReason() + ")");
                throw ApiException.ai(HttpStatus.BAD_GATEWAY, "AI 응답이 비어 있습니다");
            }
            AiCall call = logger.ok(task, model, in, out, cacheRead, duration);
            return new Result<>(value.get(), call);
        } catch (ApiException e) {
            throw e;
        } catch (AnthropicException e) {
            long duration = System.currentTimeMillis() - started;
            log.warn("Claude call failed task={} : {}", task, e.getMessage());
            logger.error(task, model, duration, e.getMessage());
            throw ApiException.ai(HttpStatus.BAD_GATEWAY, "AI 호출에 실패했습니다: " + firstLine(e.getMessage()));
        } catch (RuntimeException e) {
            long duration = System.currentTimeMillis() - started;
            log.error("Unexpected AI failure task={}", task, e);
            logger.error(task, model, duration, e.toString());
            throw ApiException.ai(HttpStatus.BAD_GATEWAY, "AI 처리 중 오류가 발생했습니다: " + firstLine(e.getMessage()));
        }
    }

    private static String firstLine(String s) {
        return s == null ? "" : s.lines().findFirst().orElse(s);
    }
}
