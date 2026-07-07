package com.library.borrowing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.borrowing.domain.BookReserve;
import com.library.borrowing.domain.Borrow;
import com.library.borrowing.feign.BookFeignClient;
import com.library.borrowing.mapper.BookReserveMapper;
import com.library.borrowing.mapper.BorrowMapper;
import com.library.common.constant.Constants;
import com.library.common.domain.BookReserveDTO;
import com.library.common.domain.BorrowDTO;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 借阅服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowMapper borrowMapper;
    private final BookReserveMapper bookReserveMapper;
    private final BookFeignClient bookFeignClient;

    // ==================== 借阅管理 ====================

    /**
     * 借出图书
     */
    @Transactional
    public Borrow borrowBook(Long userId, String username, String realName, Long bookId, String isbn, String bookTitle) {
        // 检查逾期未还
        int overdue = borrowMapper.countOverdueBorrows(userId);
        if (overdue > 0) {
            throw new BusinessException(ResultCode.BORROW_OVERDUE_EXISTS);
        }
        // 检查借阅数量上限
        int activeCount = borrowMapper.countActiveBorrows(userId);
        if (activeCount >= Constants.MAX_BORROW_COUNT) {
            throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
        }
        // 检查是否已借此书
        List<Borrow> existing = borrowMapper.findActiveBorrowByUserAndBook(userId, bookId);
        if (!existing.isEmpty()) {
            throw new BusinessException(6007, "您已经借过此书，请勿重复借阅");
        }

        // 借出（扣减库存）
        try {
            bookFeignClient.borrowBook(bookId);
        } catch (Exception e) {
            throw new BusinessException("借书失败，图书服务异常: " + e.getMessage());
        }

        Borrow borrow = new Borrow();
        borrow.setUserId(userId);
        borrow.setUsername(username);
        borrow.setRealName(realName);
        borrow.setBookId(bookId);
        borrow.setIsbn(isbn);
        borrow.setBookTitle(bookTitle);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueDate(LocalDateTime.now().plusDays(Constants.DEFAULT_BORROW_DAYS));
        borrow.setStatus(Constants.BORROW_STATUS_ACTIVE);
        borrow.setRenewCount(0);
        borrowMapper.insert(borrow);

        log.info("借书成功: 用户={} 图书={} 应还日期={}", username, bookTitle, borrow.getDueDate());
        return borrow;
    }

    /**
     * 归还图书
     */
    @Transactional
    public void returnBook(Long borrowId, Long userId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            throw new BusinessException(ResultCode.BORROW_NOT_FOUND);
        }
        if (!borrow.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (borrow.getStatus() == Constants.BORROW_STATUS_RETURNED) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RETURNED);
        }

        // 归还（恢复库存）
        try {
            bookFeignClient.returnBook(borrow.getBookId());
        } catch (Exception e) {
            throw new BusinessException("还书失败，图书服务异常: " + e.getMessage());
        }

        borrow.setReturnTime(LocalDateTime.now());
        borrow.setStatus(Constants.BORROW_STATUS_RETURNED);
        borrowMapper.updateById(borrow);

        // 检查是否有等待中的预约，通知可借（简化为标记状态）
        List<BookReserve> waitingReserves = bookReserveMapper.selectList(
                new LambdaQueryWrapper<BookReserve>()
                        .eq(BookReserve::getBookId, borrow.getBookId())
                        .eq(BookReserve::getStatus, Constants.BOOK_RESERVE_WAITING)
                        .orderByAsc(BookReserve::getCreateTime));
        if (!waitingReserves.isEmpty()) {
            BookReserve reserve = waitingReserves.get(0);
            reserve.setStatus(Constants.BOOK_RESERVE_READY);
            reserve.setExpireTime(LocalDateTime.now().plusDays(7));
            reserve.setUpdateTime(LocalDateTime.now());
            bookReserveMapper.updateById(reserve);
            log.info("图书归还，通知预约读者可取: 读者={} 图书={}", reserve.getUsername(), borrow.getBookTitle());
        }

        log.info("还书成功: 借阅ID={} 图书={}", borrowId, borrow.getBookTitle());
    }

    /**
     * 续借图书
     */
    @Transactional
    public Borrow renewBook(Long borrowId, Long userId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            throw new BusinessException(ResultCode.BORROW_NOT_FOUND);
        }
        if (!borrow.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (borrow.getStatus() != Constants.BORROW_STATUS_ACTIVE) {
            throw new BusinessException(ResultCode.BORROW_CANNOT_RENEW);
        }
        if (borrow.getRenewCount() >= Constants.MAX_RENEW_COUNT) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RENEWED);
        }
        // 检查是否有其他读者预约此书
        Long reserveCount = bookReserveMapper.selectCount(
                new LambdaQueryWrapper<BookReserve>()
                        .eq(BookReserve::getBookId, borrow.getBookId())
                        .eq(BookReserve::getStatus, Constants.BOOK_RESERVE_WAITING));
        if (reserveCount > 0) {
            throw new BusinessException(6008, "该图书已被其他读者预约，无法续借");
        }

        borrow.setDueDate(borrow.getDueDate().plusDays(Constants.RENEW_DAYS));
        borrow.setRenewCount(borrow.getRenewCount() + 1);
        borrow.setStatus(Constants.BORROW_STATUS_RENEWED);
        borrowMapper.updateById(borrow);

        log.info("续借成功: 借阅ID={} 续借至={}", borrowId, borrow.getDueDate());
        return borrow;
    }

    /**
     * 获取用户借阅记录
     */
    public IPage<Borrow> getMyBorrows(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Borrow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getUserId, userId);
        if (status != null) {
            wrapper.eq(Borrow::getStatus, status);
        }
        wrapper.orderByDesc(Borrow::getCreateTime);
        return borrowMapper.selectPage(page, wrapper);
    }

    /**
     * 管理员查询所有借阅
     */
    public IPage<Borrow> listAllBorrows(Integer pageNum, Integer pageSize, Integer status, Long userId) {
        Page<Borrow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Borrow::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Borrow::getUserId, userId);
        }
        wrapper.orderByDesc(Borrow::getCreateTime);
        return borrowMapper.selectPage(page, wrapper);
    }

    /**
     * 获取逾期记录
     */
    public List<Borrow> getOverdueBorrows() {
        return borrowMapper.selectList(
                new LambdaQueryWrapper<Borrow>()
                        .eq(Borrow::getStatus, Constants.BORROW_STATUS_OVERDUE)
                        .or().lt(Borrow::getDueDate, LocalDateTime.now())
                        .and(w -> w.eq(Borrow::getStatus, Constants.BORROW_STATUS_ACTIVE)
                                .or().eq(Borrow::getStatus, Constants.BORROW_STATUS_RENEWED)));
    }

    /**
     * 获取用户全部借阅记录（供推荐模块使用）
     */
    public List<Borrow> getUserBorrowHistory(Long userId) {
        return borrowMapper.selectList(
                new LambdaQueryWrapper<Borrow>()
                        .eq(Borrow::getUserId, userId)
                        .orderByDesc(Borrow::getBorrowTime));
    }

    // ==================== 图书预约 ====================

    /**
     * 预约图书
     */
    @Transactional
    public BookReserve reserveBook(Long userId, String username, String realName, Long bookId, String isbn, String bookTitle) {
        // 检查是否已预约此书
        Long count = bookReserveMapper.selectCount(
                new LambdaQueryWrapper<BookReserve>()
                        .eq(BookReserve::getUserId, userId)
                        .eq(BookReserve::getBookId, bookId)
                        .in(BookReserve::getStatus, Constants.BOOK_RESERVE_WAITING, Constants.BOOK_RESERVE_READY));
        if (count > 0) {
            throw new BusinessException(ResultCode.BOOK_RESERVE_ALREADY_EXISTS);
        }
        // 检查预约上限
        Long total = bookReserveMapper.selectCount(
                new LambdaQueryWrapper<BookReserve>()
                        .eq(BookReserve::getUserId, userId)
                        .in(BookReserve::getStatus, Constants.BOOK_RESERVE_WAITING, Constants.BOOK_RESERVE_READY));
        if (total >= 5) {
            throw new BusinessException(ResultCode.BOOK_RESERVE_LIMIT_EXCEEDED);
        }

        BookReserve reserve = new BookReserve();
        reserve.setUserId(userId);
        reserve.setUsername(username);
        reserve.setRealName(realName);
        reserve.setBookId(bookId);
        reserve.setIsbn(isbn);
        reserve.setBookTitle(bookTitle);
        reserve.setReserveTime(LocalDateTime.now());
        reserve.setStatus(Constants.BOOK_RESERVE_WAITING);
        bookReserveMapper.insert(reserve);

        log.info("图书预约成功: 用户={} 图书={}", username, bookTitle);
        return reserve;
    }

    /**
     * 取消预约
     */
    @Transactional
    public void cancelReserve(Long reserveId, Long userId) {
        BookReserve reserve = bookReserveMapper.selectById(reserveId);
        if (reserve == null) {
            throw new BusinessException(ResultCode.BOOK_RESERVE_NOT_FOUND);
        }
        if (!reserve.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (reserve.getStatus() != Constants.BOOK_RESERVE_WAITING) {
            throw new BusinessException(7004, "当前状态无法取消预约");
        }
        reserve.setStatus(Constants.BOOK_RESERVE_CANCELLED);
        bookReserveMapper.updateById(reserve);
        log.info("取消预约: 预约ID={}", reserveId);
    }

    /**
     * 获取用户预约列表
     */
    public IPage<BookReserve> getMyReserves(Long userId, Integer pageNum, Integer pageSize) {
        Page<BookReserve> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BookReserve> wrapper = new LambdaQueryWrapper<BookReserve>()
                .eq(BookReserve::getUserId, userId)
                .orderByDesc(BookReserve::getCreateTime);
        return bookReserveMapper.selectPage(page, wrapper);
    }

    /**
     * 获取全部预约（管理员）
     */
    public IPage<BookReserve> listAllReserves(Integer pageNum, Integer pageSize, Integer status) {
        Page<BookReserve> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BookReserve> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BookReserve::getStatus, status);
        }
        wrapper.orderByDesc(BookReserve::getCreateTime);
        return bookReserveMapper.selectPage(page, wrapper);
    }

    // ==================== DTO转换 ====================

    public BorrowDTO convertBorrowToDTO(Borrow borrow) {
        BorrowDTO dto = new BorrowDTO();
        BeanUtils.copyProperties(borrow, dto);
        return dto;
    }

    public BookReserveDTO convertReserveToDTO(BookReserve reserve) {
        BookReserveDTO dto = new BookReserveDTO();
        BeanUtils.copyProperties(reserve, dto);
        return dto;
    }
}
