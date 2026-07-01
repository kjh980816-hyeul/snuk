package com.chzikon.collab.repository;

import com.chzikon.collab.domain.ClientLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientLogoRepository extends JpaRepository<ClientLogo, Long> {
    List<ClientLogo> findAllByOrderBySortOrderAscIdAsc();
}
