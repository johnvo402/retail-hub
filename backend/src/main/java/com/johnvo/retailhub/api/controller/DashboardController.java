package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.api.exception.ResultResponseMapper;
import com.johnvo.retailhub.api.security.SecurityUtils;
import com.johnvo.retailhub.application.features.dashboard.query.getdashboardoverview.GetDashboardOverviewQuery;
import com.johnvo.retailhub.application.features.dashboard.query.getdashboardoverview.GetDashboardOverviewQueryHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GetDashboardOverviewQueryHandler getOverview;
    private final ResultResponseMapper results;

    public DashboardController(GetDashboardOverviewQueryHandler getOverview, ResultResponseMapper results) {
        this.getOverview = getOverview;
        this.results = results;
    }

    @GetMapping("/overview")
    ResponseEntity<?> overview(Authentication authentication) {
        return results.ok(getOverview.handle(new GetDashboardOverviewQuery(
                SecurityUtils.userId(authentication), SecurityUtils.isAdmin(authentication))));
    }
}
