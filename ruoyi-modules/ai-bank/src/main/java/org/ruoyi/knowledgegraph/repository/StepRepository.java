package org.ruoyi.knowledgegraph.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.knowledgegraph.domain.Step;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StepRepository extends BaseMapper<Step> {

    @Select("SELECT * FROM solution_steps WHERE problem_id = #{problemId} ORDER BY step_id")
    List<Step> findByProblemId(@Param("problemId") String problemId);

    @Select("SELECT * FROM solution_steps WHERE problem_id = #{problemId} AND step_id = #{stepId}")
    Step findByProblemIdAndStepId(@Param("problemId") String problemId, @Param("stepId") Integer stepId);
}