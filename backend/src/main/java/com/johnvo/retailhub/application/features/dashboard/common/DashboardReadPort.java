package com.johnvo.retailhub.application.features.dashboard.common;

import java.util.UUID;

public interface DashboardReadPort {
    DashboardOverviewView getOverview(UUID actorId, boolean admin, int lowStockThreshold,
                                      int lowStockLimit, int recentOrderLimit);
}
