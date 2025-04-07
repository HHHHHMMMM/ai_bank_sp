package org.ruoyi.knowledgegraph.domain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("solution_steps")
public class Step {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private String problemId;

    @TableField("step_id")
    private Integer stepId;

    @TableField("operation")
    private String operation;

    @TableField("system_a")
    private String systemA;

    @TableField("table_name")
    private String tableName;

    @TableField("field")
    private String field;

    @TableField("condition_sql")
    private String conditionSql;

    @TableField("reply_content")
    private String replyContent;
}