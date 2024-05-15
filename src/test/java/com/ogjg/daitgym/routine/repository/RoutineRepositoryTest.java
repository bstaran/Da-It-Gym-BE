package com.ogjg.daitgym.routine.repository;

import com.ogjg.daitgym.domain.User;
import com.ogjg.daitgym.domain.follow.Follow;
import com.ogjg.daitgym.domain.routine.Routine;
import com.ogjg.daitgym.follow.repository.FollowRepository;
import com.ogjg.daitgym.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
class RoutineRepositoryTest {

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @DisplayName("운동 분할 종류로 루틴을 조회한다.")
    @Test
    void findRoutinesByDivision() {
        // given
        IntStream.rangeClosed(1, 5)
                .mapToObj(this::createRoutine)
                .forEach(routineRepository::save);

        Pageable pageable = PageRequest.of(0, 10);

        // when, when
        IntStream.rangeClosed(1, 5)
                .forEach(division -> {
                    Slice<Routine> routines = routineRepository.findAllByDivision(division, pageable);
                    assertThat(routines.getContent().size()).isEqualTo(1);
                    assertThat(routines.getContent().get(0).getDivision()).isEqualTo(division);
                });
    }

    @DisplayName("해당하는 운동 분할 루틴이 존재하지 않는 경우 빈 결과를 반환한다.")
    @Test
    void returnEmptyWhenNoMatchingExerciseDivisionExists() {
        // given
        // when
        Slice<Routine> routines = routineRepository.findAllByDivision(1, PageRequest.of(0, 10));

        // then
        assertThat(routines.getContent().size()).isEqualTo(0);
        assertThat(routines.isEmpty()).isTrue();
    }

    @DisplayName("운동 분할 종류와 사용자 닉네임으로 루틴을 조회한다.")
    @Test
    void findRoutinesByDivisionAndUserNickname() {
        // given
        User user = createUser("email@test.com", "nickname");
        Routine routine1 = createRoutine(user, 1);
        Routine routine2 = createRoutine(user, 1);
        Routine routine3 = createRoutine(user, 2);

        PageRequest pageable = PageRequest.of(0, 10);
        userRepository.save(user);
        routineRepository.saveAll(List.of(routine1, routine2, routine3));

        // when
        Slice<Routine> routines1Division = routineRepository.findByDivisionAndUserNickname(1, "nickname", pageable);

        // then
        assertThat(routines1Division.getContent()).hasSize(2);
        assertThat(routines1Division.getContent()).contains(routine1, routine2);
    }

    @DisplayName("운동 분할 종류 조회 시 사용자 닉네임이 일치하지 않는 경우 빈 결과를 반환한다.")
    @Test
    void returnEmptyWhenNoMatchingUserNickname() {
        // given
        User user = createUser("email@test.com", "nickname");
        Routine routine1 = createRoutine(user, 1);
        Pageable pageable = PageRequest.of(0, 10);

        userRepository.save(user);
        routineRepository.save(routine1);

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserNickname(1, "otherNickname", pageable);

        // then
        assertThat(routines.getContent()).hasSize(0);
    }

    @DisplayName("운동 분할 종류 조회 시 사용자의 작성 루틴이 존재하지 않는 경우 빈 결과를 반환한다.")
    @Test
    void returnEmptyWhenNoMatchingUserRoutine() {
        // given
        User user = createUser("email@test.com", "nickname");
        Routine routine = createRoutine(user, 1);
        Pageable pageable = PageRequest.of(0, 10);

        userRepository.save(user);
        routineRepository.save(routine);

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserNickname(2, "nickname", pageable);

        // then
        assertThat(routines.getContent()).hasSize(0);
        assertThat(routines.isEmpty()).isTrue();
    }

