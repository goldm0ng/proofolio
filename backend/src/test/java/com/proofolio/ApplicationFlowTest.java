package com.proofolio;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationFlowTest extends IntegrationTestBase {

    @Test
    void createByCompanyName_thenBoard_thenStatusHistory() throws Exception {
        String company = "테스트은행-" + System.nanoTime();
        String body = mvc.perform(jsonPost("/api/v1/applications", Map.of(
                        "companyName", company,
                        "positionTitle", "백엔드 개발 신입",
                        "deadlineAt", "2026-09-30T14:59:00Z",
                        "requirements", List.of(Map.of("title", "이력서", "kind", "RESUME"),
                                Map.of("title", "성적증명서", "kind", "TRANSCRIPT")),
                        "essayQuestions", List.of(Map.of("question", "지원 동기를 쓰시오", "maxLength", 1000)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INTERESTED"))
                .andExpect(jsonPath("$.companyName").value(company))
                .andExpect(jsonPath("$.requirementsTotal").value(2))
                .andExpect(jsonPath("$.requirementsDone").value(0))
                .andExpect(jsonPath("$.essayTotal").value(1))
                .andExpect(jsonPath("$.statusHistory.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(body);
        String id = created.get("id").asText();
        String reqId = created.get("requirements").get(0).get("id").asText();
        String qId = created.get("essayQuestions").get(0).get("id").asText();

        // board lists every column in enum order and the card sits in INTERESTED
        JsonNode board = json.readTree(mvc.perform(auth(get("/api/v1/applications/board")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(board.get("columns")).hasSize(10);
        assertThat(board.get("columns").get(0).get("status").asText()).isEqualTo("INTERESTED");
        assertThat(board.get("columns").get(9).get("status").asText()).isEqualTo("WITHDRAWN");
        boolean found = false;
        for (JsonNode item : board.get("columns").get(0).get("items")) {
            if (item.get("id").asText().equals(id)) found = true;
        }
        assertThat(found).isTrue();

        // counters: mark requirement done, essay DONE
        mvc.perform(jsonPatch("/api/v1/applications/" + id + "/requirements/" + reqId, Map.of("done", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.done").value(true));
        mvc.perform(jsonPatch("/api/v1/applications/" + id + "/essay-questions/" + qId,
                        Map.of("draft", "초안입니다", "status", "DONE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        mvc.perform(auth(get("/api/v1/applications/" + id)))
                .andExpect(jsonPath("$.requirementsDone").value(1))
                .andExpect(jsonPath("$.essayDone").value(1));

        // status change records history
        mvc.perform(jsonPatch("/api/v1/applications/" + id + "/status", Map.of("status", "SUBMITTED", "note", "제출함")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.statusHistory[1].note").value("제출함"));

        // upcoming (deadline is in the past relative to a real clock only if today > 2026-09-30; use a wide window)
        mvc.perform(auth(get("/api/v1/applications/upcoming").param("days", "3650")))
                .andExpect(status().isOk());

        // company detail shows the application
        String companyId = created.get("companyId").asText();
        mvc.perform(auth(get("/api/v1/companies/" + companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications[0].id").value(id));

        // company with applications cannot be deleted
        mvc.perform(auth(delete("/api/v1/companies/" + companyId)))
                .andExpect(status().isConflict());

        mvc.perform(auth(delete("/api/v1/applications/" + id))).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/applications/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void fromExtraction_createsCompanyRequirementsAndEssays() throws Exception {
        String company = "추출회사-" + System.nanoTime();
        Map<String, Object> extraction = Map.of(
                "companyName", company,
                "positionTitle", "서버 개발자",
                "deadlineAt", "2026-10-15",
                "requiredSkills", List.of("Java", "Spring"),
                "preferredSkills", List.of(),
                "hiringStages", List.of("서류", "코딩테스트", "면접"),
                "requiredDocuments", List.of(Map.of("title", "자기소개서", "kind", "ESSAY")),
                "essayQuestions", List.of(Map.of("question", "성장 과정", "maxLength", 500)),
                "summary", "요약",
                "sourceUrl", "https://example.com/job");
        mvc.perform(jsonPost("/api/v1/applications/from-extraction", Map.of("extraction", extraction)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value(company))
                .andExpect(jsonPath("$.postingUrl").value("https://example.com/job"))
                .andExpect(jsonPath("$.deadlineAt").exists())
                .andExpect(jsonPath("$.requirementsTotal").value(1))
                .andExpect(jsonPath("$.essayTotal").value(1))
                .andExpect(jsonPath("$.notes").exists());
    }

    @Test
    void validationErrorShape() throws Exception {
        mvc.perform(jsonPost("/api/v1/applications", Map.of("positionTitle", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(jsonPost("/api/v1/applications", Map.of("positionTitle", "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
