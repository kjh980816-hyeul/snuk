package com.chzikon.tournament.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyFormJsonTest {

    @Test
    @DisplayName("유형·선택지 질문이 JSON 왕복 후 그대로 보존된다")
    void typedQuestionsRoundTrip() {
        var questions = List.of(
                new ApplyFormJson.ApplyQuestion("이름은?", true, "SHORT", null),
                new ApplyFormJson.ApplyQuestion("각오 한마디", false, "LONG", null),
                new ApplyFormJson.ApplyQuestion("티어는?", true, "SELECT", List.of("골드", "플래", "다이아")),
                new ApplyFormJson.ApplyQuestion("가능 요일", true, "MULTI", List.of("토", "일")));

        var restored = ApplyFormJson.questionsFromJson(ApplyFormJson.questionsToJson(questions));

        assertThat(restored).hasSize(4);
        assertThat(restored.get(0).type()).isEqualTo("SHORT");
        assertThat(restored.get(0).options()).isNull();
        assertThat(restored.get(2).type()).isEqualTo("SELECT");
        assertThat(restored.get(2).options()).containsExactly("골드", "플래", "다이아");
        assertThat(restored.get(3).type()).isEqualTo("MULTI");
        assertThat(restored.get(3).options()).containsExactly("토", "일");
        assertThat(restored.get(3).required()).isTrue();
    }

    @Test
    @DisplayName("구버전 데이터({q,required}·문자열 배열)는 SHORT 로 정규화된다")
    void legacyQuestionsNormalizeToShort() {
        var fromObj = ApplyFormJson.questionsFromJson("[{\"q\":\"티어를 알려주세요\",\"required\":false}]");
        assertThat(fromObj).hasSize(1);
        assertThat(fromObj.get(0).type()).isEqualTo("SHORT");
        assertThat(fromObj.get(0).required()).isFalse();

        var fromStr = ApplyFormJson.questionsFromJson("[\"신청 이유\"]");
        assertThat(fromStr).hasSize(1);
        assertThat(fromStr.get(0).type()).isEqualTo("SHORT");
        assertThat(fromStr.get(0).required()).isTrue();
    }

    @Test
    @DisplayName("알 수 없는 유형은 SHORT, 빈 선택지는 정리된다")
    void unknownTypeAndBlankOptionsAreSanitized() {
        var q = new ApplyFormJson.ApplyQuestion("q", true, "WEIRD", List.of("a"));
        assertThat(q.type()).isEqualTo("SHORT");
        assertThat(q.options()).isNull();

        var sel = new ApplyFormJson.ApplyQuestion("q", true, "SELECT", java.util.Arrays.asList(" a ", "", null, "b"));
        assertThat(sel.options()).containsExactly("a", "b");
    }
}
