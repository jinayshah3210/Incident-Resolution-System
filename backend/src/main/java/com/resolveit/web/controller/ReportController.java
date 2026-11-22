package com.resolveit.web.controller;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.security.CurrentUser;
import com.resolveit.service.ReportService;
import com.resolveit.web.dto.MonthReportDto;
import com.resolveit.web.dto.ReportDto;
import com.resolveit.web.dto.TechnicianReportDto;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Analytics endpoints for the admin dashboard. */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/overview")
    public ReportDto overview(@CurrentUser PortalUser user) { return reports.overview(user); }

    @GetMapping("/months")
    public List<MonthReportDto> months(@CurrentUser PortalUser user) { return reports.months(user); }

    @GetMapping("/technicians/{id}")
    public TechnicianReportDto technician(@CurrentUser PortalUser user, @PathVariable String id) { return reports.technician(user, id); }

    @GetMapping("/{month}")
    public ReportDto month(@CurrentUser PortalUser user, @PathVariable String month) { return reports.month(user, month); }
}
