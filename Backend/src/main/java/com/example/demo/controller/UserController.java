package com.example.demo.controller;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. GET ALL - Lấy danh sách tất cả user
    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    // 2. GET ONE - Lấy 1 user theo id
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. POST - Thêm user mới (permission không được set từ client)
    @PostMapping
    public UserResponseDTO createUser(@RequestBody User user) {
        return userService.convertToDTO(userService.createUser(user));
    }

    // 4. PUT - Sửa user (chỉ name/email/phone, không đổi password/permission)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id, @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. DELETE - Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}