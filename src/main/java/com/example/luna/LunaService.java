package com.example.luna;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LunaService {

    private final LunaMapper lunaMapper;

    public LunaService(LunaMapper lunaMapper) {
        this.lunaMapper = lunaMapper;
    }

    public long getCount() {
        return lunaMapper.selectCount();
    }

    @Transactional
    public long callLuna() {
        int updatedRows = lunaMapper.increaseCount();

        if (updatedRows != 1) {
            throw new IllegalStateException("호출 횟수 갱신에 실패했습니다.");
        }

        return lunaMapper.selectCount();
    }
}
