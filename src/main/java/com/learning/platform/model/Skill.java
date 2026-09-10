package com.learning.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private String id; // e.g., "SKILL_JAVA", "SKILL_K8S"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // e.g., "Backend", "Cloud", "Architecture", "DevOps", "Database", "AI/ML", "Soft Skills"

    @Column(length = 1000)
    private String description;

    private int importanceWeight; // 1 to 5

    public Skill() {
    }

    public Skill(String id, String name, String category, String description, int importanceWeight) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.importanceWeight = importanceWeight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getImportanceWeight() {
        return importanceWeight;
    }

    public void setImportanceWeight(int importanceWeight) {
        this.importanceWeight = importanceWeight;
    }
}
