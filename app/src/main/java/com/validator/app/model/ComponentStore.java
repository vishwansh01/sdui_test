package com.validator.app.model;

import com.fasterxml.jackson.databind.JsonNode;
//import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
//import org.hibernate.annotations.Type;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;
//import tools.jackson.databind.JsonNode;

@Entity
@Table(name="component_store")
public class ComponentStore {
    @Id
    private UUID id;

    @Column(name = "component_type", nullable = false)
    private String componentType;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ComponentStore() {}

    public UUID getId() { return id; }
    public String getComponentType() { return componentType; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public JsonNode getPayload() { return payload; }
    public Integer getVersion(){return schemaVersion;}


}
