package com.validator.app.controller;

import com.validator.app.service.SduiValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sdui")
public class SduiController {
    private final SduiValidationService validationService;

    public SduiController(SduiValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/layout")
    public ResponseEntity<?> getLayout(
            @RequestParam("page_name") String pageName,
            @RequestParam("platform") String platform,
            @RequestParam("app_version_code") Long appVersionCode) {

        try {
            // Orchestrate the fetch, validate, and assemble flow
            Map<String, Object> layout = validationService.getAndValidateLayout(pageName, platform, appVersionCode);

            // 200 OK: Validated layout sent to frontend
            return ResponseEntity.ok(layout);

        } catch (IllegalArgumentException e) {
            // Protect against null messages
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Invalid request";

            // 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage));

        } catch (RuntimeException e) {
            // Protect against null messages
            String details = e.getMessage() != null ? e.getMessage() : "An unexpected NullPointerException or system error occurred";

            // 500 Internal Server Error
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Layout generation failed",
                            "details", details
                    )
            );
        }
    }
}