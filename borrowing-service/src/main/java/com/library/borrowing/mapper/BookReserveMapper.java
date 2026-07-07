package com.library.borrowing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.borrowing.domain.BookReserve;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookReserveMapper extends BaseMapper<BookReserve> {
}
