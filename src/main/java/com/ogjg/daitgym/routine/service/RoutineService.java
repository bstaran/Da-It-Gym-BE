package com.ogjg.daitgym.routine.service;

import com.ogjg.daitgym.comment.routine.exception.NotFoundRoutine;
import com.ogjg.daitgym.domain.TimeTemplate;
import com.ogjg.daitgym.domain.User;
import com.ogjg.daitgym.domain.routine.Day;
import com.ogjg.daitgym.domain.routine.ExerciseDetail;
import com.ogjg.daitgym.domain.routine.Routine;
import com.ogjg.daitgym.domain.routine.UserRoutineCollection;
import com.ogjg.daitgym.common.exception.exercise.NotFoundExercise;
import com.ogjg.daitgym.exercise.repository.ExerciseRepository;
import com.ogjg.daitgym.follow.repository.FollowRepository;
import com.ogjg.daitgym.like.routine.repository.RoutineLikeRepository;
import com.ogjg.daitgym.routine.dto.RoutineDetailsResponseDto;
import com.ogjg.daitgym.routine.dto.RoutineDto;
import com.ogjg.daitgym.routine.dto.RoutineListResponseDto;
import com.ogjg.daitgym.routine.dto.RoutineRequestDto;
import com.ogjg.daitgym.common.exception.routine.AlreadyScrappedRoutine;
import com.ogjg.daitgym.common.exception.routine.NoExerciseInRoutine;
import com.ogjg.daitgym.common.exception.routine.NotFoundScrappedUserRoutine;
import com.ogjg.daitgym.routine.repository.DayRepository;
import com.ogjg.daitgym.routine.repository.RoutineRepository;
import com.ogjg.daitgym.routine.repository.UserRoutineCollectionRepository;
import com.ogjg.daitgym.common.exception.user.NotFoundUser;
import com.ogjg.daitgym.common.exception.user.UnauthorizedUser;
import com.ogjg.daitgym.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ogjg.daitgym.routine.dto.RoutineDetailsResponseDto.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineService {
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final UserRoutineCollectionRepository userRoutineCollectionRepository;

    private final RoutineRepository routineRepository;
    private final FollowRepository followRepository;
    private final DayRepository dayRepository;
    private final RoutineLikeRepository routineLikeRepository;

    @Transactional(readOnly = true)
    public RoutineListResponseDto getRoutines(Pageable pageable, Integer division, String email) {

        Slice<Routine> routines = routineRepository.findAllByDivision(division, pageable);

        return getRoutineListResponseDto(routines, email);
    }

    private RoutineListResponseDto getRoutineListResponseDto(Slice<Routine> routines, String identifier) {

        Set<Long> likedRoutineIds = getLikedRoutineIds(identifier);
        Set<Long> scrappedRoutineIds = getScrappedRoutineIds(identifier);

        List<RoutineDto> routineDtos = routines.stream()
                .map(routine -> RoutineDto.builder()
                        .id(routine.getId())
                        .title(routine.getTitle())
                        .author(routine.getUser().getNickname())
                        .authorImg(routine.getUser().getImageUrl())
                        .description(routine.getContent())
                        .division(routine.getDivision())
                        .liked(likedRoutineIds.contains(routine.getId()))
                        .likeCounts(routineLikeRepository.countByRoutineId(routine.getId()))
                        .scrapped(scrappedRoutineIds.contains(routine.getId()))
                        .scrapCounts(getScrapCounts(routine.getId()))
                        .createdAt(routine.getCreatedAt())
                        .build())
                .toList();

        return RoutineListResponseDto.builder()
                .routines(routineDtos)
                .currentPage(routines.getNumber())
                .hasNext(routines.hasNext())
                .build();
    }


    private Set<Long> getLikedRoutineIds(String identifier) {
        if (isEmail(identifier)) {
            return routineLikeRepository.findLikedRoutineIdByUserEmail(identifier);
        } else {
            return routineLikeRepository.findLikedRoutineIdByUserNickname(identifier);
        }
    }

    private Set<Long> getScrappedRoutineIds(String identifier) {
        if (isEmail(identifier)) {
            return userRoutineCollectionRepository.findScrappedRoutineIdByUserEmail(identifier);
        } else {
            return userRoutineCollectionRepository.findScrappedRoutineIdByUserNickname(identifier);
        }
    }

    private boolean isEmail(String value) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return value.matches(emailRegex);
    }

    @Transactional(readOnly = true)
    public RoutineListResponseDto getUserRoutines(String nickname, Integer division, Pageable pageable) {
        Slice<Routine> routines = routineRepository.findByDivisionAndUserNickname(division, nickname, pageable);

        return getRoutineListResponseDto(routines, nickname);
    }

    @Transactional(readOnly = true)
    public RoutineListResponseDto getFollowerRoutines(Pageable pageable, Integer division, String myEmail) {

        List<String> followingEmails = followRepository.findAllByFollowerEmail(myEmail)
                .orElse(Collections.emptyList())
                .stream()
                .map(follow -> follow.getTarget().getEmail())
                .toList();

        Slice<Routine> routines = routineRepository.findByDivisionAndUserEmailIn(division, followingEmails, pageable);

        return getRoutineListResponseDto(routines, myEmail);
    }

    @Transactional(readOnly = true)
    public RoutineDetailsResponseDto getRoutineDetails(Long routineId, String userEmail) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(NotFoundRoutine::new);

        List<Day> days = dayRepository.findAllWithExerciseDetailsByRoutineId(routine.getId())
                .orElseThrow(NoExerciseInRoutine::new);

        List<DayDto> dayDtos = getDaysDto(days);

        return RoutineDetailsResponseDto.builder()
                .writer(routine.getUser().getNickname())
                .writerImg(routine.getUser().getImageUrl())
                .title(routine.getTitle())
                .description(routine.getContent())
                .division(routine.getDivision())
                .liked(routineLikeRepository
                        .existsByUserEmailAndRoutineId(userEmail, routine.getId()))
                .likeCounts(routineLikeRepository.countByRoutineId(routine.getId()))
                .scrapped(userRoutineCollectionRepository
                        .existsByUserEmailAndRoutineId(userEmail, routine.getId()))
                .scrapCounts(getScrapCounts(routine.getId()))
                .routine(RoutineDetailsResponseDto.RoutineDto.builder()
                        .id(routine.getId())
                        .days(dayDtos)
                        .build())
                .createdAt(routine.getCreatedAt())
                .build();
    }

    private List<DayDto> getDaysDto(List<Day> days) {
        return days.stream()
                .map(day -> {
                    Map<Integer, List<ExerciseDetail>> groupedByExerciseOrder = day.getExerciseDetails().stream()
                            .collect(Collectors.groupingBy(ExerciseDetail::getExerciseOrder));

                    List<ExerciseDto> exerciseDtos = groupedByExerciseOrder.entrySet().stream()
                            .map(orderEntry -> {
                                List<ExerciseSets> exerciseSets = orderEntry.getValue().stream()
                                        .map(exerciseDetail -> ExerciseSets.builder()
                                                .id(exerciseDetail.getId())
                                                .order(exerciseDetail.getSetOrder())
                                                .weights(exerciseDetail.getWeight())
                                                .counts(exerciseDetail.getRepetitionCount())
                                                .restTime(exerciseDetail.getRestTime())
                                                .completed(false)
                                                .build())
                                        .toList();

                                int exerciseOrder = orderEntry.getKey();
                                ExerciseDetail exerciseDetail = orderEntry.getValue().get(0);

                                return ExerciseDto.builder()
                                        .id(exerciseDetail.getExercise().getId())
                                        .order(exerciseOrder)
                                        .part(exerciseDetail.getExercise().getExercisePart().getPart())
                                        .name(exerciseDetail.getExercise().getName())
                                        .restTime(RestTimeDto.builder()
                                                .hours(0)
                                                .minutes(0)
                                                .seconds(0)
                                                .build())
                                        .exerciseSets(exerciseSets)
                                        .build();
                            })
                            .toList();

                    return DayDto.builder()
                            .id(day.getId())
                            .order(day.getDayNumber())
                            .isSpread(false)
                            .exercises(exerciseDtos)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void createRoutine(RoutineRequestDto routineRequestDto, String email) {

        Routine routine = Routine.builder()
                .user(userRepository.findByEmail(email)
                        .orElseThrow(NotFoundUser::new))
                .title(routineRequestDto.getTitle())
                .content(routineRequestDto.getDescription())
                .division(routineRequestDto.getDivision())
                .duration(routineRequestDto.getRoutine().getDays().size())
                .build();

        routineRepository.save(routine);

        routineRequestDto.getRoutine().getDays()
                .forEach(dayDto -> {
                    Day day = Day.builder()
                            .routine(routine)
                            .dayNumber(dayDto.getOrder())
                            .build();

                    dayRepository.save(day);

                    dayDto.getExercises().forEach(exerciseDto -> {
                        exerciseDto.getExerciseSets().forEach(exerciseSetDto -> {
                            RoutineRequestDto.RestTimeDto restTime = exerciseDto.getRestTime();
                            TimeTemplate timeTemplate = new TimeTemplate(restTime.getHours(), restTime.getMinutes(), restTime.getSeconds());
                            ExerciseDetail exerciseDetail = ExerciseDetail.builder()
                                    .day(day)
                                    .exercise(exerciseRepository.findByName(exerciseDto.getName())
                                            .orElseThrow(NotFoundExercise::new))
                                    .exerciseOrder(exerciseDto.getOrder())
                                    .setOrder(exerciseSetDto.getOrder())
                                    .setCount(exerciseSetDto.getOrder())
                                    .repetitionCount(exerciseSetDto.getCounts())
                                    .weight(exerciseSetDto.getWeights())
                                    .restTime(timeTemplate)
                                    .build();

                            day.addExerciseDetail(exerciseDetail);
                        });
                    });
                });
    }

    @Transactional
    public void deleteRoutine(Long routineId, String email) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(NotFoundRoutine::new);

        if (!routine.getUser().getEmail().equals(email)) {
            throw new UnauthorizedUser("사용자에게 루틴을 삭제할 권한이 없습니다.");
        }

        routineRepository.deleteById(routineId);
    }

    @Transactional
    public Long scrapRoutine(Long routineId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(NotFoundUser::new);

        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(NotFoundRoutine::new);

        UserRoutineCollection userRoutineCollection = new UserRoutineCollection(user, routine);
        if (userRoutineCollectionRepository.findById(userRoutineCollection.getPk()).isPresent()) {
            throw new AlreadyScrappedRoutine();
        }

        userRoutineCollectionRepository.save(userRoutineCollection);
        return getScrapCounts(routineId);
    }

    private long getScrapCounts(Long routineId) {
        return userRoutineCollectionRepository.countByRoutineId(routineId);
    }

    @Transactional
    public Long unscrapRoutine(Long routineId, String email) {

        routineRepository.findById(routineId)
                .orElseThrow(NotFoundRoutine::new);

        UserRoutineCollection.PK pk = new UserRoutineCollection.PK(email, routineId);
        UserRoutineCollection userRoutineCollection = userRoutineCollectionRepository.findById(pk)
                .orElseThrow(NotFoundScrappedUserRoutine::new);

        userRoutineCollectionRepository.delete(userRoutineCollection);
        return getScrapCounts(routineId);
    }

    @Transactional(readOnly = true)
    public RoutineListResponseDto getScrappedRoutines(String email, Pageable pageable) {
        Slice<Routine> routinesByUserEmail = userRoutineCollectionRepository.findRoutinesByUserEmail(email, pageable);

        return getRoutineListResponseDto(routinesByUserEmail, email);
    }
}
