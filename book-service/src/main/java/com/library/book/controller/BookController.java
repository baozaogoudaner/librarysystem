package com.library.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.book.domain.Book;
import com.library.book.domain.PurchaseRecommend;
import com.library.book.service.BookService;
import com.library.common.annotation.OperationLog;
import com.library.common.domain.BookDTO;
import com.library.common.domain.PurchaseRecommendDTO;
import com.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书控制器
 */
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // ==================== 图书管理 ====================

    /**
     * 入库新书
     */
    @OperationLog(type = "ADD_BOOK", desc = "入库新书")
    @PostMapping("/add")
    public Result<BookDTO> addBook(@Valid @RequestBody Book book) {
        bookService.addBook(book);
        return Result.success("图书入库成功", bookService.convertToDTO(book));
    }

    /**
     * 更新图书
     */
    @OperationLog(type = "UPDATE_BOOK", desc = "更新图书信息")
    @PutMapping("/{bookId}")
    public Result<?> updateBook(@PathVariable Long bookId, @RequestBody Book updates) {
        bookService.updateBook(bookId, updates);
        return Result.success("图书更新成功", null);
    }

    /**
     * 下架图书
     */
    @OperationLog(type = "OFFLINE_BOOK", desc = "下架图书")
    @PutMapping("/offline/{bookId}")
    public Result<?> offlineBook(@PathVariable Long bookId) {
        bookService.offlineBook(bookId);
        return Result.success("图书已下架", null);
    }

    /**
     * 重新上架
     */
    @OperationLog(type = "REONLINE_BOOK", desc = "重新上架图书")
    @PutMapping("/reonline/{bookId}")
    public Result<?> reOnlineBook(@PathVariable Long bookId) {
        bookService.reOnlineBook(bookId);
        return Result.success("图书已重新上架", null);
    }

    /**
     * 借出图书（Feign调用扣减库存）
     */
    @PutMapping("/borrow/{bookId}")
    public Result<?> borrowBook(@PathVariable Long bookId) {
        bookService.borrowBook(bookId);
        return Result.success("借出成功", null);
    }

    /**
     * 归还图书（Feign调用恢复库存）
     */
    @PutMapping("/return/{bookId}")
    public Result<?> returnBook(@PathVariable Long bookId) {
        bookService.returnBook(bookId);
        return Result.success("归还成功", null);
    }

    /**
     * 获取图书详情
     */
    @GetMapping("/{bookId}")
    public Result<BookDTO> getBook(@PathVariable Long bookId) {
        Book book = bookService.getBookById(bookId);
        return Result.success(bookService.convertToDTO(book));
    }

    /**
     * 分页查询图书列表
     */
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

    /**
     * OPAC搜索
     */
    @GetMapping("/search")
    public Result<List<BookDTO>> searchBooks(@RequestParam String keyword) {
        List<Book> books = bookService.searchBooks(keyword);
        List<BookDTO> dtos = books.stream().map(bookService::convertToDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 热门图书
     */
    @GetMapping("/hot")
    public Result<List<BookDTO>> getHotBooks(@RequestParam(defaultValue = "20") Integer limit) {
        List<Book> books = bookService.getHotBooks(limit);
        List<BookDTO> dtos = books.stream().map(bookService::convertToDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 低库存预警
     */
    @GetMapping("/low-stock")
    public Result<List<BookDTO>> getLowStockBooks() {
        List<Book> books = bookService.getLowStockBooks();
        List<BookDTO> dtos = books.stream().map(bookService::convertToDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 分类统计
     */
    @GetMapping("/category-stats")
    public Result<List<Map<String, Object>>> getCategoryStats() {
        return Result.success(bookService.getCategoryStats());
    }

    // ==================== 荐购管理 ====================

    /**
     * 读者荐购
     */
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

    /**
     * 查看荐购列表
     */
    @GetMapping("/recommend/list")
    public Result<Map<String, Object>> listRecommends(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId) {
        IPage<PurchaseRecommend> page = bookService.listRecommends(pageNum, pageSize, userId);
        List<PurchaseRecommendDTO> dtos = page.getRecords().stream()
                .map(bookService::convertRecommendToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    /**
     * 审核荐购
     */
    @OperationLog(type = "REVIEW_RECOMMEND", desc = "审核图书荐购")
    @PutMapping("/recommend/review/{id}")
    public Result<?> reviewRecommend(@PathVariable Long id,
                                     @RequestParam Integer status,
                                     @RequestParam(required = false) String comment,
                                     @RequestHeader("X-Username") String reviewerName) {
        bookService.reviewRecommend(id, status, comment, reviewerName);
        return Result.success("审核完成", null);
    }
}
