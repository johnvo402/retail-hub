package com.johnvo.retailhub.application.features.dashboard.query.getdashboardoverview;

import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardOverviewView;
import com.johnvo.retailhub.application.features.dashboard.common.DashboardReadPort;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardOverviewQueryHandler
        implements QueryHandler<GetDashboardOverviewQuery, DashboardOverviewView> {
    static final int LOW_STOCK_THRESHOLD = 5;
    static final int LOW_STOCK_LIMIT = 6;
    static final int RECENT_ORDER_LIMIT = 5;

    private final DashboardReadPort dashboard;

    public GetDashboardOverviewQueryHandler(DashboardReadPort dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    public Result<DashboardOverviewView> handle(GetDashboardOverviewQuery query) {
        return Result.success(dashboard.getOverview(query.actorId(), query.admin(), LOW_STOCK_THRESHOLD,
                LOW_STOCK_LIMIT, RECENT_ORDER_LIMIT));
    }
}
