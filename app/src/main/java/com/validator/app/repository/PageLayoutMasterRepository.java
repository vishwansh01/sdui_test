package com.validator.app.repository;

import com.validator.app.model.PageLayoutMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PageLayoutMasterRepository extends JpaRepository<PageLayoutMaster, UUID> {
    @Query(value = """
        SELECT * FROM page_layout_master 
        WHERE page_name = :pageName 
          AND platform IN (:platform, 'all') 
          AND status = 'PUBLISHED' 
          AND min_app_version_code <= :versionCode 
          AND (max_app_version_code IS NULL OR max_app_version_code >= :versionCode) 
        ORDER BY min_app_version_code DESC 
        LIMIT 1
        """, nativeQuery = true)
    PageLayoutMaster resolveLayout(
            @Param("pageName") String pageName,
            @Param("platform") String platform,
            @Param("versionCode") Long versionCode
    );
}
