package com.learning.platform.repository;

import com.learning.platform.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, String> {
    List<Badge> findByCategory(String category);
}
