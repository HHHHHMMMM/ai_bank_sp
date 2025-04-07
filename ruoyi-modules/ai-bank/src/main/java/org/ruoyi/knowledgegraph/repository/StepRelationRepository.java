package org.ruoyi.knowledgegraph.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.knowledgegraph.domain.StepRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StepRelationRepository extends BaseMapper<StepRelation> {

    @Select("SELECT * FROM step_relations WHERE problem_id = #{problemId}")
    List<StepRelation> findByProblemId(@Param("problemId") String problemId);

    @Select("SELECT * FROM step_relations WHERE problem_id = #{problemId} AND from_step_id = #{fromStepId}")
    List<StepRelation> findByProblemIdAndFromStepId(@Param("problemId") String problemId, @Param("fromStepId") Integer fromStepId);
}