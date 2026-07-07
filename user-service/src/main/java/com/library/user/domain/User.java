package com.library.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("t_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    /** 角色（0-普通用户，1-管理员） */
    private Integer role;

    /** 状态（0-正常，1-冻结） */
    private Integer status;

    /** 违规次数 */
    private Integer violationCount;

    /** 信用积分（0-100，默认100）★ V2.0 */
    private Integer creditScore;

    /** 头像URL（MinIO）★ V2.0 */
    private String avatarUrl;

    /** 读者类型（0-学生，1-教师，2-社会读者） */
    private Integer readerType;

    /** 最大借阅数量（根据读者类型设置） */
    private Integer maxBorrowCount;

    /** 累计借阅数 */
    private Integer totalBorrows;

    /** 冻结截止时间 */
    private LocalDateTime freezeUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
