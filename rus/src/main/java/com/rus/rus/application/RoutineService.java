package com.rus.rus.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.req.RoutineAddRequestDto;
import com.rus.rus.controller.dto.req.RoutineUpdateRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.controller.dto.*;
import com.rus.rus.controller.dto.res.*;
import com.rus.rus.domain.*;
import com.rus.rus.infra.repository.*;

import lombok.RequiredArgsConstructor;

/**
 * 루틴 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 사용자의 루틴 추천, 추가, 수정, 삭제, 달성 처리 등 다양한 기능을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class RoutineService {

        private static final List<String> ALL_CATEGORIES = Arrays.asList("수면", "운동", "영양소", "햇빛", "사회적유대감");
        private static final int TOTAL_RECOMMEND_COUNT = 10;
        private static final int SERA_ROUTINE_LUX_BONUS = 10;
        private static final int GENERAL_ROUTINE_LUX_BOUNS = 5;

        private final RoutineRepository routineRepository;
        private final RoutineCollectionRepository routineCollectionRepository;
        private final RoutineCollectionMapperRepository routineCollectionMapperRepository;
        private final UserRoutineRepository userRoutineRepository;
        private final UserAttainmentRepository userAttainmentRepository;
        private final CategoryRepository categoryRepository;
        private final RoutineSeraRepository routineSeraRepository;
        private final RoutineSeraAttainmentRepository routineSeraAttainmentRepository;
        private final UserProfileRepository userProfileRepository;
        private final ChallengeUserRepository challengeUserRepository;
        private final RecoveryMissionRepository recoveryMissionRepository;
        private final RecoveryMissionItemRepository recoveryMissionItemRepository;

        /**
         * (API-3.1) 추천 루틴 생성
         * 요청된 카테고리에 가중치를 부여하여 총 10개의 루틴을 추천합니다.
         * 5개 모든 카테고리가 최소 1개 이상 포함되도록 보장합니다.
         *
         * @param requestedCategoryNames 추천 가중치를 부여할 카테고리 이름 목록
         * @return 추천 루틴 목록이 포함된 {@link RecommendResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public RecommendResponseDto getRecommendedRoutines(List<String> requestedCategoryNames) {
                List<String> requested = Optional.ofNullable(requestedCategoryNames)
                                .orElse(Collections.emptyList())
                                .stream().filter(ALL_CATEGORIES::contains).distinct().toList();

                List<Routine> all = routineRepository.findRoutinesByCategoryNames(ALL_CATEGORIES);
                Map<String, List<Routine>> byCat = all.stream()
                                .collect(Collectors.groupingBy(r -> r.getCategory().getValue()));

                Map<String, Deque<Routine>> deck = new HashMap<>();
                for (String cat : ALL_CATEGORIES) {
                        List<Routine> list = new ArrayList<>(byCat.getOrDefault(cat, List.of()));
                        Collections.shuffle(list);
                        deck.put(cat, new ArrayDeque<>(list));
                }

                List<Routine> result = new ArrayList<>(TOTAL_RECOMMEND_COUNT);

                for (String cat : ALL_CATEGORIES) {
                        Deque<Routine> q = deck.get(cat);
                        if (q != null && !q.isEmpty())
                                result.add(q.pollFirst());
                }

                int remaining = TOTAL_RECOMMEND_COUNT - result.size();
                Map<String, Integer> taken = new HashMap<>();
                for (Routine r : result)
                        taken.merge(r.getCategory().getValue(), 1, Integer::sum);

                List<String> order = new ArrayList<>(requested);
                if (!order.isEmpty()) {
                        int start = new Random().nextInt(order.size());
                        Collections.rotate(order, -start);
                }

                final int MAX_PER_CATEGORY = 3;

                int idx = 0;
                while (remaining > 0 && !order.isEmpty()) {
                        String cat = order.get(idx % order.size());
                        Deque<Routine> q = deck.get(cat);
                        int cur = taken.getOrDefault(cat, 0);
                        if (q != null && !q.isEmpty() && cur < MAX_PER_CATEGORY) {
                                result.add(q.pollFirst());
                                taken.put(cat, cur + 1);
                                remaining--;
                                idx++;
                        } else {
                                order.remove(cat);
                        }
                }

                if (remaining > 0) {
                        List<String> others = ALL_CATEGORIES.stream()
                                        .filter(c -> !requested.contains(c)).toList();
                        int j = 0;
                        while (remaining > 0 && !others.isEmpty()) {
                                String cat = others.get(j % others.size());
                                Deque<Routine> q = deck.get(cat);
                                int cur = taken.getOrDefault(cat, 0);
                                if (q != null && !q.isEmpty() && cur < MAX_PER_CATEGORY) {
                                        result.add(q.pollFirst());
                                        taken.put(cat, cur + 1);
                                        remaining--;
                                }
                                j++;
                        }
                }

                Collections.shuffle(result);

                List<RecommendedRoutineDto> dtoList = result.stream()
                                .map(r -> RecommendedRoutineDto.builder()
                                                .rid(r.getRid())
                                                .content(r.getContent())
                                                .category(CategoryDto.builder()
                                                                .categoryId(r.getCategory().getCategoryId())
                                                                .value(r.getCategory().getValue())
                                                                .build())
                                                .build())
                                .toList();

                return RecommendResponseDto.builder()
                                .category(requested)
                                .recommend(dtoList)
                                .build();
        }

        /**
         * (API-3.2) 모든 루틴 정보 반환
         * 시스템에 등록된 모든 기본 루틴 목록을 조회합니다.
         *
         * @return 모든 루틴 정보가 포함된 {@link AllRoutinesResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public AllRoutinesResponseDto getAllRoutines() {
                List<Routine> allRoutines = routineRepository.findAll();

                List<RoutineDto> dtoList = allRoutines.stream()
                                .map(routine -> RoutineDto.builder()
                                                .rid(routine.getRid())
                                                .content(routine.getContent())
                                                .category(CategoryDto.builder()
                                                                .categoryId(routine.getCategory().getCategoryId())
                                                                .value(routine.getCategory().getValue())
                                                                .build())
                                                .build())
                                .collect(Collectors.toList());

                return AllRoutinesResponseDto.builder()
                                .routines(dtoList)
                                .build();
        }

        /**
         * (API-3.3) Sera의 추천 루틴 모음 반환
         * 시스템에 등록된 모든 루틴 컬렉션(패키지) 목록을 조회합니다.
         * 각 컬렉션에 포함된 루틴 정보도 함께 반환합니다.
         *
         * @return 모든 루틴 컬렉션 정보가 포함된 {@link AllCollectionsResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public AllCollectionsResponseDto getAllRoutineCollections() {
                List<RoutineCollection> allCollections = routineCollectionRepository.findAll();
                List<RoutineCollectionMapper> allMappers = routineCollectionMapperRepository.findAllWithDetails();

                Map<Integer, List<RoutineDto>> routinesByCollectionId = allMappers.stream()
                                .collect(Collectors.groupingBy(
                                                mapper -> mapper.getRoutineCollection().getCollectionId(),
                                                Collectors.mapping(
                                                                mapper -> {
                                                                        var routine = mapper.getRoutine();
                                                                        return RoutineDto.builder()
                                                                                        .rid(routine.getRid())
                                                                                        .content(routine.getContent())
                                                                                        .category(CategoryDto.builder()
                                                                                                        .categoryId(routine
                                                                                                                        .getCategory()
                                                                                                                        .getCategoryId())
                                                                                                        .value(routine.getCategory()
                                                                                                                        .getValue())
                                                                                                        .build())
                                                                                        .build();
                                                                },
                                                                Collectors.toList())));

                List<CollectionDetailDto> resultDtoList = allCollections.stream()
                                .map(collection -> {
                                        CollectionDetailDto dto = CollectionDetailDto.builder()
                                                        .collectionId(collection.getCollectionId())
                                                        .title(collection.getTitle())
                                                        .subTitle(collection.getSubTitle())
                                                        .guide(collection.getGuide())
                                                        .build();
                                        dto.setRoutines(routinesByCollectionId.get(collection.getCollectionId()));
                                        return dto;
                                })
                                .collect(Collectors.toList());

                return AllCollectionsResponseDto.builder()
                                .collections(resultDtoList)
                                .build();
        }

        /**
         * (API-4.1) 루틴 추가
         * 특정 사용자에게 기본 루틴을 여러 개 추가합니다.
         *
         * @param uid     루틴을 추가할 사용자의 고유 식별자(UID)
         * @param request 추가할 루틴들의 ID 목록이 담긴 {@link RoutineAddRequestDto} 객체
         * @throws IllegalArgumentException 존재하지 않는 루틴 ID가 포함된 경우 발생
         */
        @Transactional
        public void addRoutinesToUser(String uid, RoutineAddRequestDto request) {
                List<Routine> routines = routineRepository.findByRidIn(request.getRid());

                if (routines.size() != request.getRid().size()) {
                        throw new IllegalArgumentException("존재하지 않는 루틴 ID가 포함되어 있습니다.");
                }

                UserProfile userProfile = userProfileRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                for (Routine routine : routines) {
                        UserRoutine userRoutine = UserRoutine.builder()
                                        .userProfile(userProfile)
                                        .category(routine.getCategory())
                                        .content(routine.getContent())
                                        .notification(null)
                                        .build();
                        userRoutineRepository.save(userRoutine);
                }
        }

        /**
         * (API-4.2) 루틴 추가 - 직접 작성
         * 사용자가 직접 작성한 커스텀 루틴을 추가합니다.
         *
         * @param uid     루틴을 추가할 사용자의 고유 식별자(UID)
         * @param request 추가할 커스텀 루틴의 내용과 카테고리 ID가 담긴 {@link RoutineAddCustomRequestDto}
         *                객체
         * @throws IllegalArgumentException 카테고리 ID나 내용이 비어있는 경우 발생
         */
        @Transactional
        public void addCustomRoutineToUser(String uid, RoutineAddCustomRequestDto request) {
                if (request.getCategoryId() == null || request.getContent() == null
                                || request.getContent().trim().isEmpty()) {
                        throw new IllegalArgumentException("카테고리 ID와 루틴 내용은 필수입니다.");
                }

                UserProfile userProfile = userProfileRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

                UserRoutine userRoutine = UserRoutine.builder()
                                .userProfile(userProfile)
                                .category(category)
                                .content(request.getContent().trim())
                                .notification(null)
                                .build();
                userRoutineRepository.save(userRoutine);
        }

        /**
         * (API-4.3) 요청 날짜 기준 사용자의 루틴 정보 반환
         * 특정 사용자의 모든 개인 루틴 목록과 오늘의 달성 여부를 조회합니다.
         *
         * @param uid 조회할 사용자의 고유 식별자(UID)
         * @return 사용자의 개인 루틴 목록이 포함된 {@link PersonalRoutineResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public PersonalRoutineResponseDto getPersonalRoutines(String uid) {
                List<UserRoutine> userRoutines = userRoutineRepository.findByUserProfile_Uid(uid);
                LocalDate today = LocalDate.now();

                List<UserAttainment> todayAttainments = userAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, today.atStartOfDay(),
                                                today.plusDays(1).atStartOfDay());

                Set<Integer> completedRoutineIds = todayAttainments.stream()
                                .map(ua -> ua.getUserRoutine().getId())
                                .collect(Collectors.toSet());

                List<PersonalRoutineItemDto> routineItems = userRoutines.stream()
                                .map(userRoutine -> {
                                        CategoryDto categoryDto = null;
                                        if (userRoutine.getCategory() != null) {
                                                categoryDto = CategoryDto.builder()
                                                                .categoryId(userRoutine.getCategory().getCategoryId())
                                                                .value(userRoutine.getCategory().getValue())
                                                                .build();
                                        }

                                        boolean isComplete = completedRoutineIds.contains(userRoutine.getId());

                                        return PersonalRoutineItemDto.builder()
                                                        .id(userRoutine.getId())
                                                        .category(categoryDto)
                                                        .content(userRoutine.getContent())
                                                        .notification(userRoutine.getNotification() != null
                                                                        ? userRoutine.getNotification().toString()
                                                                        : null)
                                                        .complete(isComplete)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return PersonalRoutineResponseDto.builder()
                                .routines(routineItems)
                                .build();
        }

        /**
         * (API-4.4) 특정 루틴 정보 반환
         * 특정 개인 루틴 하나의 상세 정보를 조회합니다.
         *
         * @param id       조회할 루틴의 고유 ID
         * @param tokenUid 요청을 보낸 사용자의 JWT 토큰에서 추출한 UID (본인 확인용)
         * @return 특정 루틴의 상세 정보가 포함된 {@link SingleRoutineResponseDto} 객체
         * @throws IllegalArgumentException 루틴이 존재하지 않거나 본인의 루틴이 아닐 경우 발생
         */
        @Transactional(readOnly = true)
        public SingleRoutineResponseDto getRoutineById(Integer id, String tokenUid) {
                UserRoutine userRoutine = userRoutineRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));

                if (!userRoutine.getUserProfile().getUid().equals(tokenUid)) {
                        throw new IllegalArgumentException("본인의 루틴만 조회할 수 있습니다.");
                }

                CategoryDto categoryDto = null;
                if (userRoutine.getCategory() != null) {
                        categoryDto = CategoryDto.builder()
                                        .categoryId(userRoutine.getCategory().getCategoryId())
                                        .value(userRoutine.getCategory().getValue())
                                        .build();
                }

                LocalDate today = LocalDate.now();
                boolean isComplete = userAttainmentRepository.existsByUserRoutine_IdAndTimestampBetween(
                                id, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

                PersonalRoutineItemDto routineDto = PersonalRoutineItemDto.builder()
                                .id(userRoutine.getId())
                                .category(categoryDto)
                                .content(userRoutine.getContent())
                                .notification(userRoutine.getNotification() != null
                                                ? userRoutine.getNotification().toString()
                                                : null)
                                .complete(isComplete)
                                .build();

                return SingleRoutineResponseDto.builder()
                                .routine(routineDto)
                                .build();
        }

        /**
         * (API-4.5) 사용자 루틴 - 루틴 수정
         * 특정 개인 루틴의 내용 또는 카테고리를 수정합니다.
         *
         * @param id       수정할 루틴의 고유 ID
         * @param request  수정할 내용과 카테고리 ID가 담긴 {@link RoutineUpdateRequestDto} 객체
         * @param tokenUid 요청을 보낸 사용자의 JWT 토큰에서 추출한 UID (본인 확인용)
         * @throws IllegalArgumentException 루틴이 존재하지 않거나 본인의 루틴이 아닐 경우 발생
         */
        @Transactional
        public void updateRoutine(Integer id, RoutineUpdateRequestDto request, String tokenUid) {
                UserRoutine userRoutine = userRoutineRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));

                if (!userRoutine.getUserProfile().getUid().equals(tokenUid)) {
                        throw new IllegalArgumentException("본인의 루틴만 수정할 수 있습니다.");
                }

                Category category = categoryRepository.findByCategoryId(request.getCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

                userRoutine.setCategory(category);
                userRoutine.setContent(request.getContent());
                userRoutineRepository.save(userRoutine);
        }

        /**
         * (API-4.6) 사용자 루틴 - 루틴 삭제
         * 특정 개인 루틴을 삭제합니다.
         *
         * @param id       삭제할 루틴의 고유 ID
         * @param tokenUid 요청을 보낸 사용자의 JWT 토큰에서 추출한 UID (본인 확인용)
         * @throws IllegalArgumentException 루틴이 존재하지 않거나 본인의 루틴이 아닐 경우 발생
         */
        @Transactional
        public void deleteRoutine(Integer id, String tokenUid) {
                UserRoutine userRoutine = userRoutineRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));

                if (!userRoutine.getUserProfile().getUid().equals(tokenUid)) {
                        throw new IllegalArgumentException("본인의 루틴만 삭제할 수 있습니다.");
                }

                userRoutineRepository.delete(userRoutine);
        }

        /**
         * (API-4.8) 사용자 루틴 - 체크 해제
         * 사용자의 일반 루틴 달성 체크를 해제하고, 지급되었던 lux를 회수합니다.
         *
         * @param uid       체크를 해제할 사용자의 고유 식별자(UID)
         * @param routineId 체크를 해제할 루틴의 고유 ID
         */
        @Transactional
        public void uncheckRoutineAttainment(String uid, Integer routineId) {
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

                // 1. 오늘 달성한 기록이 있는지 확인합니다.
                List<UserAttainment> attainments = userAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, startOfDay, endOfDay);
                Optional<UserAttainment> attainmentToCancel = attainments.stream()
                                .filter(a -> a.getUserRoutine().getId().equals(routineId))
                                .findFirst();

                // 2. 달성 기록이 있다면 lux를 차감하고 기록을 삭제합니다.
                if (attainmentToCancel.isPresent()) {
                        UserProfile userProfile = userProfileRepository.findById(uid)
                                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

                        int newLux = userProfile.getLux() - GENERAL_ROUTINE_LUX_BOUNS;
                        userProfile.setLux(Math.max(0, newLux)); // lux가 0 미만으로 내려가지 않도록 보장
                        userProfileRepository.save(userProfile);

                        userAttainmentRepository.delete(attainmentToCancel.get());
                }
        }

        /**
         * (API-4.9) 요청 날짜 기준 사용자의 추천 루틴 정보 반환
         * 특정 사용자의 Sera 추천 루틴 목록과 오늘의 달성 여부를 조회합니다.
         *
         * @param uid 조회할 사용자의 고유 식별자(UID)
         * @return Sera 추천 루틴 목록이 포함된 {@link RecommendRoutineResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public RecommendRoutineResponseDto getRecommendRoutines(String uid) {
                // 1. routines_sera 테이블에서 해당 사용자의 추천 루틴을 모두 조회합니다.
                List<RoutineSera> seraRoutines = routineSeraRepository.findByUserProfile_Uid(uid);

                // 2. routines_sera_attainment 테이블에서 '오늘' 달성한 기록만 조회하기 위해 시간 범위를 설정합니다.
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

                // 3. 오늘 달성한 Sera 추천 루틴 기록을 조회합니다.
                List<RoutineSeraAttainment> todayAttainments = routineSeraAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, startOfDay, endOfDay);

                // 4. 조회 성능을 위해 오늘 달성한 루틴의 ID만 Set으로 추출합니다.
                Set<Integer> completedRoutineIds = todayAttainments.stream()
                                .map(attainment -> attainment.getRoutineSera().getId())
                                .collect(Collectors.toSet());

                // 5. 각 추천 루틴에 대해 오늘 달성했는지 여부를 확인하고 DTO로 변환합니다.
                List<RecommendRoutineItemDto> routineItems = seraRoutines.stream()
                                .map(seraRoutine -> {
                                        CategoryDto categoryDto = null;
                                        if (seraRoutine.getCategory() != null) {
                                                categoryDto = CategoryDto.builder()
                                                                .categoryId(seraRoutine.getCategory().getCategoryId())
                                                                .value(seraRoutine.getCategory().getValue())
                                                                .build();
                                        }

                                        // Set에 해당 루틴의 ID가 포함되어 있는지 확인하여 달성 여부(complete)를 결정합니다.
                                        boolean isComplete = completedRoutineIds.contains(seraRoutine.getId());

                                        return RecommendRoutineItemDto.builder()
                                                        .id(seraRoutine.getId())
                                                        .category(categoryDto)
                                                        .content(seraRoutine.getContent())
                                                        .complete(isComplete) // API 명세에 따라 달성 여부 플래그를 추가
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 6. API 명세 형식에 맞춰 최종 응답 DTO를 생성하여 반환합니다.
                return RecommendRoutineResponseDto.builder()
                                .routines(routineItems)
                                .build();
        }

        /**
         * (API-4.10) Sera의 추천 루틴 - 달성 체크
         * Sera 추천 루틴을 달성 처리하고, 사용자에게 보상 lux를 지급합니다.
         *
         * @param uid       루틴을 달성한 사용자의 고유 식별자(UID)
         * @param routineId 달성한 Sera 추천 루틴의 고유 ID
         * @throws IllegalArgumentException 루틴이 존재하지 않거나, 본인의 루틴이 아니거나, 이미 달성한 경우 발생
         */
        @Transactional
        public void checkSeraRoutineAttainment(String uid, Integer routineId) {
                RoutineSera seraRoutine = routineSeraRepository.findById(routineId)
                                .orElseThrow(() -> new IllegalArgumentException("Sera 루틴을 찾을 수 없습니다."));

                if (!seraRoutine.getUserProfile().getUid().equals(uid)) {
                        throw new IllegalArgumentException("본인의 Sera 루틴만 체크할 수 있습니다.");
                }

                LocalDate today = LocalDate.now();
                boolean alreadyChecked = routineSeraAttainmentRepository
                                .existsByUserProfile_UidAndRoutineSera_IdAndTimestampBetween(
                                                uid, routineId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

                if (alreadyChecked) {
                        throw new IllegalArgumentException("이미 체크된 Sera 루틴입니다.");
                }

                UserProfile userProfile = userProfileRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                RoutineSeraAttainment attainment = RoutineSeraAttainment.builder()
                                .userProfile(userProfile)
                                .routineSera(seraRoutine)
                                .timestamp(LocalDateTime.now())
                                .build();
                routineSeraAttainmentRepository.save(attainment);

                userProfile.setLux(userProfile.getLux() + SERA_ROUTINE_LUX_BONUS);
                userProfileRepository.save(userProfile);
        }

        /**
         * (API-4.11) 챌린지 미션 - 발생 여부 조회
         * 오늘의 챌린지 미션 발생 여부와 상태를 조회합니다.
         *
         * @param uid 조회할 사용자의 고유 식별자(UID)
         * @return 챌린지 상태 정보가 포함된 {@link ChallengeStatusResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public ChallengeStatusResponseDto getChallengeStatus(String uid) {
                Optional<ChallengeUser> challengeUserOpt = challengeUserRepository.findById(uid);

                if (challengeUserOpt.isEmpty()) {
                        return ChallengeStatusResponseDto.builder()
                                        .challenge(null)
                                        .participants(0)
                                        .isTarget(false)
                                        .check(false)
                                        .build();
                }

                ChallengeUser challengeUser = challengeUserOpt.get();

                if (challengeUser.getChallengeCategoryId() == null || challengeUser.getChallengeContent() == null) {
                        return ChallengeStatusResponseDto.builder()
                                        .challenge(null)
                                        .participants(0)
                                        .isTarget(false)
                                        .check(false)
                                        .build();
                }

                Category category = categoryRepository.findById(challengeUser.getChallengeCategoryId())
                                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

                LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay();

                int participants = challengeUserRepository
                                .countByChallengeCategoryIdAndChallengeContentAndDatetimeBetween(
                                                challengeUser.getChallengeCategoryId(),
                                                challengeUser.getChallengeContent(),
                                                startOfDay,
                                                endOfDay);

                CategoryDto categoryDto = CategoryDto.builder()
                                .categoryId(category.getCategoryId())
                                .value(category.getValue())
                                .build();

                ChallengeInfoDto challengeDto = ChallengeInfoDto.builder()
                                .category(categoryDto)
                                .content(challengeUser.getChallengeContent())
                                .build();

                return ChallengeStatusResponseDto.builder()
                                .challenge(challengeDto)
                                .participants(participants)
                                .isTarget(true)
                                .check(challengeUser.getCheck() != null)
                                .build();
        }

        /**
         * (API-4.12) 챌린지 미션 - 도전 수락
         * 챌린지 미션 도전을 수락 처리합니다.
         * '도전' 버튼을 누른 상태로, 아직 달성은 하지 않은 상태(`check` = false)로 변경합니다.
         *
         * @param uid 챌린지를 수락한 사용자의 고유 식별자(UID)
         * @throws IllegalArgumentException 해당 사용자가 챌린지 대상이 아닐 경우 발생
         */
        @Transactional
        public void acceptChallenge(String uid) {
                ChallengeUser challengeUser = challengeUserRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("챌린지 대상자가 아닙니다."));

                challengeUser.setCheck(false);
                challengeUserRepository.save(challengeUser);
        }

        /**
         * (API-4.13) 챌린지 미션 - 다음에
         * 오늘의 챌린지 미션을 보류(다음에 하기) 처리합니다.
         * 대상자 목록에서 해당 사용자를 삭제합니다.
         *
         * @param uid 챌린지를 보류한 사용자의 고유 식별자(UID)
         * @throws IllegalArgumentException 해당 사용자가 챌린지 대상이 아닐 경우 발생
         */
        @Transactional
        public void postponeChallenge(String uid) {
                ChallengeUser challengeUser = challengeUserRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("챌린지 대상자가 아닙니다."));

                challengeUserRepository.delete(challengeUser);
        }

        /**
         * (API-4.14) 챌린지 미션 - 달성 체크
         * 챌린지 미션을 달성 완료 처리합니다.
         * `check` 상태를 `true`로 변경합니다.
         *
         * @param uid 챌린지를 달성한 사용자의 고유 식별자(UID)
         * @throws IllegalArgumentException 챌린지 대상이 아니거나, 도전을 수락하지 않았거나, 이미 완료한 경우 발생
         */
        @Transactional
        public void completeChallengeAttainment(String uid) {
                ChallengeUser challengeUser = challengeUserRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("챌린지 대상자가 아닙니다."));

                if (challengeUser.getCheck() == null) {
                        throw new IllegalArgumentException("먼저 챌린지에 도전해야 합니다.");
                }

                if (Boolean.TRUE.equals(challengeUser.getCheck())) {
                        throw new IllegalArgumentException("이미 달성 완료한 챌린지입니다.");
                }

                challengeUser.setCheck(true);
                challengeUserRepository.save(challengeUser);
        }

        /**
         * (API-4.15) 리커버리 미션 - 상태 조회
         * 사용자의 리커버리 미션 상태(유효 여부, 남은 시간, 미션 목록 등)를 조회합니다.
         *
         * @param uid 조회할 사용자의 고유 식별자(UID)
         * @return 리커버리 미션 상태 정보가 담긴 {@link RecoveryStatusResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public RecoveryStatusResponseDto getRecoveryStatus(String uid) {
                Optional<RecoveryMission> recoveryOpt = recoveryMissionRepository.findByUid(uid);

                if (recoveryOpt.isEmpty()) {
                        return RecoveryStatusResponseDto.builder()
                                        .recoveryAvailable(false)
                                        .deadline(null)
                                        .originalStreak(0)
                                        .missions(Collections.emptyList())
                                        .allCompleted(false)
                                        .build();
                }

                RecoveryMission recovery = recoveryOpt.get();

                // 마감일 체크
                if (LocalDate.now().isAfter(recovery.getDeadline())) {
                        return RecoveryStatusResponseDto.builder()
                                        .recoveryAvailable(false)
                                        .deadline(recovery.getDeadline())
                                        .originalStreak(recovery.getOriginalStreak())
                                        .missions(Collections.emptyList())
                                        .allCompleted(false)
                                        .build();
                }

                // 리커버리 미션 목록 조회
                List<RecoveryMissionItem> missionItems = recoveryMissionItemRepository.findByRecoveryMission(recovery);

                List<RecoveryMissionDto> missionDtos = missionItems.stream()
                                .map(item -> {
                                        Routine routine = item.getRoutine();
                                        return RecoveryMissionDto.builder()
                                                        .rid(routine.getRid())
                                                        .category(CategoryDto.builder()
                                                                        .categoryId(routine.getCategory()
                                                                                        .getCategoryId())
                                                                        .value(routine.getCategory().getValue())
                                                                        .build())
                                                        .content(routine.getContent())
                                                        .completed(item.getCompleted())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                boolean allCompleted = missionItems.stream().allMatch(RecoveryMissionItem::getCompleted);

                return RecoveryStatusResponseDto.builder()
                                .recoveryAvailable(true)
                                .deadline(recovery.getDeadline())
                                .originalStreak(recovery.getOriginalStreak())
                                .missions(missionDtos)
                                .allCompleted(allCompleted)
                                .build();
        }

        /**
         * (API-4.15) 리커버리 미션 - 달성 체크
         * 리커버리 미션을 달성 처리합니다.
         * 모든 미션을 완료하면 연속 달성 일수를 복구하고 리커버리 미션 데이터를 삭제합니다.
         *
         * @param uid 미션을 달성한 사용자의 고유 식별자(UID)
         * @param rid 달성한 리커버리 미션(루틴)의 고유 ID
         * @return 미션 완료 결과(전체 완료 여부, 스트릭 복구 여부 등)가 담긴
         *         {@link RecoveryAttainmentResponseDto} 객체
         * @throws ApiException 리커버리 미션 기간이 만료되었거나 이미 완료된 미션일 경우 발생
         */
        @Transactional
        public RecoveryAttainmentResponseDto checkRecoveryAttainment(String uid, Integer rid) {
                RecoveryMission recovery = recoveryMissionRepository.findByUid(uid)
                                .orElseThrow(() -> new IllegalArgumentException("리커버리 미션이 없습니다."));

                // 마감일 체크
                if (LocalDate.now().isAfter(recovery.getDeadline())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "리커버리 미션 기간이 만료되었습니다.");
                }

                // 해당 미션 찾기
                RecoveryMissionItem missionItem = recoveryMissionItemRepository
                                .findByRecoveryMissionAndRoutine_Rid(recovery, rid)
                                .orElseThrow(() -> new IllegalArgumentException("해당 리커버리 미션을 찾을 수 없습니다."));

                if (missionItem.getCompleted()) {
                        throw new ApiException(HttpStatus.CONFLICT, "이미 완료된 미션입니다.");
                }

                // 미션 완료 처리
                missionItem.setCompleted(true);
                recoveryMissionItemRepository.save(missionItem);

                // 모든 미션 완료 확인
                List<RecoveryMissionItem> allMissions = recoveryMissionItemRepository.findByRecoveryMission(recovery);
                boolean allCompleted = allMissions.stream().allMatch(RecoveryMissionItem::getCompleted);

                boolean streakRestored = false;
                int newStreak = 0;

                if (allCompleted) {
                        // 연속 달성 일수 복구
                        UserProfile userProfile = userProfileRepository.findById(uid)
                                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                        newStreak = recovery.getOriginalStreak() + 1;
                        userProfile.setStreak(newStreak);
                        userProfile.setLastStreakDate(LocalDate.now());
                        userProfileRepository.save(userProfile);

                        // 리커버리 미션 삭제
                        recoveryMissionItemRepository.deleteAll(allMissions);
                        recoveryMissionRepository.delete(recovery);

                        streakRestored = true;
                }

                return RecoveryAttainmentResponseDto.builder()
                                .message("리커버리 미션이 완료되었습니다!")
                                .allCompleted(allCompleted)
                                .streakRestored(streakRestored)
                                .newStreak(newStreak)
                                .build();
        }

        /**
         * (API-4.15) 리커버리 미션 - 연속 달성 일수 조회
         * 사용자의 현재 연속 달성 일수(Streak), 리커버리 상태, 오늘의 루틴 진행률을 조회합니다.
         *
         * @param uid 조회할 사용자의 고유 식별자(UID)
         * @return 연속 달성 관련 정보가 담긴 {@link StreakResponseDto} 객체
         */
        @Transactional(readOnly = true)
        public StreakResponseDto getStreak(String uid) {
                UserProfile userProfile = userProfileRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                // 리커버리 상태 확인
                Optional<RecoveryMission> recoveryOpt = recoveryMissionRepository.findByUid(uid);
                RecoveryStatusDto recoveryStatus;

                if (recoveryOpt.isPresent() && !LocalDate.now().isAfter(recoveryOpt.get().getDeadline())) {
                        RecoveryMission recovery = recoveryOpt.get();
                        recoveryStatus = RecoveryStatusDto.builder()
                                        .available(true)
                                        .deadline(recovery.getDeadline())
                                        .originalStreak(recovery.getOriginalStreak())
                                        .build();
                } else {
                        recoveryStatus = RecoveryStatusDto.builder()
                                        .available(false)
                                        .deadline(null)
                                        .originalStreak(0)
                                        .build();
                }

                // 오늘의 진행률 계산
                List<UserRoutine> userRoutines = userRoutineRepository.findByUserProfile_Uid(uid);
                LocalDate today = LocalDate.now();
                List<UserAttainment> todayAttainments = userAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, today.atStartOfDay(),
                                                today.plusDays(1).atStartOfDay());

                int totalRoutines = userRoutines.size();
                int completedRoutines = todayAttainments.size();
                double completionRate = totalRoutines > 0 ? (completedRoutines * 100.0 / totalRoutines) : 0.0;

                TodayProgressDto todayProgress = TodayProgressDto.builder()
                                .totalRoutines(totalRoutines)
                                .completedRoutines(completedRoutines)
                                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                                .build();

                return StreakResponseDto.builder()
                                .currentStreak(userProfile.getStreak())
                                .lastSuccessDate(userProfile.getLastStreakDate())
                                .recoveryStatus(recoveryStatus)
                                .todayProgress(todayProgress)
                                .build();
        }

        /**
         * (API-4.7) 사용자 루틴 - 달성 체크
         * 사용자의 일반 루틴을 달성 처리하고, 보상 lux를 지급합니다.
         * 모든 루틴을 달성했을 경우, 연속 달성 일수(Streak)를 업데이트합니다.
         *
         * @param uid       루틴을 달성한 사용자의 고유 식별자(UID)
         * @param routineId 달성한 루틴의 고유 ID
         * @throws IllegalArgumentException 루틴이 존재하지 않거나, 본인의 루틴이 아니거나, 이미 달성한 경우 발생
         */
        @Transactional
        public void checkRoutineAttainment(String uid, Integer routineId) {
                UserRoutine userRoutine = userRoutineRepository.findById(routineId)
                                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));

                if (!userRoutine.getUserProfile().getUid().equals(uid)) {
                        throw new IllegalArgumentException("본인의 루틴만 체크할 수 있습니다.");
                }

                LocalDate today = LocalDate.now();
                boolean alreadyChecked = userAttainmentRepository
                                .existsByUserProfile_UidAndUserRoutine_IdAndTimestampBetween(
                                                uid, routineId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

                if (alreadyChecked) {
                        throw new IllegalArgumentException("이미 체크된 루틴입니다.");
                }

                UserProfile userProfile = userProfileRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                UserAttainment attainment = UserAttainment.builder()
                                .userProfile(userProfile)
                                .userRoutine(userRoutine)
                                .timestamp(LocalDateTime.now())
                                .build();
                userAttainmentRepository.save(attainment);

                // 사용자의 lux값을 소량 증가
                userProfile.setLux(userProfile.getLux() + GENERAL_ROUTINE_LUX_BOUNS);
                userProfileRepository.save(userProfile);

                // ⭐ 모든 루틴 완료 확인 및 스트릭 업데이트
                updateStreakOnCompletion(uid);
        }

        /**
         * 사용자가 오늘의 모든 루틴을 완료했는지 확인하고, 조건에 따라 연속 달성 일수(Streak)를 업데이트하는 내부 메소드입니다.
         *
         * @param uid 검사할 사용자의 고유 식별자(UID)
         */
        private void updateStreakOnCompletion(String uid) {
                List<UserRoutine> allRoutines = userRoutineRepository.findByUserProfile_Uid(uid);
                LocalDate today = LocalDate.now();
                List<UserAttainment> todayAttainments = userAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, today.atStartOfDay(),
                                                today.plusDays(1).atStartOfDay());

                Set<Integer> completedRoutineIds = todayAttainments.stream()
                                .map(ua -> ua.getUserRoutine().getId())
                                .collect(Collectors.toSet());

                boolean allCompleted = allRoutines.stream()
                                .allMatch(routine -> completedRoutineIds.contains(routine.getId()));

                if (allCompleted) {
                        UserProfile userProfile = userProfileRepository.findById(uid)
                                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                        LocalDate lastStreakDate = userProfile.getLastStreakDate();

                        // 어제 완료했으면 연속, 아니면 1부터 시작
                        if (lastStreakDate != null && lastStreakDate.equals(today.minusDays(1))) {
                                userProfile.setStreak(userProfile.getStreak() + 1);
                        } else {
                                userProfile.setStreak(1);
                        }

                        userProfile.setLastStreakDate(today);
                        userProfileRepository.save(userProfile);
                }
        }

        /**
         * (API-3.5) Sera의 추천 루틴 모음 -> 특정 루틴 모음 반환
         * 특정 ID의 루틴 컬렉션 상세 정보를 조회합니다.
         *
         * @param collectionId 조회할 루틴 컬렉션의 고유 ID
         * @return 컬렉션 상세 정보와 포함된 루틴 목록이 담긴 {@link CollectionDetailDto} 객체
         * @throws ApiException 해당 ID의 컬렉션을 찾을 수 없는 경우 발생
         */
        @Transactional(readOnly = true)
        public CollectionDetailDto getRoutineCollectionById(Integer collectionId) {
                // 1. RoutineCollection 엔티티 조회
                RoutineCollection collection = routineCollectionRepository.findById(collectionId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "루틴 컬렉션을 찾을 수 없습니다."));

                // 2. 해당 컬렉션에 속한 루틴들을 Mapper를 통해 상세 정보와 함께 조회
                List<RoutineCollectionMapper> mappers = routineCollectionMapperRepository
                                .findByCollectionIdWithDetails(collectionId);

                // 3. 루틴 목록 DTO 생성
                List<RoutineDto> routineDtos = mappers.stream()
                                .map(mapper -> {
                                        Routine routine = mapper.getRoutine();
                                        return RoutineDto.builder()
                                                        .rid(routine.getRid())
                                                        .content(routine.getContent())
                                                        .category(CategoryDto.builder()
                                                                        .categoryId(routine.getCategory()
                                                                                        .getCategoryId())
                                                                        .value(routine.getCategory().getValue())
                                                                        .build())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 4. 최종 DTO 조립
                CollectionDetailDto responseDto = CollectionDetailDto.builder()
                                .collectionId(collection.getCollectionId())
                                .title(collection.getTitle())
                                .subTitle(collection.getSubTitle())
                                .guide(collection.getGuide())
                                .routines(routineDtos)
                                .build();

                return responseDto;
        }
}