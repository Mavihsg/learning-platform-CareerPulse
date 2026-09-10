package com.learning.platform.repository;

import com.learning.platform.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRepository extends JpaRepository<Quest, String> {
    List<Quest> findByQuestType(String questType);
}
