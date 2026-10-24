package com.lms.apigateway.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GatewayRouteConfigurationTest {

    @Autowired(required = false)
    RouteLocator routeLocator;

    @Autowired(required = false)
    GatewayProperties gatewayProperties;

    @Test
    @DisplayName("Case 1: Route Matching Identity — Khớp đúng route identity-service và identity-public")
    void identityRoutesMatchCorrectService() {
        assertThat(bestRouteId(HttpMethod.POST, "/api/identity/users")).isEqualTo("identity-private");
        assertThat(bestRouteId(HttpMethod.POST, "/api/identity/auth/login")).isEqualTo("identity-public");
    }

    @Test
    @DisplayName("Case 2: Route Matching Course — Phân biệt public read course-public-read và private course-service")
    void courseRoutesDistinguishPublicReadAndPrivateMutations() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses")).isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/categories")).isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/course-1")).isEqualTo("course-public-read");

        assertThat(bestRouteId(HttpMethod.POST, "/api/course/courses")).isEqualTo("course-service");
        assertThat(bestRouteId(HttpMethod.PUT, "/api/course/courses/course-1")).isEqualTo("course-service");
        assertThat(bestRouteId(HttpMethod.DELETE, "/api/course/courses/course-1")).isEqualTo("course-service");
    }

    @Test
    @DisplayName("Case 3: Route Matching Blog — Phân biệt public read blog-public và private blog-service")
    void blogRoutesDistinguishPublicReadAndPrivateMutations() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/blog/posts/public")).isEqualTo("blog-public");
        assertThat(bestRouteId(HttpMethod.GET, "/api/blog/tags")).isEqualTo("blog-public");

        assertThat(bestRouteId(HttpMethod.POST, "/api/blog/posts")).isEqualTo("blog-service");
        assertThat(bestRouteId(HttpMethod.PUT, "/api/blog/posts/post-1")).isEqualTo("blog-service");
        assertThat(bestRouteId(HttpMethod.DELETE, "/api/blog/posts/post-1")).isEqualTo("blog-service");
    }

    @Test
    @DisplayName("Case 4: Private Read Shortcuts — /api/course/courses/my và enrolled giữ nguyên AuthFilter")
    void privateCourseReadShortcutsKeepAuthFilter() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/my"))
                .isEqualTo("course-private-read-shortcuts");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/enrolled"))
                .isEqualTo("course-private-read-shortcuts");

        List<String> filters = filterNames("course-private-read-shortcuts");
        assertThat(filters).contains("AuthFilter");
    }

    @Test
    @DisplayName("Case 5: Sensitive Route Filter Assertion — Các route bảo vệ có AuthFilter, route public có tẩy rửa header")
    void sensitiveRoutesKeepAuthFilterAndPublicRoutesStripHeaders() {
        assertThat(filterNames("identity-public")).doesNotContain("AuthFilter");
        assertThat(filterNames("identity-private")).contains("AuthFilter");
        assertThat(filterNames("course-service")).contains("AuthFilter");
        assertThat(filterNames("assignment-service")).contains("AuthFilter");
        assertThat(filterNames("assignment-code-judge")).contains("AuthFilter");
        assertThat(filterNames("chatbot-service")).contains("AuthFilter");
    }

    @Test
    @DisplayName("Case 6: Assignment Code Judge Route — Khớp đúng route chuyên biệt cho submit và run-trial")
    void assignmentCodeJudgeRoutesMatchDedicatedJudgeRoute() {
        assertThat(bestRouteId(HttpMethod.POST, "/api/assignment/problems/1/submit"))
                .isEqualTo("assignment-code-judge");
        assertThat(bestRouteId(HttpMethod.POST, "/api/assignment/problems/1/run-trial"))
                .isEqualTo("assignment-code-judge");
    }

    private String bestRouteId(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());

        return routeLocator.getRoutes()
                .filterWhen(route -> route.getPredicate().apply(exchange))
                .sort(Comparator.comparingInt(Route::getOrder))
                .next()
                .map(Route::getId)
                .switchIfEmpty(Mono.error(new IllegalStateException("No route matched for: " + path)))
                .block();
    }

    private List<String> filterNames(String routeId) {
        RouteDefinition definition = gatewayProperties.getRoutes().stream()
                .filter(route -> route.getId().equals(routeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown routeId: " + routeId));

        return definition.getFilters().stream()
                .map(FilterDefinition::getName)
                .toList();
    }
}
