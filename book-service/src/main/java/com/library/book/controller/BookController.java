package com.library.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.book.domain.Book;
import com.library.book.domain.PurchaseRecommend;
import com.library.book.service.BookService;
import com.library.common.annotation.OperationLog;
import com.library.common.domain.BookDTO;
import com.library.common.domain.PurchaseRecommendDTO;
import com.library.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "04-图书管理")
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @ApiOperation("入库新书")
    @PostMapping("/add")
    public Result<BookDTO> addBook(@Valid @RequestBody Book book) {
        bookService.addBook(book);
        return Result.success("图书入库成功", bookService.convertToDTO(book));
    }

    @ApiOperation("更新图书信息")
    @PutMapping("/{bookId}")
    public Result<?> updateBook(@PathVariable Long bookId, @RequestBody Book updates) {
        bookService.updateBook(bookId, updates);
        return Result.success("图书更新成功", null);
    }

    @ApiOperation("下架图书")
    @PutMapping("/offline/{bookId}")
    public Result<?> offlineBook(@PathVariable Long bookId) {
        bookService.offlineBook(bookId);
        return Result.success("图书已下架", null);
    }

    @ApiOperation("重新上架图书")
    @PutMapping("/reonline/{bookId}")
    public Result<?> reOnlineBook(@PathVariable Long bookId) {
        bookService.reOnlineBook(bookId);
        return Result.success("图书已重新上架", null);
    }

    @ApiOperation("借出图书（Feign调用扣减库存）")
    @PutMapping("/borrow/{bookId}")
    public Result<?> borrowBook(@PathVariable Long bookId) {
        bookService.borrowBook(bookId);
        return Result.success("借出成功", null);
    }

    @ApiOperation("归还图书（Feign调用恢复库存）")
    @PutMapping("/return/{bookId}")
    public Result<?> returnBook(@PathVariable Long bookId) {
        bookService.returnBook(bookId);
        return Result.success("归还成功", null);
    }

    @ApiOperation("获取图书详情")
    @GetMapping("/{bookId}")
    public Result<BookDTO> getBook(@PathVariable Long bookId) {
        return Result.success(bookService.convertToDTO(bookService.getBookById(bookId)));
    }

    @ApiOperation("分页查询图书列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> listBooks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        IPage<Book> page = bookService.listBooks(pageNum, pageSize, category, keyword);
        List<BookDTO> dtos = page.getRecords().stream().map(bookService::convertToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("OPAC搜索图书")
    @GetMapping("/search")
    public Result<List<BookDTO>> searchBooks(@RequestParam String keyword) {
        List<Book> books = bookService.searchBooks(keyword);
        return Result.success(books.stream().map(bookService::convertToDTO).collect(Collectors.toList()));
    }

    @ApiOperation("获取热门图书排行")
    @GetMapping("/hot")
    public Result<List<BookDTO>> getHotBooks(@RequestParam(defaultValue = "10") Integer limit) {
        List<Book> books = bookService.getHotBooks(limit);
        return Result.success(books.stream().map(bookService::convertToDTO).collect(Collectors.toList()));
    }

    @ApiOperation("获取图书分类统计")
    @GetMapping("/category-stats")
    public Result<List<Map<String, Object>>> getCategoryStats() {
        return Result.success(bookService.getCategoryStats());
    }

    @ApiOperation("获取低库存图书列表")
    @GetMapping("/low-stock")
    public Result<List<BookDTO>> getLowStockBooks() {
        List<Book> books = bookService.getLowStockBooks();
        return Result.success(books.stream().map(bookService::convertToDTO).collect(Collectors.toList()));
    }

    @ApiOperation("读者荐购图书")
    @PostMapping("/recommend")
    public Result<PurchaseRecommendDTO> recommendBook(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Username") String username,
            @RequestHeader(value = "X-Real-Name", required = false) String realName,
            @Valid @RequestBody PurchaseRecommend recommend) {
        recommend.setUserId(userId);
        recommend.setUsername(username);
        recommend.setRealName(realName != null ? realName : username);
        bookService.recommendBook(recommend);
        return Result.success("荐购成功", bookService.convertRecommendToDTO(recommend));
    }

    @ApiOperation("查看荐购列表")
    @GetMapping("/recommend/list")
    public Result<Map<String, Object>> listRecommends(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId) {
        IPage<PurchaseRecommend> page = bookService.listRecommends(pageNum, pageSize, userId);
        List<PurchaseRecommendDTO> dtos = page.getRecords().stream().map(bookService::convertRecommendToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("审核图书荐购")
    @OperationLog(type = "REVIEW_RECOMMEND", desc = "审核图书荐购")
    @PutMapping("/recommend/review/{id}")
    public Result<?> reviewRecommend(@PathVariable Long id, @RequestParam Integer status,
                                     @RequestParam(required = false) String comment,
                                     @RequestHeader("X-Username") String reviewerName) {
        bookService.reviewRecommend(id, status, comment, reviewerName);
        return Result.success("审核完成", null);
    }

    @ApiOperation("OCR智能识别")
    @PostMapping("/ocr")
    public Result<Map<String, Object>> ocr(@RequestParam String type, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("success", false);
        result.put("message", "OCR功能需要通过前端DeepSeek调用");
        return Result.success(result);
    }

    @ApiOperation("AI智能推荐（基于内容）")
    @GetMapping("/recommend")
    public Result<List<Book>> recommendForUser(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.getHotBooks(limit));
    }

    @ApiOperation("AI智能推荐（相似图书）")
    @GetMapping("/recommend/similar/{bookId}")
    public Result<List<Book>> similarBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.getHotBooks(limit));
    }
}
