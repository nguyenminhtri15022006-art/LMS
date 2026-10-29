package com.lms.webapp.controller;

import com.lms.webapp.client.GatewayClient;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Diagnostic regression tests: production code deliberately left unchanged. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class Phase2BffVerificationTest {
    @Autowired MockMvc mvc;
    @Autowired MockRestServiceServer server;
    @Autowired GatewayExceptionHandler handler;
    @Autowired GatewayClient gatewayClient;

    @Test
    @WithMockUser(roles = "STUDENT")
    void schedulesPreserveGateway503OnRealController() throws Exception {
        assertRealEndpointPreserves503("schedules", "");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void announcementsPreserveGateway503OnRealController() throws Exception {
        assertRealEndpointPreserves503("announcements", "?page=0&size=10");
    }

    private void assertRealEndpointPreserves503(String suffix, String query) throws Exception {
        server.expect(requestTo("http://localhost:8080/api/course/courses/c-probe/" + suffix + query))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":1405,\"message\":\"Service unavailable\"}"));
        var response = mvc.perform(get("/courses/c-probe/" + suffix).accept(MediaType.ALL))
            .andReturn().getResponse();
        server.verify();
        assertThat(response.getStatus()).as("real BFF route must preserve upstream 503; body=%s",
            response.getContentAsString()).isEqualTo(503);
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void handlerHonorsJsonEndpointForWildcardAccept() {
        // Handler-level test only: MVC exposes this attribute for produces=application/json.
        var request = new MockHttpServletRequest("GET", "/courses/c-probe/schedules");
        request.addHeader(HttpHeaders.ACCEPT, "*/*");
        request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(MediaType.APPLICATION_JSON));
        Object result = handler.handleGatewayUnavailable(
            new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE), request);
        assertThat(result).as("JSON endpoint must not receive an HTML ModelAndView")
            .isInstanceOf(ResponseEntity.class);
    }

    @Test
    void handlerDoesNotExposeInternalDetailsIn4xx() {
        var request = new MockHttpServletRequest("GET", "/probe");
        request.addHeader(HttpHeaders.ACCEPT, "application/json");
        String body = "{\"code\":1003,\"message\":\"Not found\",\"debug\":\"http://private-service:8081/internal; SQL_DIAGNOSTIC_MARKER\"}";
        server.expect(requestTo("http://localhost:8080/api/probe"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(body));
        var exception = assertThrows(HttpClientErrorException.class,
            () -> gatewayClient.get("/probe", String.class));
        server.verify();
        var response = (ResponseEntity<?>) handler.handleHttpClientError(exception, request);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(String.valueOf(response.getBody()))
            .doesNotContain("private-service", "SQL_DIAGNOSTIC_MARKER");
    }
}
