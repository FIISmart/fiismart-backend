package com.fiismart.service;

import com.fiismart.dto.InitialsDTO;
import database.dao.UserDAO;
import database.model.User;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class InitialsService {

    private final UserDAO userDAO = new UserDAO();

    public InitialsDTO getInitials(String studentId) {
        ObjectId id = new ObjectId(studentId);
        User user = userDAO.findById(id);

        if (user == null) return null;

        String initials = computeInitials(user.getDisplayName());

        InitialsDTO dto = new InitialsDTO();

        dto.setInitials(initials);

        return dto;
    }

    private String computeInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "";

        String[] parts = displayName.trim().split("\\s+");

        if (parts.length == 1) {
            // Un singur cuvânt → prima literă
            return String.valueOf(parts[0].charAt(0)).toUpperCase();
        } else if (parts.length == 2) {
            // Prenume Nume → P.N.
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        } else {
            // Prenume1 Prenume2 Nume → P1.P2.
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
    }
}