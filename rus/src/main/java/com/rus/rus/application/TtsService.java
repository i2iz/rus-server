package com.rus.rus.application;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    @Value("${tts.languageCode:ko-KR}")
    private String languageCode;

    @Value("${tts.voiceName:ko-KR-Wavenet-B}")
    private String voiceName;

    @Value("${tts.speakingRate:1.0}")
    private double speakingRate;

    @Value("${tts.pitch:0.0}")
    private double pitch;

    @Value("${tts.audioEncoding:MP3}")
    private String audioEncoding;

    /**
     * 순수 텍스트를 SSML로 감싸서 합성
     */
    public byte[] synthesize(String text) throws Exception {
        String ssml = toSsml(text);
        return synthesizeSsml(ssml, null, null, null, null);
    }

    /**
     * SSML 직접 합성 (파라미터 오버라이드 가능)
     */
    public byte[] synthesizeSsml(String ssml,
                                 String overrideLanguage,
                                 String overrideVoice,
                                 Double overrideRate,
                                 Double overridePitch) throws Exception {

        try (TextToSpeechClient tts = TextToSpeechClient.create()) {

            // 입력(SSML)
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setSsml(ssml)
                    .build();

            // 보이스
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(overrideLanguage != null ? overrideLanguage : languageCode)
                    .setName(overrideVoice != null ? overrideVoice : voiceName)
                    .build();

            // 오디오 설정
            AudioConfig.Builder audioCfg = AudioConfig.newBuilder()
                    .setAudioEncoding(parseEncoding(audioEncoding))
                    .setSpeakingRate(overrideRate != null ? overrideRate : speakingRate)
                    .setPitch(overridePitch != null ? overridePitch : pitch);

            SynthesizeSpeechResponse response = tts.synthesizeSpeech(
                    SynthesizeSpeechRequest.newBuilder()
                            .setInput(input)
                            .setVoice(voice)
                            .setAudioConfig(audioCfg.build())
                            .build());

            ByteString audioContents = response.getAudioContent();
            return audioContents.toByteArray();
        }
    }

    private AudioEncoding parseEncoding(String enc) {
        try {
            return AudioEncoding.valueOf(enc);
        } catch (Exception e) {
            return AudioEncoding.MP3;
        }
    }

    /**
     * 간단 SSML: 문장 끝에 짧은 쉬는 호흡 추가, URL/코드 제거 등 안전 처리
     */
    private String toSsml(String text) {
        String cleaned = text
                .replaceAll("[<>]", "")            // SSML 태그 충돌 방지
                .replaceAll("https?://\\S+", "");  // URL 제거(읽을 때 어색함)
        return "<speak>" + cleaned + "<break time=\"300ms\"/></speak>";
    }
}
