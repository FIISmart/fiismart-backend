package database.api;

import database.dao.CourseDAO;
import database.dao.UserDAO;
import database.model.Course;
import io.javalin.Javalin;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class LandingPageAPI {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        CourseDAO courseDAO = new CourseDAO();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(7070);


        app.get("/api/statistics", ctx -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeStudents", userDAO.countByRole("student"));
            stats.put("totalTeachers", userDAO.countByRole("teacher"));

            List<Course> courses = courseDAO.findPublishedVisible();
            stats.put("freeCourses", courses.size());
            stats.put("satisfactionRate", "98%");
            ctx.json(stats);
        });


        app.get("/api/categories", ctx -> {
            List<Course> allCourses = courseDAO.findPublishedVisible();

            Map<String, Long> categoryCounts = allCourses.stream()
                    .map(c -> {
                        Document doc = c.toDocument();
                        Object catValue = doc.get("category");


                        if (catValue == null) catValue = doc.get("subject");
                        if (catValue == null) catValue = doc.get("tags");


                        if (catValue instanceof List) {
                            List<?> list = (List<?>) catValue;
                            return list.isEmpty() ? "Diverse" : list.get(0).toString();
                        } else if (catValue != null) {
                            return catValue.toString();
                        }

                        return "Diverse";
                    })
                    .collect(Collectors.groupingBy(cat -> cat, Collectors.counting()));

            categoryCounts.put("Toate", (long) allCourses.size());
            ctx.json(categoryCounts);
        });


        app.get("/api/courses/popular", ctx -> {
            List<Course> popular = courseDAO.findPublishedVisible().stream()
                    .limit(4)
                    .collect(Collectors.toList());
            ctx.json(popular);
        });

        System.out.println("\n Server pornit corect pe http://localhost:7070");
    }
}