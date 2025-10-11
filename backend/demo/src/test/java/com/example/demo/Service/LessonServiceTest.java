// src/test/java/com/example/demo/Service/LessonServiceTest.java
package com.example.demo.Service;

import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LessonService using Arrange–Act–Assert.
 */
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LessonService lessonService;

    private User teacher;
    private User student;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        teacher = new User();
        teacher.setId(1L);
        teacher.setName("Jane Teacher");
        teacher.setEmail("teacher@mail.com");
        teacher.setRole("TEACHER");

        student = new User();
        student.setId(2L);
        student.setName("John Student");
        student.setEmail("student@mail.com");
        student.setRole("USER");

        lesson = new Lesson();
        lesson.setId(10L);
        lesson.setInstrument("Piano");
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setLessonDate(LocalDate.now());
        lesson.setStartTime(LocalTime.of(14, 0));
        lesson.setEndTime(LocalTime.of(15, 0));
        lesson.setHomework("Practice arpeggios");
    }

    // 1️⃣ Save a lesson successfully
    @Test
    void saveLesson_ShouldReturnSavedLesson() {
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);

        Lesson result = lessonService.saveLesson(lesson);

        assertNotNull(result);
        assertEquals("Piano", result.getInstrument());
        verify(lessonRepository, times(1)).save(lesson);
    }

    // 2️⃣ Find lesson by ID successfully
    @Test
    void getLessonById_ShouldReturnLesson() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        Lesson found = lessonService.getLessonById(10L);

        assertNotNull(found);
        assertEquals("Piano", found.getInstrument());
        verify(lessonRepository, times(1)).findById(10L);
    }

    // 3️⃣ Throw exception if lesson not found
    @Test
    void getLessonById_ShouldThrowException_WhenNotFound() {
        when(lessonRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> lessonService.getLessonById(999L));
    }

    // 4️⃣ Find lessons by teacher ID
    @Test
    void getLessonsByTeacherId_ShouldReturnList() {
        when(lessonRepository.findSimpleByTeacherId(1L)).thenReturn(List.of());

        List<?> result = lessonService.getLessonsByTeacherId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(lessonRepository).findSimpleByTeacherId(1L);
    }

    // 5️⃣ Delete lesson by ID
    @Test
    void deleteLesson_ShouldDeleteSuccessfully() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(10L);

        verify(lessonRepository, times(1)).delete(lesson);
    }

    // 6️⃣ Delete lesson should throw if not found
    @Test
    void deleteLesson_ShouldThrow_WhenLessonNotFound() {
        when(lessonRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> lessonService.deleteLesson(5L));
    }

    // 7️⃣ Save lesson should set correct teacher and student
    @Test
    void addLesson_ShouldBindTeacherAndStudent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);

        Lesson created = lessonService.createLesson("Piano", 1L, 2L,
                LocalDate.now(), LocalTime.of(14, 0), LocalTime.of(15, 0), "Practice");

        assertEquals("Piano", created.getInstrument());
        assertEquals(teacher, created.getTeacher());
        assertEquals(student, created.getStudent());

        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(captor.capture());
        Lesson saved = captor.getValue();
        assertEquals("Piano", saved.getInstrument());
        assertEquals(teacher, saved.getTeacher());
        assertEquals(student, saved.getStudent());
    }
}
