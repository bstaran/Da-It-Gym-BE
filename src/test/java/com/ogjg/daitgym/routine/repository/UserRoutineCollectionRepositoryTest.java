package com.ogjg.daitgym.routine.repository;

import com.ogjg.daitgym.domain.Role;
import com.ogjg.daitgym.domain.User;
import com.ogjg.daitgym.domain.routine.Routine;
import com.ogjg.daitgym.domain.routine.UserRoutineCollection;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
class UserRoutineCollectionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private UserRoutineCollectionRepository userRoutineCollectionRepository;

    @DisplayName("유저가 스크랩한 루틴을 유저의 이메일로 조회한다.")
    @Test
    void testFindRoutinesByUserEmail() {
        // given
        User user = createTestUser("test@test.com", "test");
        User routineAuthor = createTestUser("author@test.com", "author");
        Routine routine1 = getRoutine(routineAuthor, "title1", "content1");
        Routine routine2 = getRoutine(routineAuthor, "title2", "content2");

        UserRoutineCollection userRoutineCollection1 = new UserRoutineCollection(user, routine1);
        UserRoutineCollection userRoutineCollection2 = new UserRoutineCollection(user, routine2);

        Pageable pageable = PageRequest.of(0, 10);

        userRepository.saveAll(List.of(user, routineAuthor));
        routineRepository.saveAll(List.of(routine1, routine2));
        userRoutineCollectionRepository.saveAll(List.of(userRoutineCollection1, userRoutineCollection2));

        // when
        Slice<Routine> routinesByUserEmail = userRoutineCollectionRepository.findRoutinesByUserEmail(user.getEmail(), pageable);

        // then
        assertThat(routinesByUserEmail.getContent().size()).isEqualTo(2);
    }

    @DisplayName("존재하지 않는 이메일로 스크랩한 루틴을 조회하면 빈 결과를 반환한다.")
    @Test
    void testFindRoutinesByNonExistingEmail() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Routine> routines = userRoutineCollectionRepository.findRoutinesByUserEmail("noneExistingEmail@test.com", pageable);

        // then
        assertThat(routines.getContent()).isEmpty();
    }

    @DisplayName("루틴이 스크랩된 개수를 조회한다.")
    @Test
    void testCountOfScrapedRoutines() {
        // given
        User user = createTestUser("test@test.com", "test");
        User routineAuthor = createTestUser("author@test.com", "author");
        Routine routine1 = getRoutine(routineAuthor, "title1", "content1");
        Routine routine2 = getRoutine(routineAuthor, "title2", "content2");

        UserRoutineCollection userRoutineCollection1 = new UserRoutineCollection(user, routine1);
        UserRoutineCollection userRoutineCollection2 = new UserRoutineCollection(user, routine2);

        userRepository.saveAll(List.of(user, routineAuthor));
        routineRepository.saveAll(List.of(routine1, routine2));
        userRoutineCollectionRepository.saveAll(List.of(userRoutineCollection1, userRoutineCollection2));

        // when
        long routine1Count = userRoutineCollectionRepository.countByRoutineId(routine1.getId());
        long routine2Count = userRoutineCollectionRepository.countByRoutineId(routine2.getId());

        // then
        assertThat(routine1Count).isEqualTo(1);
        assertThat(routine2Count).isEqualTo(1);
    }

    @DisplayName("스크랩되지 않은 루틴의 스크랩 개수를 조회하면 0을 반환한다.")
    @Test
    void testCountOfUnscrapedRoutine() {
        // given
        User user = createTestUser("test@test.com", "test");
        Routine routine = getRoutine(user, "title1", "content1");

        userRepository.save(user);
        routineRepository.save(routine);

        // when
        long count = userRoutineCollectionRepository.countByRoutineId(routine.getId());

        // then
        assertThat(count).isEqualTo(0);
    }

    @DisplayName("유저가 스크랩한 루틴 목록을 이메일로 조회한다.")
    @Test
    void testFindScrappedRoutinesByUserEmail() {
        // given
        User user = createTestUser("test@test.com", "test");
        User routineAuthor = createTestUser("author@test.com", "author");
        Routine routine1 = getRoutine(routineAuthor, "title1", "content1");
        Routine routine2 = getRoutine(routineAuthor, "title2", "content2");

        UserRoutineCollection userRoutineCollection1 = new UserRoutineCollection(user, routine1);
        UserRoutineCollection userRoutineCollection2 = new UserRoutineCollection(user, routine2);

        userRepository.saveAll(List.of(user, routineAuthor));
        routineRepository.saveAll(List.of(routine1, routine2));
        userRoutineCollectionRepository.saveAll(List.of(userRoutineCollection1, userRoutineCollection2));

        // when
        Set<Long> scrapedRoutineIds = userRoutineCollectionRepository.findScrappedRoutineIdByUserEmail(user.getEmail());

        // then
        assertThat(scrapedRoutineIds.size()).isEqualTo(2);
        assertThat(scrapedRoutineIds).contains(routine1.getId(), routine2.getId());
    }

    @DisplayName("이메일로 조회한 유저의 스크랩 루틴 목록이 없다면 빈 결과를 반환한다.")
    @Test
    void getEmptyResultWhenNoScrappedRoutinesByUserEmail() {
        // given
        User user = createTestUser("test@test.com", "test");
        userRepository.save(user);

        // when
        Set<Long> scrapedRoutineIds = userRoutineCollectionRepository.findScrappedRoutineIdByUserEmail(user.getEmail());

        // then
        assertThat(scrapedRoutineIds.size()).isEqualTo(0);
        assertThat(scrapedRoutineIds).isEmpty();
    }

    @DisplayName("유저가 스크랩한 루틴 목록을 닉네임으로 조회한다.")
    @Test
    void testFindScrappedRoutinesByUserNickname() {
        // given
        User user = createTestUser("test@test.com", "test");
        User routineAuthor = createTestUser("author@test.com", "author");
        Routine routine1 = getRoutine(routineAuthor, "title1", "content1");
        Routine routine2 = getRoutine(routineAuthor, "title2", "content2");

        UserRoutineCollection userRoutineCollection1 = new UserRoutineCollection(user, routine1);
        UserRoutineCollection userRoutineCollection2 = new UserRoutineCollection(user, routine2);

        userRepository.saveAll(List.of(user, routineAuthor));
        routineRepository.saveAll(List.of(routine1, routine2));
        userRoutineCollectionRepository.saveAll(List.of(userRoutineCollection1, userRoutineCollection2));

        // when
        Set<Long> scrapedRoutineIds = userRoutineCollectionRepository.findScrappedRoutineIdByUserNickname(user.getNickname());

        // then
        assertThat(scrapedRoutineIds.size()).isEqualTo(2);
        assertThat(scrapedRoutineIds).contains(routine1.getId(), routine2.getId());
    }

    @DisplayName("닉네임으로 조회한 유저의 스크랩 루틴 목록이 없다면 빈 결과를 반환한다.")
    @Test
    void getEmptyResultWhenNoScrappedRoutinesByUserNickname() {
        // given
        User user = createTestUser("test@test.com", "test");
        userRepository.save(user);

        // when
        Set<Long> scrapedRoutineIds = userRoutineCollectionRepository.findScrappedRoutineIdByUserNickname(user.getNickname());

        // then
        assertThat(scrapedRoutineIds.size()).isEqualTo(0);
        assertThat(scrapedRoutineIds).isEmpty();
    }

    @DisplayName("유저가 스크랩한 루틴이다.")
    @Test
    void testExistsByUserEmailAndRoutineId() {
        // given
        User user = createTestUser("test@test.com", "test");
        User routineAuthor = createTestUser("author@test.com", "author");
        Routine routine1 = getRoutine(routineAuthor, "title1", "content1");

        UserRoutineCollection userRoutineCollection1 = new UserRoutineCollection(user, routine1);

        userRepository.saveAll(List.of(user, routineAuthor));
        routineRepository.save(routine1);
        userRoutineCollectionRepository.save(userRoutineCollection1);

        // when
        boolean exists = userRoutineCollectionRepository.existsByUserEmailAndRoutineId(user.getEmail(), routine1.getId());

        // then
        assertThat(exists).isTrue();
    }

    @DisplayName("유저가 스크랩한 루틴이 아니다.")
    @Test
    void testNotExistsByUserEmailAndRoutineId() {
        // given
        User user = createTestUser("test@test.com", "test");
        Routine routine1 = getRoutine(user, "title1", "content1");

        userRepository.save(user);

        // when
        boolean exists = userRoutineCollectionRepository.existsByUserEmailAndRoutineId(user.getEmail(), routine1.getId());

        // then
        assertThat(exists).isFalse();
    }

    private User createTestUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }

    private Routine getRoutine(User user, String title, String content) {
        return Routine.builder()
                .user(user)
                .title(title)
                .content(content)
                .duration(30)
                .division(3)
                .build();
    }
}
