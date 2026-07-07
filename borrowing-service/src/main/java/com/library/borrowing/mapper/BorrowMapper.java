package com.library.borrowing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.borrowing.domain.Borrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {

    @Select("SELECT COUNT(*) FROM t_borrow WHERE user_id = #{userId} AND status IN (0, 3)")
    int countActiveBorrows(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_borrow WHERE user_id = #{userId} AND status = 2")
    int countOverdueBorrows(@Param("userId") Long userId);

    @Select("SELECT * FROM t_borrow WHERE user_id = #{userId} AND book_id = #{bookId} AND status IN (0, 3)")
    List<Borrow> findActiveBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
