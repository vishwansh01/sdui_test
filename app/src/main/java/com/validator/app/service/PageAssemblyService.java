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

    public JsonNode getAssembledLayout(String pageName) {
        // 1. Fetch the Layout Tree (Simplifying the query for testing)
        PageLayoutMaster layoutMaster = layoutRepo.findAll().stream()
                .filter(page -> page.getPageName().equals(pageName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Page not found"));

        // Convert your custom LayoutNode POJO into a dynamic JsonNode
        JsonNode layoutTree = objectMapper.valueToTree(layoutMaster.getLayoutTree());

        // 2. Extract all Component IDs from the tree
        Set<UUID> componentIds = new HashSet<>();
        extractComponentIds(layoutTree, componentIds);

        // 3. Fetch all components in one go
        List<ComponentStore> components = componentRepo.findAllById(componentIds);

        // 4. RUN YOUR CUSTOM VALIDATION ENGINE
        validationService.validateFetchedComponents(components, layoutTree);

        // 5. Merge the component data back into the tree
        Map<UUID, JsonNode> componentMap = new HashMap<>();
        for (ComponentStore comp : components) {
            componentMap.put(comp.getId(), comp.getPayload());
        }

        return mergeDataIntoTree(layoutTree.deepCopy(), componentMap);
    }

    // Helper: Recursively find component IDs
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

    // Helper: Recursively inject payload data into the tree
    private JsonNode mergeDataIntoTree(JsonNode node, Map<UUID, JsonNode> componentMap) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            // If this node has a component_id, inject its styling and data
            if (objectNode.has("component_id")) {
                UUID compId = UUID.fromString(objectNode.get("component_id").asText());
                JsonNode payload = componentMap.get(compId);

                if (payload != null) {
                    // Merge payload fields (like containerStyle, placement, data) into this node
                    Iterator<Map.Entry<String, JsonNode>> fields = payload.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        objectNode.set(field.getKey(), field.getValue());
                    }
                }
                // We can remove the component_id now so the frontend doesn't see it
                objectNode.remove("component_id");
            }

            // Continue merging children
            if (objectNode.has("children")) {
                for (JsonNode child : objectNode.get("children")) {
                    mergeDataIntoTree(child, componentMap);
                }
            }
        }
        return node;
    }
}