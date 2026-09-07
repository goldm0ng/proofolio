package com.proofolio;

import com.proofolio.ai.dto.JobPostingExtraction;
import com.proofolio.ai.entity.AiCall;
import com.proofolio.ai.service.ClaudeGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiExtractionTest extends IntegrationTestBase {

    @Test
    void extractFromText_usesGatewayAndReturnsExtraction() throws Exception {
        JobPostingExtraction fake = new JobPostingExtraction("모의회사", "백엔드", "신입", "서울", "2026-10-01",
                List.of("Java"), List.of(), List.of("서류", "면접"),
                List.of(new JobPostingExtraction.RequiredDocument("이력서", com.proofolio.application.entity.RequirementKind.RESUME)),
                List.of(new JobPostingExtraction.EssayQuestionItem("지원 동기", 800)), "요약", null);
        AiCall call = new AiCall(UUID.fromString("00000000-0000-0000-0000-000000000001"), "extract-posting",
                "claude-opus-5", AiCall.Status.OK, 10, 5, 0, BigDecimal.ZERO, 1, null);
        when(claudeGateway.isConfigured()).thenReturn(true);
        when(claudeGateway.structured(eq("extract-posting"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                eq(JobPostingExtraction.class)))
                .thenReturn(new ClaudeGateway.Result<>(fake, call));

        mvc.perform(jsonPost("/api/v1/ai/extract-posting", Map.of("text", "[모의회사] 백엔드 신입 채용 ... 마감 10/1", "url", "https://ex.com/p")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraction.companyName").value("모의회사"))
                .andExpect(jsonPath("$.extraction.sourceUrl").value("https://ex.com/p"))
                .andExpect(jsonPath("$.extraction.essayQuestions[0].maxLength").value(800))
                .andExpect(jsonPath("$.sourceText").exists())
                .andExpect(jsonPath("$.callId").value(call.getId().toString()));
    }

    @Test
    void extractWithoutInputIs400() throws Exception {
        mvc.perform(jsonPost("/api/v1/ai/extract-posting", Map.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void extractWithoutKeyIs503() throws Exception {
        when(claudeGateway.isConfigured()).thenReturn(false);
        mvc.perform(jsonPost("/api/v1/ai/extract-posting", Map.of("text", "아무 공고")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_ERROR"));
    }

    @Test
    void callsEndpointWorks() throws Exception {
        mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/ai/calls")))
                .andExpect(status().isOk());
    }
}
