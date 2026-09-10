package com.learning.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.dto.AiPlanResponseDto;
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
public class AiPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanService.class);

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

    public AiPlanService(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
    }

    public AiPlanResponseDto generateCoursePlan(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API key is not configured. Please set gemini.api.key in application.properties.");
        }

        String systemPrompt = buildSystemPrompt(userPrompt);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", systemPrompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 8192,
                "responseMimeType", "application/json"
            )
        );

        // Prepare list of endpoint URLs to try: primary first, followed by fallbacks
        List<String> urlsToTry = new ArrayList<>();
        urlsToTry.add(apiUrl);
        for (String model : FALLBACK_MODELS) {
            String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
            if (!urlsToTry.contains(fallbackUrl)) {
                urlsToTry.add(fallbackUrl);
            }
        }

        Exception lastException = null;

        for (String endpointUrl : urlsToTry) {
            try {
                String fullUrl = endpointUrl + "?key=" + apiKey;
                log.info("Calling Gemini API endpoint: {} for prompt: {}", endpointUrl, userPrompt);

                String responseBody = webClient.post()
                        .uri(fullUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(35))
                        .block();

                AiPlanResponseDto plan = parseGeminiResponse(responseBody);
                if (plan != null && plan.getModules() != null && !plan.getModules().isEmpty()) {
                    log.info("Successfully generated plan '{}' with {} modules using {}",
                            plan.getTitle(), plan.getModules().size(), endpointUrl);
                    return plan;
                }
            } catch (WebClientResponseException e) {
                lastException = e;
                if (e.getStatusCode().value() == 429) {
                    log.warn("Gemini model at {} exceeded quota (429). Attempting fallback model...", endpointUrl);
                } else {
                    log.warn("Gemini model at {} returned HTTP {}. Attempting fallback model...", endpointUrl, e.getStatusCode());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Call to Gemini model at {} failed: {}. Trying next fallback...", endpointUrl, e.getMessage());
            }
        }

        log.error("All Gemini API endpoints failed or exhausted quota. Generating curated fallback curriculum.", lastException);
        return createFallbackResponse(userPrompt);
    }

    private String buildSystemPrompt(String userPrompt) {
        return """
            You are a professional course curriculum designer for an enterprise learning platform called Career Pulse.
            
            The user wants to learn about: "%s"
            
            Generate a comprehensive and structured course plan in JSON format with the following exact schema:
            {
                "title": "Course Title",
                "track": "Category (e.g., Cloud, Data, Backend, Frontend, DevOps)",
                "description": "A 1-2 sentence course description",
                "modules": [
                    {
                        "title": "Module Title",
                        "lessons": [
                            {
                                "title": "Lesson Title",
                                "resourceType": "VIDEO or READING or EXERCISE or PROJECT",
                                "durationMinutes": 20,
                                "summary": "Brief lesson description",
                                "videoUrl": "https://www.youtube-nocookie.com/embed/VIDEO_ID",
                                "content": "Markdown formatted lesson content with key takeaways and code or architecture concepts."
                            }
                        ]
                    }
                ],
                "udemyRecommendations": [
                    {
                        "title": "Course Title on Udemy",
                        "url": "https://www.udemy.com/course/course-slug/",
                        "instructor": "Instructor Name",
                        "rating": 4.8,
                        "hasCertificate": true
                    }
                ],
                "youtubeRecommendations": [
                    {
                        "title": "Full Course Title",
                        "url": "https://youtube.com/watch?v=...",
                        "channelName": "Channel Name",
                        "duration": "10 hours"
                    }
                ]
            }
            
            Requirements:
            - Create 3 modules with 2-3 lessons each.
            - Mix resource types: include VIDEO, READING, EXERCISE, and PROJECT lessons.
            - For VIDEO lessons, use real popular YouTube video embed URLs from reputable educational channels.
            - For READING lessons, provide clear, high-yield markdown content (around 150-250 words) with key points and syntax/examples.
            - Include 3 Udemy course recommendations that are well-known, highly-rated, and provide certificates.
            - Include 3 Free YouTube full course / tutorial recommendations from top channels (e.g., freeCodeCamp, Traversy Media, Programming with Mosh, etc.).
            - Make the curriculum practical, modern, and engaging.
            
            Return ONLY the raw JSON object, without markdown code fences or conversational text.
            """.formatted(userPrompt);
    }

    private AiPlanResponseDto parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : parts) {
                        // Extract text parts; skip thoughts if marked
                        if (part.has("text") && !part.has("thought")) {
                            sb.append(part.path("text").asText());
                        }
                    }

                    String text = sb.toString().trim();
                    if (text.isEmpty()) {
                        text = parts.get(0).path("text").asText().trim();
                    }

                    // Remove markdown code fences if present
                    if (text.startsWith("```json")) {
                        text = text.substring(7);
                    } else if (text.startsWith("```")) {
                        text = text.substring(3);
                    }
                    if (text.endsWith("```")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    text = text.trim();

                    // Find first '{' and last '}' in case of leading/trailing commentary
                    int firstBrace = text.indexOf('{');
                    int lastBrace = text.lastIndexOf('}');
                    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                        text = text.substring(firstBrace, lastBrace + 1);
                    }

                    AiPlanResponseDto dto = objectMapper.readValue(text, AiPlanResponseDto.class);
                    if (dto.getUdemyRecommendations() == null) {
                        dto.setUdemyRecommendations(new ArrayList<>());
                    }
                    if (dto.getYoutubeRecommendations() == null) {
                        dto.setYoutubeRecommendations(new ArrayList<>());
                    }
                    if (dto.getModules() == null) {
                        dto.setModules(new ArrayList<>());
                    }
                    return dto;
                }
            }

            log.warn("Unexpected Gemini response structure: {}", responseBody);
            return null;

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage(), e);
            return null;
        }
    }

    private AiPlanResponseDto createFallbackResponse(String userPrompt) {
        String cleanTopic = (userPrompt != null && !userPrompt.isBlank()) ? userPrompt.trim() : "Software Engineering";
        String capitalizedTopic = cleanTopic.substring(0, 1).toUpperCase() + cleanTopic.substring(1);

        AiPlanResponseDto fallback = new AiPlanResponseDto();
        fallback.setTitle(capitalizedTopic + " Enterprise Mastery");
        fallback.setTrack("Backend");
        fallback.setDescription("Comprehensive learning path covering core syntax, architecture patterns, and production engineering in " + capitalizedTopic + ".");

        List<AiPlanResponseDto.AiModuleDto> modules = new ArrayList<>();

        // Module 1
        AiPlanResponseDto.AiModuleDto mod1 = new AiPlanResponseDto.AiModuleDto();
        mod1.setTitle("Foundations & Core Principles");
        List<AiPlanResponseDto.AiLessonDto> mod1Lessons = new ArrayList<>();
        
        AiPlanResponseDto.AiLessonDto l1 = new AiPlanResponseDto.AiLessonDto();
        l1.setTitle("Architecture & Setup Overview");
        l1.setResourceType("VIDEO");
        l1.setDurationMinutes(20);
        l1.setSummary("Fundamental concepts, environment configuration, and execution lifecycle.");
        l1.setVideoUrl("https://www.youtube-nocookie.com/embed/1FUcniACzmc");
        l1.setContent("### Getting Started with " + capitalizedTopic + "\n\nLearn fundamental primitives, toolchains, and development conventions.");
        mod1Lessons.add(l1);

        AiPlanResponseDto.AiLessonDto l2 = new AiPlanResponseDto.AiLessonDto();
        l2.setTitle("Core Primitives & Best Practices");
        l2.setResourceType("READING");
        l2.setDurationMinutes(25);
        l2.setSummary("Idiomatic patterns, memory efficiency, and structural paradigms.");
        l2.setContent("### Core Design Patterns in " + capitalizedTopic + "\n\n* **Modularity**: Structuring robust components\n* **Error Handling**: Graceful fault recovery and validation\n* **Concurrency**: Thread safety and asynchronous workflows.");
        mod1Lessons.add(l2);

        mod1.setLessons(mod1Lessons);
        modules.add(mod1);

        // Module 2
        AiPlanResponseDto.AiModuleDto mod2 = new AiPlanResponseDto.AiModuleDto();
        mod2.setTitle("Building Enterprise Services");
        List<AiPlanResponseDto.AiLessonDto> mod2Lessons = new ArrayList<>();

        AiPlanResponseDto.AiLessonDto l3 = new AiPlanResponseDto.AiLessonDto();
        l3.setTitle("API Design & Data Layer");
        l3.setResourceType("EXERCISE");
        l3.setDurationMinutes(35);
        l3.setSummary("Building resilient endpoints with database integration.");
        l3.setContent("### Hands-on: Building RESTful Endpoints\n\nImplement CRUD operations, persistence logic, and validation schemas.");
        mod2Lessons.add(l3);

        AiPlanResponseDto.AiLessonDto l4 = new AiPlanResponseDto.AiLessonDto();
        l4.setTitle("Production Deployment & Testing");
        l4.setResourceType("PROJECT");
        l4.setDurationMinutes(50);
        l4.setSummary("Packaging with Docker, testing strategies, and CI/CD pipelines.");
        l4.setContent("### Capstone Project\n\nContainerize your application and establish automated integration tests.");
        mod2Lessons.add(l4);

        mod2.setLessons(mod2Lessons);
        modules.add(mod2);

        fallback.setModules(modules);

        // Curated Udemy Courses
        List<AiPlanResponseDto.UdemyRecommendation> udemy = new ArrayList<>();
        AiPlanResponseDto.UdemyRecommendation u1 = new AiPlanResponseDto.UdemyRecommendation();
        u1.setTitle("Mastering " + capitalizedTopic + ": From Beginner to Professional");
        u1.setUrl("https://www.udemy.com/topic/" + cleanTopic.toLowerCase().replaceAll("\\s+", "-") + "/");
        u1.setInstructor("Lead Enterprise Architect");
        u1.setRating(4.8);
        u1.setHasCertificate(true);
        udemy.add(u1);

        AiPlanResponseDto.UdemyRecommendation u2 = new AiPlanResponseDto.UdemyRecommendation();
        u2.setTitle(capitalizedTopic + " Deep Dive: Advanced Concepts & Best Practices");
        u2.setUrl("https://www.udemy.com/topic/" + cleanTopic.toLowerCase().replaceAll("\\s+", "-") + "/");
        u2.setInstructor("Senior Software Engineer");
        u2.setRating(4.7);
        u2.setHasCertificate(true);
        udemy.add(u2);

        AiPlanResponseDto.UdemyRecommendation u3 = new AiPlanResponseDto.UdemyRecommendation();
        u3.setTitle(capitalizedTopic + " Full Stack Development Bootcamp");
        u3.setUrl("https://www.udemy.com/topic/" + cleanTopic.toLowerCase().replaceAll("\\s+", "-") + "/");
        u3.setInstructor("Industry Expert");
        u3.setRating(4.9);
        u3.setHasCertificate(true);
        udemy.add(u3);

        fallback.setUdemyRecommendations(udemy);

        // Curated YouTube Courses
        List<AiPlanResponseDto.YoutubeRecommendation> youtube = new ArrayList<>();
        AiPlanResponseDto.YoutubeRecommendation y1 = new AiPlanResponseDto.YoutubeRecommendation();
        y1.setTitle(capitalizedTopic + " Full Course - Learn in 10 Hours");
        y1.setUrl("https://www.youtube.com/results?search_query=" + cleanTopic.toLowerCase().replaceAll("\\s+", "+") + "+full+course");
        y1.setChannelName("freeCodeCamp.org");
        y1.setDuration("10 hours");
        youtube.add(y1);

        AiPlanResponseDto.YoutubeRecommendation y2 = new AiPlanResponseDto.YoutubeRecommendation();
        y2.setTitle(capitalizedTopic + " Tutorial for Beginners - Full Crash Course");
        y2.setUrl("https://www.youtube.com/results?search_query=" + cleanTopic.toLowerCase().replaceAll("\\s+", "+") + "+crash+course");
        y2.setChannelName("Programming with Mosh");
        y2.setDuration("3 hours");
        youtube.add(y2);

        AiPlanResponseDto.YoutubeRecommendation y3 = new AiPlanResponseDto.YoutubeRecommendation();
        y3.setTitle(capitalizedTopic + " Masterclass: Complete Guide");
        y3.setUrl("https://www.youtube.com/results?search_query=" + cleanTopic.toLowerCase().replaceAll("\\s+", "+") + "+masterclass");
        y3.setChannelName("Traversy Media");
        y3.setDuration("5 hours");
        youtube.add(y3);

        fallback.setYoutubeRecommendations(youtube);

        return fallback;
    }
}
