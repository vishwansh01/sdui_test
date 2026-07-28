package com.validator.app.model;

//import com.networknt.schema.JsonType;
//import io.hypersistence.utils.hibernate.type.json.JsonType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "page_layout_master")
public class PageLayoutMaster {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "page_name", nullable = false) private String pageName;
    @Column(nullable = false) private String platform;

    @Column(name = "min_app_version", nullable = false) private String minAppVersion;
    @Column(name = "max_app_version") private String maxAppVersion;

    @Column(name = "min_app_version_code", nullable = false) private Long minAppVersionCode;
    @Column(name = "max_app_version_code") private Long maxAppVersionCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_tree", columnDefinition = "jsonb", nullable = false)
    private JsonNode layoutTree;

    @Column(nullable = false) private String status;

    @Column(name = "experiment_id") private UUID experimentId;
    @Column(name = "variant_key") private String variantKey;
    @Column(name = "rollout_percentage") private Integer rolloutPercentage;

    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    protected PageLayoutMaster() {}

    public JsonNode getLayoutTree() {
        return layoutTree;
    }
    public String getPageName() {
        return pageName;
    }
}
