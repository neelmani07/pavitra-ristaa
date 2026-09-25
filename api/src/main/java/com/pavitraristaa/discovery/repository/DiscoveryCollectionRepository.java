package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.discovery.entity.DiscoveryCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryCollectionRepository extends JpaRepository<DiscoveryCollection, Long> {

    List<DiscoveryCollection> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<DiscoveryCollection> findByCodeIgnoreCaseAndActiveTrue(String code);
}
