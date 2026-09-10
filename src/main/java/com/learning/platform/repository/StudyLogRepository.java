package com.learning.platform.repository;

import com.learning.platform.model.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {
    List<StudyLog> findByUserId(String userId);
    List<StudyLog> findByUserIdAndLogDateBetween(String userId, LocalDate start, LocalDate end);
    List<StudyLog> findByLogDateBetween(LocalDate start, LocalDate end);
    Optional<StudyLog> findByUserIdAndLogDate(String userId, LocalDate date);
}
