package com.example.flood.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T02:30:00Z"), ZoneOffset.UTC);
        PublicIdGenerator idGenerator = new PublicIdGenerator(clock);
        RequestIdFilter requestIdFilter = new RequestIdFilter(idGenerator);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(clock, idGenerator);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(handler)
            .addFilters(requestIdFilter)
            .build();
    }

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void malformedJsonUsesStableEnvelope() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("MALFORMED_REQUEST");
        assertThat(body.get("requestId").asText()).startsWith("req_");
        assertThat(result.getResponse().getHeader(RequestIdFilter.HEADER_NAME))
            .isEqualTo(body.get("requestId").asText());
    }

    @Test
    void beanValidationUses422AndFieldDetails() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.get("details").get(0).get("field").asText()).isEqualTo("name");
    }

    @Test
    void apiExceptionUsesItsStatusAndCode() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/missing"))
            .andExpect(status().isNotFound())
            .andReturn();

        ApiErrorResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), ApiErrorResponse.class);
        assertThat(response.errorCode()).isEqualTo("REGION_NOT_FOUND");
        assertThat(response.message()).isEqualTo("region not found");
    }

    @Test
    void missingHeaderUses400ValidationEnvelope() throws Exception {
        assertBindingError(mockMvc.perform(get("/test/header")).andReturn());
    }

    @Test
    void missingQueryParameterUses400ValidationEnvelope() throws Exception {
        assertBindingError(mockMvc.perform(get("/test/query")).andReturn());
    }

    @Test
    void invalidEnumUses400ValidationEnvelope() throws Exception {
        assertBindingError(mockMvc.perform(get("/test/enum").param("value", "UNKNOWN"))
            .andReturn());
    }

    private void assertBindingError(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("errorCode").asText()).isEqualTo("MALFORMED_REQUEST");
    }

    record TestRequest(@NotBlank String name) {}

    @RestController
    static class TestController {
        enum TestValue { KNOWN }

        @PostMapping("/test/body")
        TestRequest body(@Valid @RequestBody TestRequest request) {
            return request;
        }

        @PostMapping("/test/missing")
        void missing() {
            throw new ApiException(ErrorCode.REGION_NOT_FOUND, "region not found");
        }

        @GetMapping("/test/header")
        void header(@RequestHeader("X-Required") String value) {}

        @GetMapping("/test/query")
        void query(@RequestParam("value") String value) {}

        @GetMapping("/test/enum")
        void enumValue(@RequestParam("value") TestValue value) {}
    }
}
