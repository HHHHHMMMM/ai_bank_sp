package org.ruoyi.knowledgegraph.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("step_relations")
public class StepRelation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private String problemId;

    @TableField("from_step_id")
    private Integer fromStepId;

    @TableField("to_step_id")
    private Integer toStepId;

    @TableField("relation_type")
    private String relationType;

    @TableField("condition_expression")
    private String conditionExpression;
}