package com.learning.platform.repository;

import com.learning.platform.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByCategory(String category);
    List<Course> findByDifficultyLevel(String difficultyLevel);
}
