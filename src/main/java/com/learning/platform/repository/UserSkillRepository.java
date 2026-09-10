package com.learning.platform.repository;

import com.learning.platform.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserId(String userId);
    Optional<UserSkill> findByUserIdAndSkillId(String userId, String skillId);
}
