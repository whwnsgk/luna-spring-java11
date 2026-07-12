package com.example.luna;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LunaMapper {

    int increaseCount();

    long selectCount();
}
