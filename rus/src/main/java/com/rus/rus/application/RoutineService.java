package com.rus.rus.application;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.controller.dto.CategoryDto;
import com.rus.rus.controller.dto.CollectionDetailDto;
import com.rus.rus.controller.dto.RecommendedRoutineDto;
import com.rus.rus.controller.dto.RoutineDto;
import com.rus.rus.controller.dto.res.AllCollectionsResponseDto;
import com.rus.rus.controller.dto.res.AllRoutinesResponseDto;
import com.rus.rus.controller.dto.res.RecommendResponseDto;
import com.rus.rus.domain.Routine;
import com.rus.rus.domain.RoutineCollection;
import com.rus.rus.domain.RoutineCollectionMapper;
import com.rus.rus.infra.repository.RoutineCollectionMapperRepository;
import com.rus.rus.infra.repository.RoutineCollectionRepository;
import com.rus.rus.infra.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private static final List < String > ALL_CATEGORIES = Arrays.asList("수면", "운동", "영양소", "햇빛", "사회적유대감");
    private static final int TOTAL_RECOMMEND_COUNT = 10;

    private final RoutineRepository routineRepository;
    private final RoutineCollectionRepository routineCollectionRepository;
    private final RoutineCollectionMapperRepository routineCollectionMapperRepository;

    /**
     * 요청된 카테고리에 가중치를 부여하여 총 10개의 루틴을 추천합니다.
     * 5개 모든 카테고리가 최소 1개 이상 포함됩니다.
     *
     * @param requestedCategoryNames 추천받고 싶은 카테고리 이름 목록
     * @return 추천 루틴 정보가 담긴 DTO
     */
    @Transactional(readOnly = true)
    public RecommendResponseDto getRecommendedRoutines(List < String > requestedCategoryNames) {
        // 입력 정제
        List < String > requested = Optional.ofNullable(requestedCategoryNames)
            .orElse(Collections.emptyList())
            .stream().filter(ALL_CATEGORIES::contains).distinct().toList();

        // 전 카테고리 조회/그룹
        List < Routine > all = routineRepository.findRoutinesByCategoryNames(ALL_CATEGORIES);
        Map < String, List < Routine >> byCat = all.stream()
            .collect(Collectors.groupingBy(r -> r.getCategory().getValue()));

        // 카테고리별 데크 준비(셔플 후 pop)
        Map < String, Deque < Routine >> deck = new HashMap < > ();
        for (String cat: ALL_CATEGORIES) {
            List < Routine > list = new ArrayList < > (byCat.getOrDefault(cat, List.of()));
            Collections.shuffle(list);
            deck.put(cat, new ArrayDeque < > (list));
        }

        List < Routine > result = new ArrayList < > (TOTAL_RECOMMEND_COUNT);

        // 1) 전 카테고리 최소 1개 보장
        for (String cat: ALL_CATEGORIES) {
            Deque < Routine > q = deck.get(cat);
            if (q != null && !q.isEmpty()) result.add(q.pollFirst());
        }

        // 2) 요청 가중치: 라운드로빈 + 무작위 시작 (+ 선택적 상한)
        int remaining = TOTAL_RECOMMEND_COUNT - result.size();
        Map < String, Integer > taken = new HashMap < > ();
        for (Routine r: result) taken.merge(r.getCategory().getValue(), 1, Integer::sum);

        List < String > order = new ArrayList < > (requested);
        if (!order.isEmpty()) {
            int start = new Random().nextInt(order.size()); // 편향 제거
            Collections.rotate(order, -start);
        }

        final int MAX_PER_CATEGORY = 3; // 선택: 한 카테고리 총 3개 초과 금지(과다 편중 방지)

        int idx = 0;
        while (remaining > 0 && !order.isEmpty()) {
            String cat = order.get(idx % order.size());
            Deque < Routine > q = deck.get(cat);
            int cur = taken.getOrDefault(cat, 0);
            if (q != null && !q.isEmpty() && cur < MAX_PER_CATEGORY) {
                result.add(q.pollFirst());
                taken.put(cat, cur + 1);
                remaining--;
                idx++;
            } else {
                order.remove(cat); // 고갈/상한 도달 시 제외
            }
        }

        // 3) 아직 남으면 비요청에서 보충(상한 고려)
        if (remaining > 0) {
            List < String > others = ALL_CATEGORIES.stream()
                .filter(c -> !requested.contains(c)).toList();
            int j = 0;
            while (remaining > 0 && !others.isEmpty()) {
                String cat = others.get(j % others.size());
                Deque < Routine > q = deck.get(cat);
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

        // DTO 매핑
        List < RecommendedRoutineDto > dtoList = result.stream()
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
     * routines 테이블에 저장된 모든 루틴 정보를 조회합니다
     * @return 모든 루틴 정보
     */
    @Transactional(readOnly = true) // 읽기 전용
    public AllRoutinesResponseDto getAllRoutines() {
        List < Routine > allRoutines = routineRepository.findAll();

        List < RoutineDto > dtoList = allRoutines.stream()
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
     * 모든 루틴 컬렉션 정보를 반환합니다.
     * @return 루틴 컬렉션 정보
     */
    @Transactional(readOnly = true)
    public AllCollectionsResponseDto getAllRoutineCollections() {
        // 1. 모든 루틴 컬렉션(패키지)을 조회합니다. (1번의 쿼리)
        List < RoutineCollection > allCollections = routineCollectionRepository.findAll();

        // 2. 모든 매퍼와 관련 루틴 정보를 한 번에 조회합니다. (1번의 쿼리)
        List < RoutineCollectionMapper > allMappers = routineCollectionMapperRepository.findAllWithDetails();

        // 3. 조회된 매퍼들을 collectionId를 기준으로 그룹화하여 Map으로 만듭니다. (메모리에서 처리)
        // Key: collectionId, Value: 해당 collection에 속한 RoutineDto 리스트
        Map < Integer, List < RoutineDto >> routinesByCollectionId = allMappers.stream()
            .collect(Collectors.groupingBy(
                mapper -> mapper.getRoutineCollection().getCollectionId(),
                Collectors.mapping(
                    mapper -> {
                        var routine = mapper.getRoutine();
                        return RoutineDto.builder()
                            .rid(routine.getRid())
                            .content(routine.getContent())
                            .category(CategoryDto.builder()
                                .categoryId(routine.getCategory().getCategoryId())
                                .value(routine.getCategory().getValue())
                                .build())
                            .build();
                    },
                    Collectors.toList()
                )
            ));

        // 4. 컬렉션 목록을 순회하며, 위에서 만든 Map을 이용해 각 컬렉션에 루틴 목록을 채워넣습니다.
        List < CollectionDetailDto > resultDtoList = allCollections.stream()
            .map(collection -> {
                CollectionDetailDto dto = CollectionDetailDto.builder()
                .collectionId(collection.getCollectionId())
                .title(collection.getTitle())
                .subTitle(collection.getSubTitle())
                .guide(collection.getGuide())
                .build();
                // Map에서 해당 collectionId의 루틴 리스트를 찾아 설정합니다.
                dto.setRoutines(routinesByCollectionId.get(collection.getCollectionId()));
                return dto;
            })
            .collect(Collectors.toList());

        // 5. 최종 응답 객체를 빌드하여 반환합니다.
        return AllCollectionsResponseDto.builder()
            .collections(resultDtoList)
            .build();
    }
}