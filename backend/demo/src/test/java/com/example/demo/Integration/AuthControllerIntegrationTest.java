package com.example.demo.Integration;

import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=true")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    void registerUser_ShouldReturn200AndJwtPayload() throws Exception {
        Map<String, Object> registerPayload = Map.of(
                "name", "Integration Test User",
                "email", "integration_" + UUID.randomUUID() + "@mail.com",
                "password", "secret123",
                "instrument", "Piano"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").exists());
    }

    @Test
    void registerUser_MissingRequiredFields_ShouldReturnValidationDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "",
                              "email": "",
                              "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validation failed")))
                .andExpect(jsonPath("$.validationErrors[*].field").value(org.hamcrest.Matchers.hasItems(
                        "name",
                        "email",
                        "password"
                )));
    }

    @Test
    void loginUser_WrongPassword_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "nonexistent_user@mail.com",
                              "password": "wrongpass"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_SeededTeacherCredentials_ShouldReturnJwtPayload() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "teacherpiano@email.com",
                              "password": "Test123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("teacherpiano@email.com"))
                .andExpect(jsonPath("$.user.role").value("TEACHER"));
    }

    @Test
    void updateProfile_WithPassword_ShouldAllowLoginWithNewPassword() throws Exception {
        String email = "password_reset_" + UUID.randomUUID() + "@mail.com";
        String oldPassword = "secret123";
        String newPassword = "newSecret123";

        Map<String, Object> registerPayload = Map.of(
                "name", "Password Reset User",
                "email", email,
                "password", oldPassword,
                "instrument", "Piano"
        );

        String registerJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerResponse = objectMapper.readTree(registerJson);
        String token = registerResponse.path("token").asText();

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", newPassword
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void deleteUser_AsAdmin_WithTeacherStudentAndLesson_ShouldReturnNoContent() throws Exception {
        User teacher = new User();
        teacher.setName("Delete Teacher");
        teacher.setEmail("delete_teacher_" + UUID.randomUUID() + "@mail.com");
        teacher.setPassword("encoded");
        teacher.setRole("TEACHER");
        teacher.setInstrument("Piano");
        teacher = userRepository.save(teacher);

        User student = new User();
        student.setName("Delete Student");
        student.setEmail("delete_student_" + UUID.randomUUID() + "@mail.com");
        student.setPassword("encoded");
        student.setRole("USER");
        student.setInstrument("Piano");
        student.setTeacher(teacher);
        student = userRepository.save(student);

        Lesson lesson = new Lesson();
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setInstrument("Piano");
        lesson.setLessonDate(LocalDate.now());
        lesson.setStartTime(LocalTime.of(10, 0));
        lesson.setEndTime(LocalTime.of(10, 30));
        lesson.setHomework("Practice scales");
        lessonRepository.save(lesson);

        mockMvc.perform(delete("/api/users/" + teacher.getId())
                        .with(user("admin@mail.com").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsers_AsRegularUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("student@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
