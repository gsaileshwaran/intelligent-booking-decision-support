package com.pvk.cinemas.common.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.common.time.BusinessDateProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
public class BusinessDateController {

    private final BusinessDateProvider businessDateProvider;

    public BusinessDateController(BusinessDateProvider businessDateProvider) {
        this.businessDateProvider = businessDateProvider;
    }

    public record BusinessDateInfo(
            String businessDate,
            String windowStartDate,
            String windowEndDate,
            int windowDays,
            List<String> demoDates,
            String timeZone
    ) {}

    @GetMapping("/business-date")
    public ResponseEntity<ApiResponse<BusinessDateInfo>> getBusinessDate() {
        List<String> demoDateStrings = businessDateProvider.getDemoBookingWindow()
                .stream()
                .map(LocalDate::toString)
                .toList();

        BusinessDateInfo info = new BusinessDateInfo(
                businessDateProvider.getBusinessDate().toString(),
                businessDateProvider.getDemoWindowStart().toString(),
                businessDateProvider.getDemoWindowEnd().toString(),
                businessDateProvider.getWindowDays(),
                demoDateStrings,
                businessDateProvider.getZone().getId()
        );

        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
