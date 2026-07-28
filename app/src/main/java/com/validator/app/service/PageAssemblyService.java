package com.validator.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.validator.app.model.ComponentStore;
import com.validator.app.model.PageLayoutMaster;
import com.validator.app.repository.ComponentStoreRepository;
import com.validator.app.repository.PageLayoutMasterRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PageAssemblyService {

    private final PageLayoutMasterRepository layoutRepo;
    private final ComponentStoreRepository componentRepo;
    private final SduiValidationService validationService;
    private final ObjectMapper objectMapper;

    public PageAssemblyService(PageLayoutMasterRepository layoutRepo,
                               ComponentStoreRepository componentRepo,
                               SduiValidationService validationService,
                               ObjectMapper objectMapper) {
        this.layoutRepo = layoutRepo;
        this.componentRepo = componentRepo;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

//    public JsonNode getAssembledLayout(String pageName) {
//        PageLayoutMaster layoutMaster = layoutRepo.findAll().stream()
//                .filter(page -> page.getPageName().equals(pageName))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Page not found"));
//
//        JsonNode layoutTree = objectMapper.valueToTree(layoutMaster.getLayoutTree());
//
//        Set<UUID> componentIds = new HashSet<>();
//        extractComponentIds(layoutTree, componentIds);
//
//        List<ComponentStore> components = componentRepo.findAllById(componentIds);
//
//        validationService.validateFetchedComponents(components, layoutTree);
//
//        Map<UUID, JsonNode> componentMap = new HashMap<>();
//        for (ComponentStore comp : components) {
//            componentMap.put(comp.getId(), comp.getPayload());
//        }
//
//        return mergeDataIntoTree(layoutTree.deepCopy(), componentMap);
//    }
public JsonNode getAssembledLayout(String pageName) {
    PageLayoutMaster layoutMaster = layoutRepo.findAll().stream()
            .filter(page -> page.getPageName().equals(pageName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Page not found"));

    JsonNode layoutTree = objectMapper.valueToTree(layoutMaster.getLayoutTree());

    Set<UUID> componentIds = new HashSet<>();
    extractComponentIds(layoutTree, componentIds);

    List<UUID> idList = new ArrayList<>(componentIds);
    int batchSize = 3;
    List<CompletableFuture<List<ComponentStore>>> futures = new ArrayList<>();

    for (int i = 0; i < idList.size(); i += batchSize) {
        List<UUID> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
        CompletableFuture<List<ComponentStore>> future = CompletableFuture.supplyAsync(() -> {
            return componentRepo.findAllById(batch);
        });

        futures.add(future);
    }

    CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

    List<ComponentStore> components = allOf.thenApply(v ->
            futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList())
    ).join();

    validationService.validateFetchedComponents(components, layoutTree);

    Map<UUID, JsonNode> componentMap = new HashMap<>();
    for (ComponentStore comp : components) {
        componentMap.put(comp.getId(), comp.getPayload());
    }

    return mergeDataIntoTree(layoutTree.deepCopy(), componentMap);
}

    private void extractComponentIds(JsonNode node, Set<UUID> ids) {
        if (node.has("component_id")) {
            ids.add(UUID.fromString(node.get("component_id").asText()));
        }
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                extractComponentIds(child, ids);
            }
        }
    }

    private JsonNode mergeDataIntoTree(JsonNode node, Map<UUID, JsonNode> componentMap) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            if (objectNode.has("component_id")) {
                UUID compId = UUID.fromString(objectNode.get("component_id").asText());
                JsonNode payload = componentMap.get(compId);

                if (payload != null) {
                    Iterator<Map.Entry<String, JsonNode>> fields = payload.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        objectNode.set(field.getKey(), field.getValue());
                    }
                }
                objectNode.remove("component_id");
            }

            if (objectNode.has("children")) {
                for (JsonNode child : objectNode.get("children")) {
                    mergeDataIntoTree(child, componentMap);
                }
            }
        }
        return node;
    }
}