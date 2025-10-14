package com.rus.rus.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.UserRankingItemDto;
import com.rus.rus.controller.dto.req.EditProfileRequestDto;
import com.rus.rus.controller.dto.req.EditSettingRequestDto;
import com.rus.rus.controller.dto.res.UserProfileResponseDto;
import com.rus.rus.controller.dto.res.UserRankingResponseDto;
import com.rus.rus.controller.dto.res.UserSettingResponseDto;
import com.rus.rus.domain.Title;
import com.rus.rus.domain.UserAttainment;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.domain.UserRoutine;
import com.rus.rus.domain.UserSetting;
import com.rus.rus.domain.WeeklyAttendance;
import com.rus.rus.infra.repository.TitleRepository;
import com.rus.rus.infra.repository.UserAttainmentRepository;
import com.rus.rus.infra.repository.UserProfileRepository;
import com.rus.rus.infra.repository.UserRoutineRepository;
import com.rus.rus.infra.repository.UserSettingRepository;
import com.rus.rus.infra.repository.WeeklyAttendanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

        private final SupabaseAuthService supabaseAuthService;
        private final UserProfileRepository userProfileRepository;
        private final UserSettingRepository userSettingRepository;
        private final WeeklyAttendanceRepository weeklyAttendanceRepository;
        private final TitleRepository titleRepository;
        private final UserRoutineRepository userRoutineRepository;
        private final UserAttainmentRepository userAttainmentRepository;
        private final ReportService reportService;

        /**
         * Supabase Authentication에 새 사용자를 생성하고,
         * users_profile 및 users_setting 테이블에 초기 데이터를 생성합니다.
         *
         * @param email    사용자 이메일
         * @param password 사용자의 비밀번호
         * @return 생성된 사용자의 UID: Mono<String>
         */
        @Transactional
        public String signUpUser(String email, String password) {
                // 1. Supabase Auth를 통해 사용자 생성 및 UID 반환
                String uid = supabaseAuthService.createUser(email, password);

                // 2. UserProfile 엔티티 생성 및 저장
                // uid와 email을 설정하고, 나머지는 엔티티에 정의된 기본값 또는 @PrePersist로 자동 설정됩니다.
                UserProfile userProfile = UserProfile.builder()
                                .uid(uid)
                                .email(email)
                                .build();
                // saveAndFlush를 사용하여 즉시 DB에 INSERT 쿼리를 보내고, 영속성 컨텍스트에 반영합니다.
                // 이를 통해 UserSetting 저장 전에 UserProfile이 확실히 존재함을 보장합니다.
                UserProfile savedUserProfile = userProfileRepository.saveAndFlush(userProfile);

                // 3. 기본 칭호(Title) 조회
                // ID가 0인 칭호를 기본값으로 가정합니다. 해당 칭호가 DB에 반드시 존재해야 합니다.
                Title defaultTitle = titleRepository.findById(0)
                                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "기본 칭호(ID: 0)를 찾을 수 없습니다."));

                // 4. UserSetting 엔티티 생성 및 저장
                // @MapsId를 사용하므로 userProfile 객체를 연결해주면 uid가 자동으로 매핑됩니다.
                UserSetting userSetting = UserSetting.builder()
                                .userProfile(savedUserProfile)
                                .title(defaultTitle) // 조회한 기본 칭호 설정
                                .backgroundColor(0)
                                .lumiImage(0)
                                .build();
                userSettingRepository.save(userSetting);

                // 5. 출석 체크 엔티티 생성 및 저장
                WeeklyAttendance weeklyAttendance = WeeklyAttendance.builder()
                                .userProfile(savedUserProfile) // @MapsId를 사용하므로 userProfile만 연결해주면 됩니다.
                                .build();
                weeklyAttendanceRepository.save(weeklyAttendance);

                // 5. 생성된 UID 반환
                return uid;
        }

        /**
         * 사용자 고유 식별자를 통해 사용자 프로필 정보를 반환합니다.
         * 
         * @param uid 사용자 고유 식별자
         * @return profile 테이블의 UID를 제외한 나머지 정보
         */
        public UserProfileResponseDto getUserProfileById(UUID uid) {
                UserProfile profile = userProfileRepository.findById(uid.toString())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자 프로필 정보를 찾을 수 없습니다."));

                return UserProfileResponseDto.from(profile);
        }

        /**
         * 사용자 고유 식별자를 통해 사용자 설정값 정보를 반환합니다.
         * 
         * @param uid 사용자 고유 식별자
         * @return settings 테이블의 UID를 제외한 나머지 정보
         */
        public UserSettingResponseDto getUserSettingById(UUID uid) {
                UserSetting userSetting = userSettingRepository.findById(uid.toString())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자 프로필 정보를 찾을 수 없습니다."));

                return UserSettingResponseDto.from(userSetting);
        }

        /**
         * 사용자 프로필(이름, 생년월일, 성별)을 수정합니다
         * 
         * @param uid        사용자 고유 식별자
         * @param requestDto
         */
        @Transactional
        public void editUserProfile(UUID uid, EditProfileRequestDto requestDto) {
                UserProfile userProfile = userProfileRepository.findById(uid.toString())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
                userProfile.setName(requestDto.getName());
                userProfile.setBirthDate(requestDto.getBirthDate());
                userProfile.setGender(requestDto.getGender());
                userProfile.setHeight(requestDto.getHeight());
                userProfile.setWeight(requestDto.getWeight());
        }

        /**
         * 사용자의 설정값을 수정합니다
         * 
         * @param uid        사용자 고유 식별자
         * @param requestDto
         */
        @Transactional
        public void editUserSetting(UUID uid, EditSettingRequestDto requestDto) {
                UserSetting userSetting = userSettingRepository.findById(uid.toString())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자 설정을 찾을 수 없습니다."));

                Title title = titleRepository.findById(requestDto.getTitleId())
                                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "존재하지 않는 칭호 ID입니다."));
                userSetting.setTitle(title);
                userSetting.setBackgroundColor(requestDto.getBackgroundColor());
                userSetting.setLumiImage(requestDto.getLumiImage());
        }

        /**
         * 사용자 프로필의 isFirstLogin 상태를 true로 업데이트합니다.
         * 
         * @param uid 사용자 고유 식별자
         */
        @Transactional
        public void updateIsFirstLogin(UUID uid) {
                UserProfile userProfile = userProfileRepository.findById(uid.toString())
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

                userProfile.setFirstLogin(true);
        }

        /**
         * 모든 사용자의 랭킹 정보를 반환합니다.
         *
         * @return 랭킹 정보 목록
         */
        @Transactional(readOnly = true)
        public UserRankingResponseDto getUserRankings() {
                // 1. 모든 사용자 프로필을 lux 기준으로 내림차순 정렬하여 조회합니다.
                List<UserProfile> userProfiles = userProfileRepository.findAll();
                userProfiles.sort(Comparator.comparingInt(UserProfile::getLux).reversed());

                // 2. 모든 사용자 설정을 조회하여 Map으로 변환합니다 (빠른 조회를 위해).
                Map<String, UserSetting> userSettingsMap = userSettingRepository.findAll().stream()
                                .collect(Collectors.toMap(UserSetting::getUid, setting -> setting));

                // 3. 정렬된 프로필과 설정 데이터를 결합하여 DTO 목록을 생성합니다.
                List<UserRankingItemDto> rankingList = userProfiles.stream()
                                .map(profile -> {
                                        UserSetting setting = userSettingsMap.get(profile.getUid());
                                        return UserRankingItemDto.from(profile, setting);
                                })
                                .collect(Collectors.toList());

                return UserRankingResponseDto.builder()
                                .rankings(rankingList)
                                .build();
        }

        /**
         * 모바일 웹뷰에 표시할 일일 리포트에 필요한 데이터를 조회하여 반환합니다.
         * 
         * @param uid 사용자 고유 식별자
         * @return HTML 템플릿에 전달될 데이터 맵
         */
        public Map<String, Object> getDailyReportData(UUID uid) {
                String userId = uid.toString();
                UserProfile userProfile = userProfileRepository.findById(userId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

                // 동적 인사말 생성
                String userName = userProfile.getName() != null && !userProfile.getName().isEmpty()
                                ? userProfile.getName()
                                : "사용자";
                String timeBasedGreeting = getTimeBasedGreeting();
                String fullGreeting = "🌤️ " + timeBasedGreeting + ", " + userName + "님!";

                // 오늘의 루틴 달성 현황 데이터 생성
                LocalDate today = LocalDate.now();
                List<UserRoutine> allUserRoutines = userRoutineRepository.findByUserProfileUid(userId);
                List<UserAttainment> todayAttainments = userAttainmentRepository
                                .findByUserProfileUidAndTimestampBetween(
                                                userId, today.atStartOfDay(), today.atTime(LocalTime.MAX));

                int totalRoutines = allUserRoutines.size();
                int completedRoutines = todayAttainments.size();
                int completionRate = 0;
                if (totalRoutines > 0) {
                        completionRate = (int) Math.round((double) completedRoutines / totalRoutines * 100);
                }

                Set<Integer> completedRoutineIds = todayAttainments.stream()
                                .map(attainment -> attainment.getUserRoutine().getId())
                                .collect(Collectors.toSet());

                List<Map<String, Object>> routineStatusList = allUserRoutines.stream()
                                .map(routine -> {
                                        Map<String, Object> routineStatus = new HashMap<>();
                                        String categoryName = (routine.getCategory() != null)
                                                        ? routine.getCategory().getValue()
                                                        : "";
                                        routineStatus.put("category", categoryName);
                                        routineStatus.put("content", routine.getContent());
                                        routineStatus.put("isCompleted", completedRoutineIds.contains(routine.getId()));
                                        return routineStatus;
                                })
                                .collect(Collectors.toList());

                // 7일간의 루틴 달성 기록 데이터 생성
                LocalDate sevenDaysAgo = today.minusDays(6);

                List<UserAttainment> attainmentsLast7Days = userAttainmentRepository
                                .findByUserProfileUidAndTimestampBetween(
                                                userId, sevenDaysAgo.atStartOfDay(), today.atTime(LocalTime.MAX));

                Map<Integer, Map<LocalDate, LocalTime>> attainmentsByRoutine = attainmentsLast7Days.stream()
                                .collect(Collectors.groupingBy(
                                                att -> att.getUserRoutine().getId(),
                                                Collectors.toMap(
                                                                att -> att.getTimestamp().toLocalDate(),
                                                                att -> att.getTimestamp().toLocalTime(),
                                                                (time1, time2) -> time1)));

                List<String> chartDateLabels = IntStream.range(0, 7)
                                .mapToObj(i -> sevenDaysAgo.plusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")))
                                .collect(Collectors.toList());

                List<Map<String, Object>> chartDatasets = new ArrayList<>();
                List<String> colors = List.of("#34A853", "#FBBC05", "#4285F4", "#EA4335", "#9C27B0");
                int colorIndex = 0;

                for (UserRoutine routine : allUserRoutines) {
                        Map<LocalDate, LocalTime> dailyRecords = attainmentsByRoutine.getOrDefault(routine.getId(),
                                        Collections.emptyMap());

                        List<Double> dataPoints = new ArrayList<>();
                        for (int i = 0; i < 7; i++) {
                                LocalDate date = sevenDaysAgo.plusDays(i);
                                LocalTime time = dailyRecords.get(date);
                                if (time != null) {
                                        dataPoints.add(time.getHour() + time.getMinute() / 60.0);
                                } else {
                                        dataPoints.add(null);
                                }
                        }

                        if (dataPoints.stream().anyMatch(Objects::nonNull)) {
                                String color = colors.get(colorIndex % colors.size());
                                Map<String, Object> dataset = new LinkedHashMap<>();
                                dataset.put("label", routine.getContent());
                                dataset.put("data", dataPoints);
                                dataset.put("borderColor", color);
                                dataset.put("backgroundColor", color);

                                dataset.put("fill", false);
                                dataset.put("tension", 0.4); // 라인을 부드럽게
                                dataset.put("pointRadius", 5); // 데이터 포인트 크기
                                dataset.put("pointHoverRadius", 7); // 마우스 올렸을 때 포인트 크기
                                dataset.put("pointStyle", "circle"); // 포인트 스타일

                                chartDatasets.add(dataset);
                                colorIndex++;
                        }
                }

                // AI에게 전달할 데이터 요약 텍스트 생성
                String summaryForAI = createSummaryForAI(attainmentsLast7Days);

                // AIService를 호출하여 피드백 받기
                String aiFeedback = reportService.getFeedback(summaryForAI);

                Map<String, Object> data = new HashMap<>();
                data.put("greeting", fullGreeting);
                data.put("completionRate", completionRate);
                data.put("routineStatusList", routineStatusList);
                data.put("chartDateLabels", chartDateLabels);
                data.put("chartDatasets", chartDatasets);
                data.put("aiFeedback", aiFeedback);

                return data;
        }

        // 시간에 따라 다른 말을 반환하는 헬퍼 method
        private String getTimeBasedGreeting() {
                int hour = LocalTime.now().getHour();
                if (hour >= 5 && hour < 12) {
                        return "좋은 아침이에요";
                } else if (hour >= 12 && hour < 18) {
                        return "활기찬 오후예요";
                } else if (hour >= 18 && hour < 22) {
                        return "편안한 저녁 되세요";
                } else {
                        return "고요한 밤이에요";
                }
        }

        private String createSummaryForAI(List<UserAttainment> attainments) {
                if (attainments.isEmpty()) {
                        return "최근 7일간 달성 기록 없음.";
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return attainments.stream()
                                .map(att -> String.format("- %s, '%s' 루틴 달성",
                                                att.getTimestamp().format(formatter),
                                                att.getUserRoutine().getContent()))
                                .collect(Collectors.joining("\n"));
        }

}
