package com.validator.app.model;

//import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "component_interface")
public class ComponentInterface {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "component_type", nullable = false)
    private String componentType;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_schema", columnDefinition = "jsonb", nullable = false)
    private String jsonSchema;

    @Column(name = "min_app_version_code")
    private Long minAppVersionCode;

    @Column(name = "deprecated_at")
    private Long deprecatedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ComponentInterface() {}

    public String getComponentType() { return componentType; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public String getJsonSchema() { return jsonSchema; }
}
