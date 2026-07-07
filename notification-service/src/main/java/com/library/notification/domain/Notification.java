package com.library.notification.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Data
@TableName("t_notification")
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知类型：0-到期提醒，1-逾期警告，2-预约可取，3-荐购进度，4-库存预警 */
    private Integer type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：0-未读，1-已读 */
    private Integer status;

    /** 关联ID */
    private Long referenceId;

    /** 关联类型 */
    private String referenceType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
