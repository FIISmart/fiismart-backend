package com.fiismart.backend.config;

import database.dao.CommentDAO;
import database.dao.CourseDAO;
import database.dao.QuizDAO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfig {

    @Bean
    public CourseDAO courseDAO() {
        return new CourseDAO();
    }

    @Bean
    public QuizDAO quizDAO() {
        return new QuizDAO();
    }

    @Bean
    public CommentDAO commentDAO() {
        return new CommentDAO();
    }
}
