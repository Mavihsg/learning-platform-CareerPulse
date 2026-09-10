package com.learning.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.dto.QuizDto;
import com.learning.platform.dto.QuizDto.QuizGenerationRequestDto;
import com.learning.platform.dto.QuizDto.QuizQuestionDto;
import com.learning.platform.model.Course;
import com.learning.platform.model.CourseModule;
import com.learning.platform.model.Lesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class AiQuizService {

    private static final Logger log = LoggerFactory.getLogger(AiQuizService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent}")
    private String apiUrl;

    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-flash-latest",
            "gemini-3.7-flash",
            "gemini-3.1-flash-lite",
            "gemini-flash-lite-latest"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CourseService courseService;

    public AiQuizService(ObjectMapper objectMapper, CourseService courseService) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.courseService = courseService;
    }

    public QuizDto generateQuiz(QuizGenerationRequestDto request) {
        String mode = request.getMode() != null ? request.getMode().toUpperCase() : "COMPLETED_COURSES";
        int count = request.getQuestionCount() > 0 ? Math.min(10, Math.max(3, request.getQuestionCount())) : 5;
        String difficulty = request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "INTERMEDIATE";

        String title;
        String description;
        String systemPrompt;
        String quizId = "QUIZ_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if ("COMPLETED_COURSES".equals(mode)) {
            List<String> courseIds = request.getCourseIds();
            if (courseIds == null || courseIds.isEmpty()) {
                courseIds = List.of("PLAN_ADE_01", "PLAN_K8S_01", "COURSE_SPRING_CLOUD_01");
            }

            StringBuilder courseContext = new StringBuilder();
            List<String> courseTitles = new ArrayList<>();
            for (String cId : courseIds) {
                Course c = courseService.getCachedCourse(cId);
                if (c != null) {
                    courseTitles.add(c.getTitle());
                    courseContext.append("Course: ").append(c.getTitle()).append("\n");
                    if (c.getDescription() != null) courseContext.append("Summary: ").append(c.getDescription()).append("\n");
                    if (c.getModules() != null) {
                        for (CourseModule m : c.getModules()) {
                            courseContext.append("  Module: ").append(m.getTitle()).append("\n");
                            if (m.getLessons() != null) {
                                for (Lesson l : m.getLessons()) {
                                    courseContext.append("    - ").append(l.getTitle());
                                    if (l.getSummary() != null) courseContext.append(": ").append(l.getSummary());
                                    courseContext.append("\n");
                                }
                            }
                        }
                    }
                }
            }

            String joinedTitles = String.join(", ", courseTitles);
            title = courseTitles.size() == 1 ? courseTitles.get(0) + " Mastery Quiz" : "Completed Courses Knowledge Assessment";
            description = "Reinforce and test your retained knowledge across your completed coursework: " + joinedTitles;
            systemPrompt = buildCompletedCoursesPrompt(joinedTitles, courseContext.toString(), count, difficulty);

        } else if ("DAILY_CHALLENGE".equals(mode)) {
            title = "Daily Knowledge Sprint (" + LocalDate.now() + ")";
            description = "Daily 3-question streak challenge. Complete today's quiz to extend your active streak and claim +75 XP!";
            count = 3;
            systemPrompt = buildDailyChallengePrompt(count);

        } else {
            // CUSTOM_TOPIC
            String topic = (request.getCustomTopic() != null && !request.getCustomTopic().isBlank())
                    ? request.getCustomTopic().trim() : "Software Architecture & Best Practices";
            title = topic + " Knowledge Check";
            description = "AI-generated quiz focusing on " + topic + " at " + difficulty.toLowerCase() + " proficiency level.";
            systemPrompt = buildCustomTopicPrompt(topic, difficulty, count);
        }

        QuizDto quiz = fetchQuizFromGemini(systemPrompt);
        if (quiz != null && quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            quiz.setId(quizId);
            quiz.setTitle(title);
            quiz.setDescription(description);
            quiz.setQuizType(mode);
            quiz.setDifficultyLevel(difficulty);
            quiz.setTotalQuestions(quiz.getQuestions().size());
            quiz.setTotalXpReward(quiz.getQuestions().size() * 25);
            return quiz;
        }

        log.warn("Gemini generation returned empty or failed. Generating rich curated fallback quiz for mode: {}", mode);
        return createFallbackQuiz(mode, request, quizId, title, description, count, difficulty);
    }

    public QuizDto getDailyChallenge(String userId) {
        QuizGenerationRequestDto request = new QuizGenerationRequestDto();
        request.setMode("DAILY_CHALLENGE");
        request.setQuestionCount(3);
        request.setUserId(userId);
        return generateQuiz(request);
    }

    private QuizDto fetchQuizFromGemini(String systemPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Gemini API key is not configured. Using high-yield fallback quiz.");
            return null;
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", systemPrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 4096,
                        "responseMimeType", "application/json"
                )
        );

        List<String> urlsToTry = new ArrayList<>();
        urlsToTry.add(apiUrl);
        for (String model : FALLBACK_MODELS) {
            String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
            if (!urlsToTry.contains(fallbackUrl)) urlsToTry.add(fallbackUrl);
        }

        for (String endpointUrl : urlsToTry) {
            try {
                String fullUrl = endpointUrl + "?key=" + apiKey;
                log.info("Calling Gemini for Quiz Generation at: {}", endpointUrl);

                String responseBody = webClient.post()
                        .uri(fullUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(6))
                        .block();

                QuizDto parsed = parseGeminiQuizResponse(responseBody);
                if (parsed != null && parsed.getQuestions() != null && !parsed.getQuestions().isEmpty()) {
                    return parsed;
                }
            } catch (WebClientResponseException e) {
                log.warn("Gemini call failed with HTTP {}: {}", e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.warn("Gemini call error: {}", e.getMessage());
            }
        }

        return null;
    }

    private String buildCompletedCoursesPrompt(String titles, String context, int count, String difficulty) {
        return """
            You are an expert technical evaluator for Career Pulse, an enterprise engineering learning platform.
            
            The learner has completed the following course(s): %s
            Course curriculum context:
            %s
            
            Generate a %d-question multiple-choice technical assessment testing practical, retained engineering knowledge.
            Difficulty Level: %s.
            
            Return JSON in this EXACT schema:
            {
                "questions": [
                    {
                        "questionText": "A clear, realistic engineering problem statement or conceptual question",
                        "options": [
                            "Option A",
                            "Option B",
                            "Option C",
                            "Option D"
                        ],
                        "correctOptionIndex": 0,
                        "explanation": "Why this option is correct and why other options are suboptimal or incorrect in production.",
                        "xpPoints": 25,
                        "topic": "Core topic name"
                    }
                ]
            }
            
            Rules:
            - Exactly 4 options per question.
            - correctOptionIndex MUST be 0, 1, 2, or 3.
            - Ensure questions focus on real-world engineering trade-offs, architecture, and syntax.
            - Return ONLY valid raw JSON, with no markdown code blocks.
            """.formatted(titles, context, count, difficulty);
    }

    private String buildCustomTopicPrompt(String topic, String difficulty, int count) {
        return """
            You are an expert technical evaluator for Career Pulse, an enterprise engineering learning platform.
            
            Create a %d-question multiple-choice quiz on the topic: "%s"
            Difficulty Level: %s.
            
            Return JSON in this EXACT schema:
            {
                "questions": [
                    {
                        "questionText": "A clear, realistic engineering problem statement or conceptual question",
                        "options": [
                            "Option A",
                            "Option B",
                            "Option C",
                            "Option D"
                        ],
                        "correctOptionIndex": 0,
                        "explanation": "Comprehensive technical explanation detailing why the correct answer is right and correcting common misconceptions.",
                        "xpPoints": 25,
                        "topic": "%s"
                    }
                ]
            }
            
            Rules:
            - Exactly 4 options per question.
            - correctOptionIndex MUST be 0, 1, 2, or 3.
            - Practical scenarios, production best practices, and edge cases.
            - Return ONLY valid raw JSON, with no markdown code blocks.
            """.formatted(count, topic, difficulty, topic);
    }

    private String buildDailyChallengePrompt(int count) {
        return """
            You are the quiz master for Career Pulse daily streak challenges.
            
            Generate %d high-yield, quick-fire multiple-choice engineering questions across modern backend, cloud native, and data architectures (e.g. Docker/K8s, Postgres/SQL, Microservices, Java/Spring, System Design).
            
            Return JSON in this EXACT schema:
            {
                "questions": [
                    {
                        "questionText": "Question statement",
                        "options": ["A", "B", "C", "D"],
                        "correctOptionIndex": 0,
                        "explanation": "Brief explanation reinforcing key concept.",
                        "xpPoints": 25,
                        "topic": "Topic name"
                    }
                ]
            }
            
            Rules:
            - Exactly 4 options per question.
            - correctOptionIndex MUST be 0, 1, 2, or 3.
            - Return ONLY valid raw JSON, without markdown fences.
            """.formatted(count);
    }

    private QuizDto parseGeminiQuizResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : parts) {
                        if (part.has("text") && !part.has("thought")) {
                            sb.append(part.path("text").asText());
                        }
                    }
                    String text = sb.toString().trim();
                    if (text.isEmpty()) text = parts.get(0).path("text").asText().trim();

                    if (text.startsWith("```json")) text = text.substring(7);
                    else if (text.startsWith("```")) text = text.substring(3);
                    if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
                    text = text.trim();

                    int firstBrace = text.indexOf('{');
                    int lastBrace = text.lastIndexOf('}');
                    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                        text = text.substring(firstBrace, lastBrace + 1);
                    }

                    JsonNode quizNode = objectMapper.readTree(text);
                    JsonNode questionsArray = quizNode.path("questions");
                    if (questionsArray.isArray() && questionsArray.size() > 0) {
                        QuizDto dto = new QuizDto();
                        List<QuizQuestionDto> questions = new ArrayList<>();
                        int idx = 1;
                        for (JsonNode qNode : questionsArray) {
                            QuizQuestionDto q = new QuizQuestionDto();
                            q.setId("Q_" + idx++);
                            q.setQuestionText(qNode.path("questionText").asText("Technical Question"));
                            List<String> options = new ArrayList<>();
                            for (JsonNode opt : qNode.path("options")) {
                                options.add(opt.asText());
                            }
                            if (options.size() < 4) {
                                while (options.size() < 4) options.add("Additional Option " + (options.size() + 1));
                            }
                            q.setOptions(options.subList(0, 4));
                            q.setCorrectOptionIndex(Math.max(0, Math.min(3, qNode.path("correctOptionIndex").asInt(0))));
                            q.setExplanation(qNode.path("explanation").asText("Correct application of engineering design principles."));
                            q.setXpPoints(qNode.path("xpPoints").asInt(25));
                            q.setTopic(qNode.path("topic").asText("Engineering"));
                            questions.add(q);
                        }
                        dto.setQuestions(questions);
                        return dto;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini quiz response: {}", e.getMessage());
        }
        return null;
    }

    private QuizDto createFallbackQuiz(String mode, QuizGenerationRequestDto request, String quizId,
                                       String title, String description, int count, String difficulty) {
        QuizDto quiz = new QuizDto();
        quiz.setId(quizId);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setQuizType(mode);
        quiz.setDifficultyLevel(difficulty);

        List<QuizQuestionDto> pool = getCuratedQuestionPool();
        // Shuffle and pick requested count
        Collections.shuffle(pool);
        List<QuizQuestionDto> selected = new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
        for (int i = 0; i < selected.size(); i++) {
            selected.get(i).setId("Q_" + (i + 1));
        }

        quiz.setQuestions(selected);
        quiz.setTotalQuestions(selected.size());
        quiz.setTotalXpReward(selected.size() * 25);
        return quiz;
    }

    private List<QuizQuestionDto> getCuratedQuestionPool() {
        List<QuizQuestionDto> pool = new ArrayList<>();

        pool.add(new QuizQuestionDto(
                "Q1",
                "Why is columnar storage format (such as Apache Parquet) vastly superior to row-based formats (like CSV or JSON) for analytical OLAP workloads?",
                List.of(
                        "It reads all record columns sequentially, making row updates faster.",
                        "It allows projection pushdown to read only queried columns and enables heavy compression across identical data types.",
                        "It eliminates the need for foreign keys and primary keys in relational databases.",
                        "It executes single-record INSERT statements with lower latency."
                ),
                1,
                "Columnar formats store data of the same type contiguously on disk. Analytical queries rarely request all columns, so skipping unqueried columns drastically minimizes disk I/O and enables aggressive encoding techniques like dictionary and run-length compression.",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q2",
                "In Kubernetes, what is the key behavioral difference between a Readiness Probe and a Liveness Probe?",
                List.of(
                        "A failed readiness probe restarts the container, while a failed liveness probe removes the Pod from Service endpoints.",
                        "A failed liveness probe restarts the container, while a failed readiness probe removes the Pod from Service routing endpoints until it reports healthy.",
                        "Both probes restart the Pod immediately upon failure.",
                        "Readiness probes only execute once during initial startup, while liveness probes run indefinitely."
                ),
                1,
                "Liveness probes detect unrecoverable deadlocks and trigger container restarts. Readiness probes detect temporary unavailability (e.g. loading cache, heavy queue) and isolate traffic without restarting the container.",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q3",
                "When implementing the Circuit Breaker pattern with Resilience4j in a microservice gateway, what transition occurs when failure rates drop back below the threshold in HALF_OPEN state?",
                List.of(
                        "The circuit transitions from HALF_OPEN to CLOSED, resuming normal traffic passthrough.",
                        "The circuit transitions to OPEN, blocking all downstream requests.",
                        "The circuit restarts the underlying container pod.",
                        "The circuit immediately returns HTTP 503 Service Unavailable."
                ),
                0,
                "In the HALF_OPEN state, the circuit breaker allows a configurable trial batch of requests. If they succeed above the threshold, the circuit transitions to CLOSED (healthy). If failures persist, it reverts to OPEN.",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q4",
                "What is the primary architectural purpose of writing an Architecture Decision Record (ADR)?",
                List.of(
                        "To generate automatic OpenAPI Swagger schemas for REST clients.",
                        "To capture the context, options evaluated, chosen decision, and resulting consequences of structural decisions for future teams.",
                        "To replace pull request reviews and Git commit messages.",
                        "To track hourly developer billable time and sprint velocity."
                ),
                1,
                "ADRs document significant architectural choices along with trade-offs and consequences. This provides invaluable institutional memory for onboarding and future evolutions.",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q5",
                "In PostgreSQL query optimization, when will the query planner choose a Sequential Scan over an existing B-Tree index scan?",
                List.of(
                        "When the table contains more than 10 million rows.",
                        "When the planner estimates that the query will retrieve a large percentage of total table pages, making random I/O via index slower than sequential I/O.",
                        "When the query uses a WHERE clause with an equality operator (=).",
                        "PostgreSQL never uses a sequential scan if an index exists."
                ),
                1,
                "Indexes involve random page reads. If a query returns 30-50%+ of a table, scanning sequential disk pages linearly is significantly faster than repeated random index lookups.",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q6",
                "Under the CAP theorem, in the event of an unavoidable network partition between distributed database nodes, what choice must the system make?",
                List.of(
                        "Trade off between Consistency (linearizable reads) and Availability (answering every non-failing node request).",
                        "Sacrifice Partition Tolerance by stopping the network traffic.",
                        "Guarantee all three: Consistency, Availability, and Partition Tolerance simultaneously.",
                        "Disable database transactions completely."
                ),
                0,
                "When a network partition occurs, a distributed system must choose between returning an error or stale data (choosing Availability over Consistency) or refusing writes/stale reads until partition resolves (choosing Consistency over Availability).",
                25
        ));

        pool.add(new QuizQuestionDto(
                "Q7",
                "What is the primary advantage of Docker multi-stage builds in production container pipelines?",
                List.of(
                        "They allow running multiple web servers inside a single container.",
                        "They drastically reduce final image size and attack surface by leaving build tools, compilers, and source files out of the runtime image.",
                        "They automatically convert x86 images to ARM architecture.",
                        "They remove the need for Kubernetes Pod manifests."
                ),
                1,
                "Multi-stage builds allow compiling code with heavy SDKs (JDK, Go, Maven) in a build stage and copying only the compiled binary or JAR into a lightweight runtime Alpine/Distroless container.",
                25
        ));

        return pool;
    }
}
