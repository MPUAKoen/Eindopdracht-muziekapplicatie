// src/main/java/com/example/demo/repository/LessonRepository.java
package com.example.demo.repository;

import com.example.demo.dto.LessonSimpleView;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    // TEACHER view: include student id + name
    @Query("""
        select l.id as id,
               l.instrument as instrument,
               l.lessonDate as lessonDate,
               l.startTime as startTime,
               l.endTime as endTime,
               s.id as studentId,
               coalesce(s.name, s.email) as studentName
        from Lesson l
        join l.student s
        where l.teacher.id = :teacherId
        order by l.lessonDate desc, l.startTime asc
    """)
    List<LessonSimpleView> findSimpleByTeacherId(@Param("teacherId") Long teacherId);

    // STUDENT view (still fine to join student to fill the same projection)
    @Query("""
        select l.id as id,
               l.instrument as instrument,
               l.lessonDate as lessonDate,
               l.startTime as startTime,
               l.endTime as endTime,
               s.id as studentId,
               coalesce(s.name, s.email) as studentName
        from Lesson l
        join l.student s
        where l.student.id = :studentId
        order by l.lessonDate desc, l.startTime asc
    """)
    List<LessonSimpleView> findSimpleByStudentId(@Param("studentId") Long studentId);

    // keep if used elsewhere
    List<Lesson> findByStudent(User student);
    List<Lesson> findByTeacher(User teacher);
}
