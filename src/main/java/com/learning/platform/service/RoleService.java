package com.learning.platform.service;

import com.learning.platform.model.RoleBenchmark;
import com.learning.platform.repository.RoleBenchmarkRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleBenchmarkRepository roleBenchmarkRepository;

    public RoleService(RoleBenchmarkRepository roleBenchmarkRepository) {
        this.roleBenchmarkRepository = roleBenchmarkRepository;
    }

    public List<RoleBenchmark> getAllRoles() {
        return roleBenchmarkRepository.findAll();
    }

    public Optional<RoleBenchmark> getRoleById(String id) {
        return roleBenchmarkRepository.findById(id);
    }
}
