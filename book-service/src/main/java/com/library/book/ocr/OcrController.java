package com.library.book.ocr;

import com.library.book.domain.Book;
import com.library.book.mapper.BookMapper;
import com.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OCR 智能识别控制器（统一接口）
 *
 * POST /book/ocr?type=isbn        → 识别ISBN，自动补全图书信息
 * POST /book/ocr?type=barcode     → 识别条码，返回对应图书信息
 * POST /book/ocr?type=callnumber  → 识别索书号，返回馆藏位置
 */
@Slf4j
@RestController
@RequestMapping("/book/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final BookLookupService bookLookupService;
    private final BookMapper bookMapper;

    @PostMapping
    public Result<Map<String, Object>> ocr(
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return Result.error("请上传图片文件");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);

        switch (type) {
            case "isbn":
                handleIsbn(file, result);
                break;
            case "barcode":
                handleBarcode(file, result);
                break;
            case "callnumber":
                handleCallNumber(file, result);
                break;
            default:
                return Result.error("未知的识别类型: " + type + "，支持: isbn/barcode/callnumber");
        }

        return Result.success(result);
    }

    // ==================== ISBN 识别 ====================

    /**
     * 识别 ISBN 并自动补全图书信息
     */
    private void handleIsbn(MultipartFile file, Map<String, Object> result) {
        try {
            // 1. 先用 ZXing 尝试解码条码（封面可能带条码）
            String barcode = null;
            try {
                barcode = ocrService.decodeBarcode(file);
                log.info("条码解码成功: {}", barcode);
            } catch (Exception e) {
                log.info("条码解码失败，转为文字识别: {}", e.getMessage());
            }

            // 2. 如果条码解码失败，用文字识别
            String isbn = barcode;
            if (isbn == null) {
                List<String> texts = ocrService.recognizeText(file);
                isbn = ocrService.extractIsbn(texts);
            }

            if (isbn == null) {
                result.put("success", false);
                result.put("message", "未能识别出ISBN，请确保图片清晰并包含条码或ISBN编号");
                return;
            }

            // 3. 标准化 ISBN（去掉连字符）
            isbn = isbn.replaceAll("[\\s-]", "");
            result.put("isbn", isbn);

            // 4. 查本地数据库
            Book localBook = bookMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getIsbn, isbn));
            if (localBook != null) {
                result.put("success", true);
                result.put("source", "local");
                result.put("bookId", localBook.getId());
                result.put("title", localBook.getTitle());
                result.put("author", localBook.getAuthor());
                result.put("publisher", localBook.getPublisher());
                result.put("category", localBook.getCategory());
                result.put("callNumber", localBook.getCallNumber());
                result.put("location", localBook.getLocation());
                result.put("description", localBook.getDescription());
                result.put("price", localBook.getPrice());
                result.put("cover", localBook.getCoverUrl());
                result.put("message", "图书已存在馆藏中");
                return;
            }

            // 5. 本地没有，从豆瓣查询
            Map<String, Object> lookup = bookLookupService.lookupByIsbn(isbn);
            if (lookup.containsKey("title") && !lookup.get("title").toString().isEmpty()) {
                result.put("success", true);
                result.put("source", lookup.get("source"));
                result.put("title", lookup.get("title"));
                result.put("author", lookup.get("author"));
                result.put("publisher", lookup.get("publisher"));
                result.put("pubdate", lookup.get("pubdate"));
                result.put("description", lookup.get("summary"));
                result.put("price", lookup.get("price"));
                result.put("cover", lookup.get("cover"));
                result.put("message", "已从外部数据源查询到图书信息");
            } else {
                result.put("success", false);
                result.put("message", "仅识别出ISBN，未能获取图书详情，请手动录入");
            }

        } catch (Exception e) {
            log.error("ISBN识别失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "识别失败: " + e.getMessage());
        }
    }

    // ==================== 条码识别 ====================

    /**
     * 识别条码并返回对应图书信息
     */
    private void handleBarcode(MultipartFile file, Map<String, Object> result) {
        try {
            // 1. 解码条码
            String barcode = ocrService.decodeBarcode(file);
            result.put("barcode", barcode);

            // 2. 按 ISBN 查图书
            String isbn = barcode.replaceAll("[\\s-]", "");
            Book book = bookMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getIsbn, isbn));

            if (book != null) {
                result.put("success", true);
                result.put("bookId", book.getId());
                result.put("isbn", book.getIsbn());
                result.put("title", book.getTitle());
                result.put("author", book.getAuthor());
                result.put("publisher", book.getPublisher());
                result.put("category", book.getCategory());
                result.put("location", book.getLocation());
                result.put("callNumber", book.getCallNumber());
                result.put("status", book.getStatus());
                result.put("availableStock", book.getAvailableStock());
                result.put("message", "识别成功，找到对应图书");
            } else {
                result.put("success", false);
                result.put("message", "未在馆藏中找到此条码对应的图书（ISBN: " + isbn + "）");
            }

        } catch (Exception e) {
            log.error("条码识别失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "条码识别失败: " + e.getMessage());
        }
    }

    // ==================== 索书号识别 ====================

    /**
     * 识别索书号并返回馆藏位置
     */
    private void handleCallNumber(MultipartFile file, Map<String, Object> result) {
        try {
            // 1. 文字识别
            List<String> texts = ocrService.recognizeText(file);
            String callNumber = ocrService.extractCallNumber(texts);

            if (callNumber == null) {
                result.put("success", false);
                result.put("message", "未能识别出索书号，请确保图片包含清晰的索书号标签");
                result.put("recognizedTexts", texts);
                return;
            }

            result.put("callNumber", callNumber);

            // 2. 根据索书号查找馆藏图书
            List<Book> books = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getCallNumber, callNumber));

            if (!books.isEmpty()) {
                result.put("success", true);
                result.put("location", books.get(0).getLocation());
                result.put("count", books.size());
                result.put("books", books.stream().map(b -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("bookId", b.getId());
                    info.put("title", b.getTitle());
                    info.put("author", b.getAuthor());
                    info.put("availableStock", b.getAvailableStock());
                    return info;
                }).collect(Collectors.toList()));
                result.put("message", "找到 " + books.size() + " 本图书位于 " + books.get(0).getLocation());
            } else {
                result.put("success", false);
                result.put("location", "未在馆藏中找到此索书号对应的图书");
                result.put("message", "识别到索书号 " + callNumber + "，但馆藏中无匹配记录");
            }

        } catch (Exception e) {
            log.error("索书号识别失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "索书号识别失败: " + e.getMessage());
        }
    }
}
