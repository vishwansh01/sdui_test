package com.validator.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.validator.app.service.PageAssemblyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sdui")
public class SduiController {

    private final PageAssemblyService assemblyService;

    // Injecting the Assembly Service instead of the Validation Service directly
    public SduiController(PageAssemblyService assemblyService) {
        this.assemblyService = assemblyService;
    }

    /**
     * Endpoint to fetch the fully assembled and validated SDUI layout.
     * Example usage: GET http://localhost:8080/api/v1/sdui/layout?pageName=/home
     */
    @GetMapping("/layout")
    public ResponseEntity<JsonNode> getLayout(@RequestParam String pageName) {
        // The PageAssemblyService fetches the tree, validates components, and merges the data
        JsonNode assembledPage = assemblyService.getAssembledLayout(pageName);

        return ResponseEntity.ok(assembledPage);
    }
}