package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.model.RoleBenchmark;
import com.learning.platform.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleBenchmark>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getAllRoles()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleBenchmark>> getRoleById(@PathVariable String id) {
        return roleService.getRoleById(id)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Role not found with id: " + id)));
    }
}
