package com.learning.platform.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role_benchmarks")
public class RoleBenchmark {

    @Id
    private String id; // e.g., "ROLE_CLOUD_ARCHITECT", "ROLE_SR_BACKEND"

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category; // e.g., "Architecture", "Backend", "DevOps"

    @Column(length = 1500)
    private String description;

    private int targetExperienceYears;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_required_skills", joinColumns = @JoinColumn(name = "role_id"))
    private List<RoleRequiredSkill> requiredSkills = new ArrayList<>();

    @Embeddable
    public static class RoleRequiredSkill {
        private String skillId;
        private String skillName;
        private String minimumProficiency; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
        private int minimumScore; // e.g., 75
        private int weight; // 1 to 5

        public RoleRequiredSkill() {
        }

        public RoleRequiredSkill(String skillId, String skillName, String minimumProficiency, int minimumScore, int weight) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.minimumProficiency = minimumProficiency;
            this.minimumScore = minimumScore;
            this.weight = weight;
        }

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }

        public String getSkillName() {
            return skillName;
        }

        public void setSkillName(String skillName) {
            this.skillName = skillName;
        }

        public String getMinimumProficiency() {
            return minimumProficiency;
        }

        public void setMinimumProficiency(String minimumProficiency) {
            this.minimumProficiency = minimumProficiency;
        }

        public int getMinimumScore() {
            return minimumScore;
        }

        public void setMinimumScore(int minimumScore) {
            this.minimumScore = minimumScore;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    public RoleBenchmark() {
    }

    public RoleBenchmark(String id, String title, String category, String description, int targetExperienceYears) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.targetExperienceYears = targetExperienceYears;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTargetExperienceYears() {
        return targetExperienceYears;
    }

    public void setTargetExperienceYears(int targetExperienceYears) {
        this.targetExperienceYears = targetExperienceYears;
    }

    public List<RoleRequiredSkill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<RoleRequiredSkill> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }
}
