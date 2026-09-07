package com.proofolio.importer.service;

import com.proofolio.application.entity.Application;
import com.proofolio.application.entity.ApplicationStatus;
import com.proofolio.application.repository.ApplicationRepository;
import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import com.proofolio.company.entity.Company;
import com.proofolio.company.service.CompanyService;
import com.proofolio.importer.dto.ImportDtos.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotionImportService {

    private static final int SAMPLE_ROWS = 5;
    private static final String DEFAULT_POSITION = "미지정";

    private final ApplicationRepository applicationRepository;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    public PreviewResponse preview(InputStream csv) {
        Parsed parsed = parse(csv);
        List<List<String>> samples = parsed.records().stream().limit(SAMPLE_ROWS)
                .map(r -> parsed.columns().stream().map(c -> value(r, c)).toList())
                .toList();
        return new PreviewResponse(parsed.columns(), samples, suggestMapping(parsed.columns()), parsed.records().size());
    }

    @Transactional
    public ImportResult importCsv(InputStream csv, NotionMapping mapping) {
        Parsed parsed = parse(csv);
        if (!parsed.columns().contains(mapping.companyName())) {
            throw ApiException.importError("companyName 컬럼을 찾을 수 없습니다: " + mapping.companyName());
        }
        Map<String, ApplicationStatus> statusValues = new HashMap<>(defaultStatusValues());
        if (mapping.statusValues() != null) statusValues.putAll(mapping.statusValues());

        int created = 0, updated = 0, skipped = 0;
        List<ImportError> errors = new ArrayList<>();
        int rowNo = 1;
        for (CSVRecord r : parsed.records()) {
            rowNo++;
            String companyName = value(r, mapping.companyName());
            if (companyName.isBlank()) {
                skipped++;
                errors.add(new ImportError(rowNo, "회사 이름이 비어 있어 건너뜀"));
                continue;
            }
            String position = mapping.positionTitle() == null ? "" : value(r, mapping.positionTitle());
            if (position.isBlank()) position = DEFAULT_POSITION;

            Company company = companyService.findOrCreateByName(companyName);
            final String positionTitle = position;
            Optional<Application> existing = applicationRepository
                    .findFirstByCompanyIdAndPositionTitleIgnoreCase(company.getId(), positionTitle);
            Application app = existing.orElseGet(() -> new Application(currentUser.id(), company, positionTitle));
            boolean isNew = existing.isEmpty();

            if (mapping.deadlineAt() != null) {
                String raw = value(r, mapping.deadlineAt());
                if (!raw.isBlank()) {
                    Optional<Instant> d = NotionDateParser.parse(raw);
                    if (d.isPresent()) app.setDeadlineAt(d.get());
                    else errors.add(new ImportError(rowNo, "마감일을 해석하지 못해 비워 둠: " + raw));
                }
            }
            if (mapping.postingUrl() != null) {
                String url = value(r, mapping.postingUrl());
                if (!url.isBlank()) app.setPostingUrl(url);
            }
            if (mapping.notes() != null && !mapping.notes().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String col : mapping.notes()) {
                    String v = value(r, col);
                    if (!v.isBlank()) sb.append(col).append(": ").append(v).append('\n');
                }
                if (!sb.isEmpty()) app.setNotes(sb.toString().trim());
            }

            ApplicationStatus status = null;
            if (mapping.status() != null) {
                String raw = value(r, mapping.status());
                if (!raw.isBlank()) {
                    status = resolveStatus(raw, statusValues);
                    if (status == null) errors.add(new ImportError(rowNo, "상태값을 매핑하지 못함: " + raw));
                }
            }
            if (isNew) {
                app.changeStatus(status == null ? ApplicationStatus.INTERESTED : status, "노션 임포트");
                applicationRepository.save(app);
                created++;
            } else {
                if (status != null && status != app.getStatus()) app.changeStatus(status, "노션 임포트");
                updated++;
            }
        }
        return new ImportResult(created, updated, skipped, errors);
    }

    // ---- helpers ----

    private record Parsed(List<String> columns, List<CSVRecord> records) {}

    private static Parsed parse(InputStream in) {
        try (InputStream bom = BOMInputStream.builder().setInputStream(in).get();
             InputStreamReader reader = new InputStreamReader(bom, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true)
                     .get().parse(reader)) {
            List<String> columns = parser.getHeaderNames().stream().map(String::trim).toList();
            if (columns.isEmpty()) throw ApiException.importError("CSV 헤더가 없습니다");
            return new Parsed(columns, parser.getRecords());
        } catch (IOException | IllegalArgumentException e) {
            throw ApiException.importError("CSV를 읽지 못했습니다: " + e.getMessage());
        }
    }

    private static String value(CSVRecord r, String column) {
        if (column == null || !r.isMapped(column)) return "";
        String v = r.get(column);
        return v == null ? "" : v.trim();
    }

    static NotionMapping suggestMapping(List<String> columns) {
        String company = first(columns, "회사", "기업", "company", "이름", "name", "title");
        String position = first(columns, "직무", "포지션", "position", "직군", "role");
        String deadline = first(columns, "마감", "deadline", "due", "날짜", "date");
        String status = first(columns, "상태", "status", "진행", "단계", "stage");
        String url = first(columns, "링크", "url", "link", "공고");
        List<String> notes = columns.stream()
                .filter(c -> containsAny(c, "메모", "비고", "note", "인재상", "분석", "특이"))
                .toList();
        return new NotionMapping(company == null ? columns.get(0) : company, position, deadline, status, url,
                notes.isEmpty() ? null : notes, defaultStatusValues());
    }

    private static String first(List<String> columns, String... keys) {
        for (String key : keys) {
            for (String c : columns) {
                if (c.toLowerCase(Locale.ROOT).contains(key.toLowerCase(Locale.ROOT))) return c;
            }
        }
        return null;
    }

    private static boolean containsAny(String s, String... keys) {
        String lower = s.toLowerCase(Locale.ROOT);
        return Arrays.stream(keys).anyMatch(k -> lower.contains(k.toLowerCase(Locale.ROOT)));
    }

    static Map<String, ApplicationStatus> defaultStatusValues() {
        Map<String, ApplicationStatus> m = new LinkedHashMap<>();
        m.put("관심", ApplicationStatus.INTERESTED);
        m.put("지원 예정", ApplicationStatus.PLANNED);
        m.put("지원예정", ApplicationStatus.PLANNED);
        m.put("예정", ApplicationStatus.PLANNED);
        m.put("작성 중", ApplicationStatus.WRITING);
        m.put("작성중", ApplicationStatus.WRITING);
        m.put("진행 중", ApplicationStatus.WRITING);
        m.put("진행중", ApplicationStatus.WRITING);
        m.put("제출", ApplicationStatus.SUBMITTED);
        m.put("지원 완료", ApplicationStatus.SUBMITTED);
        m.put("지원완료", ApplicationStatus.SUBMITTED);
        m.put("서류 통과", ApplicationStatus.DOCUMENT_PASSED);
        m.put("서류통과", ApplicationStatus.DOCUMENT_PASSED);
        m.put("서류 합격", ApplicationStatus.DOCUMENT_PASSED);
        m.put("코테", ApplicationStatus.TEST);
        m.put("코딩테스트", ApplicationStatus.TEST);
        m.put("필기", ApplicationStatus.TEST);
        m.put("인적성", ApplicationStatus.TEST);
        m.put("면접", ApplicationStatus.INTERVIEW);
        m.put("최종 합격", ApplicationStatus.ACCEPTED);
        m.put("최종합격", ApplicationStatus.ACCEPTED);
        m.put("합격", ApplicationStatus.ACCEPTED);
        m.put("불합격", ApplicationStatus.REJECTED);
        m.put("탈락", ApplicationStatus.REJECTED);
        m.put("포기", ApplicationStatus.WITHDRAWN);
        m.put("철회", ApplicationStatus.WITHDRAWN);
        return m;
    }

    private static ApplicationStatus resolveStatus(String raw, Map<String, ApplicationStatus> values) {
        String s = raw.trim();
        if (values.containsKey(s)) return values.get(s);
        try { return ApplicationStatus.valueOf(s.toUpperCase(Locale.ROOT).replace(' ', '_')); } catch (IllegalArgumentException ignored) {}
        // longest key contained in the raw value wins
        return values.entrySet().stream()
                .filter(e -> s.contains(e.getKey()))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
