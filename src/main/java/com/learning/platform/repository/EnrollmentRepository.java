package com.learning.platform.repository;

import com.learning.platform.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserId(String userId);
    List<Enrollment> findByUserIdAndCourseId(String userId, String courseId);
    List<Enrollment> findByUserIdAndIsCoreTrack(String userId, boolean isCoreTrack);
}
