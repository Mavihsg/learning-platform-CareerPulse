package com.learning.platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanResponseDto {
    private String title;
    private String track;
    private String description;
    private List<AiModuleDto> modules;
    private List<UdemyRecommendation> udemyRecommendations;
    private List<YoutubeRecommendation> youtubeRecommendations;

    public AiPlanResponseDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<AiModuleDto> getModules() { return modules; }
    public void setModules(List<AiModuleDto> modules) { this.modules = modules; }
    public List<UdemyRecommendation> getUdemyRecommendations() { return udemyRecommendations; }
    public void setUdemyRecommendations(List<UdemyRecommendation> udemyRecommendations) { this.udemyRecommendations = udemyRecommendations; }
    public List<YoutubeRecommendation> getYoutubeRecommendations() { return youtubeRecommendations; }
    public void setYoutubeRecommendations(List<YoutubeRecommendation> youtubeRecommendations) { this.youtubeRecommendations = youtubeRecommendations; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiModuleDto {
        private String title;
        private List<AiLessonDto> lessons;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<AiLessonDto> getLessons() { return lessons; }
        public void setLessons(List<AiLessonDto> lessons) { this.lessons = lessons; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiLessonDto {
        private String title;
        private String resourceType;
        private int durationMinutes;
        private String summary;
        private String videoUrl;
        private String content;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UdemyRecommendation {
        private String title;
        private String url;
        private String instructor;
        private double rating;
        private boolean hasCertificate;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getInstructor() { return instructor; }
        public void setInstructor(String instructor) { this.instructor = instructor; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public boolean isHasCertificate() { return hasCertificate; }
        public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoutubeRecommendation {
        private String title;
        private String url;
        private String channelName;
        private String duration;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }
}
