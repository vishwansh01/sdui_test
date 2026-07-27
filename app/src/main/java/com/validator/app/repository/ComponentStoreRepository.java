package com.validator.app.repository;

import com.validator.app.model.ComponentStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComponentStoreRepository extends JpaRepository<ComponentStore, UUID> {
    List<ComponentStore> findByIdInAndStatus(List<UUID> ids, String status);
}
