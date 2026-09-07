package com.proofolio;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotionImportTest extends IntegrationTestBase {

    @Test
    void previewSuggestsMapping_andImportIsIdempotent() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String csv = "﻿이름,직무,마감일,상태,링크,메모\n"
                + "노션은행" + suffix + ",백엔드,2026년 9월 30일 오후 6:00,지원 예정,https://ex.com/1,인재상: 도전\n"
                + "노션전자" + suffix + ",SW개발,\"September 20, 2026\",서류 통과,,\n"
                + "노션공사" + suffix + ",,이상한날짜,면접,,\n"
                + ",비어있음,,,,\n";
        MockMultipartFile file = new MockMultipartFile("file", "notion.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        JsonNode preview = json.readTree(mvc.perform(auth(multipart("/api/v1/import/notion/preview").file(file)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(4))
                .andReturn().getResponse().getContentAsString());
        assertThat(preview.get("columns").get(0).asText()).isEqualTo("이름"); // BOM stripped
        JsonNode m = preview.get("suggestedMapping");
        assertThat(m.get("companyName").asText()).isEqualTo("이름");
        assertThat(m.get("positionTitle").asText()).isEqualTo("직무");
        assertThat(m.get("deadlineAt").asText()).isEqualTo("마감일");
        assertThat(m.get("status").asText()).isEqualTo("상태");
        assertThat(m.get("postingUrl").asText()).isEqualTo("링크");
        assertThat(m.get("notes").get(0).asText()).isEqualTo("메모");

        String mapping = json.writeValueAsString(m);
        JsonNode first = json.readTree(mvc.perform(auth(multipart("/api/v1/import/notion").file(file).param("mapping", mapping)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(first.get("created").asInt()).isEqualTo(3);
        assertThat(first.get("updated").asInt()).isEqualTo(0);
        assertThat(first.get("skipped").asInt()).isEqualTo(1);
        assertThat(first.get("errors").toString()).contains("이상한날짜");

        // second run sends mapping as a JSON blob part, the way a browser FormData does
        MockPart mappingPart = new MockPart("mapping", "mapping.json", mapping.getBytes(StandardCharsets.UTF_8));
        mappingPart.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        JsonNode second = json.readTree(mvc.perform(auth(multipart("/api/v1/import/notion").file(file).part(mappingPart)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(second.get("created").asInt()).isEqualTo(0);
        assertThat(second.get("updated").asInt()).isEqualTo(3);

        JsonNode cards = json.readTree(mvc.perform(auth(get("/api/v1/applications")))
                .andReturn().getResponse().getContentAsString());
        JsonNode bank = null;
        for (JsonNode c : cards) if (c.get("companyName").asText().equals("노션은행" + suffix)) bank = c;
        assertThat(bank).isNotNull();
        assertThat(bank.get("status").asText()).isEqualTo("PLANNED");
        assertThat(bank.get("postingUrl").asText()).isEqualTo("https://ex.com/1");
        assertThat(bank.get("deadlineAt").asText()).isEqualTo("2026-09-30T09:00:00Z"); // 18:00 KST
        assertThat(bank.get("positionTitle").asText()).isEqualTo("백엔드");
    }

    @Test
    void importRejectsUnknownCompanyColumn() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x.csv", "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));
        mvc.perform(auth(multipart("/api/v1/import/notion").file(file)
                        .param("mapping", json.writeValueAsString(Map.of("companyName", "없는컬럼")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_ERROR"));
    }
}
