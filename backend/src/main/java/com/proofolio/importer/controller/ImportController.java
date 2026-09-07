package com.proofolio.importer.controller;

import com.proofolio.common.ApiException;
import com.proofolio.importer.dto.ImportDtos.ImportResult;
import com.proofolio.importer.dto.ImportDtos.NotionMapping;
import com.proofolio.importer.dto.ImportDtos.PreviewResponse;
import com.proofolio.importer.service.NotionImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.charset.StandardCharsets;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/import/notion")
@RequiredArgsConstructor
public class ImportController {

    private final NotionImportService importService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreviewResponse preview(@RequestParam("file") MultipartFile file) throws IOException {
        requireCsv(file);
        return importService.preview(file.getInputStream());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file,
                                  MultipartHttpServletRequest request) throws IOException {
        requireCsv(file);
        String mappingJson = readMappingJson(request);
        if (mappingJson == null || mappingJson.isBlank()) throw ApiException.importError("mapping이 필요합니다");
        NotionMapping mapping;
        try {
            mapping = objectMapper.readValue(mappingJson, NotionMapping.class);
        } catch (IOException e) {
            throw ApiException.importError("mapping JSON을 해석하지 못했습니다: " + e.getMessage());
        }
        var violations = validator.validate(mapping);
        if (!violations.isEmpty()) {
            throw ApiException.importError("mapping." + violations.iterator().next().getPropertyPath() + " 값이 필요합니다");
        }
        return importService.importCsv(file.getInputStream(), mapping);
    }

    /** mapping may arrive as a JSON blob part (browser FormData, has a filename) or a plain form field (curl -F). */
    private static String readMappingJson(MultipartHttpServletRequest request) throws IOException {
        MultipartFile part = request.getFile("mapping");
        if (part != null && !part.isEmpty()) {
            return new String(part.getBytes(), StandardCharsets.UTF_8);
        }
        return request.getParameter("mapping");
    }

    private static void requireCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.importError("CSV 파일이 비어 있습니다");
    }
}
