package com.learning.platform.repository;

import com.learning.platform.model.RoleBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleBenchmarkRepository extends JpaRepository<RoleBenchmark, String> {
    List<RoleBenchmark> findByCategory(String category);
}
