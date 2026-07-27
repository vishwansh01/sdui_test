package com.validator.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.app.model.ComponentInterface;
import com.validator.app.model.ComponentStore;
import com.validator.app.repository.ComponentInterfaceRepository;
import com.validator.app.repository.ComponentStoreRepository;
import com.validator.app.repository.PageLayoutMasterRepository;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SduiValidationService {

    private final PageLayoutMasterRepository layoutRepo;
    private final ComponentStoreRepository componentRepo;
    private final ComponentInterfaceRepository interfaceRepo;
    private final ObjectMapper objectMapper;

    public SduiValidationService(PageLayoutMasterRepository layoutRepo,
                                 ComponentStoreRepository componentRepo,
                                 ComponentInterfaceRepository interfaceRepo,
                                 ObjectMapper objectMapper) {
        this.layoutRepo = layoutRepo;
        this.componentRepo = componentRepo;
        this.interfaceRepo = interfaceRepo;
        this.objectMapper = objectMapper;
    }

    // ─── 1. MAIN ENTRY POINT ──────────────────────────────────────────────────

    /**
     * Call this method after fetching your components from the database to validate them
     * against your custom JSON rules before sending them to the frontend.
     */
    public void validateFetchedComponents(List<ComponentStore> components, JsonNode layoutTree) {
        if (components == null || components.isEmpty()) return;

        // Fetch the custom rules from the component_interface table
        List<String> requiredTypes = components.stream()
                .map(ComponentStore::getComponentType)
                .distinct()
                .collect(Collectors.toList());

        List<ComponentInterface> interfaces = interfaceRepo.findByComponentTypeIn(requiredTypes);

        // Map Component Type to its raw JSON rules
        Map<String, String> schemaMap = interfaces.stream()
                .collect(Collectors.toMap(
                        ComponentInterface::getComponentType,
                        ComponentInterface::getJsonSchema
                ));

        // Validate tree structure first (if a layout tree is provided)
        if (layoutTree != null) {
            validateLayoutTree(layoutTree, schemaMap);
        }

        // Validate individual payloads
        for (ComponentStore comp : components) {
            String rawRules = schemaMap.get(comp.getComponentType());

            if (rawRules == null) {
                throw new IllegalStateException("Missing schema interface for component type: " + comp.getComponentType());
            }

            try {
                JsonNode interfaceRules = objectMapper.readTree(rawRules);
                validatePayloadDynamically(interfaceRules, comp.getPayload(), comp.getComponentType());
            } catch (Exception e) {
                throw new RuntimeException("Validation failed for ID " + comp.getId() + ": " + e.getMessage());
            }
        }
    }

    // ─── 2. TREE STRUCTURE VALIDATION ─────────────────────────────────────────

    private void validateLayoutTree(JsonNode node, Map<String, String> schemaMap) {
        if (!node.has("type")) return;
        String type = node.get("type").asText();

        // Parse the rules for this specific component type
        String rawRules = schemaMap.get(type);
        if (rawRules == null) return; // Skip if no rules defined (or throw error if strictly required)

        try {
            JsonNode rules = objectMapper.readTree(rawRules);

            // Check allowed children
            if (node.has("children") && node.get("children").isArray() && node.get("children").size() > 0) {
                if (!rules.has("allowedChildren")) {
                    throw new RuntimeException("Component '" + type + "' is not allowed to have children.");
                }

                JsonNode allowedChildren = rules.get("allowedChildren");
                for (JsonNode child : node.get("children")) {
                    String childType = child.get("type").asText();
                    boolean isAllowed = false;

                    for (JsonNode allowed : allowedChildren) {
                        if (allowed.asText().equals(childType)) {
                            isAllowed = true;
                            break;
                        }
                    }

                    if (!isAllowed) {
                        throw new RuntimeException("Invalid layout: '" + childType + "' cannot be placed inside '" + type + "'");
                    }

                    // Recursively validate child node trees
                    validateLayoutTree(child, schemaMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Tree validation failed: " + e.getMessage());
        }
    }

    // ─── 3. RECURSIVE DATA PAYLOAD VALIDATION ─────────────────────────────────

    private void validatePayloadDynamically(JsonNode rules, JsonNode data, String currentPath) {
        if (rules == null || data == null || data.isNull()) return;

        // 1. Check REQUIRED fields
        if (rules.has("required")) {
            for (JsonNode requiredNode : rules.get("required")) {
                String requiredField = requiredNode.asText();
                if (!data.has(requiredField) || data.get(requiredField).isNull()) {
                    throw new RuntimeException("Missing required field at '" + currentPath + "." + requiredField + "'");
                }
            }
        }

        // 2. Check ALLOWED TYPES (with recursion for Objects)
        if (rules.has("allowedTypes")) {
            JsonNode allowedTypes = rules.get("allowedTypes");
            Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();

                if (!allowedTypes.has(fieldName)) continue;

                String expectedType = allowedTypes.get(fieldName).asText();
                String fieldPath = currentPath + "." + fieldName;

                if (expectedType.equals("string") && !fieldValue.isTextual()) {
                    throw new RuntimeException("Field '" + fieldPath + "' must be a string");
                }
                if (expectedType.equals("number") && !fieldValue.isNumber()) {
                    throw new RuntimeException("Field '" + fieldPath + "' must be a number");
                }
                if (expectedType.equals("boolean") && !fieldValue.isBoolean()) {
                    throw new RuntimeException("Field '" + fieldPath + "' must be a boolean");
                }

                // Handle nested objects (like containerStyle or placement)
                if (expectedType.equals("object")) {
                    if (!fieldValue.isObject()) {
                        throw new RuntimeException("Field '" + fieldPath + "' must be an object");
                    }

                    if (rules.has("nestedRules") && rules.get("nestedRules").has(fieldName)) {
                        JsonNode nestedRules = rules.get("nestedRules").get(fieldName);
                        // Recursive call
                        validatePayloadDynamically(nestedRules, fieldValue, fieldPath);
                    }
                }
            }
        }

        // 3. Check NUMBER RANGES
        if (rules.has("numberRanges")) {
            JsonNode numberRanges = rules.get("numberRanges");
            Iterator<Map.Entry<String, JsonNode>> rangeFields = numberRanges.fields();

            while (rangeFields.hasNext()) {
                Map.Entry<String, JsonNode> rangeEntry = rangeFields.next();
                String fieldName = rangeEntry.getKey();
                JsonNode limits = rangeEntry.getValue();

                if (data.has(fieldName) && data.get(fieldName).isNumber()) {
                    double actualValue = data.get(fieldName).asDouble();
                    String fieldPath = currentPath + "." + fieldName;

                    if (limits.has("min") && actualValue < limits.get("min").asDouble()) {
                        throw new RuntimeException("Field '" + fieldPath + "' must be at least " + limits.get("min").asText());
                    }
                    if (limits.has("max") && actualValue > limits.get("max").asDouble()) {
                        throw new RuntimeException("Field '" + fieldPath + "' must be at most " + limits.get("max").asText());
                    }
                }
            }
        }

        // 4. Check ALLOWED VALUES (Enums)
        if (rules.has("allowedValues")) {
            JsonNode allowedValues = rules.get("allowedValues");
            Iterator<Map.Entry<String, JsonNode>> enumFields = allowedValues.fields();

            while (enumFields.hasNext()) {
                Map.Entry<String, JsonNode> enumEntry = enumFields.next();
                String fieldName = enumEntry.getKey();
                JsonNode validOptions = enumEntry.getValue();

                if (data.has(fieldName) && !data.get(fieldName).isNull()) {
                    String actualValue = data.get(fieldName).asText();
                    boolean isValid = false;

                    for (JsonNode option : validOptions) {
                        if (actualValue.equals(option.asText())) {
                            isValid = true;
                            break;
                        }
                    }

                    if (!isValid) {
                        throw new RuntimeException("Field '" + currentPath + "." + fieldName +
                                "' has invalid value: '" + actualValue + "'. Allowed values are: " + validOptions);
                    }
                }
            }
        }
    }
}