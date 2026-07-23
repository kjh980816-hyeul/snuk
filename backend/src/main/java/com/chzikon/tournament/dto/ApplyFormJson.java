package com.chzikon.tournament.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 대회 참가 질문/답변 JSON 컬럼 직렬화(V15 항목 17 확장).
 * 질문 = [{"q":"...","required":true}], 답변 = [{"text":"...","imageUrl":"..."}].
 * 구버전(문자열 배열) 데이터도 관대하게 읽음. 실패 시 빈 값.
 */
public final class ApplyFormJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApplyFormJson() {
    }

    /** 참가 질문 — required=false 면 선택 항목. */
    public record ApplyQuestion(String q, boolean required) {
    }

    /** 참가 답변 — 텍스트/이미지 중 하나 이상. */
    public record ApplyAnswer(String text, String imageUrl) {
        public boolean isBlank() {
            return (text == null || text.isBlank()) && (imageUrl == null || imageUrl.isBlank());
        }
    }

    public static String questionsToJson(List<ApplyQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(questions);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ApplyQuestion> questionsFromJson(String json) {
        List<ApplyQuestion> out = new ArrayList<>();
        for (JsonNode n : readArray(json)) {
            if (n.isTextual()) {
                out.add(new ApplyQuestion(n.asText(), true)); // 구버전(문자열) = 필수
            } else if (n.isObject()) {
                String q = n.path("q").asText("");
                if (!q.isBlank()) {
                    out.add(new ApplyQuestion(q, n.path("required").asBoolean(true)));
                }
            }
        }
        return out;
    }

    public static String answersToJson(List<ApplyAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(answers);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ApplyAnswer> answersFromJson(String json) {
        List<ApplyAnswer> out = new ArrayList<>();
        for (JsonNode n : readArray(json)) {
            if (n.isTextual()) {
                out.add(new ApplyAnswer(n.asText(), null)); // 구버전(문자열)
            } else if (n.isObject()) {
                String text = n.path("text").isNull() ? null : n.path("text").asText(null);
                String img = n.path("imageUrl").isNull() ? null : n.path("imageUrl").asText(null);
                out.add(new ApplyAnswer(text, img));
            }
        }
        return out;
    }

    private static JsonNode readArray(String json) {
        if (json == null || json.isBlank()) {
            return MAPPER.createArrayNode();
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return node.isArray() ? node : MAPPER.createArrayNode();
        } catch (Exception e) {
            return MAPPER.createArrayNode();
        }
    }
}
