package com.fiismart.backend.service.teacher;

import com.fiismart.backend.dto.teacher.TeacherQuizPreviewDTO;
import database.dao.CourseDAO;
import database.dao.QuizAttemptDAO;
import database.dao.QuizDAO;
import database.model.Course;
import database.model.Quiz;
import database.model.QuizAttempt;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeacherQuizzesService {
    private final CourseDAO courseDAO = new CourseDAO();
    private  final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO quizAttemptDAO = new QuizAttemptDAO();

    public List<TeacherQuizPreviewDTO> getQuizzes(String teacherIdHex, int limit, int offset){
        ObjectId teacherId = new ObjectId(teacherIdHex);

        List<Course> courses = courseDAO.findByTeacherId(teacherId);

        List<TeacherQuizPreviewDTO> result = new ArrayList<>();
        for (Course course: courses){
            Quiz quiz = quizDAO.findByCourseId(course.getId());
            if (quiz == null){
                //
                continue;
            }
            List<QuizAttempt> attempts = quizAttemptDAO.findByQuizId(quiz.getId());
            int attemptCount = attempts.size();
            double avgScore = attempts.isEmpty()
                    ? 0
                    : attempts.stream().mapToDouble(QuizAttempt::getScore).average().orElse(0);

            TeacherQuizPreviewDTO dto = new TeacherQuizPreviewDTO();
            dto.setQuizId(quiz.getId().toHexString());
            dto.setTitle(quiz.getTitle());
            dto.setCourseId(course.getId().toHexString());
            dto.setCourseTitle(course.getTitle());
            dto.setAttemptsCount(attemptCount);
            dto.setAvgScorePct(avgScore);
            dto.setStatus("active");
            result.add(dto);
        }
        return result.stream().skip(offset).limit(limit).toList();

    }
}
