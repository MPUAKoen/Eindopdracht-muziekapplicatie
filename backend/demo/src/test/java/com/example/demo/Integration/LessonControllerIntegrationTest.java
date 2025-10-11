package com.example.demo.Integration;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class LessonControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    private Long teacherId;
    private Long studentId;
    private String teacherEmail;
    private String studentEmail;

    @BeforeEach
    void setup() {
        // 🧹 Clean existing data to avoid FK constraint issues
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        // 🧑‍🏫 Create teacher
        teacherEmail = "teacher_" + System.currentTimeMillis() + "@mail.com";
        User teacher = new User();
        teacher.setName("Integration Test Teacher");
        teacher.setEmail(teacherEmail);
        teacher.setPassword("1234");
        teacher.setRole("TEACHER");
        teacher.setInstrument("Piano");
        teacher = userRepository.save(teacher);
        teacherId = teacher.getId();

        // 👩‍🎓 Create student
        studentEmail = "student_" + System.currentTimeMillis() + "@mail.com";
        User student = new User();
        student.setName("Integration Test Student");
        student.setEmail(studentEmail);
        student.setPassword("abcd");
        student.setRole("USER");
        student.setInstrument("Violin");
        student.setTeacher(teacher);
        student = userRepository.save(student);
        studentId = student.getId();
    }

    // ✅ Test: teacher can add lesson
    @Test
    void addLesson_AsTeacher_ShouldReturnOk() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "pdfFiles", "lesson.pdf",
                MediaType.APPLICATION_PDF_VALUE, "Dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/lesson/add")
                        .file(pdf)
                        .param("instrument", "Violin")
                        .param("studentId", String.valueOf(studentId))
                        .param("lessonDate", LocalDate.now().toString())
                        .param("startTime", "14:00")
                        .param("endTime", "15:00")
                        .param("homework", "Practice scales")
                        .with(user(teacherEmail).roles("TEACHER")) // 👈 dynamically authenticated
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    // 🚫 Test: student cannot add lesson
    @Test
    void addLesson_AsStudent_ShouldReturnForbidden() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "pdfFiles", "lesson.pdf",
                MediaType.APPLICATION_PDF_VALUE, "Dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/lesson/add")
                        .file(pdf)
                        .param("instrument", "Flute")
                        .param("studentId", String.valueOf(studentId))
                        .param("lessonDate", LocalDate.now().toString())
                        .param("startTime", "11:00")
                        .param("endTime", "12:00")
                        .param("homework", "Practice long tones")
                        .with(user(studentEmail).roles("USER")) // 👈 student auth
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }
}
