package com.tlat.repository;

import com.tlat.entity.LearningResource;
import com.tlat.entity.LearningResourcePublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    @Query("SELECT lr FROM LearningResource lr JOIN FETCH lr.lecture ORDER BY lr.createdAt DESC")
    List<LearningResource> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT lr FROM LearningResource lr JOIN FETCH lr.lecture l JOIN l.lecturers lec WHERE lec.id = :lecturerId ORDER BY lr.createdAt DESC")
    List<LearningResource> findDistinctByLecture_Lecturers_IdOrderByCreatedAtDesc(@Param("lecturerId") Long lecturerId);

    Optional<LearningResource> findByIdAndLecture_Lecturer(Long id, String lecturer);

    @Query("""
            select distinct lr from LearningResource lr
            join fetch lr.lecture l
            join l.groups g
            where g.id = :groupId
              and lr.publishStatus = :status
              and (lr.visibleFrom is null or lr.visibleFrom <= :now)
              and (lr.visibleUntil is null or lr.visibleUntil >= :now)
            order by lr.createdAt desc
            """)
    List<LearningResource> findVisibleForGroup(@Param("groupId") Long groupId,
                                               @Param("status") LearningResourcePublishStatus status,
                                               @Param("now") LocalDateTime now);

    List<LearningResource> findByLectureId(Long lectureId);
}
