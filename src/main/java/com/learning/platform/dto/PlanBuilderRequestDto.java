package com.learning.platform.dto;

import java.util.List;

public class PlanBuilderRequestDto {
    private String id;
    private String title;
    private String track;
    private String category = "Engineering";
    private String difficultyLevel = "INTERMEDIATE";
    private String description;
    private String authorName = "Shivam Gupta";
    private String authorId;
    private String status = "DRAFT"; // DRAFT, PUBLISHED
    private List<ModuleDto> modules;

    public static class ModuleDto {
        private String id;
        private String title;
        private int orderIndex;
        private List<LessonDto> lessons;

        public ModuleDto() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }

        public List<LessonDto> getLessons() {
            return lessons;
        }

        public void setLessons(List<LessonDto> lessons) {
            this.lessons = lessons;
        }
    }

    public static class LessonDto {
        private String id;
        private String title;
        private String resourceType; // VIDEO, READING, EXERCISE, PROJECT, LINK
        private int durationMinutes;
        private String summary;
        private String videoUrl;
        private String content;
        private int orderIndex;

        public LessonDto() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }
    }

    public PlanBuilderRequestDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ModuleDto> getModules() {
        return modules;
    }

    public void setModules(List<ModuleDto> modules) {
        this.modules = modules;
    }
}
