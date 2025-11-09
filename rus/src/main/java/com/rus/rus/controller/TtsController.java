package com.rus.rus.controller;

import com.rus.rus.application.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/tts")
public class TtsController {

    private final TtsService ttsService;

    @PostMapping(value = "/speak", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speak(@RequestBody TtsRequest req) throws Exception {
        byte[] audio = (req.ssml() != null && !req.ssml().isBlank())
                ? ttsService.synthesizeSsml(
                req.ssml(),
                req.languageCode(), req.voiceName(),
                req.speakingRate(), req.pitch())
                : ttsService.synthesize(req.text());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "audio/mpeg");
        return new ResponseEntity<>(audio, headers, HttpStatus.OK);
    }

    // 그대로 둬도 되는 record
    public static record TtsRequest(
            String text,
            String ssml,
            String languageCode,
            String voiceName,
            Double speakingRate,
            Double pitch
    ) {}

}
