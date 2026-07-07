package com.library.book.recommend;

import com.library.book.domain.Book;
import com.library.book.feign.BorrowingFeignClient;
import com.library.book.mapper.BookMapper;
import com.library.common.constant.Constants;
import com.library.common.domain.BorrowDTO;
import com.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务 - 基于内容的图书推荐
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final BookMapper bookMapper;
    private final BorrowingFeignClient borrowingFeignClient;
    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 缓存 Key 前缀 */
    private static final String REDIS_RECOMMEND_PREFIX = "recommend:user:";
    /** 缓存 TTL（秒） */
    private static final long CACHE_TTL = 3600;
    /** 默认推荐数量 */
    private static final int DEFAULT_LIMIT = 10;

    // ==================== 对外接口 ====================

    /**
     * 为用户推荐图书
     *
     * @param userId 用户ID
     * @param limit  返回数量
     * @return 推荐的图书列表
     */
    public List<Book> recommendForUser(Long userId, int limit) {
        if (limit <= 0) limit = DEFAULT_LIMIT;

        // 1. 查缓存
        String cacheKey = REDIS_RECOMMEND_PREFIX + userId;
        String cachedIds = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Long> ids = Arrays.stream(cachedIds.split(","))
                    .map(Long::parseLong).collect(Collectors.toList());
            List<Book> cached = ids.stream()
                    .map(id -> bookMapper.selectById(id))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!cached.isEmpty()) {
                // 按缓存中的顺序排序
                Map<Long, Integer> order = new HashMap<>();
                for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
                cached.sort(Comparator.comparingInt(b -> order.getOrDefault(b.getId(), 999)));
                return cached.size() > limit ? cached.subList(0, limit) : cached;
            }
        }

        // 2. 获取用户的借阅历史
        List<BorrowDTO> borrows = fetchUserBorrows(userId);
        if (borrows == null || borrows.isEmpty()) {
            // 冷启动：无借阅记录，返回热门图书
            log.info("用户 {} 无借阅记录，返回热门图书推荐", userId);
            return getHotBooksFallback(limit);
        }

        // 3. 构建用户兴趣画像
        Set<Long> borrowedBookIds = borrows.stream()
                .map(BorrowDTO::getBookId).collect(Collectors.toSet());
        UserProfile profile = buildUserProfile(borrows);

        // 4. 计算所有在库图书的相似度
        List<Book> candidates = bookMapper.selectList(null).stream()
                .filter(b -> b.getStatus() == Constants.BOOK_STATUS_IN
                        && !borrowedBookIds.contains(b.getId()))
                .collect(Collectors.toList());

        // 5. 评分并排序
        List<ScoredBook> scored = new ArrayList<>();
        for (Book book : candidates) {
            double score = calculateSimilarity(profile, book);
            if (score > 0) {
                scored.add(new ScoredBook(book, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 6. 取 Top-N
        List<Book> result = scored.stream()
                .limit(limit)
                .map(sb -> sb.book)
                .collect(Collectors.toList());

        // 7. 写入缓存
        if (!result.isEmpty()) {
            String idsStr = result.stream()
                    .map(b -> String.valueOf(b.getId()))
                    .collect(Collectors.joining(","));
            stringRedisTemplate.opsForValue().set(cacheKey, idsStr, CACHE_TTL, TimeUnit.SECONDS);
        }

        log.info("为用户 {} 推荐了 {} 本图书", userId, result.size());
        return result;
    }

    /**
     * 找某本图书的相似图书（"你可能也喜欢"）
     *
     * @param bookId 基准图书ID
     * @param limit  返回数量
     * @return 相似图书列表
     */
    public List<Book> findSimilarBooks(Long bookId, int limit) {
        if (limit <= 0) limit = DEFAULT_LIMIT;

        Book target = bookMapper.selectById(bookId);
        if (target == null) return Collections.emptyList();

        // 用目标图书的特征构建临时画像
        UserProfile profile = new UserProfile();
        profile.addCategory(target.getCategory(), 1.0);
        if (target.getAuthor() != null && !target.getAuthor().isEmpty()) {
            profile.addAuthor(target.getAuthor(), 1.0);
        }
        // 添加书名关键词
        if (target.getTitle() != null) {
            for (String kw : extractKeywords(target.getTitle())) {
                profile.addKeyword(kw, 1.0);
            }
        }

        List<Book> candidates = bookMapper.selectList(null).stream()
                .filter(b -> b.getStatus() == Constants.BOOK_STATUS_IN
                        && !b.getId().equals(bookId))
                .collect(Collectors.toList());

        List<ScoredBook> scored = new ArrayList<>();
        for (Book book : candidates) {
            double score = calculateSimilarity(profile, book);
            if (score > 0) scored.add(new ScoredBook(book, score));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream().limit(limit).map(sb -> sb.book).collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    /**
     * 从 Feign 获取用户的借阅记录
     */
    private List<BorrowDTO> fetchUserBorrows(Long userId) {
        try {
            Result<List<BorrowDTO>> result = borrowingFeignClient.getBorrowHistory(userId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("获取用户借阅记录失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 冷启动降级：返回热门图书
     */
    private List<Book> getHotBooksFallback(int limit) {
        return bookMapper.findHotBooks(limit);
    }

    /**
     * 从借阅历史构建用户兴趣画像
     */
    private UserProfile buildUserProfile(List<BorrowDTO> borrows) {
        UserProfile profile = new UserProfile();

        // 统计各类别/作者的权重（借阅次数越多权重越高）
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> authorCount = new HashMap<>();

        for (BorrowDTO b : borrows) {
            Book book = bookMapper.selectById(b.getBookId());
            if (book == null) continue;

            categoryCount.merge(book.getCategory(), 1, Integer::sum);
            if (book.getAuthor() != null && !book.getAuthor().isEmpty()) {
                authorCount.merge(book.getAuthor(), 1, Integer::sum);
            }
            // 提取书名关键词
            if (book.getTitle() != null) {
                for (String kw : extractKeywords(book.getTitle())) {
                    profile.addKeyword(kw, 0.5);
                }
            }
        }

        // 将统计结果归一化到 [0, 1] 区间
        int maxCat = categoryCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int maxAuth = authorCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        categoryCount.forEach((cat, cnt) -> profile.addCategory(cat, (double) cnt / maxCat));
        authorCount.forEach((auth, cnt) -> profile.addAuthor(auth, (double) cnt / maxAuth));

        return profile;
    }

    /**
     * 计算图书与用户兴趣画像的相似度
     * 加权公式：类别Jaccard×0.4 + 作者Jaccard×0.3 + 关键词匹配×0.3
     */
    private double calculateSimilarity(UserProfile profile, Book book) {
        double catSim = profile.categories.isEmpty() ? 0 :
                jaccard(profile.categories.keySet(), Collections.singleton(book.getCategory()));

        double authSim = (profile.authors.isEmpty() || book.getAuthor() == null) ? 0 :
                jaccard(profile.authors.keySet(), Collections.singleton(book.getAuthor()));

        double kwSim = 0;
        if (!profile.keywords.isEmpty() && book.getTitle() != null) {
            Set<String> bookKws = extractKeywords(book.getTitle());
            if (!bookKws.isEmpty()) {
                kwSim = jaccard(profile.keywords.keySet(), bookKws);
            }
        }

        return catSim * 0.4 + authSim * 0.3 + kwSim * 0.3;
    }

    /**
     * Jaccard 系数：两集合交集大小 / 并集大小
     */
    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 0;
        long inter = a.stream().filter(b::contains).count();
        return (double) inter / union.size();
    }

    /**
     * 从文本中提取关键词（按常见分隔符拆分）
     */
    private Set<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        return Arrays.stream(text.split("[（(）)\\s、，,：:.。·\\-—]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .collect(Collectors.toSet());
    }

    // ==================== 内部类 ====================

    /**
     * 用户兴趣画像
     */
    private static class UserProfile {
        final Map<String, Double> categories = new HashMap<>();
        final Map<String, Double> authors = new HashMap<>();
        final Map<String, Double> keywords = new HashMap<>();

        void addCategory(String cat, double weight) {
            categories.merge(cat, weight, Double::max);
        }

        void addAuthor(String author, double weight) {
            authors.merge(author, weight, Double::max);
        }

        void addKeyword(String kw, double weight) {
            keywords.merge(kw, weight, Double::max);
        }
    }

    /**
     * 带评分的图书封装
     */
    @lombok.AllArgsConstructor
    private static class ScoredBook {
        final Book book;
        final double score;
    }
}
