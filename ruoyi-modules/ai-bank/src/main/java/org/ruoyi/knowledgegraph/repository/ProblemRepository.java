package org.ruoyi.knowledgegraph.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.knowledgegraph.domain.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ProblemRepository extends BaseMapper<Problem> {

    @Select("SELECT * FROM problem_definitions WHERE is_active = 1")
    List<Problem> findAllActiveProblems();

    @Select("SELECT * FROM problem_definitions WHERE problem_id = #{problemId}")
    Problem findByProblemId(@Param("problemId") String problemId);
}