    @DisplayName("팔로우한 유저들의 루틴을 운동 분할 별로 조회한다.")
    @Test
    void findRoutinesByFollowUsersAndDivision() {
        // given
        User user = createUser("email@test.com", "nickname");
        User followUser1 = createUser("follow1@test.com", "follow1");
        User followUser2 = createUser("follow2@test.com", "follow2");

        Follow.PK followPK = Follow.createFollowPK(followUser1.getEmail(), user.getEmail());
        Follow.PK followPK2 = Follow.createFollowPK(followUser2.getEmail(), user.getEmail());

        Routine routine1 = createRoutine(followUser1, 1);
        Routine routine2 = createRoutine(followUser2, 1);

        userRepository.saveAll(List.of(user, followUser1, followUser2));
        followRepository.save(new Follow(followPK, followUser1, user));
        followRepository.save(new Follow(followPK2, followUser2, user));
        routineRepository.saveAll(List.of(routine1, routine2));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserEmailIn(
                1,
                List.of(followUser1.getEmail(), followUser2.getEmail()),
                pageable);

        // then
        assertThat(routines.getContent()).hasSize(2);
        assertThat(routines.getContent()).contains(routine1, routine2);
        assertThat(routines.getContent().get(0).getUser()).isIn(followUser1, followUser2);
        assertThat(routines.getContent().get(1).getUser()).isIn(followUser1, followUser2);
    }

    @DisplayName("팔로우한 유저들의 모든 루틴을 조회한다.")
    @Test
    void findAllRoutinesByFollowUsers() {
        // given
        User user = createUser("email@test.com", "nickname");
        User followUser1 = createUser("follow1@test.com", "follow1");
        User followUser2 = createUser("follow2@test.com", "follow2");

        Follow.PK followPK = Follow.createFollowPK(followUser1.getEmail(), user.getEmail());
        Follow.PK followPK2 = Follow.createFollowPK(followUser2.getEmail(), user.getEmail());

        Routine routine1 = createRoutine(followUser1, 1);
        Routine routine2 = createRoutine(followUser2, 2);

        userRepository.saveAll(List.of(user, followUser1, followUser2));
        followRepository.save(new Follow(followPK, followUser1, user));
        followRepository.save(new Follow(followPK2, followUser2, user));
        routineRepository.saveAll(List.of(routine1, routine2));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserEmailIn(
                null,
                List.of(followUser1.getEmail(), followUser2.getEmail()),
                pageable);

        // then
        assertThat(routines.getContent()).hasSize(2);
        assertThat(routines.getContent()).contains(routine1, routine2);
        assertThat(routines.getContent().get(0).getUser()).isIn(followUser1, followUser2);
        assertThat(routines.getContent().get(1).getUser()).isIn(followUser1, followUser2);
    }

    @DisplayName("팔로우한 유저의 루틴을 조회할 때 팔로우한 유저가 존재하지 않는 경우 빈 결과를 반환한다.")
    @Test
    void returnEmptyWhenNoMatchingFollowUser() {
        // given
        User user = createUser("email@test.com", "nickname");
        User followUser = createUser("follow@test.com", "follow");

        userRepository.saveAll(List.of(user, followUser));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserEmailIn(
                1,
                List.of(followUser.getEmail()),
                pageable);

        // then
        assertThat(routines.getContent()).hasSize(0);
        assertThat(routines.isEmpty()).isTrue();
    }

    @DisplayName("팔로우한 유저의 루틴을 조회할 때 팔로우한 유저의 루틴이 존재하지 않는 경우 빈 결과를 반환한다.")
    @Test
    void returnEmptyWhenNoMatchingFollowUserRoutines() {
        // given
        User user = createUser("email@test.com", "nickname");
        User followUser = createUser("follow@test.com", "follow");

        Follow.PK followPK = Follow.createFollowPK(followUser.getEmail(), user.getEmail());

        Pageable pageable = PageRequest.of(0, 10);

        userRepository.saveAll(List.of(user, followUser));
        followRepository.save(new Follow(followPK, followUser, user));

        // when
        Slice<Routine> routines = routineRepository.findByDivisionAndUserEmailIn(
                1,
                List.of(followUser.getEmail()),
                pageable);

        // then
        assertThat(routines.getContent()).hasSize(0);
        assertThat(routines.isEmpty()).isTrue();
    }

    private User createUser(String mail, String nickname) {
        return User.builder()
                .email(mail)
                .nickname(nickname)
                .build();
    }

    Routine createRoutine(int division) {
        return Routine.builder()
                .division(division)
                .build();
    }

    Routine createRoutine(User user, int division) {
        return Routine.builder()
                .user(user)
                .division(division)
                .build();
    }
}
