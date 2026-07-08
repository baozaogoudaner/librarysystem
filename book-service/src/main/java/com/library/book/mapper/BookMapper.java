package com.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.book.domain.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Select("SELECT * FROM t_book WHERE MATCH(title, author, description) AGAINST(#{keyword} IN BOOLEAN MODE)")
    List<Book> searchByFulltext(@Param("keyword") String keyword);

    @Select("SELECT * FROM t_book WHERE title LIKE CONCAT('%', #{keyword}, '%') OR author LIKE CONCAT('%', #{keyword}, '%') OR REPLACE(isbn, '-', '') LIKE CONCAT('%', REPLACE(#{keyword}, '-', ''), '%')")
    List<Book> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM t_book WHERE available_stock <= 3 AND status = 0 ORDER BY available_stock ASC")
    List<Book> findLowStockBooks();

    @Select("SELECT * FROM t_book WHERE status = 0 ORDER BY borrow_count DESC LIMIT #{limit}")
    List<Book> findHotBooks(@Param("limit") int limit);

    @Select("SELECT category, COUNT(*) as cnt FROM t_book WHERE status != 2 GROUP BY category ORDER BY cnt DESC")
    List<Map<String, Object>> getCategoryStats();
}
