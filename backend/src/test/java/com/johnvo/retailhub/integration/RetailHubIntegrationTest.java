package com.johnvo.retailhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommandHandler;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import com.johnvo.retailhub.domain.inventory.InventoryMovementType;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.infrastructure.persistence.jpa.inventory.InventoryJpaEntity;
import com.johnvo.retailhub.infrastructure.persistence.jpa.inventory.InventoryMovementJpaEntity;
import com.johnvo.retailhub.infrastructure.persistence.jpa.inventory.SpringDataInventoryMovementRepository;
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
import java.util.ArrayList;
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
    @Autowired IncreaseStockCommandHandler increaseStock;
    @Autowired SpringDataInventoryMovementRepository inventoryMovements;
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
                "{\"quantity\":10,\"reason\":\"Initial supplier delivery\"}", accessToken, null);
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
        assertThat(json(get("/api/inventory/" + productId, accessToken)).get("quantity").asInt()).isEqualTo(8);

        JsonNode movements = json(get("/api/inventory/" + productId + "/movements", accessToken));
        assertThat(movements.get("totalItems").asInt()).isEqualTo(2);
        assertThat(movements.get("items").get(0).get("type").asText()).isEqualTo("ORDER_CONFIRMATION");
        assertThat(movements.get("items").get(0).get("quantityDelta").asInt()).isEqualTo(-2);
        assertThat(movements.get("items").get(0).get("referenceId").asText()).isEqualTo(orderId.toString());
        assertThat(movements.get("items").get(1).get("type").asText()).isEqualTo("MANUAL_INCREASE");
        assertThat(movements.get("items").get(1).get("reason").asText())
                .isEqualTo("Initial supplier delivery");

        JsonNode order = json(get("/api/orders/" + orderId, accessToken));
        assertThat(order.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(order.get("totalAmount").decimalValue()).isEqualByComparingTo("319.80");
        assertThat(order.get("items")).hasSize(1);
        assertThat(orders.findById(new OrderId(orderId))).get()
                .extracting(saved -> saved.items().size()).isEqualTo(1);
        HttpResponse<String> secondConfirm = post("/api/orders/" + orderId + "/confirm", "", accessToken, null);
        assertThat(secondConfirm.statusCode()).isEqualTo(422);
        assertThat(json(secondConfirm).get("type").asText()).isEqualTo("ORDER_INVALID_STATE");
        assertThat(json(get("/api/inventory/" + productId, accessToken)).get("quantity").asInt()).isEqualTo(8);

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
        assertThat(get("/api/inventory/" + productId, otherAccessToken).statusCode()).isEqualTo(200);
        assertThat(get("/api/inventory/" + productId + "/movements", otherAccessToken).statusCode())
                .isEqualTo(200);
        assertThat(post("/api/inventory/" + productId + "/increase",
                "{\"quantity\":1}", otherAccessToken, null).statusCode()).isEqualTo(403);
        assertThat(post("/api/inventory/" + productId + "/decrease",
                "{\"quantity\":1}", otherAccessToken, null).statusCode()).isEqualTo(403);
        assertThat(get("/api/orders/" + orderId, otherAccessToken).statusCode()).isEqualTo(403);
        assertThat(post("/api/orders/" + orderId + "/confirm", "", otherAccessToken, null).statusCode())
                .isEqualTo(403);

        HttpResponse<String> logout = post("/api/auth/logout", "", null, rotatedCookie);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().firstValue("set-cookie").orElseThrow()).contains("Max-Age=0");
        assertThat(post("/api/auth/refresh", "", null, rotatedCookie).statusCode()).isEqualTo(401);
    }

    @Test
    void orderConfirmationIsAtomicWhenAnyProductHasInsufficientStock() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String accessToken = json(post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null))
                .get("accessToken").asText();

        HttpResponse<String> categoryResponse = post("/api/categories",
                "{\"name\":\"Atomic " + suffix + "\",\"description\":\"Atomic stock test\",\"active\":true}",
                accessToken, null);
        UUID categoryId = UUID.fromString(json(categoryResponse).get("id").asText());
        UUID firstProductId = UUID.fromString(json(post("/api/products",
                objectMapper.writeValueAsString(new ProductPayload("Available " + suffix, "Enough stock",
                        "ATOMIC-A-" + suffix, BigDecimal.TEN, categoryId, true)), accessToken, null))
                .get("id").asText());
        UUID secondProductId = UUID.fromString(json(post("/api/products",
                objectMapper.writeValueAsString(new ProductPayload("Scarce " + suffix, "Insufficient stock",
                        "ATOMIC-B-" + suffix, BigDecimal.TEN, categoryId, true)), accessToken, null))
                .get("id").asText());
        assertThat(post("/api/inventory/" + firstProductId + "/increase",
                "{\"quantity\":10}", accessToken, null).statusCode()).isEqualTo(200);
        assertThat(post("/api/inventory/" + secondProductId + "/increase",
                "{\"quantity\":2}", accessToken, null).statusCode()).isEqualTo(200);

        UUID orderId = UUID.fromString(json(post("/api/orders", "", accessToken, null)).get("id").asText());
        assertThat(post("/api/orders/" + orderId + "/items",
                "{\"productId\":\"" + firstProductId + "\",\"quantity\":3}", accessToken, null).statusCode())
                .isEqualTo(201);
        assertThat(post("/api/orders/" + orderId + "/items",
                "{\"productId\":\"" + secondProductId + "\",\"quantity\":3}", accessToken, null).statusCode())
                .isEqualTo(201);

        HttpResponse<String> confirmation = post("/api/orders/" + orderId + "/confirm", "", accessToken, null);

        assertThat(confirmation.statusCode()).isEqualTo(422);
        assertThat(json(confirmation).get("type").asText()).isEqualTo("INVENTORY_INSUFFICIENT_STOCK");
        assertThat(json(confirmation).get("detail").asText()).contains("Scarce " + suffix);
        assertThat(json(get("/api/inventory/" + firstProductId, accessToken)).get("quantity").asInt()).isEqualTo(10);
        assertThat(json(get("/api/inventory/" + secondProductId, accessToken)).get("quantity").asInt()).isEqualTo(2);
        assertThat(json(get("/api/orders/" + orderId, accessToken)).get("status").asText()).isEqualTo("DRAFT");
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

    @Test
    void inventoryPersistenceUsesJpaOptimisticLocking() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String accessToken = json(post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null))
                .get("accessToken").asText();
        UUID categoryId = UUID.fromString(json(post("/api/categories",
                "{\"name\":\"Concurrency " + suffix + "\",\"description\":\"Concurrency test\",\"active\":true}",
                accessToken, null)).get("id").asText());
        UUID productId = UUID.fromString(json(post("/api/products",
                objectMapper.writeValueAsString(new ProductPayload("Concurrent " + suffix, "Concurrent stock",
                        "LOCK-" + suffix, BigDecimal.TEN, categoryId, true)), accessToken, null))
                .get("id").asText());
        assertThat(post("/api/inventory/" + productId + "/increase",
                "{\"quantity\":5}", accessToken, null).statusCode()).isEqualTo(200);
        long movementCountBeforeConflict = inventoryMovements.countByProductId(productId);

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            InventoryJpaEntity first = firstManager.find(InventoryJpaEntity.class, productId);
            InventoryJpaEntity second = secondManager.find(InventoryJpaEntity.class, productId);
            long initialVersion = first.getVersion();
            first.update(4, Instant.now());
            second.update(3, Instant.now().plusSeconds(1));
            firstManager.persist(new InventoryMovementJpaEntity(InventoryMovement.create(
                    new ProductId(productId), InventoryMovementType.MANUAL_DECREASE, 5, 4,
                    null, null, "First concurrent change", Instant.now())));
            secondManager.persist(new InventoryMovementJpaEntity(InventoryMovement.create(
                    new ProductId(productId), InventoryMovementType.MANUAL_DECREASE, 5, 3,
                    null, null, "Conflicting concurrent change", Instant.now().plusSeconds(1))));

            firstManager.getTransaction().commit();
            assertThat(first.getVersion()).isGreaterThan(initialVersion);
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
        assertThat(inventoryMovements.countByProductId(productId)).isEqualTo(movementCountBeforeConflict + 1);
    }

    @Test
    void movementPersistenceFailureRollsBackStockMutation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String accessToken = json(post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null))
                .get("accessToken").asText();
        UUID categoryId = UUID.fromString(json(post("/api/categories",
                "{\"name\":\"Movement rollback " + suffix
                        + "\",\"description\":\"Movement transaction test\",\"active\":true}",
                accessToken, null)).get("id").asText());
        UUID productId = UUID.fromString(json(post("/api/products",
                objectMapper.writeValueAsString(new ProductPayload("Movement rollback " + suffix,
                        "Movement transaction test", "MOVE-ROLLBACK-" + suffix, BigDecimal.TEN,
                        categoryId, true)), accessToken, null)).get("id").asText());
        assertThat(post("/api/inventory/" + productId + "/increase",
                "{\"quantity\":5}", accessToken, null).statusCode()).isEqualTo(200);
        long movementCount = inventoryMovements.countByProductId(productId);

        assertThatThrownBy(() -> increaseStock.handle(new IncreaseStockCommand(productId, 1,
                UUID.randomUUID(), "Actor FK failure"))).isInstanceOf(RuntimeException.class);

        assertThat(json(get("/api/inventory/" + productId, accessToken)).get("quantity").asInt()).isEqualTo(5);
        assertThat(inventoryMovements.countByProductId(productId)).isEqualTo(movementCount);
    }

    @Test
    void productVisibilityIsEnforcedForListsDetailsCacheAndSearch() throws Exception {
        String marker = "visibility" + UUID.randomUUID().toString().substring(0, 8);
        String adminToken = json(post("/api/auth/login",
                "{\"email\":\"admin@retailhub.test\",\"password\":\"Integration123!\"}", null, null))
                .get("accessToken").asText();

        String userEmail = marker + "@retailhub.test";
        assertThat(post("/api/auth/register",
                "{\"email\":\"" + userEmail + "\",\"password\":\"Integration123!\"}", null, null)
                .statusCode()).isEqualTo(201);
        String userToken = json(post("/api/auth/login",
                "{\"email\":\"" + userEmail + "\",\"password\":\"Integration123!\"}", null, null))
                .get("accessToken").asText();

        UUID categoryId = UUID.fromString(json(post("/api/categories",
                "{\"name\":\"" + marker + "\",\"description\":\"Visibility tests\",\"active\":true}",
                adminToken, null)).get("id").asText());
        UUID alphaId = createProduct(adminToken, categoryId, "Alpha " + marker,
                "VIS-A-" + marker, "10.00");
        UUID betaId = createProduct(adminToken, categoryId, "Beta " + marker,
                "VIS-B-" + marker, "20.00");
        UUID inactiveId = createProduct(adminToken, categoryId, "Hidden " + marker,
                "VIS-H-" + marker, "15.00");
        assertThat(put("/api/products/" + inactiveId,
                objectMapper.writeValueAsString(new ProductPayload("Hidden " + marker,
                        "Inactive visibility product", "VIS-H-" + marker, new BigDecimal("15.00"),
                        categoryId, false)), adminToken).statusCode()).isEqualTo(204);

        String filter = "keyword=" + marker + "&size=20";
        assertThat(productIds(json(get("/api/products?" + filter, null))))
                .containsExactlyInAnyOrder(alphaId, betaId);
        assertThat(productIds(json(get("/api/products?active=false&" + filter, null))))
                .containsExactlyInAnyOrder(alphaId, betaId);
        assertThat(productIds(json(get("/api/products?active=false&" + filter, userToken))))
                .containsExactlyInAnyOrder(alphaId, betaId);
        assertThat(productIds(json(get("/api/products?" + filter, adminToken))))
                .containsExactlyInAnyOrder(alphaId, betaId, inactiveId);
        assertThat(productIds(json(get("/api/products?active=false&" + filter, adminToken))))
                .containsExactly(inactiveId);

        String combinedFilters = "category=" + categoryId + "&minPrice=9&maxPrice=25&keyword=" + marker
                + "&sort=name,desc&size=20";
        assertThat(productIds(json(get("/api/products?" + combinedFilters, null))))
                .containsExactly(betaId, alphaId);

        assertThat(get("/api/products/" + alphaId, null).statusCode()).isEqualTo(200);
        assertThat(get("/api/products/" + alphaId, userToken).statusCode()).isEqualTo(200);

        redis.delete("product:" + inactiveId);
        assertThat(get("/api/products/" + inactiveId, adminToken).statusCode()).isEqualTo(200);
        assertThat(redis.hasKey("product:" + inactiveId)).isTrue();
        assertThat(get("/api/products/" + inactiveId, null).statusCode()).isEqualTo(404);
        assertThat(get("/api/products/" + inactiveId, userToken).statusCode()).isEqualTo(404);
        assertThat(redis.hasKey("product:" + inactiveId)).isTrue();

        List<UUID> searchIds = List.of();
        for (int attempt = 0; attempt < 15; attempt++) {
            HttpResponse<String> response = get("/api/products/search?q=" + marker + "&size=20", null);
            assertThat(response.statusCode()).isEqualTo(200);
            searchIds = productIds(json(response));
            if (searchIds.containsAll(List.of(alphaId, betaId))) {
                break;
            }
            Thread.sleep(200);
        }
        assertThat(searchIds).contains(alphaId, betaId).doesNotContain(inactiveId);
    }

    private UUID createProduct(String accessToken, UUID categoryId, String name, String sku, String price)
            throws Exception {
        HttpResponse<String> response = post("/api/products",
                objectMapper.writeValueAsString(new ProductPayload(name, name, sku, new BigDecimal(price),
                        categoryId, true)), accessToken, null);
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(json(response).get("id").asText());
    }

    private static List<UUID> productIds(JsonNode page) {
        List<UUID> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(UUID.fromString(item.get("id").asText())));
        return ids;
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
