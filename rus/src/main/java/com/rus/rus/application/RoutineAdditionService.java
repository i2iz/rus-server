package com.rus.rus.application;

import com.rus.rus.controller.dto.req.AddRecommendedRoutineRequestDto;
import com.rus.rus.controller.dto.req.ManualAddRoutineRequestDto;
import com.rus.rus.controller.dto.res.AddRoutineResponseDto;
import com.rus.rus.controller.dto.res.ParsedRoutineDto;
import com.rus.rus.domain.Category;
import com.rus.rus.domain.ChatMessage;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.domain.UserRoutine;
import com.rus.rus.infra.repository.CategoryRepository;
import com.rus.rus.infra.repository.ChatMessageRepository;
import com.rus.rus.infra.repository.UserProfileRepository;
import com.rus.rus.infra.repository.UserRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 챗봇이 추천한 루틴을 사용자에게 추가하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineAdditionService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRoutineRepository userRoutineRepository;
    private final CategoryRepository categoryRepository;
    private final UserProfileRepository userProfileRepository;

    private static final List<String> VALID_CATEGORIES = List.of(
            "수면", "운동", "영양소", "햇빛", "사회적유대감"
    );

    /**
     * AI 메시지에서 추천된 루틴을 파싱하여 사용자에게 추가
     */
    @Transactional
    public List<AddRoutineResponseDto> addRecommendedRoutines(
            String uid,
            AddRecommendedRoutineRequestDto request) {

        log.info("Adding recommended routines - uid: {}, messageId: {}", uid, request.getMessageId());

        // 1. 사용자 확인
        UserProfile userProfile = userProfileRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 2. 챗봇 메시지 조회
        ChatMessage chatMessage = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다"));

        // 3. BOT 메시지인지 확인
        if (!"BOT".equals(chatMessage.getMessageType())) {
            throw new IllegalArgumentException("봇 메시지만 선택 가능합니다");
        }

        // 4. AI 응답에서 추천 루틴 파싱
        List<ParsedRoutineDto> recommendations = parseRecommendations(chatMessage.getMessage());
        log.info("Parsed {} recommendations from AI message", recommendations.size());

        if (recommendations.isEmpty()) {
            log.warn("No routines found in message: {}", request.getMessageId());
            return List.of(AddRoutineResponseDto.builder()
                    .success(false)
                    .message("추천 루틴을 찾을 수 없습니다")
                    .build());
        }

        // 5. 선택된 루틴만 추가
        List<AddRoutineResponseDto> results = new ArrayList<>();
        List<Integer> selectedIndexes = request.getSelectedIndexes();

        if (selectedIndexes == null || selectedIndexes.isEmpty()) {
            // 모든 추천 루틴 추가
            log.info("Adding all {} recommendations", recommendations.size());
            for (ParsedRoutineDto rec : recommendations) {
                AddRoutineResponseDto result = addRoutineToUser(userProfile, rec);
                results.add(result);
            }
        } else {
            // 선택된 인덱스의 루틴만 추가
            log.info("Adding selected {} routines", selectedIndexes.size());
            for (Integer index : selectedIndexes) {
                if (index >= 0 && index < recommendations.size()) {
                    ParsedRoutineDto rec = recommendations.get(index);
                    AddRoutineResponseDto result = addRoutineToUser(userProfile, rec);
                    results.add(result);
                } else {
                    log.warn("Invalid index: {}", index);
                    results.add(AddRoutineResponseDto.builder()
                            .success(false)
                            .message("유효하지 않은 인덱스: " + index)
                            .build());
                }
            }
        }

        long successCount = results.stream().filter(AddRoutineResponseDto::isSuccess).count();
        log.info("Routine addition completed - Success: {}, Total: {}", successCount, results.size());

        return results;
    }

    /**
     * 수동으로 루틴 추가
     */
    @Transactional
    public AddRoutineResponseDto addManualRoutine(String uid, ManualAddRoutineRequestDto request) {
        log.info("Adding manual routine - uid: {}, routine: {}", uid, request.getName());

        // 사용자 확인
        UserProfile userProfile = userProfileRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 카테고리 검증
        if (!VALID_CATEGORIES.contains(request.getCategory())) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
        }

        ParsedRoutineDto recommendation = ParsedRoutineDto.builder()
                .category(request.getCategory())
                .name(request.getName())
                .description(request.getDescription())
                .timeOfDay(request.getTimeOfDay())
                .build();

        return addRoutineToUser(userProfile, recommendation);
    }


    /**
     * 실제로 사용자에게 루틴 추가
     */
    private AddRoutineResponseDto addRoutineToUser(UserProfile userProfile, ParsedRoutineDto recommendation) {
        try {
            // 1. 카테고리 찾기
            Category category = categoryRepository.findAll().stream()
                    .filter(c -> c.getValue().equals(recommendation.getCategory()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리: " + recommendation.getCategory()));

            // 2. 중복 확인 (같은 사용자 + 같은 카테고리 + 같은 내용)
            boolean isDuplicate = userRoutineRepository.findAll().stream()
                    .anyMatch(ur -> ur.getUserProfile().getUid().equals(userProfile.getUid()) &&
                            ur.getCategory().getCategoryId().equals(category.getCategoryId()) &&
                            ur.getContent().equals(recommendation.getName()));

            if (isDuplicate) {
                log.warn("Duplicate routine detected - uid: {}, routine: {}",
                        userProfile.getUid(), recommendation.getName());
                return AddRoutineResponseDto.builder()
                        .success(false)
                        .routineName(recommendation.getName())
                        .category(category.getValue())
                        .message("이미 추가된 루틴입니다")
                        .build();
            }

            // 3. UserRoutine 생성
            UserRoutine userRoutine = UserRoutine.builder()
                    .userProfile(userProfile)
                    .category(category)
                    .content(recommendation.getName())
                    .notification(null)  // 알림 시간은 나중에 설정
                    .build();

            UserRoutine saved = userRoutineRepository.save(userRoutine);

            log.info("Successfully added routine - uid: {}, routineId: {}, name: {}",
                    userProfile.getUid(), saved.getId(), saved.getContent());

            return AddRoutineResponseDto.builder()
                    .success(true)
                    .routineId(saved.getId().longValue())
                    .routineName(saved.getContent())
                    .category(category.getValue())
                    .description(recommendation.getDescription())
                    .message("루틴이 성공적으로 추가되었습니다")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error adding routine - uid: {}, routine: {}",
                    userProfile.getUid(), recommendation.getName(), e);
            return AddRoutineResponseDto.builder()
                    .success(false)
                    .routineName(recommendation.getName())
                    .category(recommendation.getCategory())
                    .message("루틴 추가 중 오류 발생: " + e.getMessage())
                    .build();
        }
    }

    /**
     * AI 응답에서 루틴 추천 파싱
     */
    private List<ParsedRoutineDto> parseRecommendations(String aiResponse) {
        List<ParsedRoutineDto> recommendations = new ArrayList<>();

        String currentCategory = null;
        String[] lines = aiResponse.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 카테고리 감지
            for (String category : VALID_CATEGORIES) {
                if (line.contains(category) && !isRoutineItem(line)) {
                    currentCategory = category;
                    log.debug("Detected category: {}", category);
                    break;
                }
            }

            // 루틴 아이템 감지
            if (currentCategory != null) {
                ParsedRoutineDto routine = parseRoutineLine(line, currentCategory);
                if (routine != null && !isDuplicateInList(routine, recommendations)) {
                    recommendations.add(routine);
                    log.debug("Parsed routine: {} in category: {}", routine.getName(), currentCategory);
                }
            }
        }

        // 카테고리가 없는 경우 일반 파싱 시도
        if (recommendations.isEmpty()) {
            log.warn("No category-based recommendations found, trying general parsing");
            recommendations = parseGeneralRecommendations(aiResponse);
        }

        return recommendations;
    }

    /**
     * 중복 체크
     */
    private boolean isDuplicateInList(ParsedRoutineDto routine, List<ParsedRoutineDto> list) {
        return list.stream().anyMatch(r ->
                r.getName().equals(routine.getName()) && r.getCategory().equals(routine.getCategory()));
    }

    /**
     * 라인이 루틴 아이템인지 확인
     */
    private boolean isRoutineItem(String line) {
        return line.matches("^[0-9]+\\..*") ||
                line.startsWith("-") ||
                line.startsWith("•") ||
                line.startsWith("*");
    }

    /**
     * 개별 라인에서 루틴 파싱
     */
    private ParsedRoutineDto parseRoutineLine(String line, String category) {
        String[] patterns = {
                "^[0-9]+\\.\\s*(.+?)(?:\\(|:|$)",
                "^-\\s*(.+?)(?:\\(|:|$)",
                "^•\\s*(.+?)(?:\\(|:|$)",
                "^\\*\\s*(.+?)(?:\\(|:|$)",
                "^\\*\\*우선순위\\s*[0-9]+:\\s*(.+?)\\*\\*",
                "^\\*\\*(.+?)\\*\\*"
        };

        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(line);

            if (m.find()) {
                String routineName = m.group(1).trim()
                        .replaceAll("\\*\\*", "")
                        .replaceAll("\\[|\\]", "")
                        .trim();

                if (routineName.length() < 2 || routineName.length() > 100) {
                    continue;
                }

                String description = extractDescription(line);
                String timeOfDay = extractTimeOfDay(line);

                return ParsedRoutineDto.builder()
                        .category(category)
                        .name(routineName)
                        .description(description)
                        .timeOfDay(timeOfDay)
                        .build();
            }
        }

        return null;
    }

    /**
     * 카테고리 없이 전체 텍스트에서 추천 추출
     */
    private List<ParsedRoutineDto> parseGeneralRecommendations(String text) {
        List<ParsedRoutineDto> recommendations = new ArrayList<>();

        Pattern pattern = Pattern.compile("([0-9]+)\\.\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String routineName = matcher.group(2).trim();
            String category = inferCategory(routineName);

            if (category != null && routineName.length() >= 2 && routineName.length() <= 100) {
                recommendations.add(ParsedRoutineDto.builder()
                        .category(category)
                        .name(routineName)
                        .priority(Integer.parseInt(matcher.group(1)))
                        .build());
            }
        }

        return recommendations;
    }

    private String extractDescription(String line) {
        Pattern pattern = Pattern.compile("\\((.+?)\\)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        if (line.contains(":")) {
            String[] parts = line.split(":", 2);
            if (parts.length > 1) {
                String desc = parts[1].trim();
                if (desc.length() > 0 && !desc.matches("^[0-9]+.*")) {
                    return desc;
                }
            }
        }

        return null;
    }

    private String extractTimeOfDay(String line) {
        String lower = line.toLowerCase();

        if (lower.contains("아침") || lower.contains("morning")) return "아침";
        if (lower.contains("점심") || lower.contains("lunch")) return "점심";
        if (lower.contains("저녁") || lower.contains("evening")) return "저녁";
        if (lower.contains("밤") || lower.contains("night")) return "밤";

        return null;
    }

    private String inferCategory(String routineName) {
        String lower = routineName.toLowerCase();

        if (lower.contains("수면") || lower.contains("잠") || lower.contains("취침") || lower.contains("sleep")) {
            return "수면";
        }
        if (lower.contains("운동") || lower.contains("조깅") || lower.contains("스트레칭") ||
                lower.contains("근력") || lower.contains("요가") || lower.contains("exercise")) {
            return "운동";
        }
        if (lower.contains("영양") || lower.contains("식사") || lower.contains("물") ||
                lower.contains("과일") || lower.contains("채소") || lower.contains("비타민")) {
            return "영양소";
        }
        if (lower.contains("햇빛") || lower.contains("산책") || lower.contains("야외") || lower.contains("sunlight")) {
            return "햇빛";
        }
        if (lower.contains("친구") || lower.contains("가족") || lower.contains("통화") ||
                lower.contains("만남") || lower.contains("사회") || lower.contains("social")) {
            return "사회적유대감";
        }

        return null;
    }
}