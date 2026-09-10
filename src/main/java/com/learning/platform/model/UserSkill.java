package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "user_skills")
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "skill_id", nullable = false)
    private String skillId;

    @Column(name = "skill_name")
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.BEGINNER;

    private int score = 50; // 0 to 100 percentage proficiency

    private boolean verified = false;

    public enum ProficiencyLevel {
        BEGINNER(1),
        INTERMEDIATE(2),
        ADVANCED(3),
        EXPERT(4);

        private final int rank;

        ProficiencyLevel(int rank) {
            this.rank = rank;
        }

        public int getRank() {
            return rank;
        }
    }

    public UserSkill() {
    }

    public UserSkill(User user, String skillId, String skillName, ProficiencyLevel proficiencyLevel, int score, boolean verified) {
        this.user = user;
        this.skillId = skillId;
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
        this.score = score;
        this.verified = verified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
