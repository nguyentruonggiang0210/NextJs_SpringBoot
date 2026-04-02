package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserResponseDTO> findById(Long id) {
        return userRepository.findById(id).map(this::convertToDTO);
    }

    /** Tạo user mới — password BCrypt, permission bỏ qua (không cho client tự set). */
    public User createUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setPermission(null); // chặn privilege escalation
        return userRepository.save(user);
    }

    /** Cập nhật name/email/phone, không thay đổi password và permission. */
    public Optional<UserResponseDTO> updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(existing -> {
            existing.setName(userDetails.getName());
            existing.setEmail(userDetails.getEmail());
            existing.setPhone(userDetails.getPhone());
            return convertToDTO(userRepository.save(existing));
        });
    }

    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) return false;
        userRepository.deleteById(id);
        return true;
    }

    // Entity → DTO, password không trả về client
    public UserResponseDTO convertToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setPassword(null);
        dto.setCode(UUID.randomUUID().toString());
        dto.setPermissionName(
                user.getPermission() != null ? user.getPermission().getName() : null);
        return dto;
    }
}