package org.ruoyi.knowledgegraph.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("problem_definitions")
public class Problem {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private String problemId;

    @TableField("problem_type")
    private String problemType;

    @TableField("description")
    private String description;

    @TableField("has_special_rules")
    private Boolean hasSpecialRules;

    @TableField("special_field")
    private String specialField;

    @TableField("special_condition")
    private String specialCondition;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}