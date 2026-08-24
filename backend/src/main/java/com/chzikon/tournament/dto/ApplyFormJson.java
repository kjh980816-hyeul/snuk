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

    /** 질문 유형 — 단답/장문/객관식(하나)/체크박스(복수). 알 수 없는 값은 SHORT 취급. */
    public static final List<String> QUESTION_TYPES = List.of("SHORT", "LONG", "SELECT", "MULTI");

    /**
     * 참가 질문 — required=false 면 선택 항목.
     * type: SHORT(단답)/LONG(장문)/SELECT(객관식)/MULTI(체크박스), options: SELECT·MULTI 선택지.
     * 구버전 데이터/클라이언트({q,required}만)는 type=null 로 들어와 SHORT 로 정규화된다.
     */
    public record ApplyQuestion(String q, boolean required, String type, List<String> options) {
        public ApplyQuestion {
            type = (type != null && QUESTION_TYPES.contains(type)) ? type : "SHORT";
            if ("SELECT".equals(type) || "MULTI".equals(type)) {
                options = options == null ? List.of()
                        : options.stream().filter(o -> o != null && !o.isBlank()).map(String::trim).toList();
            } else {
                options = null;
            }
        }

        public ApplyQuestion(String q, boolean required) {
            this(q, required, null, null);
        }
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
                    List<String> options = null;
                    if (n.path("options").isArray()) {
                        options = new ArrayList<>();
                        for (JsonNode o : n.path("options")) {
                            options.add(o.asText(""));
                        }
                    }
                    out.add(new ApplyQuestion(q, n.path("required").asBoolean(true),
                            n.path("type").asText(null), options));
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
