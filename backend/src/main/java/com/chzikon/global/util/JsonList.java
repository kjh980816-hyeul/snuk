package com.chzikon.global.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** 문자열 리스트 ↔ JSON 컬럼(TEXT) 변환 — 대회 참가 질문/답변(V15) 등. 실패 시 관대하게 빈 값. */
public final class JsonList {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonList() {
    }

    public static String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
