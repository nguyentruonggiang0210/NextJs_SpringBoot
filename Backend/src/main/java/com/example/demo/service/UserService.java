package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Hàm chuyển Entity → DTO + generate GUID
    private UserResponseDTO convertToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setPassword(user.getPassword());

        // Generate GUID ngẫu nhiên mỗi lần gọi API
        dto.setCode(UUID.randomUUID().toString());

        return dto;
    }

    // === PHẦN ĐĂNG NHẬP MỚI ===
   public LoginResponse login(LoginRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return new LoginResponse(false, "Mật khẩu không được để trống", null, null, null);
        }

        Optional<User> optionalUser = userRepository.findById(request.getId());

        if (optionalUser.isEmpty()) {
            return new LoginResponse(false, "ID không tồn tại", null, null, null);
        }

        User user = optionalUser.get();

        // Kiểm tra password an toàn hơn (tránh NullPointerException)
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.trim().isEmpty()) {
            return new LoginResponse(false, "Tài khoản chưa có mật khẩu. Liên hệ admin!", null, null, null);
        }

        // So sánh mật khẩu (hiện tại vẫn là plaintext)
        if (!storedPassword.equals(request.getPassword())) {
            return new LoginResponse(false, "Mật khẩu không đúng", null, null, null);
        }

        // Đăng nhập thành công
        UserResponseDTO userDTO = convertToDTO(user);
        String token = UUID.randomUUID().toString();

        return new LoginResponse(true, "Đăng nhập thành công", token, null, userDTO);
    }
}