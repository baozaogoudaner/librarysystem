package com.library.borrowing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.borrowing.domain.BookReserve;
import com.library.borrowing.domain.Borrow;
import com.library.borrowing.service.BorrowService;
import com.library.common.annotation.OperationLog;
import com.library.common.domain.BookReserveDTO;
import com.library.common.domain.BorrowDTO;
import com.library.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "05-借阅管理")
@RestController
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @ApiOperation("借书")
    @PostMapping("/borrow")
    public Result<BorrowDTO> borrowBook(@RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("X-Username") String username,
                                        @RequestHeader(value = "X-Real-Name", required = false) String realName,
                                        @RequestBody @Valid BorrowRequest request) {
        Borrow borrow = borrowService.borrowBook(userId, username,
                realName != null ? realName : username, request.getBookId(), request.getIsbn(), request.getBookTitle());
        return Result.success("借书成功", borrowService.convertBorrowToDTO(borrow));
    }

    @ApiOperation("还书")
    @PutMapping("/return/{borrowId}")
    public Result<?> returnBook(@PathVariable Long borrowId, @RequestHeader("X-User-Id") Long userId) {
        borrowService.returnBook(borrowId, userId);
        return Result.success("还书成功", null);
    }

    @ApiOperation("续借")
    @PutMapping("/renew/{borrowId}")
    public Result<BorrowDTO> renewBook(@PathVariable Long borrowId, @RequestHeader("X-User-Id") Long userId) {
        return Result.success("续借成功", borrowService.convertBorrowToDTO(borrowService.renewBook(borrowId, userId)));
    }

    @ApiOperation("获取我的借阅记录")
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyBorrows(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<Borrow> page = borrowService.getMyBorrows(userId, pageNum, pageSize, status);
        List<BorrowDTO> dtos = page.getRecords().stream().map(borrowService::convertBorrowToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("管理员查看全部借阅记录")
    @GetMapping("/list")
    public Result<Map<String, Object>> listAllBorrows(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {
        IPage<Borrow> page = borrowService.listAllBorrows(pageNum, pageSize, status, userId);
        List<BorrowDTO> dtos = page.getRecords().stream().map(borrowService::convertBorrowToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("获取逾期记录")
    @GetMapping("/overdue")
    public Result<List<BorrowDTO>> getOverdueBorrows() {
        return Result.success(borrowService.getOverdueBorrows().stream()
                .map(borrowService::convertBorrowToDTO).collect(Collectors.toList()));
    }

    @ApiOperation("预约图书")
    @PostMapping("/reserve")
    public Result<BookReserveDTO> reserveBook(@RequestHeader("X-User-Id") Long userId,
                                               @RequestHeader("X-Username") String username,
                                               @RequestHeader(value = "X-Real-Name", required = false) String realName,
                                               @RequestBody @Valid ReserveRequest request) {
        BookReserve reserve = borrowService.reserveBook(userId, username,
                realName != null ? realName : username, request.getBookId(), request.getIsbn(), request.getBookTitle());
        return Result.success("预约成功", borrowService.convertReserveToDTO(reserve));
    }

    @ApiOperation("取消图书预约")
    @PutMapping("/reserve/cancel/{reserveId}")
    public Result<?> cancelReserve(@PathVariable Long reserveId, @RequestHeader("X-User-Id") Long userId) {
        borrowService.cancelReserve(reserveId, userId);
        return Result.success("取消成功", null);
    }

    @ApiOperation("获取我的图书预约")
    @GetMapping("/reserve/my")
    public Result<Map<String, Object>> getMyReserves(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<BookReserve> page = borrowService.getMyReserves(userId, pageNum, pageSize);
        List<BookReserveDTO> dtos = page.getRecords().stream().map(borrowService::convertReserveToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("管理员查看全部图书预约")
    @GetMapping("/reserve/list")
    public Result<Map<String, Object>> listAllReserves(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        IPage<BookReserve> page = borrowService.listAllReserves(pageNum, pageSize, status);
        List<BookReserveDTO> dtos = page.getRecords().stream().map(borrowService::convertReserveToDTO).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("records", dtos);
        return Result.success(result);
    }

    @ApiOperation("获取用户借阅历史（Feign内部调用）")
    @GetMapping("/internal/history/{userId}")
    public Result<List<BorrowDTO>> getBorrowHistory(@PathVariable Long userId) {
        return Result.success(borrowService.getUserBorrowHistory(userId).stream()
                .map(borrowService::convertBorrowToDTO).collect(Collectors.toList()));
    }

    // ---- 内部请求体 ----

    @lombok.Data
    public static class BorrowRequest {
        @NotNull private Long bookId;
        @NotNull private String isbn;
        @NotNull private String bookTitle;
    }

    @lombok.Data
    public static class ReserveRequest {
        @NotNull private Long bookId;
        @NotNull private String isbn;
        @NotNull private String bookTitle;
    }
}
