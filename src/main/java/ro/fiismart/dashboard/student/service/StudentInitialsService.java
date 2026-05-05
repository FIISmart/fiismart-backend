package ro.fiismart.dashboard.student.service;

import org.springframework.stereotype.Service;
import ro.fiismart.common.model.User;
import ro.fiismart.common.repository.UserRepository;
import ro.fiismart.dashboard.student.dto.InitialsDTO;

@Service
public class StudentInitialsService {

    private final UserRepository userRepository;

    public StudentInitialsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public InitialsDTO getInitials(String studentId) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user == null) return null;

        InitialsDTO dto = new InitialsDTO();
        dto.setInitials(computeInitials(user.getDisplayName()));
        return dto;
    }

    private String computeInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[1].charAt(0)).toUpperCase();
    }
}
