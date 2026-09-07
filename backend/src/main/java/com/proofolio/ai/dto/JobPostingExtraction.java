package com.proofolio.ai.dto;

import com.proofolio.application.entity.RequirementKind;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Structured-output schema for job posting extraction. Field descriptions are sent to Claude as the JSON schema. */
@JsonClassDescription("채용 공고에서 추출한 구조화 정보")
public record JobPostingExtraction(
        @JsonPropertyDescription("회사 이름. 그룹명이 아니라 실제 채용 주체(예: '네이버', '카카오뱅크', '한국전력공사')")
        String companyName,
        @JsonPropertyDescription("직무/포지션 이름. 여러 직무가 있으면 백엔드/서버 개발과 가장 가까운 것 하나")
        String positionTitle,
        @JsonPropertyDescription("고용 형태. 예: 신입 정규직, 인턴, 계약직. 없으면 null")
        String employmentType,
        @JsonPropertyDescription("근무지. 없으면 null")
        String location,
        @JsonPropertyDescription("지원 마감 시각. ISO-8601 형식(예: 2026-09-30T23:59:00+09:00 또는 2026-09-30). 상시채용/미기재면 null")
        String deadlineAt,
        @JsonPropertyDescription("자격 요건(필수) 목록. 짧은 문장 단위")
        List<String> requiredSkills,
        @JsonPropertyDescription("우대 사항 목록. 짧은 문장 단위")
        List<String> preferredSkills,
        @JsonPropertyDescription("전형 절차를 순서대로. 예: ['서류', '코딩테스트', '1차 면접', '2차 면접']")
        List<String> hiringStages,
        @JsonPropertyDescription("지원 시 제출해야 하는 서류/준비물 목록")
        List<RequiredDocument> requiredDocuments,
        @JsonPropertyDescription("자기소개서 문항 목록. 공고에 문항이 없으면 빈 배열")
        List<EssayQuestionItem> essayQuestions,
        @JsonPropertyDescription("공고 요약. 3~5문장, 한국어")
        String summary,
        @JsonPropertyDescription("공고 원문 URL. 모르면 null")
        String sourceUrl
) {
    public record RequiredDocument(
            @JsonPropertyDescription("서류 이름. 예: 이력서, 자기소개서, 성적증명서, 어학 성적, 포트폴리오, 경험기술서")
            String title,
            @JsonPropertyDescription("서류 종류. RESUME(이력서) ESSAY(자소서) TRANSCRIPT(성적) CERTIFICATE(자격증) LANGUAGE(어학) PORTFOLIO(포트폴리오) EXPERIENCE_DESC(경험기술서) OTHER(기타) 중 하나")
            RequirementKind kind
    ) {}

    public record EssayQuestionItem(
            @JsonPropertyDescription("문항 전문")
            String question,
            @JsonPropertyDescription("글자 수 제한. 없으면 null")
            Integer maxLength
    ) {}
}
