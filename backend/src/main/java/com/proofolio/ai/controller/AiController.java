package com.proofolio.ai.controller;

import com.proofolio.ai.dto.AiDtos.AiCallResponse;
import com.proofolio.ai.dto.AiDtos.ExtractPostingRequest;
import com.proofolio.ai.dto.AiDtos.ExtractPostingResponse;
import com.proofolio.ai.service.PostingExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final PostingExtractionService extractionService;

    @PostMapping("/extract-posting")
    public ExtractPostingResponse extract(@RequestBody ExtractPostingRequest req) {
        return extractionService.extract(req);
    }

    @GetMapping("/calls")
    public List<AiCallResponse> calls(@RequestParam(defaultValue = "50") int limit) {
        return extractionService.recentCalls(limit);
    }
}
