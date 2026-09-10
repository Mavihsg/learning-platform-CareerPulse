package com.learning.platform.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuizDto {

    private String id;
    private String title;
    private String description;
    private String quizType; // COMPLETED_COURSES, CUSTOM_TOPIC, DAILY_CHALLENGE
    private String topicOrCourse;
    private String difficultyLevel; // BEGINNER, INTERMEDIATE, ADVANCED
    private int totalQuestions;
    private int passScorePercentage = 70;
    private int totalXpReward = 75;
    private List<QuizQuestionDto> questions = new ArrayList<>();

    public QuizDto() {}

    public QuizDto(String id, String title, String description, String quizType, int totalQuestions, int totalXpReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.quizType = quizType;
        this.totalQuestions = totalQuestions;
        this.totalXpReward = totalXpReward;
    }

    public static class QuizQuestionDto {
        private String id;
        private String questionText;
        private List<String> options = new ArrayList<>();
        private int correctOptionIndex;
        private String explanation;
        private int xpPoints = 25;
        private String topic;

        public QuizQuestionDto() {}

        public QuizQuestionDto(String id, String questionText, List<String> options, int correctOptionIndex, String explanation, int xpPoints) {
            this.id = id;
            this.questionText = questionText;
            this.options = options;
            this.correctOptionIndex = correctOptionIndex;
            this.explanation = explanation;
            this.xpPoints = xpPoints;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public int getCorrectOptionIndex() { return correctOptionIndex; }
        public void setCorrectOptionIndex(int correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public int getXpPoints() { return xpPoints; }
        public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }

    public static class QuizGenerationRequestDto {
        private String mode; // COMPLETED_COURSES, CUSTOM_TOPIC, DAILY_CHALLENGE
        private List<String> courseIds = new ArrayList<>();
        private String customTopic;
        private String difficultyLevel = "INTERMEDIATE";
        private int questionCount = 5;
        private String userId = "user_1";

        public QuizGenerationRequestDto() {}

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public List<String> getCourseIds() { return courseIds; }
        public void setCourseIds(List<String> courseIds) { this.courseIds = courseIds; }
        public String getCustomTopic() { return customTopic; }
        public void setCustomTopic(String customTopic) { this.customTopic = customTopic; }
        public String getDifficultyLevel() { return difficultyLevel; }
        public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
        public int getQuestionCount() { return questionCount; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class QuizSubmitRequestDto {
        private String quizId;
        private String userId;
        private boolean isDaily = false;
        private Map<Integer, Integer> answers; // questionIndex -> selectedOptionIndex
        private int totalQuestions;
        private List<QuizQuestionDto> questions;

        public QuizSubmitRequestDto() {}

        public String getQuizId() { return quizId; }
        public void setQuizId(String quizId) { this.quizId = quizId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public boolean isDaily() { return isDaily; }
        public void setDaily(boolean daily) { isDaily = daily; }
        public Map<Integer, Integer> getAnswers() { return answers; }
        public void setAnswers(Map<Integer, Integer> answers) { this.answers = answers; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public List<QuizQuestionDto> getQuestions() { return questions; }
        public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions; }
    }

    public static class QuizResultDto {
        private String quizId;
        private int totalQuestions;
        private int correctCount;
        private int scorePercentage;
        private boolean passed;
        private int totalXpEarned;
        private boolean streakIncremented;
        private int newStreakDays;
        private List<String> newBadgesUnlocked = new ArrayList<>();
        private List<QuestionReviewDto> reviews = new ArrayList<>();

        public QuizResultDto() {}

        public static class QuestionReviewDto {
            private int questionIndex;
            private String questionText;
            private List<String> options;
            private int selectedOptionIndex;
            private int correctOptionIndex;
            private boolean isCorrect;
            private String explanation;

            public QuestionReviewDto() {}

            public QuestionReviewDto(int questionIndex, String questionText, List<String> options,
                                     int selectedOptionIndex, int correctOptionIndex, boolean isCorrect, String explanation) {
                this.questionIndex = questionIndex;
                this.questionText = questionText;
                this.options = options;
                this.selectedOptionIndex = selectedOptionIndex;
                this.correctOptionIndex = correctOptionIndex;
                this.isCorrect = isCorrect;
                this.explanation = explanation;
            }

            public int getQuestionIndex() { return questionIndex; }
            public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }
            public String getQuestionText() { return questionText; }
            public void setQuestionText(String questionText) { this.questionText = questionText; }
            public List<String> getOptions() { return options; }
            public void setOptions(List<String> options) { this.options = options; }
            public int getSelectedOptionIndex() { return selectedOptionIndex; }
            public void setSelectedOptionIndex(int selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
            public int getCorrectOptionIndex() { return correctOptionIndex; }
            public void setCorrectOptionIndex(int correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }
            public boolean isCorrect() { return isCorrect; }
            public void setCorrect(boolean correct) { isCorrect = correct; }
            public String getExplanation() { return explanation; }
            public void setExplanation(String explanation) { this.explanation = explanation; }
        }

        public String getQuizId() { return quizId; }
        public void setQuizId(String quizId) { this.quizId = quizId; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
        public int getScorePercentage() { return scorePercentage; }
        public void setScorePercentage(int scorePercentage) { this.scorePercentage = scorePercentage; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public int getTotalXpEarned() { return totalXpEarned; }
        public void setTotalXpEarned(int totalXpEarned) { this.totalXpEarned = totalXpEarned; }
        public boolean isStreakIncremented() { return streakIncremented; }
        public void setStreakIncremented(boolean streakIncremented) { this.streakIncremented = streakIncremented; }
        public int getNewStreakDays() { return newStreakDays; }
        public void setNewStreakDays(int newStreakDays) { this.newStreakDays = newStreakDays; }
        public List<String> getNewBadgesUnlocked() { return newBadgesUnlocked; }
        public void setNewBadgesUnlocked(List<String> newBadgesUnlocked) { this.newBadgesUnlocked = newBadgesUnlocked; }
        public List<QuestionReviewDto> getReviews() { return reviews; }
        public void setReviews(List<QuestionReviewDto> reviews) { this.reviews = reviews; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getQuizType() { return quizType; }
    public void setQuizType(String quizType) { this.quizType = quizType; }
    public String getTopicOrCourse() { return topicOrCourse; }
    public void setTopicOrCourse(String topicOrCourse) { this.topicOrCourse = topicOrCourse; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getPassScorePercentage() { return passScorePercentage; }
    public void setPassScorePercentage(int passScorePercentage) { this.passScorePercentage = passScorePercentage; }
    public int getTotalXpReward() { return totalXpReward; }
    public void setTotalXpReward(int totalXpReward) { this.totalXpReward = totalXpReward; }
    public List<QuizQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions; }
}
