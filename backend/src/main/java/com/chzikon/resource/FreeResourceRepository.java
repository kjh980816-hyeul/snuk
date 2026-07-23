package com.chzikon.resource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreeResourceRepository extends JpaRepository<FreeResource, Long> {

    List<FreeResource> findAllByOrderByCreatedAtDesc();
}
