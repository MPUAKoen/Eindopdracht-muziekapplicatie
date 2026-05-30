// src/main/java/com/example/demo/repository/LessonRepository.java
package com.example.demo.repository;

import com.example.demo.dto.LessonSimpleView;
import com.example.demo.model.Lesson;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
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
               coalesce(s.name, s.email) as studentName,
               t.id as teacherId,
               coalesce(t.name, t.email) as teacherName
        from Lesson l
        join l.student s
        join l.teacher t
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
               coalesce(s.name, s.email) as studentName,
               t.id as teacherId,
               coalesce(t.name, t.email) as teacherName
        from Lesson l
        join l.student s
        join l.teacher t
        where l.student.id = :studentId
        order by l.lessonDate desc, l.startTime asc
    """)
    List<LessonSimpleView> findSimpleByStudentId(@Param("studentId") Long studentId);

    // keep if used elsewhere
    List<Lesson> findByStudent(User student);
    List<Lesson> findByTeacher(User teacher);

    @Query("""
        select l
        from Lesson l
        where l.lessonDate = :lessonDate
          and (:excludeLessonId is null or l.id <> :excludeLessonId)
          and (l.teacher.id = :teacherId or l.student.id = :studentId)
          and l.startTime < :endTime
          and l.endTime > :startTime
    """)
    List<Lesson> findOverlappingLessons(
            @Param("teacherId") Long teacherId,
            @Param("studentId") Long studentId,
            @Param("lessonDate") LocalDate lessonDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeLessonId") Long excludeLessonId
    );
}
