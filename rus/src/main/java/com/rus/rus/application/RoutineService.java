package com.rus.rus.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import com.rus.rus.controller.dto.req.RoutineAddRequestDto;
import com.rus.rus.controller.dto.req.RoutineUpdateRequestDto;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.*;
import com.rus.rus.controller.dto.res.*;
import com.rus.rus.domain.*;
import com.rus.rus.infra.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoutineService {

        private static final List<String> ALL_CATEGORIES = Arrays.asList("수면", "운동", "영양소", "햇빛", "사회적유대감");
        private static final int TOTAL_RECOMMEND_COUNT = 10;
        private static final int SERA_ROUTINE_LUX_BONUS = 10;

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

        /**
         * 요청된 카테고리에 가중치를 부여하여 총 10개의 루틴을 추천합니다.
         * 5개 모든 카테고리가 최소 1개 이상 포함됩니다.
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

        // ==================== 4-1. 루틴 추가 ====================
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

        // ==================== 4-2. 루틴 추가 - 직접 작성 ====================
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

        // ==================== 4-3. 사용자 루틴 정보 반환 ====================
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

        // ==================== 4-4. 특정 루틴 정보 반환 ====================
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

        // ==================== 4-5. 사용자 루틴 수정 ====================
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

        // ==================== 4-6. 사용자 루틴 삭제 ====================
        @Transactional
        public void deleteRoutine(Integer id, String tokenUid) {
                UserRoutine userRoutine = userRoutineRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));

                if (!userRoutine.getUserProfile().getUid().equals(tokenUid)) {
                        throw new IllegalArgumentException("본인의 루틴만 삭제할 수 있습니다.");
                }

                userRoutineRepository.delete(userRoutine);
        }

        // ==================== 4-7. 사용자 루틴 달성 체크 ====================
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
        }

        // ==================== 4-8. 사용자 루틴 체크 해제 ====================
        @Transactional
        public void uncheckRoutineAttainment(String uid, Integer routineId) {
                LocalDate today = LocalDate.now();
                userAttainmentRepository.deleteByUserProfile_UidAndUserRoutine_IdAndTimestampBetween(
                                uid, routineId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        }

        // ==================== 4-9. Sera 추천 루틴 정보 반환 ====================
        @Transactional(readOnly = true)
        public RecommendRoutineResponseDto getRecommendRoutines(String uid) {
                List<RoutineSera> seraRoutines = routineSeraRepository.findByUserProfile_Uid(uid);
                LocalDate today = LocalDate.now();

                List<RoutineSeraAttainment> todayAttainments = routineSeraAttainmentRepository
                                .findByUserProfile_UidAndTimestampBetween(uid, today.atStartOfDay(),
                                                today.plusDays(1).atStartOfDay());

                Set<Integer> completedRoutineIds = todayAttainments.stream()
                                .map(rsa -> rsa.getRoutineSera().getId())
                                .collect(Collectors.toSet());

                List<RecommendRoutineItemDto> routineItems = seraRoutines.stream()
                                .map(seraRoutine -> {
                                        CategoryDto categoryDto = null;
                                        if (seraRoutine.getCategory() != null) {
                                                categoryDto = CategoryDto.builder()
                                                                .categoryId(seraRoutine.getCategory().getCategoryId())
                                                                .value(seraRoutine.getCategory().getValue())
                                                                .build();
                                        }

                                        boolean isComplete = completedRoutineIds.contains(seraRoutine.getId());

                                        return RecommendRoutineItemDto.builder()
                                                        .id(seraRoutine.getId())
                                                        .category(categoryDto)
                                                        .content(seraRoutine.getContent())
                                                        .complete(isComplete)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return RecommendRoutineResponseDto.builder()
                                .routines(routineItems)
                                .build();
        }

        // ==================== 4-10. Sera 추천 루틴 달성 체크 ====================
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

        // ==================== 4-11. 챌린지 미션 - 발생 여부 조회 ====================
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

        // ==================== 4-12. 챌린지 미션 - 도전 수락 ====================
        @Transactional
        public void acceptChallenge(String uid) {
                ChallengeUser challengeUser = challengeUserRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("챌린지 대상자가 아닙니다."));

                challengeUser.setCheck(false);
                challengeUserRepository.save(challengeUser);
        }

        // ==================== 4-13. 챌린지 미션 - 다음에 ====================
        @Transactional
        public void postponeChallenge(String uid) {
                ChallengeUser challengeUser = challengeUserRepository.findById(uid)
                                .orElseThrow(() -> new IllegalArgumentException("챌린지 대상자가 아닙니다."));

                challengeUserRepository.delete(challengeUser);
        }

        // ==================== 4-14. 챌린지 미션 - 달성 체크 ====================
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
         * 특정 루틴 컬렉션 ID에 해당하는 상세 정보와 루틴 목록을 반환합니다.
         *
         * @param collectionId 조회할 루틴 컬렉션 ID
         * @return 컬렉션 상세 정보 DTO
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