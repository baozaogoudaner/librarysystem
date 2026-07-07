package com.library.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.domain.Book;
import com.library.book.domain.PurchaseRecommend;
import com.library.book.mapper.BookMapper;
import com.library.book.mapper.PurchaseRecommendMapper;
import com.library.common.constant.Constants;
import com.library.common.domain.BookDTO;
import com.library.common.domain.PurchaseRecommendDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图书服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final PurchaseRecommendMapper purchaseRecommendMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // ==================== 图书管理 ====================

    /**
     * 入库新书
     */
    @Transactional
    public Book addBook(Book book) {
        Book exist = bookMapper.selectOne(
                new LambdaQueryWrapper<Book>().eq(Book::getIsbn, book.getIsbn()));
        if (exist != null) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_EXISTS);
        }
        book.setStatus(Constants.BOOK_STATUS_IN);
        book.setBorrowCount(0);
        if (book.getAvailableStock() == null) {
            book.setAvailableStock(book.getTotalStock());
        }
        bookMapper.insert(book);
        // 更新 Redis 库存缓存
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_BOOK_STOCK_PREFIX + book.getId(),
                String.valueOf(book.getAvailableStock()));
        log.info("新书入库: {} ID:{} ISBN:{} 库存:{}", book.getTitle(), book.getId(), book.getIsbn(), book.getTotalStock());
        return book;
    }

    /**
     * 更新图书信息
     */
    @Transactional
    public void updateBook(Long bookId, Book updates) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (updates.getTitle() != null) book.setTitle(updates.getTitle());
        if (updates.getAuthor() != null) book.setAuthor(updates.getAuthor());
        if (updates.getPublisher() != null) book.setPublisher(updates.getPublisher());
        if (updates.getCategory() != null) book.setCategory(updates.getCategory());
        if (updates.getDescription() != null) book.setDescription(updates.getDescription());
        if (updates.getCoverUrl() != null) book.setCoverUrl(updates.getCoverUrl());
        if (updates.getPrice() != null) book.setPrice(updates.getPrice());
        if (updates.getLocation() != null) book.setLocation(updates.getLocation());
        if (updates.getCallNumber() != null) book.setCallNumber(updates.getCallNumber());
        if (updates.getEbookUrl() != null) {
            book.setEbookUrl(updates.getEbookUrl());
            book.setHasEbook(updates.getEbookUrl().length() > 0);
        }
        if (updates.getTotalStock() != null) {
            int delta = updates.getTotalStock() - book.getTotalStock();
            book.setTotalStock(updates.getTotalStock());
            book.setAvailableStock(Math.max(0, book.getAvailableStock() + delta));
        }
        bookMapper.updateById(book);
        log.info("图书更新: ID:{} {}", bookId, book.getTitle());
    }

    /**
     * 下架图书
     */
    @Transactional
    public void offlineBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (book.getStatus() == Constants.BOOK_STATUS_OFFLINE) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_OFFLINE);
        }
        book.setStatus(Constants.BOOK_STATUS_OFFLINE);
        bookMapper.updateById(book);
        stringRedisTemplate.delete(Constants.REDIS_BOOK_STOCK_PREFIX + bookId);
        log.info("图书下架: ID:{} {}", bookId, book.getTitle());
    }

    /**
     * 重新上架
     */
    @Transactional
    public void reOnlineBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        book.setStatus(Constants.BOOK_STATUS_IN);
        bookMapper.updateById(book);
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_BOOK_STOCK_PREFIX + bookId, String.valueOf(book.getAvailableStock()));
        log.info("图书重新上架: ID:{} {}", bookId, book.getTitle());
    }

    /**
     * 借出图书（扣减库存）
     */
    @Transactional
    public void borrowBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (book.getStatus() != Constants.BOOK_STATUS_IN) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }
        if (book.getAvailableStock() <= 0) {
            throw new BusinessException(ResultCode.BOOK_STOCK_INSUFFICIENT);
        }
        book.setAvailableStock(book.getAvailableStock() - 1);
        book.setBorrowCount(book.getBorrowCount() + 1);
        if (book.getAvailableStock() == 0) {
            book.setStatus(Constants.BOOK_STATUS_OUT);
        }
        bookMapper.updateById(book);
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_BOOK_STOCK_PREFIX + bookId, String.valueOf(book.getAvailableStock()));
        log.info("图书借出: ID:{} {} 剩余库存:{}", bookId, book.getTitle(), book.getAvailableStock());
    }

    /**
     * 归还图书（归还库存）
     */
    @Transactional
    public void returnBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        book.setAvailableStock(book.getAvailableStock() + 1);
        if (book.getStatus() == Constants.BOOK_STATUS_OUT) {
            book.setStatus(Constants.BOOK_STATUS_IN);
        }
        bookMapper.updateById(book);
        stringRedisTemplate.opsForValue().set(
                Constants.REDIS_BOOK_STOCK_PREFIX + bookId, String.valueOf(book.getAvailableStock()));
        log.info("图书归还: ID:{} {} 剩余库存:{}", bookId, book.getTitle(), book.getAvailableStock());
    }

    /**
     * 获取图书详情
     */
    public Book getBookById(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return book;
    }

    /**
     * 分页查询图书列表
     */
    public IPage<Book> listBooks(Integer pageNum, Integer pageSize, String category, String keyword) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Book::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        wrapper.ne(Book::getStatus, Constants.BOOK_STATUS_OFFLINE);
        wrapper.orderByDesc(Book::getCreateTime);
        return bookMapper.selectPage(page, wrapper);
    }

    /**
     * 搜索图书（OPAC检索）
     */
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(ResultCode.SEARCH_KEYWORD_EMPTY);
        }
        return bookMapper.searchByKeyword(keyword.trim());
    }

    /**
     * 获取热门图书
     */
    public List<Book> getHotBooks(int limit) {
        return bookMapper.findHotBooks(limit);
    }

    /**
     * 获取低库存图书（库存预警）
     */
    public List<Book> getLowStockBooks() {
        return bookMapper.findLowStockBooks();
    }

    /**
     * 获取分类统计
     */
    public List<Map<String, Object>> getCategoryStats() {
        // 直接用 MyBatis 的 selectList 避免 Object[] 构造问题
        return bookMapper.getCategoryStats().stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", row.get("category"));
            map.put("count", row.get("cnt"));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    public BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        BeanUtils.copyProperties(book, dto);
        return dto;
    }

    // ==================== 荐购管理 ====================

    /**
     * 读者荐购
     */
    @Transactional
    public PurchaseRecommend recommendBook(PurchaseRecommend recommend) {
        // 检查是否已在荐购列表中
        Long count = purchaseRecommendMapper.selectCount(
                new LambdaQueryWrapper<PurchaseRecommend>()
                        .eq(PurchaseRecommend::getIsbn, recommend.getIsbn())
                        .ne(PurchaseRecommend::getStatus, Constants.PURCHASE_RECOMMEND_REJECTED));
        if (count > 0) {
            throw new BusinessException(ResultCode.PURCHASE_RECOMMEND_DUPLICATE);
        }
        recommend.setStatus(Constants.PURCHASE_RECOMMEND_PENDING);
        purchaseRecommendMapper.insert(recommend);
        log.info("读者荐购: {} ISBN:{} 用户:{}", recommend.getTitle(), recommend.getIsbn(), recommend.getUsername());
        return recommend;
    }

    /**
     * 查看荐购列表
     */
    public IPage<PurchaseRecommend> listRecommends(Integer pageNum, Integer pageSize, Long userId) {
        Page<PurchaseRecommend> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PurchaseRecommend> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PurchaseRecommend::getUserId, userId);
        }
        wrapper.orderByDesc(PurchaseRecommend::getCreateTime);
        return purchaseRecommendMapper.selectPage(page, wrapper);
    }

    /**
     * 审核荐购
     */
    @Transactional
    public void reviewRecommend(Long id, Integer status, String comment, String reviewerName) {
        PurchaseRecommend recommend = purchaseRecommendMapper.selectById(id);
        if (recommend == null) {
            throw new BusinessException(ResultCode.PURCHASE_RECOMMEND_NOT_FOUND);
        }
        recommend.setStatus(status);
        recommend.setReviewComment(comment);
        recommend.setReviewerName(reviewerName);
        recommend.setReviewTime(LocalDateTime.now());
        purchaseRecommendMapper.updateById(recommend);
        log.info("荐购审核: ID:{} -> {}", id, status);
    }

    /**
     * 荐购转DTO
     */
    public PurchaseRecommendDTO convertRecommendToDTO(PurchaseRecommend recommend) {
        PurchaseRecommendDTO dto = new PurchaseRecommendDTO();
        BeanUtils.copyProperties(recommend, dto);
        return dto;
    }
}
