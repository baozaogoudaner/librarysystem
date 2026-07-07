package com.library.book.recommend;

import com.library.book.domain.Book;
import com.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐控制器
 */
@RestController
@RequestMapping("/book/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * 为用户个性化推荐图书
     *
     * @param userId 用户ID（可从请求头或查询参数获取）
     * @param limit  返回数量，默认10
     * @return 推荐图书列表
     */
    @GetMapping
    public Result<List<Book>> recommend(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Book> books = recommendService.recommendForUser(userId, limit);
        return Result.success(books);
    }

    /**
     * 找相似图书（"你可能也喜欢"）
     *
     * @param bookId 基准图书ID
     * @param limit  返回数量，默认10
     * @return 相似图书列表
     */
    @GetMapping("/similar/{bookId}")
    public Result<List<Book>> similarBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Book> books = recommendService.findSimilarBooks(bookId, limit);
        return Result.success(books);
    }
}
