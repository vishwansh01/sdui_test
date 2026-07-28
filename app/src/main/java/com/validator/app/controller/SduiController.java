package com.validator.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.validator.app.service.PageAssemblyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sdui")
public class SduiController {

    private final PageAssemblyService assemblyService;
    private final ObjectMapper objectMapper;
    public SduiController(PageAssemblyService assemblyService, ObjectMapper objectMapper) {
        this.assemblyService = assemblyService;
        this.objectMapper = objectMapper;
    }
    @GetMapping("/layout")
    public ResponseEntity<Map<String, Object>> getLayout(@RequestParam String pageName) {

        JsonNode layoutNode = assemblyService.getAssembledLayout(pageName);

        Map<String, Object> layoutMap = objectMapper.convertValue(layoutNode, new TypeReference<Map<String, Object>>(){});
        return ResponseEntity.ok(layoutMap);
    }
}