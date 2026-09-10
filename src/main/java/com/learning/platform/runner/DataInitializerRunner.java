package com.learning.platform.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.model.*;
import com.learning.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerRunner.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final SkillRepository skillRepository;
    private final RoleBenchmarkRepository roleBenchmarkRepository;
    private final CourseRepository courseRepository;
    private final BadgeRepository badgeRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyLogRepository studyLogRepository;
    private final DiscussionThreadRepository discussionThreadRepository;
    private final DiscussionReplyRepository discussionReplyRepository;

    private boolean initialized = false;

    public DataInitializerRunner(ResourceLoader resourceLoader,
                                 ObjectMapper objectMapper,
                                 SkillRepository skillRepository,
                                 RoleBenchmarkRepository roleBenchmarkRepository,
                                 CourseRepository courseRepository,
                                 BadgeRepository badgeRepository,
                                 QuestRepository questRepository,
                                 UserRepository userRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 StudyLogRepository studyLogRepository,
                                 DiscussionThreadRepository discussionThreadRepository,
                                 DiscussionReplyRepository discussionReplyRepository) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.skillRepository = skillRepository;
        this.roleBenchmarkRepository = roleBenchmarkRepository;
        this.courseRepository = courseRepository;
        this.badgeRepository = badgeRepository;
        this.questRepository = questRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyLogRepository = studyLogRepository;
        this.discussionThreadRepository = discussionThreadRepository;
        this.discussionReplyRepository = discussionReplyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure all existing courses have authorId populated
        backfillCourseAuthors();

        // Always ensure badge catalog is loaded/updated
        try {
            loadBadges();
        } catch (Exception e) {
            log.warn("Badge seeding notice: {}", e.getMessage());
        }

        // Always ensure discussion threads are seeded if none exist
        try {
            seedDiscussions();
        } catch (Exception e) {
            log.warn("Discussion seeding notice: {}", e.getMessage());
        }

        if (courseRepository.count() > 0) {
            log.info("Database already initialized with courses ({}) and skills ({}). Skipping initial seed.",
                    courseRepository.count(), skillRepository.count());
            this.initialized = true;
            return;
        }

        log.info("Starting initial data seeding from classpath:data/...");
        try {
            // 1. Load Skills
            loadSkills();

            // 2. Load Roles
            loadRoles();

            // 3. Load Badges
            loadBadges();

            // 4. Load Quests
            loadQuests();

            // 5. Load Courses & Lessons & Quizzes
            loadCourses();

            // 6. Load Users & Initial Skill Profiles
            loadUsers();

            // 7. Seed Sample Enrollments & Study Logs for Analytics
            seedEnrollmentsAndLogs();

            this.initialized = true;
            log.info("Flat-file seeding completed successfully! [Skills: {}, Roles: {}, Courses: {}, Badges: {}, Quests: {}, Users: {}]",
                    skillRepository.count(),
                    roleBenchmarkRepository.count(),
                    courseRepository.count(),
                    badgeRepository.count(),
                    questRepository.count(),
                    userRepository.count());

        } catch (Exception e) {
            log.error("Fatal error during flat-file seed ingestion: {}", e.getMessage(), e);
            throw new RuntimeException("Flat-file seed ingestion failed", e);
        }
    }

    private void loadSkills() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/skills.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<Skill> skills = objectMapper.readValue(is, new TypeReference<List<Skill>>() {});
                skillRepository.saveAll(skills);
                log.info("Seeded {} skills into H2 in-memory store.", skills.size());
            }
        }
    }

    private void loadRoles() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/roles.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<RoleBenchmark> roles = objectMapper.readValue(is, new TypeReference<List<RoleBenchmark>>() {});
                roleBenchmarkRepository.saveAll(roles);
                log.info("Seeded {} role benchmarks into H2 in-memory store.", roles.size());
            }
        }
    }

    private void loadBadges() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/badges.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<Badge> badges = objectMapper.readValue(is, new TypeReference<List<Badge>>() {});
                badgeRepository.saveAll(badges);
                log.info("Seeded {} badges into H2 in-memory store.", badges.size());
            }
        }
    }

    private void loadQuests() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/quests.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<Quest> quests = objectMapper.readValue(is, new TypeReference<List<Quest>>() {});
                questRepository.saveAll(quests);
                log.info("Seeded {} quests into H2 in-memory store.", quests.size());
            }
        }
    }

    private void backfillCourseAuthors() {
        try {
            List<Course> existingCourses = courseRepository.findAll();
            boolean changed = false;
            for (Course course : existingCourses) {
                if (course.getAuthorId() == null || course.getAuthorId().isBlank()) {
                    if ("Shivam Gupta".equalsIgnoreCase(course.getAuthorName()) || (course.getId() != null && course.getId().startsWith("PLAN_A"))) {
                        course.setAuthorId("user_1");
                        course.setAuthorName("Shivam Gupta");
                    } else {
                        course.setAuthorId("SYSTEM");
                    }
                    changed = true;
                }
            }
            if (changed) {
                courseRepository.saveAll(existingCourses);
                log.info("Backfilled authorId for {} existing courses in database.", existingCourses.size());
            }
        } catch (Exception e) {
            log.warn("Non-critical: could not backfill course author IDs: {}", e.getMessage());
        }
    }

    private void loadCourses() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/courses.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<Course> courses = objectMapper.readValue(is, new TypeReference<List<Course>>() {});
                for (Course course : courses) {
                    if (course.getAuthorId() == null || course.getAuthorId().isBlank()) {
                        if ("Shivam Gupta".equalsIgnoreCase(course.getAuthorName()) || (course.getId() != null && course.getId().startsWith("PLAN_A"))) {
                            course.setAuthorId("user_1");
                            course.setAuthorName("Shivam Gupta");
                        } else {
                            course.setAuthorId("SYSTEM");
                        }
                    }
                    if (course.getModules() != null) {
                        for (CourseModule mod : course.getModules()) {
                            mod.setCourse(course);
                            if (mod.getLessons() != null) {
                                for (Lesson les : mod.getLessons()) {
                                    les.setCourseModule(mod);
                                }
                            }
                        }
                    }
                    if (course.getQuiz() != null) {
                        Quiz quiz = course.getQuiz();
                        quiz.setCourse(course);
                        if (quiz.getQuestions() != null) {
                            for (QuizQuestion q : quiz.getQuestions()) {
                                q.setQuiz(quiz);
                            }
                        }
                    }
                }
                courseRepository.saveAll(courses);
                log.info("Seeded {} courses with modules, lessons & quizzes into H2 in-memory store.", courses.size());
            }
        }
    }

    private void loadUsers() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:data/users.json");
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                List<User> users = objectMapper.readValue(is, new TypeReference<List<User>>() {});
                for (User user : users) {
                    if (user.getSkills() != null) {
                        for (UserSkill us : user.getSkills()) {
                            us.setUser(user);
                        }
                    }
                }
                userRepository.saveAll(users);
                log.info("Seeded {} learner profiles into H2 in-memory store.", users.size());
            }
        }
    }

    private void seedEnrollmentsAndLogs() {
        if (enrollmentRepository.count() > 0) {
            log.info("Enrollments already present. Skipping initial enrollment seed.");
            return;
        }

        // Seed Manish / Alex Chen Core Track Enrollment
        Enrollment alexCore = new Enrollment("user_1", "PLAN_ADE_01");
        alexCore.setIsCoreTrack(true);
        alexCore.setProgressPercentage(38);
        alexCore.setTotalLessons(13);
        alexCore.setCompletedLessonsCount(5);
        alexCore.setRemainingHours(6.0);
        alexCore.setTargetDate("12 Nov 2026");

        List<String> completedLessons = new ArrayList<>();
        completedLessons.add("LES_ADE_01");
        completedLessons.add("LES_ADE_02");
        completedLessons.add("LES_ADE_03");
        completedLessons.add("LES_ADE_04");
        completedLessons.add("LES_ADE_05");
        alexCore.setCompletedLessonIds(completedLessons);

        List<String> completedMods = new ArrayList<>();
        completedMods.add("MOD_ADE_01");
        alexCore.setCompletedModuleIds(completedMods);

        enrollmentRepository.save(alexCore);

        // Seed Alex's other plans
        Enrollment ae = new Enrollment("user_1", "PLAN_AE_01");
        ae.setIsCoreTrack(false);
        ae.setProgressPercentage(72);
        enrollmentRepository.save(ae);

        Enrollment tw = new Enrollment("user_1", "PLAN_TW_01");
        tw.setIsCoreTrack(false);
        tw.setProgressPercentage(100);
        tw.setStatus(Enrollment.Status.COMPLETED);
        tw.setTotalLessons(2);
        tw.setCompletedLessonsCount(2);
        tw.setRemainingHours(0.0);
        tw.setCompletedAt(LocalDateTime.now().minusDays(2).withHour(14).withMinute(30));
        tw.setCompletedLessonIds(new ArrayList<>(List.of("LES_TW_01", "LES_TW_02")));
        enrollmentRepository.save(tw);

        Enrollment k8s = new Enrollment("user_1", "PLAN_K8S_01");
        k8s.setIsCoreTrack(false);
        k8s.setProgressPercentage(8);
        enrollmentRepository.save(k8s);

        // Seed Study Logs for Alex (Mon-Sun: 250 min total, target met on Friday)
        LocalDate today = LocalDate.now();
        studyLogRepository.save(new StudyLog("user_1", "M", 35, today.minusDays(6)));
        studyLogRepository.save(new StudyLog("user_1", "T", 0, today.minusDays(5)));
        studyLogRepository.save(new StudyLog("user_1", "W", 55, today.minusDays(4)));
        studyLogRepository.save(new StudyLog("user_1", "TH", 30, today.minusDays(3)));
        studyLogRepository.save(new StudyLog("user_1", "F", 85, today.minusDays(2)));
        studyLogRepository.save(new StudyLog("user_1", "SA", 0, today.minusDays(1)));
        studyLogRepository.save(new StudyLog("user_1", "SU", 45, today));
 
        log.info("Seeded sample enrollments and study activity logs for analytics.");
    }

    private void seedDiscussions() {
        if (discussionThreadRepository.count() > 0) {
            log.info("Discussion threads already present ({}). Skipping seed.", discussionThreadRepository.count());
            return;
        }

        log.info("Seeding initial course discussion threads and replies...");

        // Thread 1: StatefulSets vs Deployments
        DiscussionThread t1 = new DiscussionThread(
                "THREAD_INIT_01",
                "PLAN_K8S_01",
                "Production Kubernetes Deployment & Operations",
                "user_2",
                "Priya Sharma",
                "👩‍💻",
                "DevOps Engineer",
                "When should we favor StatefulSets over Deployments in production Kafka clusters?",
                "I'm designing a high-throughput event streaming architecture using Apache Kafka on our Kubernetes cluster. The course mentions StatefulSets are preferred for stateful storage, but our storage team has provisioned Ceph CSI storage. What are the key advantages of StatefulSets here compared to standard Deployments with persistent volumes?"
        );
        t1.setTags(new ArrayList<>(List.of("Kubernetes", "Kafka", "StatefulSet", "Storage")));
        t1.setUpvotes(6);
        t1.setUpvotedUserIds(new ArrayList<>(List.of("user_1", "user_3")));
        t1.setResolved(true);
        t1.setAcceptedReplyId("REPLY_INIT_01_USER");

        DiscussionReply t1r1 = new DiscussionReply(
                "REPLY_INIT_01_AI",
                t1,
                "ai_mentor",
                "CareerPulse AI Mentor",
                "🤖",
                "Staff Technical Mentor",
                "### Direct Answer & Core Architecture\\nIn Kubernetes, **StatefulSets** provide three critical guarantees that standard Deployments cannot offer for distributed databases and messaging brokers like Kafka:\\n\\n1. **Predictable Network Identity**: StatefulSets assign stable, zero-indexed hostnames (`kafka-0`, `kafka-1`, `kafka-2`) paired with a Headless Service for direct DNS resolution (`kafka-0.kafka-service.default.svc.cluster.local`). Kafka brokers rely on this for cluster metadata and leader election.\\n2. **Dedicated Storage per Replica**: Using `volumeClaimTemplates`, Kubernetes automatically provisions a separate PersistentVolumeClaim (PVC) for *each* ordinal pod. Scaling down never deletes PVCs, safeguarding data against accidental corruption.\\n3. **Ordered Rollouts & Graceful Shutdown**: StatefulSet pods spin up sequentially (`0` must be healthy before `1` starts) and terminate in reverse order (`N-1` down to `0`), preserving quorum during cluster updates.",
                true
        );
        t1r1.setUpvotes(5);

        DiscussionReply t1r2 = new DiscussionReply(
                "REPLY_INIT_01_USER",
                t1,
                "user_1",
                "Shivam Gupta",
                "⚡",
                "Senior Backend Engineer",
                "Can confirm what AI Mentor noted. In our staging cluster, when we initially attempted Deployments, broker pods would randomly restart on new nodes and mount whatever volume was free, causing log partition desync. Switching to StatefulSets with volumeClaimTemplates fixed broker ID pinning completely.",
                false
        );
        t1r2.setUpvotes(4);
        t1r2.setAcceptedSolution(true);

        t1.addReply(t1r1);
        t1.addReply(t1r2);
        discussionThreadRepository.save(t1);

        // Thread 2: Circuit Breaker Tuning
        DiscussionThread t2 = new DiscussionThread(
                "THREAD_INIT_02",
                "COURSE_SPRING_CLOUD_01",
                "Microservices Architecture with Spring Cloud",
                "user_3",
                "Marcus Vance",
                "👨‍💻",
                "Cloud Solutions Architect",
                "Circuit Breaker threshold tuning: Sliding Window vs Time Window in Resilience4j?",
                "In high-traffic microservices handling >2,000 req/sec, is it safer to configure Resilience4j with a count-based sliding window or a time-based sliding window? When sudden transient spikes occur, how do you prevent false-positive breaker trips?"
        );
        t2.setTags(new ArrayList<>(List.of("Spring Cloud", "Resilience4j", "Microservices", "High Traffic")));
        t2.setUpvotes(4);
        t2.setUpvotedUserIds(new ArrayList<>(List.of("user_1")));
        t2.setResolved(false);

        DiscussionReply t2r1 = new DiscussionReply(
                "REPLY_INIT_02_AI",
                t2,
                "ai_mentor",
                "CareerPulse AI Mentor",
                "🤖",
                "Staff Technical Mentor",
                "### Resilience4j Sliding Window Strategy\\n\\nFor high-throughput systems (>2,000 req/s), a **COUNT_BASED** sliding window with a reasonable capacity (e.g. `slidingWindowSize=100` to `200`) and high `minimumNumberOfCalls` (e.g. 50-80) is generally much more resilient than `TIME_BASED`:\\n\\n- `COUNT_BASED`: Fast reaction time based directly on request density. Smooths out temporary 100ms blips if the overall failure rate remains under 50%.\\n- `TIME_BASED`: Requires pre-allocating ring-buffer buckets per second, which can trigger sudden state transitions during traffic surges.\\n\\n**Pro Tip**: Always configure `slowCallRateThreshold` and `slowCallDurationThreshold` in tandem with `failureRateThreshold` to catch hung thread pools before complete timeouts occur.",
                true
        );
        t2r1.setUpvotes(3);
        t2.addReply(t2r1);
        discussionThreadRepository.save(t2);

        // Thread 3: Spark Data Skew
        DiscussionThread t3 = new DiscussionThread(
                "THREAD_INIT_03",
                "PLAN_ADE_01",
                "Advanced Data Engineering with Apache Spark",
                "user_1",
                "Shivam Gupta",
                "⚡",
                "Senior Backend Engineer",
                "Best strategy to mitigate severe data skew during Spark Join operations?",
                "One of our daily aggregation jobs hangs at 99% for 45 minutes because 60% of incoming log events share a default user_id (null or guest). What is the cleanest approach to balance this shuffle without corrupting downstream aggregations?"
        );
        t3.setTags(new ArrayList<>(List.of("Spark", "Big Data", "Data Skew", "Performance")));
        t3.setUpvotes(8);
        t3.setUpvotedUserIds(new ArrayList<>(List.of("user_2", "user_3")));
        t3.setResolved(false);

        DiscussionReply t3r1 = new DiscussionReply(
                "REPLY_INIT_03_AI",
                t3,
                "ai_mentor",
                "CareerPulse AI Mentor",
                "🤖",
                "Staff Technical Mentor",
                "### Mitigating Partition Skew in Spark\\n\\nWhen a single key dominates a large dataset, standard hash partitioning dumps 60% of your records into a single executor. Here are the top three industrial strategies:\\n\\n1. **Separate Null/Default Handling**: Filter out null/guest keys into a separate stream, process the clean skewed keys with salting, and `union` the null records post-join.\\n2. **Key Salting**: Append a pseudo-random integer (0..N-1) to the skewed key on the fact table, and replicate the dimension table entries N times with matching salts.\\n3. **Adaptive Query Execution (AQE)**: In Spark 3.x, ensure `spark.sql.adaptive.skewJoin.enabled = true`. Spark detects skewed partitions at runtime and splits them dynamically across worker cores.",
                true
        );
        t3r1.setUpvotes(7);
        t3.addReply(t3r1);
        discussionThreadRepository.save(t3);

        log.info("Successfully seeded {} discussion threads with sample replies and AI Mentor answers.", discussionThreadRepository.count());
    }

    public boolean isInitialized() {
        return initialized;
    }
}
