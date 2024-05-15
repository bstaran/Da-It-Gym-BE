package com.ogjg.daitgym.routine.repository;

import com.ogjg.daitgym.domain.routine.Routine;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutineRepository extends JpaRepository<Routine, Long>, RoutineRepositoryCustom {

    @Query("SELECT r FROM Routine r WHERE (:division IS NULL OR r.division = :division)")
    Slice<Routine> findAllByDivision(@Param("division") Integer division, Pageable pageable);

    @Query("SELECT r FROM Routine r WHERE (:division IS NULL OR r.division = :division) AND r.user.nickname = :nickname")
    Slice<Routine> findByDivisionAndUserNickname(@Param("division") Integer division, @Param("nickname") String nickname, Pageable pageable);

    @Query("SELECT r FROM Routine r WHERE (:division IS NULL OR r.division = :division) AND r.user.email IN :followerEmails")
    Slice<Routine> findByDivisionAndUserEmailIn(@Param("division") Integer division, @Param("followerEmails") List<String> followerEmails, Pageable pageable);

//    Optional<Routine> findById(Long routineId);

}
