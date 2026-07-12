package com.example.luna;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/luna")
public class LunaController {

    private final AtomicLong callCount = new AtomicLong(0);

    @GetMapping("/count")
    public Map<String, Object> getCount() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("count", callCount.get());
        return result;
    }

    @PostMapping("/call")
    public Map<String, Object> callLuna() {
        long count = callCount.incrementAndGet();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("message", "네, 준하 주인님. 루나 여기 있어요!");
        result.put("count", count);

        return result;
    }
}
