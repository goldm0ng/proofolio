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

class ProjectExperienceTest extends IntegrationTestBase {

    @Test
    void projectSectionsReplace_andExperienceCrud_andTags() throws Exception {
        String pid = json.readTree(mvc.perform(jsonPost("/api/v1/projects", Map.of(
                        "name", "ALLCLL", "tagline", "수강신청 도우미", "role", "백엔드", "teamSize", 4,
                        "repoUrl", "https://github.com/x/allcll", "visibility", "PUBLIC")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(jsonPut("/api/v1/projects/" + pid + "/sections", Map.of("sections", List.of(
                        Map.of("type", "OVERVIEW", "body", "개요"),
                        Map.of("type", "TROUBLESHOOTING", "body", "SSE 타임아웃")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(2));

        // replace again with one section -> old one gone
        mvc.perform(jsonPut("/api/v1/projects/" + pid + "/sections", Map.of("sections", List.of(
                        Map.of("type", "OVERVIEW", "body", "개요 v2")))))
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].body").value("개요 v2"));

        mvc.perform(jsonPut("/api/v1/projects/" + pid + "/tech-stack", Map.of("items", List.of(
                        Map.of("category", "language", "name", "Java"),
                        Map.of("category", "FRAMEWORK", "name", "Spring Boot")))))
                .andExpect(jsonPath("$.techStackItems[0].category").value("LANGUAGE"))
                .andExpect(jsonPath("$.techStack.length()").value(2));

        mvc.perform(jsonPut("/api/v1/projects/" + pid + "/metrics", Map.of("items", List.of(
                        Map.of("label", "피크 동시접속", "value", "516")))))
                .andExpect(jsonPath("$.metrics[0].value").value("516"));

        String tagA = "성능-" + System.nanoTime();
        String eid = json.readTree(mvc.perform(jsonPost("/api/v1/experiences", Map.of(
                        "projectId", pid, "title", "SSE 재연결 폭풍 해결",
                        "situation", "정정기간 첫날", "task", "재연결 폭풍", "action", "타임아웃 조정", "result", "안정화",
                        "tags", List.of(tagA, "장애대응", tagA),
                        "evidence", List.of(Map.of("type", "PR", "url", "https://github.com/x/allcll/pull/1", "label", "PR #1")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectName").value("ALLCLL"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.evidence[0].type").value("PR"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(auth(get("/api/v1/projects/" + pid)))
                .andExpect(jsonPath("$.experienceCount").value(1))
                .andExpect(jsonPath("$.experiences[0].id").value(eid));

        JsonNode tags = json.readTree(mvc.perform(auth(get("/api/v1/experiences/tags")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        boolean seen = false;
        for (JsonNode t : tags) if (t.get("tag").asText().equals(tagA)) { seen = true; assertThat(t.get("count").asLong()).isEqualTo(1); }
        assertThat(seen).isTrue();

        mvc.perform(auth(get("/api/v1/experiences").param("tag", tagA)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(get("/api/v1/experiences").param("q", "타임아웃")))
                .andExpect(jsonPath("$[0].id").value(eid));

        mvc.perform(jsonPut("/api/v1/experiences/" + eid, Map.of("title", "수정됨", "tags", List.of("협업"))))
                .andExpect(jsonPath("$.title").value("수정됨"))
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.evidence.length()").value(0));

        // deleting the project leaves the experience with a null project
        mvc.perform(auth(delete("/api/v1/projects/" + pid))).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/experiences/" + eid))).andExpect(status().isOk());
        mvc.perform(auth(delete("/api/v1/experiences/" + eid))).andExpect(status().isNoContent());
    }
}
