package com.validator.app.repository;

import com.validator.app.model.ComponentInterface;
//import com.validator.app.model.PageLayoutMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComponentInterfaceRepository extends JpaRepository<ComponentInterface, UUID> {
    @Query("SELECT c FROM ComponentInterface c WHERE (c.componentType, c.schemaVersion) IN :typeVersionPairs")
    List<ComponentInterface> findSchemasForComponents(@Param("typeVersionPairs") List<Object[]> typeVersionPairs);
    List<ComponentInterface> findByComponentTypeIn(List<String> componentTypes);
}
