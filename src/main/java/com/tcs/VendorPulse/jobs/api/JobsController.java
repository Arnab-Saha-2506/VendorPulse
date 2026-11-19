package com.tcs.VendorPulse.jobs.api;

import com.tcs.VendorPulse.jobs.service.ScheduledJobsService;
import com.tcs.VendorPulse.jobs.service.ScheduledJobsService.RunJobsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@Tag(name = "Scheduled Jobs", description = "Background jobs trigger and administration APIs")
public class JobsController {

    private final ScheduledJobsService scheduledJobsService;

    @PostMapping("/run-all")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Run all scheduled background jobs immediately (Demo / Admin)", description = "Runs delay checks, reminders, CDI threshold checks, inventory risk evaluation, and score recalculations")
    public ResponseEntity<RunJobsResponse> runAllJobs() {
        return ResponseEntity.ok(scheduledJobsService.runAllJobsNow());
    }
}

