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
        assertThat(filterNames("blog-service")).contains("AuthFilter");

        assertPublicReadFilters("course-public-read");
        assertPublicReadFilters("blog-public");
        assertPublicReadFilters("identity-public-profile");
    }

    private String bestRouteId(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());

        return routeLocator.getRoutes()
                .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
                .sort(Comparator.comparingInt(Route::getOrder))
                .map(Route::getId)
                .next()
                .block();
    }

    private void assertPublicReadFilters(String routeId) {
        List<String> filters = filterNames(routeId);
        assertThat(filters).doesNotContain("AuthFilter");

        String filterDefinitions = routeDefinition(routeId).getFilters().toString();
        assertThat(filterDefinitions)
                .contains("Authorization")
                .contains("X-User-Id")
                .contains("X-User-Role")
                .contains("X-User-Email")
                .contains("X-Gateway-Authenticated");
    }

    private List<String> filterNames(String routeId) {
        return routeDefinition(routeId).getFilters().stream()
                .map(FilterDefinition::getName)
                .toList();
    }

    private RouteDefinition routeDefinition(String routeId) {
        return gatewayProperties.getRoutes().stream()
                .filter(route -> route.getId().equals(routeId))
                .findFirst()
                .orElseThrow();
    }
}
