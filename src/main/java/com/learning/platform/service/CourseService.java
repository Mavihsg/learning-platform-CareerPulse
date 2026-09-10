package com.learning.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learning.platform.dto.PlanBuilderRequestDto;
import com.learning.platform.model.Course;
import com.learning.platform.model.CourseModule;
import com.learning.platform.model.Enrollment;
import com.learning.platform.model.Lesson;
import com.learning.platform.model.StudyLog;
import com.learning.platform.repository.CourseRepository;
import com.learning.platform.repository.EnrollmentRepository;
import com.learning.platform.repository.StudyLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyLogRepository studyLogRepository;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final Map<String, Course> courseCache = new ConcurrentHashMap<>();

    public CourseService(CourseRepository courseRepository,
                         EnrollmentRepository enrollmentRepository,
                         StudyLogRepository studyLogRepository,
                         ObjectMapper objectMapper,
                         UserService userService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyLogRepository = studyLogRepository;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @PostConstruct
    public void initCourseCache() {
        refreshCourseCache();
    }

    public synchronized void refreshCourseCache() {
        try {
            List<Course> all = courseRepository.findAll();
            courseCache.clear();
            for (Course c : all) {
                courseCache.put(c.getId(), c);
            }
            log.info("Initialized in-memory courseCache with {} courses.", courseCache.size());
        } catch (Exception e) {
            log.warn("Failed to refresh courseCache: {}", e.getMessage());
        }
    }

    public Course getCachedCourse(String courseId) {
        if (courseCache.isEmpty()) {
            refreshCourseCache();
        }
        return courseCache.get(courseId);
    }

    public Map<String, Course> getCourseMap() {
        if (courseCache.isEmpty()) {
            refreshCourseCache();
        }
        return courseCache;
    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(getCourseMap().values());
    }

    public Optional<Course> getCourseById(String id) {
        return Optional.ofNullable(getCachedCourse(id));
    }

    public List<Course> getCoursesByCategory(String category) {
        return getAllCourses().stream()
                .filter(c -> category.equalsIgnoreCase(c.getCategory()))
                .toList();
    }

    public List<Course> getCoursesByDifficulty(String difficulty) {
        return getAllCourses().stream()
                .filter(c -> difficulty.equalsIgnoreCase(c.getDifficultyLevel()))
                .toList();
    }

    /**
     * Returns all courses the user is enrolled in, along with enrollment info.
     * Uses in-memory course map to completely eliminate N+1 round-trips.
     */
    @Cacheable(value = "enrolled_courses", key = "#userId")
    public List<EnrolledCourseInfo> getEnrolledCourses(String userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        List<EnrolledCourseInfo> result = new ArrayList<>();
        Map<String, Course> courseMap = getCourseMap();

        for (Enrollment enrollment : enrollments) {
            Course course = courseMap.get(enrollment.getCourseId());
            if (course != null) {
                result.add(new EnrolledCourseInfo(course, enrollment));
            }
        }

        return result;
    }

    /**
     * Simple wrapper for enrolled course + enrollment progress.
     */
    public static class EnrolledCourseInfo {
        private Course course;
        private Enrollment enrollment;

        public EnrolledCourseInfo(Course course, Enrollment enrollment) {
            this.course = course;
            this.enrollment = enrollment;
        }

        public Course getCourse() { return course; }
        public void setCourse(Course course) { this.course = course; }
        public Enrollment getEnrollment() { return enrollment; }
        public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }
    }

    @Transactional
    @CacheEvict(value = {"courses", "course", "dashboard", "enrolled_courses"}, allEntries = true)
    public Course createOrPublishPlan(PlanBuilderRequestDto dto) {
        String id = (dto.getId() != null && !dto.getId().isBlank()) ? dto.getId() : "COURSE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Course course = courseRepository.findById(id).orElse(new Course());
        
        course.setId(id);
        course.setTitle(dto.getTitle() != null && !dto.getTitle().isBlank() ? dto.getTitle() : "Untitled Plan");
        course.setTrack(dto.getTrack() != null && !dto.getTrack().isBlank() ? dto.getTrack() : "Engineering");
        course.setCategory(dto.getCategory() != null ? dto.getCategory() : "Backend");
        course.setDifficultyLevel(dto.getDifficultyLevel() != null ? dto.getDifficultyLevel() : "INTERMEDIATE");
        if (dto.getAuthorName() != null && !dto.getAuthorName().isBlank()) {
            course.setAuthorName(dto.getAuthorName());
        } else if (course.getAuthorName() == null) {
            course.setAuthorName("Shivam Gupta");
        }
        if (dto.getAuthorId() != null && !dto.getAuthorId().isBlank()) {
            course.setAuthorId(dto.getAuthorId());
        } else if (course.getAuthorId() == null) {
            if ("Shivam Gupta".equalsIgnoreCase(course.getAuthorName())) {
                course.setAuthorId("user_1");
            }
        }
        course.setStatus(dto.getStatus() != null ? dto.getStatus() : "PUBLISHED");
        course.setRating(4.9);
        course.setIcon("layers");

        // Clear existing modules if editing
        if (course.getModules() != null) {
            course.getModules().clear();
        } else {
            course.setModules(new ArrayList<>());
        }

        int totalMinutes = 0;
        int totalXp = 0;

        if (dto.getModules() != null) {
            int modIdx = 1;
            for (PlanBuilderRequestDto.ModuleDto mDto : dto.getModules()) {
                String modId = id + "_MOD_" + modIdx;
                CourseModule mod = new CourseModule(modId, mDto.getTitle(), "Module " + modIdx + " of " + course.getTitle(), 0, 50, modIdx);
                mod.setCourse(course);

                if (mDto.getLessons() != null) {
                    int lesIdx = 1;
                    int modMin = 0;
                    for (PlanBuilderRequestDto.LessonDto lDto : mDto.getLessons()) {
                        String lesId = (lDto.getId() != null && !lDto.getId().isBlank()) ? lDto.getId() : modId + "_LES_" + lesIdx;
                        int dur = lDto.getDurationMinutes() > 0 ? lDto.getDurationMinutes() : 20;
                        int xp = 25;
                        Lesson lesson = new Lesson(lesId, lDto.getTitle(), lDto.getSummary(), lDto.getResourceType() != null ? lDto.getResourceType() : "READING", dur, lesIdx, xp);
                        lesson.setVideoUrl(lDto.getVideoUrl());
                        lesson.setContent(lDto.getContent());
                        mod.addLesson(lesson);
                        modMin += dur;
                        totalMinutes += dur;
                        totalXp += xp;
                        lesIdx++;
                    }
                    mod.setDurationMinutes(modMin);
                }

                course.addModule(mod);
                modIdx++;
            }
        }

        course.setEstimatedHours(Math.max(1, (int) Math.ceil(totalMinutes / 60.0)));
        course.setXpReward(Math.max(100, totalXp));

        Course saved = courseRepository.save(course);
        refreshCourseCache();
        syncCoursesToDisk();
        return saved;
    }

    @Transactional
    @CacheEvict(value = {"courses", "course", "dashboard", "enrolled_courses"}, allEntries = true)
    public boolean deleteCourse(String id) {
        if (courseRepository.existsById(id)) {
            courseRepository.deleteById(id);
            refreshCourseCache();
            syncCoursesToDisk();
            return true;
        }
        return false;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "dashboard", key = "#userId"),
            @CacheEvict(value = "enrolled_courses", key = "#userId"),
            @CacheEvict(value = "weekly_activity", key = "#userId"),
            @CacheEvict(value = "leaderboard", allEntries = true)
    })
    public Optional<Enrollment> toggleLesson(String userId, String courseId, String lessonId) {
        List<Enrollment> list = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        Enrollment enrollment;
        if (list.isEmpty()) {
            enrollment = new Enrollment(userId, courseId);
            enrollment.setTotalLessons(13);
            enrollment.setCompletedLessonsCount(0);
            enrollment.setRemainingHours(6.0);
            enrollment.setIsCoreTrack(false);
        } else {
            enrollment = list.get(0);
            // Deduplicate safely if multiple rows exist in DB
            if (list.size() > 1) {
                for (int i = 1; i < list.size(); i++) {
                    Enrollment dup = list.get(i);
                    enrollment.getLessonSet().addAll(dup.getLessonSet());
                    enrollmentRepository.delete(dup);
                }
            }
        }

        // In-place mutation preserves Hibernate collection wrapper, generating 1 single delta SQL statement
        boolean isCompleting = enrollment.toggleLessonId(lessonId);
        int completedCount = enrollment.getLessonSet().size();
        enrollment.setCompletedLessonsCount(completedCount);

        // In-memory course lookup (0ms, no DB query)
        Course c = getCachedCourse(courseId);

        int total = 13;
        double totalHours = 6.0;
        int toggledLessonMinutes = 0;

        if (c != null) {
            int count = 0;
            int totalMins = 0;
            if (c.getModules() != null) {
                for (CourseModule m : c.getModules()) {
                    if (m.getLessons() != null) {
                        count += m.getLessons().size();
                        for (Lesson l : m.getLessons()) {
                            totalMins += l.getDurationMinutes();
                            if (l.getId().equals(lessonId)) {
                                toggledLessonMinutes = l.getDurationMinutes();
                            }
                        }
                    }
                }
            }
            if (count > 0) total = count;
            if (totalMins > 0) totalHours = totalMins / 60.0;
        }

        // Asynchronous study logging: eliminates 2 sequential remote DB roundtrips from HTTP response
        if (isCompleting && toggledLessonMinutes > 0) {
            final int mins = toggledLessonMinutes;
            CompletableFuture.runAsync(() -> {
                try {
                    logStudyMinutes(userId, mins);
                } catch (Exception e) {
                    log.warn("Async logStudyMinutes error: {}", e.getMessage());
                }
            });
        }

        enrollment.setTotalLessons(total);
        int pct = total > 0 ? (int) Math.round(((double) completedCount / total) * 100) : 0;
        enrollment.setProgressPercentage(pct);

        double remaining = totalHours * (1.0 - ((double) completedCount / total));
        enrollment.setRemainingHours(Math.round(remaining * 10.0) / 10.0);

        if (pct >= 100) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
            if (!enrollment.isXpAwarded()) {
                int reward = (c != null && c.getXpReward() > 0) ? c.getXpReward() : 200;
                enrollment.setXpAwarded(true);
                CompletableFuture.runAsync(() -> {
                    try {
                        userService.awardCourseCompletionXp(userId, reward);
                        log.info("Awarded {} course completion XP to user {} for plan {}", reward, userId, courseId);
                    } catch (Exception e) {
                        log.warn("Async awardCourseCompletionXp error: {}", e.getMessage());
                    }
                });
            }
        } else {
            enrollment.setStatus(Enrollment.Status.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        }

        return Optional.of(enrollmentRepository.save(enrollment));
    }

    /**
     * Logs study minutes for a user on the current day.
     */
    private void logStudyMinutes(String userId, int minutes) {
        LocalDate today = LocalDate.now();
        Optional<StudyLog> existing = studyLogRepository.findByUserIdAndLogDate(userId, today);

        if (existing.isPresent()) {
            StudyLog sl = existing.get();
            sl.setMinutesLogged(sl.getMinutesLogged() + minutes);
            studyLogRepository.save(sl);
        } else {
            DayOfWeek dow = today.getDayOfWeek();
            String dayCode;
            switch (dow) {
                case MONDAY: dayCode = "M"; break;
                case TUESDAY: dayCode = "T"; break;
                case WEDNESDAY: dayCode = "W"; break;
                case THURSDAY: dayCode = "TH"; break;
                case FRIDAY: dayCode = "F"; break;
                case SATURDAY: dayCode = "SA"; break;
                case SUNDAY: dayCode = "SU"; break;
                default: dayCode = "M";
            }
            StudyLog newLog = new StudyLog(userId, dayCode, minutes, today);
            studyLogRepository.save(newLog);
        }
        log.info("Logged {} study minutes for user {} on {}", minutes, userId, today);
    }

    public Optional<Enrollment> getUserEnrollment(String userId, String courseId) {
        List<Enrollment> list = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        Course c = getCachedCourse(courseId);
        if (c != null) {
            Enrollment preview = new Enrollment(userId, courseId);
            int total = 0;
            int totalMins = 0;
            if (c.getModules() != null) {
                for (CourseModule m : c.getModules()) {
                    if (m.getLessons() != null) {
                        total += m.getLessons().size();
                        for (Lesson l : m.getLessons()) {
                            totalMins += l.getDurationMinutes();
                        }
                    }
                }
            }
            preview.setTotalLessons(total > 0 ? total : 13);
            preview.setCompletedLessonsCount(0);
            preview.setRemainingHours(totalMins > 0 ? (Math.round((totalMins / 60.0) * 10.0) / 10.0) : 6.0);
            preview.setProgressPercentage(0);
            preview.setIsCoreTrack(false);
            return Optional.of(preview);
        }
        return Optional.empty();
    }

    public void syncCoursesToDisk() {
        try {
            List<Course> all = courseRepository.findAll();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Write to src/main/resources/data/courses.json
            File srcFile = new File("src/main/resources/data/courses.json");
            if (srcFile.getParentFile().exists()) {
                mapper.writeValue(srcFile, all);
                log.info("Synchronized {} courses to disk: {}", all.size(), srcFile.getAbsolutePath());
            }

            // Write to target/classes/data/courses.json if exists
            File targetFile = new File("target/classes/data/courses.json");
            if (targetFile.exists()) {
                mapper.writeValue(targetFile, all);
            }
        } catch (Exception e) {
            log.warn("Disk sync skipped or failed (non-critical): {}", e.getMessage());
        }
    }
}
