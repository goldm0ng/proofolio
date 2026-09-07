package com.proofolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofolio.ai.service.ClaudeGateway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final String TOKEN = "test-token";

    @BeforeAll
    static void startDb() {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.api-token", () -> TOKEN);
        r.add("app.claude.api-key", () -> "");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @MockitoBean protected ClaudeGateway claudeGateway;

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + TOKEN);
    }

    protected MockHttpServletRequestBuilder jsonPost(String path, Object body) throws Exception {
        return auth(post(path)).contentType("application/json").content(json.writeValueAsString(body));
    }

    protected MockHttpServletRequestBuilder jsonPut(String path, Object body) throws Exception {
        return auth(put(path)).contentType("application/json").content(json.writeValueAsString(body));
    }

    protected MockHttpServletRequestBuilder jsonPatch(String path, Object body) throws Exception {
        return auth(patch(path)).contentType("application/json").content(json.writeValueAsString(body));
    }
}
