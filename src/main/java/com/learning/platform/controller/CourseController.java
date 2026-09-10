package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.PlanBuilderRequestDto;
import com.learning.platform.model.Course;
import com.learning.platform.model.Enrollment;
import com.learning.platform.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getAllCourses()));
    }

    @GetMapping("/enrolled/{userId}")
    public ResponseEntity<ApiResponse<List<CourseService.EnrolledCourseInfo>>> getEnrolledCourses(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getEnrolledCourses(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id) {
        return courseService.getCourseById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.ok(c)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Course not found with id: " + id)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Course>>> getCoursesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCoursesByCategory(category)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createPlan(@RequestBody PlanBuilderRequestDto dto) {
        Course created = courseService.createOrPublishPlan(dto);
        return ResponseEntity.ok(ApiResponse.ok("Course plan created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updatePlan(@PathVariable String id, @RequestBody PlanBuilderRequestDto dto) {
        dto.setId(id);
        Optional<Course> existingOpt = courseService.getCourseById(id);
        if (existingOpt.isPresent()) {
            Course existing = existingOpt.get();
            if (dto.getAuthorId() != null && !dto.getAuthorId().isBlank() 
                    && existing.getAuthorId() != null && !existing.getAuthorId().equalsIgnoreCase("SYSTEM")) {
                if (!existing.getAuthorId().equals(dto.getAuthorId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("Permission denied: You can only edit your own authored courses."));
                }
            }
        }
        Course updated = courseService.createOrPublishPlan(dto);
        return ResponseEntity.ok(ApiResponse.ok("Course plan updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String id, @RequestParam(required = false) String userId) {
        Optional<Course> existingOpt = courseService.getCourseById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Course not found"));
        }
        Course existing = existingOpt.get();
        if (userId != null && !userId.isBlank() 
                && existing.getAuthorId() != null && !existing.getAuthorId().equalsIgnoreCase("SYSTEM")) {
            if (!existing.getAuthorId().equals(userId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Permission denied: You can only delete your own authored courses."));
            }
        }
        boolean deleted = courseService.deleteCourse(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.ok("Course plan deleted successfully", null));
        }
        return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete course"));
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/toggle")
    public ResponseEntity<ApiResponse<Enrollment>> toggleLesson(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @RequestParam(defaultValue = "user_1") String userId) {
        return courseService.toggleLesson(userId, courseId, lessonId)
                .map(e -> ResponseEntity.ok(ApiResponse.ok("Lesson progress updated", e)))
                .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Unable to update lesson progress")));
    }

    @GetMapping("/{courseId}/enrollment/{userId}")
    public ResponseEntity<ApiResponse<Enrollment>> getEnrollment(
            @PathVariable String courseId,
            @PathVariable String userId) {
        return courseService.getUserEnrollment(userId, courseId)
                .map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(new Enrollment(userId, courseId))));
    }
}
