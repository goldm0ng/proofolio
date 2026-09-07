package com.proofolio.ai.service;

import com.proofolio.ai.dto.AiDtos.AiCallResponse;
import com.proofolio.ai.dto.AiDtos.ExtractPostingRequest;
import com.proofolio.ai.dto.AiDtos.ExtractPostingResponse;
import com.proofolio.ai.dto.JobPostingExtraction;
import com.proofolio.ai.repository.AiCallRepository;
import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostingExtractionService {

    static final String TASK = "extract-posting";

    private final ClaudeGateway gateway;
    private final PageTextFetcher fetcher;
    private final AiCallRepository aiCallRepository;
    private final CurrentUser currentUser;

    public ExtractPostingResponse extract(ExtractPostingRequest req) {
        boolean hasUrl = req.url() != null && !req.url().isBlank();
        boolean hasText = req.text() != null && !req.text().isBlank();
        if (!hasUrl && !hasText) {
            throw ApiException.validation("url 또는 text 중 하나는 필요합니다");
        }
        if (!gateway.isConfigured()) {
            throw ApiException.ai(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "ANTHROPIC_API_KEY가 설정되지 않아 AI 기능을 쓸 수 없습니다");
        }
        String sourceText = hasText ? PageTextFetcher.collapse(req.text()) : fetcher.fetch(req.url().trim());

        StringBuilder user = new StringBuilder();
        if (hasUrl) user.append("원문 URL: ").append(req.url().trim()).append("\n\n");
        user.append("<posting>\n").append(sourceText).append("\n</posting>");

        ClaudeGateway.Result<JobPostingExtraction> result =
                gateway.structured(TASK, systemPrompt(), user.toString(), JobPostingExtraction.class);

        JobPostingExtraction x = result.value();
        if (hasUrl && (x.sourceUrl() == null || x.sourceUrl().isBlank())) {
            x = withSourceUrl(x, req.url().trim());
        }
        return new ExtractPostingResponse(x, sourceText, result.call().getId());
    }

    public List<AiCallResponse> recentCalls(int limit) {
        return aiCallRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.id(), PageRequest.of(0, Math.min(Math.max(limit, 1), 500)))
                .stream().map(AiCallResponse::from).toList();
    }

    private static String systemPrompt() {
        try {
            return new ClassPathResource("prompts/extract-posting.txt").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JobPostingExtraction withSourceUrl(JobPostingExtraction x, String url) {
        return new JobPostingExtraction(x.companyName(), x.positionTitle(), x.employmentType(), x.location(),
                x.deadlineAt(), x.requiredSkills(), x.preferredSkills(), x.hiringStages(), x.requiredDocuments(),
                x.essayQuestions(), x.summary(), url);
    }
}
