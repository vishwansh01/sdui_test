package com.validator.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.validator.app.service.PageAssemblyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@RestController
//@RequestMapping("/api/v1/sdui")
public class LayoutController {

    private final PageAssemblyService assemblyService;

    public LayoutController(PageAssemblyService assemblyService) {
        this.assemblyService = assemblyService;
    }

    @GetMapping("/layout")
    public ResponseEntity<JsonNode> getLayout(@RequestParam String pageName) {
        JsonNode assembledPage = assemblyService.getAssembledLayout(pageName);
        return ResponseEntity.ok(assembledPage);
    }
}