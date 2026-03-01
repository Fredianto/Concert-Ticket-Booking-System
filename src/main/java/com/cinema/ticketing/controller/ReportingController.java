package com.cinema.ticketing.controller;

import com.cinema.ticketing.dto.report.DashboardResponse;
import com.cinema.ticketing.dto.report.SettlementRow;
import com.cinema.ticketing.dto.report.TransactionRow;
import com.cinema.ticketing.service.ReportingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/concerts/{id}/settlement")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public List<SettlementRow> settlement(@PathVariable UUID id) {
        return reportingService.settlement(id);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public List<TransactionRow> transactions() {
        return reportingService.transactions();
    }

    @GetMapping("/analytics/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public DashboardResponse dashboard() {
        return reportingService.dashboard();
    }
}
