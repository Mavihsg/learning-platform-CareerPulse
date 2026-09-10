package com.learning.platform.repository;

import com.learning.platform.model.DiscussionThread;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, String> {

    List<DiscussionThread> findAllByOrderByCreatedAtDesc();

    List<DiscussionThread> findByCourseIdOrderByCreatedAtDesc(String courseId);

    List<DiscussionThread> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    long countByCourseId(String courseId);

    @Query("SELECT t FROM DiscussionThread t WHERE " +
           "(:courseId IS NULL OR :courseId = '' OR t.courseId = :courseId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.content) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<DiscussionThread> searchThreads(@Param("courseId") String courseId,
                                         @Param("search") String search,
                                         Sort sort);
}
