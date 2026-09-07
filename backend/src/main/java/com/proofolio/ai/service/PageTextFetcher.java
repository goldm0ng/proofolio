package com.proofolio.ai.service;

import com.proofolio.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PageTextFetcher {

    static final int MAX_CHARS = 40_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    public String fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .followRedirects(true)
                    .get();
            doc.select("script, style, noscript, nav, footer, header, iframe, svg").remove();
            String text = collapse(doc.body() != null ? doc.body().text() : doc.text());
            if (text.length() < 200) {
                throw ApiException.ai(HttpStatus.UNPROCESSABLE_ENTITY,
                        "페이지에서 본문을 충분히 읽지 못했습니다 (동적 렌더링 가능성). 본문을 붙여넣어 주세요");
            }
            return text;
        } catch (ApiException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            log.info("fetch failed for {}: {}", url, e.toString());
            throw ApiException.ai(HttpStatus.UNPROCESSABLE_ENTITY, "페이지를 가져오지 못했습니다. 본문을 붙여넣어 주세요");
        }
    }

    public static String collapse(String raw) {
        String s = raw == null ? "" : raw.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\s*\\n\\s*", "\n").trim();
        return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS) : s;
    }
}
