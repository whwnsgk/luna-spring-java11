package com.example.luna;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luna")
public class LunaController {

    private final LunaService lunaService;

    public LunaController(LunaService lunaService) {
        this.lunaService = lunaService;
    }

    @GetMapping("/count")
    public Map<String, Object> getCount() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("count", lunaService.getCount());
        return result;
    }

    @PostMapping("/call")
    public Map<String, Object> callLuna() {
        long count = lunaService.callLuna();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("message", "네, 준하 주인님. 루나 여기 있어요!");
        result.put("count", count);

        return result;
    }
}
