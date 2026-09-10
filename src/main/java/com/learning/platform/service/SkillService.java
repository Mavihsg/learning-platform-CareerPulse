package com.learning.platform.service;

import com.learning.platform.model.Skill;
import com.learning.platform.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public Optional<Skill> getSkillById(String id) {
        return skillRepository.findById(id);
    }

    public List<Skill> getSkillsByCategory(String category) {
        return skillRepository.findByCategory(category);
    }

    public List<String> getAllCategories() {
        return skillRepository.findAll().stream()
                .map(Skill::getCategory)
                .distinct()
                .toList();
    }
}
