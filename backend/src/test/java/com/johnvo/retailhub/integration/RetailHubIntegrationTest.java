package com.johnvo.retailhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.infrastructure.persistence.jpa.ordering.OrderJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "retailhub.bootstrap.admin-email=admin@retailhub.test",
        "retailhub.bootstrap.admin-password=Integration123!",
        "retailhub.security.jwt-secret=integration-test-secret-that-is-at-least-32-bytes",
        "retailhub.security.cookie-secure=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class RetailHubIntegrationTest {
    @LocalServerPort
    int port;

    @Autowired ObjectMapper objectMapper;
    @Autowired StringRedisTemplate redis;
    @Autowired OrderRepository orders;
    @Autowired EntityManagerFactory entityManagerFactory;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE))
            .build();

    @Test
    void completeAuthenticationCatalogInventoryOrderingCacheAndSearchFlow() throws Exception {
        HttpResponse<String> invalidLogin = post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"wrong-password\"}", null, null);
        assertThat(invalidLogin.statusCode()).isEqualTo(401);

        HttpResponse<String> login = post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null);
        assertThat(login.statusCode()).isEqualTo(200);
        String originalCookieHeader = login.headers().firstValue("set-cookie").orElseThrow();
        assertThat(originalCookieHeader).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/auth");
        String originalCookie = cookiePair(originalCookieHeader);
        String accessToken = json(login).get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        HttpResponse<String> protectedWithoutToken = get("/api/inventory", null);
        assertThat(protectedWithoutToken.statusCode()).isEqualTo(401);

        HttpResponse<String> refresh = post("/api/auth/refresh", "", null, originalCookie);
        assertThat(refresh.statusCode()).isEqualTo(200);
        String rotatedCookieHeader = refresh.headers().firstValue("set-cookie").orElseThrow();
        String rotatedCookie = cookiePair(rotatedCookieHeader);
        assertThat(rotatedCookie).isNotEqualTo(originalCookie);
        accessToken = json(refresh).get("accessToken").asText();

        HttpResponse<String> reusedOldToken = post("/api/auth/refresh", "", null, originalCookie);
        assertThat(reusedOldToken.statusCode()).isEqualTo(401);

        HttpResponse<String> categoryResponse = post("/api/categories",
                "{\"name\":\"Keyboards\",\"description\":\"Mechanical keyboards\",\"active\":true}",
                accessToken, null);
        assertThat(categoryResponse.statusCode()).isEqualTo(201);
        UUID categoryId = UUID.fromString(json(categoryResponse).get("id").asText());

        HttpResponse<String> productResponse = post("/api/products", objectMapper.writeValueAsString(new ProductPayload(
                "Atlas Keyboard", "Low profile mechanical keyboard", "KEY-ATLAS", new BigDecimal("149.90"),
                categoryId, true)), accessToken, null);
        assertThat(productResponse.statusCode()).isEqualTo(201);
        UUID productId = UUID.fromString(json(productResponse).get("id").asText());

        HttpResponse<String> productGet = get("/api/products/" + productId, null);
        assertThat(productGet.statusCode()).isEqualTo(200);
        assertThat(redis.hasKey("product:" + productId)).isTrue();

        HttpResponse<String> productUpdate = put("/api/products/" + productId,
                objectMapper.writeValueAsString(new ProductPayload("Atlas Keyboard V2", "Updated keyboard",
                        "KEY-ATLAS", new BigDecimal("159.90"), categoryId, true)), accessToken);
        assertThat(productUpdate.statusCode()).isEqualTo(204);
        assertThat(redis.hasKey("product:" + productId)).isFalse();

        JsonNode searchResult = waitForSearch("KEY-ATLAS");
        assertThat(searchResult.get("totalItems").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(searchResult.get("items").get(0).get("id").asText()).isEqualTo(productId.toString());

        HttpResponse<String> increase = post("/api/inventory/" + productId + "/increase",
                "{\"quantity\":10}", accessToken, null);
        assertThat(increase.statusCode()).isEqualTo(200);
        assertThat(json(increase).get("quantity").asInt()).isEqualTo(10);

        HttpResponse<String> negativeStock = post("/api/inventory/" + productId + "/decrease",
                "{\"quantity\":11}", accessToken, null);
        assertThat(negativeStock.statusCode()).isEqualTo(422);

        HttpResponse<String> orderResponse = post("/api/orders", "", accessToken, null);
        assertThat(orderResponse.statusCode()).isEqualTo(201);
        UUID orderId = UUID.fromString(json(orderResponse).get("id").asText());
        HttpResponse<String> addItem = post("/api/orders/" + orderId + "/items",
                "{\"productId\":\"" + productId + "\",\"quantity\":2}", accessToken, null);
        assertThat(addItem.statusCode()).isEqualTo(201);
        HttpResponse<String> confirm = post("/api/orders/" + orderId + "/confirm", "", accessToken, null);
        assertThat(confirm.statusCode()).isEqualTo(204);

        JsonNode order = json(get("/api/orders/" + orderId, accessToken));
        assertThat(order.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(order.get("totalAmount").decimalValue()).isEqualByComparingTo("319.80");
        assertThat(order.get("items")).hasSize(1);
        assertThat(orders.findById(new OrderId(orderId))).get()
                .extracting(saved -> saved.items().size()).isEqualTo(1);

        HttpResponse<String> orderList = get("/api/orders", accessToken);
        assertThat(orderList.statusCode()).isEqualTo(200);
        assertThat(json(orderList).get("items").get(0).get("id").asText()).isEqualTo(orderId.toString());

        HttpResponse<String> emptyOrderResponse = post("/api/orders", "", accessToken, null);
        UUID emptyOrderId = UUID.fromString(json(emptyOrderResponse).get("id").asText());
        HttpResponse<String> emptyConfirm = post("/api/orders/" + emptyOrderId + "/confirm", "", accessToken, null);
        assertThat(emptyConfirm.statusCode()).isEqualTo(422);
        assertThat(json(emptyConfirm).get("type").asText()).isEqualTo("ORDER_EMPTY");

        HttpResponse<String> invalidItem = post("/api/orders/" + emptyOrderId + "/items",
                "{\"productId\":\"" + productId + "\",\"quantity\":0}", accessToken, null);
        assertThat(invalidItem.statusCode()).isEqualTo(400);
        assertThat(get("/api/orders/" + UUID.randomUUID(), accessToken).statusCode()).isEqualTo(404);

        HttpResponse<String> mutableOrderResponse = post("/api/orders", "", accessToken, null);
        UUID mutableOrderId = UUID.fromString(json(mutableOrderResponse).get("id").asText());
        HttpResponse<String> mutableItemResponse = post("/api/orders/" + mutableOrderId + "/items",
                "{\"productId\":\"" + productId + "\",\"quantity\":1}", accessToken, null);
        UUID mutableItemId = UUID.fromString(json(mutableItemResponse).get("id").asText());
        assertThat(delete("/api/orders/" + mutableOrderId + "/items/" + mutableItemId, accessToken).statusCode())
                .isEqualTo(204);
        assertThat(post("/api/orders/" + mutableOrderId + "/cancel", "", accessToken, null).statusCode())
                .isEqualTo(204);
        assertThat(json(get("/api/orders/" + mutableOrderId, accessToken)).get("status").asText())
                .isEqualTo("CANCELLED");

        assertThat(post("/api/auth/register",
                "{\"email\":\"other@retailhub.test\",\"password\":\"Integration123!\"}", null, null)
                .statusCode()).isEqualTo(201);
        HttpResponse<String> otherLogin = post("/api/auth/login",
                "{\"email\":\"other@retailhub.test\",\"password\":\"Integration123!\"}", null, null);
        String otherAccessToken = json(otherLogin).get("accessToken").asText();
        assertThat(get("/api/orders/" + orderId, otherAccessToken).statusCode()).isEqualTo(403);

        HttpResponse<String> logout = post("/api/auth/logout", "", null, rotatedCookie);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().firstValue("set-cookie").orElseThrow()).contains("Max-Age=0");
        assertThat(post("/api/auth/refresh", "", null, rotatedCookie).statusCode()).isEqualTo(401);
    }

    @Test
    void orderPersistenceUsesJpaOptimisticLocking() throws Exception {
        HttpResponse<String> login = post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null);
        String accessToken = json(login).get("accessToken").asText();
        UUID orderId = UUID.fromString(json(post("/api/orders", "", accessToken, null)).get("id").asText());

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            OrderJpaEntity first = firstManager.find(OrderJpaEntity.class, orderId);
            OrderJpaEntity second = secondManager.find(OrderJpaEntity.class, orderId);

            Order firstDomain = Order.reconstitute(new OrderId(first.getId()), first.getCustomerId(),
                    first.getStatus(), List.of(), first.getCreatedAt(), first.getUpdatedAt(),
                    first.getConfirmedAt(), first.getCancelledAt());
            Order secondDomain = Order.reconstitute(new OrderId(second.getId()), second.getCustomerId(),
                    second.getStatus(), List.of(), second.getCreatedAt(), second.getUpdatedAt(),
                    second.getConfirmedAt(), second.getCancelledAt());
            firstDomain.cancel(Instant.now());
            secondDomain.cancel(Instant.now().plusSeconds(1));
            first.update(firstDomain);
            second.update(secondDomain);

            firstManager.getTransaction().commit();
            assertThat(first.getVersion()).isEqualTo(1);
            assertThatThrownBy(secondManager.getTransaction()::commit)
                    .isInstanceOfAny(RollbackException.class, OptimisticLockException.class);
        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }
            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }
            firstManager.close();
            secondManager.close();
        }
    }

    private JsonNode waitForSearch(String query) throws Exception {
        JsonNode result = null;
        for (int attempt = 0; attempt < 15; attempt++) {
            HttpResponse<String> response = get("/api/products/search?q=" + query, null);
            assertThat(response.statusCode()).isEqualTo(200);
            result = json(response);
            if (result.get("totalItems").asLong() > 0) {
                return result;
            }
            Thread.sleep(200);
        }
        return result;
    }

    private HttpResponse<String> get(String path, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
        authorize(builder, accessToken);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String accessToken, String cookie)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, accessToken);
        if (cookie != null) {
            builder.header(HttpHeaders.COOKIE, cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, accessToken);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).DELETE();
        authorize(builder, accessToken);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void authorize(HttpRequest.Builder builder, String accessToken) {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
    }

    private JsonNode json(HttpResponse<String> response) throws IOException {
        return objectMapper.readTree(response.body());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String cookiePair(String setCookie) {
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    record ProductPayload(String name, String description, String sku, BigDecimal price,
                          UUID categoryId, boolean active) {
    }
}
