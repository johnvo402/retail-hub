package com.johnvo.retailhub.infrastructure.persistence.jdbc.dashboard;

import com.johnvo.retailhub.application.features.dashboard.common.DashboardLowStockView;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardOrderSummaryView;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardOverviewView;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardReadPort;
import com.johnvo.retailhub.domain.ordering.OrderStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JdbcDashboardReadAdapter implements DashboardReadPort {
    private static final String ACTIVE_PRODUCT_COUNT_SQL = """
            SELECT COUNT(*)
            FROM products
            WHERE active = TRUE
            """;
    private static final String INVENTORY_METRICS_SQL = """
            SELECT COUNT(*) AS inventory_line_count,
                   COUNT(*) FILTER (WHERE quantity < :lowStockThreshold) AS low_stock_count
            FROM inventory_items
            """;
    private static final String LOW_STOCK_ITEMS_SQL = """
            SELECT i.product_id, p.name AS product_name, p.sku, i.quantity
            FROM inventory_items i
            JOIN products p ON p.id = i.product_id
            WHERE i.quantity < :lowStockThreshold
            ORDER BY i.quantity ASC, p.name ASC
            LIMIT :lowStockLimit
            """;
    private static final String ORDER_METRICS_SQL = """
            WITH scoped_orders AS (
                SELECT o.id,
                       o.status,
                       COALESCE(SUM(oi.unit_price * oi.quantity), 0.00) AS total_amount
                FROM orders o
                LEFT JOIN order_items oi ON oi.order_id = o.id
                %s
                GROUP BY o.id, o.status
            )
            SELECT COUNT(*) FILTER (WHERE status = 'DRAFT') AS draft_order_count,
                   COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_order_count,
                   COALESCE(SUM(total_amount) FILTER (WHERE status = 'CONFIRMED'), 0.00)
                       AS confirmed_order_value
            FROM scoped_orders
            """;
    private static final String RECENT_ORDERS_SQL = """
            SELECT o.id,
                   o.status,
                   COALESCE(SUM(oi.unit_price * oi.quantity), 0.00) AS total_amount,
                   o.created_at
            FROM orders o
            LEFT JOIN order_items oi ON oi.order_id = o.id
            %s
            GROUP BY o.id, o.status, o.created_at
            ORDER BY o.created_at DESC, o.id DESC
            LIMIT :recentOrderLimit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDashboardReadAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DashboardOverviewView getOverview(UUID actorId, boolean admin, int lowStockThreshold,
                                             int lowStockLimit, int recentOrderLimit) {
        long activeProductCount = Objects.requireNonNull(
                jdbc.queryForObject(ACTIVE_PRODUCT_COUNT_SQL, Map.of(), Long.class));

        MapSqlParameterSource inventoryParameters = new MapSqlParameterSource()
                .addValue("lowStockThreshold", lowStockThreshold)
                .addValue("lowStockLimit", lowStockLimit);
        InventoryMetrics inventory = Objects.requireNonNull(jdbc.queryForObject(INVENTORY_METRICS_SQL,
                inventoryParameters, (result, rowNumber) -> new InventoryMetrics(
                        result.getLong("inventory_line_count"), result.getLong("low_stock_count"))));
        List<DashboardLowStockView> lowStockItems = jdbc.query(LOW_STOCK_ITEMS_SQL,
                inventoryParameters, (result, rowNumber) -> new DashboardLowStockView(
                        result.getObject("product_id", UUID.class), result.getString("product_name"),
                        result.getString("sku"), result.getInt("quantity")));

        String orderScope = admin ? "" : "WHERE o.customer_id = :actorId";
        MapSqlParameterSource orderParameters = new MapSqlParameterSource()
                .addValue("actorId", actorId)
                .addValue("recentOrderLimit", recentOrderLimit);
        OrderMetrics orders = Objects.requireNonNull(jdbc.queryForObject(
                ORDER_METRICS_SQL.formatted(orderScope), orderParameters,
                (result, rowNumber) -> new OrderMetrics(result.getLong("draft_order_count"),
                        result.getLong("confirmed_order_count"),
                        result.getBigDecimal("confirmed_order_value"))));
        List<DashboardOrderSummaryView> recentOrders = jdbc.query(
                RECENT_ORDERS_SQL.formatted(orderScope), orderParameters,
                (result, rowNumber) -> new DashboardOrderSummaryView(
                        result.getObject("id", UUID.class),
                        OrderStatus.valueOf(result.getString("status")),
                        result.getBigDecimal("total_amount"),
                        result.getTimestamp("created_at").toInstant()));

        return new DashboardOverviewView(activeProductCount, inventory.inventoryLineCount(),
                inventory.lowStockCount(), orders.draftOrderCount(), orders.confirmedOrderCount(),
                orders.confirmedOrderValue(), recentOrders, lowStockItems);
    }

    private record InventoryMetrics(long inventoryLineCount, long lowStockCount) {
    }

    private record OrderMetrics(long draftOrderCount, long confirmedOrderCount,
                                BigDecimal confirmedOrderValue) {
    }
}
