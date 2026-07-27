package com.validator.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LayoutNode {
    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("component_type")
    private String componentType;

    @JsonProperty("component_id")
    private String componentId;

    @JsonProperty("schema_version")
    private Integer schemaVersion;

    private List<LayoutNode> children;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public List<LayoutNode> getChildren() { return children; }
    public void setChildren(List<LayoutNode> children) { this.children = children; }
}
