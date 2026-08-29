package com.johnvo.retailhub.application.features.dashboard.query.getdashboardoverview;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardOverviewView;

import java.util.UUID;

public record GetDashboardOverviewQuery(
        UUID actorId,
        boolean admin
) implements Query<DashboardOverviewView> {
}
