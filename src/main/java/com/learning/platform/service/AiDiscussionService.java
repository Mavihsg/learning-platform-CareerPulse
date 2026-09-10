package com.learning.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.model.Course;
import com.learning.platform.model.CourseModule;
import com.learning.platform.model.DiscussionThread;
import com.learning.platform.model.Lesson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiDiscussionService {

    private static final Logger log = LoggerFactory.getLogger(AiDiscussionService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent}")
    private String apiUrl;

    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-3.1-flash-lite"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CourseService courseService;

    public AiDiscussionService(ObjectMapper objectMapper, CourseService courseService) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
        .build();
        this.objectMapper = objectMapper;
        this.courseService = courseService;
    }

    public String generateAiAnswer(DiscussionThread thread) {
        if (thread == null) {
            return "No thread context provided for AI Mentor.";
        }

        String courseTitle = thread.getCourseTitle() != null ? thread.getCourseTitle() : "Software Engineering";
        String lessonInfo = thread.getLessonTitle() != null && !thread.getLessonTitle().isBlank()
                ? "Lesson Context: " + thread.getLessonTitle()
                : "";

        StringBuilder courseSummary = new StringBuilder();
        try {
            Course course = courseService.getCachedCourse(thread.getCourseId());
            if (course != null) {
                if (course.getDescription() != null) {
                    courseSummary.append("Course Overview: ").append(course.getDescription()).append("\n");
                }
                if (course.getModules() != null && !course.getModules().isEmpty()) {
                    courseSummary.append("Key Syllabus Topics: ");
                    List<String> moduleTitles = new ArrayList<>();
                    for (CourseModule mod : course.getModules()) {
                        moduleTitles.add(mod.getTitle());
                    }
                    courseSummary.append(String.join(", ", moduleTitles)).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve course metadata for AI discussion answer: {}", e.getMessage());
        }

        String prompt = buildPrompt(thread, courseTitle, lessonInfo, courseSummary.toString());

        String aiResponse = fetchAnswerFromGemini(prompt);
        if (aiResponse != null && !aiResponse.isBlank()) {
            return aiResponse;
        }

        log.info("Gemini API not available or empty; using domain-grounded intelligent fallback answer for thread {}", thread.getId());
        return generateFallbackAnswer(thread);
    }

    private String buildPrompt(DiscussionThread thread, String courseTitle, String lessonInfo, String courseContext) {
        return """
               You are the CareerPulse AI Technical Mentor and Senior Staff Architect.
               A learner has posted a technical question regarding the course: "%s".
               %s
               %s
               
               Learner's Question Title:
               "%s"
               
               Learner's Question Body:
               \"\"\"
               %s
               \"\"\"
               
               Tags: %s
               
               TASK:
               Provide a thorough, highly technical, yet approachable and encouraging answer.
               Structure your response in GitHub-flavored Markdown:
               1. **Direct Summary / Core Concept**: Explain the underlying mechanism or direct answer in 2-3 sentences.
               2. **Step-by-Step Explanation / Architecture**: Break down how it works or how to solve the problem.
               3. **Code Example or Configuration Snippet**: Provide a realistic, well-commented code snippet or CLI/config example.
               4. **Common Pitfalls & Best Practices**: Mention 2-3 key gotchas engineers face in production.
               5. **Next Steps**: Offer an encouraging closing tip or recommendation.
               
               Tone: Inspiring, precise, authoritative yet friendly mentor.
               """.formatted(
                courseTitle,
                lessonInfo,
                courseContext,
                thread.getTitle(),
                thread.getContent(),
                thread.getTags() != null ? String.join(", ", thread.getTags()) : "General"
        );
    }

    private String fetchAnswerFromGemini(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty() || "dummy_key".equals(apiKey)) {
            return null;
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 1200
                )
        );

        for (String modelName : FALLBACK_MODELS) {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey.trim();
            try {
                String responseBody = webClient.post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(2500))
                        .block();

                if (responseBody != null) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode textNode = firstCandidate.path("content").path("parts").path(0).path("text");
                        if (!textNode.isMissingNode()) {
                            return textNode.asText().trim();
                        }
                    }
                }
            } catch (WebClientResponseException e) {
                log.warn("Gemini model {} failed with HTTP {}: {}", modelName, e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.warn("Gemini model {} call failed: {}", modelName, e.getMessage());
                // Avoid compounding timeouts if network/host is unreachable
                break;
            }
        }
        return null;
    }

    private String generateFallbackAnswer(DiscussionThread thread) {
        String title = thread.getTitle() != null ? thread.getTitle() : "Course Query";
        String content = thread.getContent() != null ? thread.getContent() : "";
        String courseTitle = thread.getCourseTitle() != null ? thread.getCourseTitle() : "Course Material";
        String lower = (title + " " + content).toLowerCase();

        if (lower.contains("statefulset") || lower.contains("k8s") || lower.contains("kubernetes") || lower.contains("pod") || lower.contains("pv") || lower.contains("pvc")) {
            return """
                   ### Direct Answer & Core Concept
                   In Kubernetes, **Deployments** and **StatefulSets** serve fundamentally different workload topologies:
                   
                   - **Deployments** manage stateless pods where replicas are interchangeable, have random pod hostnames (`backend-6d8f7b-xk29`), and share storage or manage state externally (e.g. managed DBs).
                   - **StatefulSets** are purpose-built for stateful distributed systems (Kafka, Cassandra, PostgreSQL, ZooKeeper) where identity, ordering, and dedicated storage are required.
                   
                   ### Key Differences Breakdown
                   
                   1. **Predictable Network Identity**: StatefulSets assign stable, zero-indexed hostnames (`kafka-0`, `kafka-1`, `kafka-2`) paired with a Headless Service for direct DNS resolution (`kafka-0.kafka-service.default.svc.cluster.local`).
                   2. **Dedicated Storage per Replica**: Using `volumeClaimTemplates`, Kubernetes automatically provisions a separate PersistentVolumeClaim (PVC) for *each* ordinal pod. Scaling down never deletes PVCs, safeguarding data against loss.
                   3. **Ordered Deployment & Graceful Rollouts**: StatefulSet pods spin up sequentially (`0` must be healthy before `1` starts) and terminate in reverse order (`N-1` down to `0`).
                   
                   ### Recommended StatefulSet Manifest
                   ```yaml
                   apiVersion: apps/v1
                   kind: StatefulSet
                   metadata:
                     name: redis-cluster
                   spec:
                     serviceName: "redis-headless"
                     replicas: 3
                     selector:
                       matchLabels:
                         app: redis
                     template:
                       metadata:
                         labels:
                           app: redis
                       spec:
                         containers:
                         - name: redis
                           image: redis:7.0-alpine
                           ports:
                           - containerPort: 6379
                             name: redis
                           volumeMounts:
                           - name: redis-data
                             mountPath: /data
                     volumeClaimTemplates:
                     - metadata:
                         name: redis-data
                       spec:
                         accessModes: [ "ReadWriteOnce" ]
                         resources:
                           requests:
                             storage: 10Gi
                   ```
                   
                   ### Production Gotchas to Keep in Mind
                   - **PVC Retention**: When deleting or scaling down a StatefulSet, remember PVCs are intentionally retained to prevent data loss. Clean them up manually if discarding the cluster.
                   - **Node Failures & PDBs**: Always configure a `PodDisruptionBudget` (PDB) to prevent cluster maintenance nodes from evicting quorum nodes simultaneously.
                   
                   *Hope this clarifies the architectural design! Keep building great systems!* 🚀
                   """;
        } else if (lower.contains("circuit breaker") || lower.contains("resilience") || lower.contains("spring cloud") || lower.contains("resilience4j") || lower.contains("fallback")) {
            return """
                   ### Direct Answer & Core Concept
                   A **Circuit Breaker** acts as an electrical safety switch between distributed microservices. When a downstream dependency begins failing or experiencing severe latency, the circuit breaker opens to fail fast, shielding the client service from thread starvation and cascading system failure.
                   
                   ### State Machine Lifecycle
                   
                   - **CLOSED (Normal Operation)**: All requests flow through to the target service. Failures are tracked in a sliding time/count window.
                   - **OPEN (Tripped)**: If the failure rate crosses the configured threshold (e.g., 50%), the breaker trips. Subsequent requests immediately execute fallback logic without waiting for timeouts.
                   - **HALF-OPEN (Canary Probe)**: After a defined wait duration, the breaker permits a limited trial number of requests. If they succeed, it returns to CLOSED; if they fail, it trips back to OPEN.
                   
                   ### Resilience4j Implementation Example
                   ```java
                   @Service
                   public class PaymentProcessingService {
                   
                       private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);
                   
                       @CircuitBreaker(name = "paymentGateway", fallbackMethod = "handlePaymentFallback")
                       @Retry(name = "paymentGateway")
                       public PaymentResponse processTransaction(PaymentRequest request) {
                           return paymentClient.executeCharge(request);
                       }
                   
                       // Fallback signature must match original method + Throwable argument
                       public PaymentResponse handlePaymentFallback(PaymentRequest request, Throwable ex) {
                           log.warn("Payment Gateway unavailable. Queuing offline settlement: {}", ex.getMessage());
                           return PaymentResponse.queuedForAsyncProcessing(request.getTransactionId());
                       }
                   }
                   ```
                   
                   ### Best Practices
                   - **Fallback Design**: Fallbacks must never make another blocking remote call. Use cached data, asynchronous queueing, or friendly graceful degradation.
                   - **Thread Pool Bulkheads**: Combine Circuit Breakers with Bulkheads (`@Bulkhead`) so high-volume failing requests cannot exhaust your thread pool.
                   """;
        } else if (lower.contains("spark") || lower.contains("partition") || lower.contains("skew") || lower.contains("rdd") || lower.contains("shuffle") || lower.contains("data engineering")) {
            return """
                   ### Direct Answer & Core Concept
                   **Data Skew** in Apache Spark occurs when data is unevenly distributed across partitions. When one partition receives significantly more records than others (for instance, a hot grouping key like nulls or high-frequency customer IDs), a single executor gets overwhelmed, causing the entire stage to hang at 99%.
                   
                   ### Proven Strategies to Resolve Partition Skew
                   
                   1. **Salting the Skewed Key**:
                      Add a random integer salt (e.g. 0 to 9) to the joining key, replicating dimension records on the other side. This splits the hot key across multiple executors.
                   2. **Adaptive Query Execution (AQE)**:
                      In Spark 3.x+, make sure AQE skew join optimization is enabled. Spark detects skewed partitions at runtime and automatically splits them into smaller sub-partitions.
                   3. **Broadcast Hash Join for Asymmetric Data**:
                      If one table is small enough (under 10MB default, can be tuned up to 100MB+), hint a broadcast join to completely eliminate shuffling.
                   
                   ### Spark Salting Code Example (PySpark)
                   ```python
                   from pyspark.sql import functions as F
                   
                   # Salt skewed transactions before join
                   NUM_SALTS = 8
                   salted_transactions = transactions.withColumn(
                       "salt", F.concat(F.col("user_id"), F.lit("_"), (F.rand() * NUM_SALTS).cast("int"))
                   )
                   
                   # Explode user reference table across all salt values
                   users_exploded = users.withColumn("salt_idx", F.explode(F.array([F.lit(i) for i in range(NUM_SALTS)]))) \\
                                         .withColumn("salt", F.concat(F.col("user_id"), F.lit("_"), F.col("salt_idx")))
                   
                   # Join on the salted composite key
                   balanced_df = salted_transactions.join(users_exploded, on="salt", how="inner")
                   ```
                   
                   ### Production Best Practice
                   Always monitor the **Spark UI Event Timeline**: if you see 99 tasks finish in 5 seconds and 1 task takes 15 minutes, you have confirmed data skew!
                   """;
        } else {
            return String.format("""
                   ### AI Mentor Overview for: %s
                   
                   Hello! Thanks for posting this question regarding **%s**. Let's break down the technical concept and best practice approach.
                   
                   ### Key Principles & Solution Architecture
                   
                   1. **Deconstruct the Core Requirement**:
                      When working through this concept, isolate the state management and contract boundaries. Ensure decoupling between presentation and underlying business domains.
                   
                   2. **Pattern Application**:
                      - Follow separation of concerns: keep controllers lean and business validations centralized.
                      - Implement idempotent behavior where state transformations or network communications occur.
                      - Verify that error boundaries and meaningful fallback behaviors are defined.
                   
                   3. **Recommended Implementation Pattern**:
                   ```java
                   // Best practice pattern demonstration
                   public class ExecutionPipeline {
                       public Result executeStep(ExecutionContext context) {
                           // 1. Validate preconditions
                           context.validate();
                           
                           // 2. Perform localized operation with bounded scope
                           return context.proceedWithFallback(() -> {
                               return computeDirectly(context);
                           });
                       }
                   }
                   ```
                   
                   ### Pro Tips & Production Considerations
                   - **Observability**: Always log key operational metrics and structured errors so issues are easily traceable in production APM tools.
                   - **Unit & Integration Verification**: Back up your implementation with parameterized unit tests covering both positive and boundary conditions.
                   
                   *Feel free to reply if you'd like us to dive deeper into any specific line of code or edge cases!*
                   """, title, courseTitle);
        }
    }
}
