package com.library.book.ocr;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ISBN 外部数据查询服务
 * 通过豆瓣API补全图书信息
 */
@Slf4j
@Service
public class BookLookupService {

    /** 豆瓣图书API（通过 ISBN 查询） */
    private static final String DOUBAN_API_URL = "https://api.douban.com/v2/book/isbn/";
    /** 备用API代理 */
    private static final String DOUBAN_PROXY_URL = "https://douban.uieee.xyz/v2/book/isbn/";

    /**
     * 通过 ISBN 查询图书信息
     *
     * @param isbn ISBN（10位或13位）
     * @return 图书信息 Map，包含 title/author/publisher/pubdate/summary 等字段，失败返回空 Map
     */
    public Map<String, Object> lookupByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return new HashMap<>();
        }
        isbn = isbn.trim();

        // 先尝试主API
        Map<String, Object> result = tryApi(DOUBAN_API_URL + isbn);
        if (!result.isEmpty()) return result;

        // 再尝试备用代理
        result = tryApi(DOUBAN_PROXY_URL + isbn);
        if (!result.isEmpty()) return result;

        log.warn("ISBN {} 查询失败，所有API均不可用", isbn);
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("isbn", isbn);
        fallback.put("note", "自动查询失败，请手动录入");
        return fallback;
    }

    /**
     * 尝试调用一个 API 端点
     */
    private Map<String, Object> tryApi(String url) {
        try {
            String resp = HttpUtil.get(url, 5000);
            if (resp == null || resp.isEmpty()) return new HashMap<>();

            JSONObject json = JSONUtil.parseObj(resp);
            // 检查是否包含错误信息
            if (json.containsKey("code") && json.getInt("code") != 0) {
                log.warn("API {} 返回错误: {}", url, json.get("msg"));
                return new HashMap<>();
            }
            if (json.containsKey("title")) {
                Map<String, Object> result = new HashMap<>();
                result.put("isbn", isbn13(json));
                result.put("title", json.getStr("title", ""));
                result.put("author", formatList(json.get("author")));
                result.put("publisher", json.getStr("publisher", ""));
                result.put("pubdate", json.getStr("pubdate", ""));
                result.put("summary", json.getStr("summary", ""));
                result.put("price", json.getStr("price", ""));
                result.put("cover", json.getStr("image", ""));
                result.put("source", url.contains("douban.uieee") ? "douban_proxy" : "douban");
                log.info("ISBN {} 查询成功，书名: {}", result.get("isbn"), result.get("title"));
                return result;
            }
        } catch (Exception e) {
            log.warn("API {} 调用异常: {}", url, e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * 从豆瓣响应中提取 ISBN-13
     */
    private String isbn13(JSONObject json) {
        if (json.containsKey("isbn13")) {
            String isbn13 = json.getStr("isbn13");
            if (isbn13 != null && !isbn13.isEmpty()) return isbn13;
        }
        return json.getStr("isbn", "");
    }

    /**
     * 格式化作者列表（数组转逗号分隔字符串）
     */
    private String formatList(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof cn.hutool.json.JSONArray) {
            return ((cn.hutool.json.JSONArray) obj).toList(String.class)
                    .stream().reduce((a, b) -> a + " / " + b).orElse("");
        }
        return obj.toString();
    }
}
