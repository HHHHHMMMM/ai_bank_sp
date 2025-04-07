package org.ruoyi.knowledgegraph.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.knowledgegraph.domain.Problem;
import org.ruoyi.knowledgegraph.domain.Step;
import org.ruoyi.knowledgegraph.domain.StepRelation;
import org.ruoyi.knowledgegraph.repository.ProblemRepository;
import org.ruoyi.knowledgegraph.repository.StepRelationRepository;
import org.ruoyi.knowledgegraph.repository.StepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识图谱-问题管理
 */
@RestController
@RequestMapping("/api/problem")
public class ProblemController extends BaseController {

    private final ProblemRepository problemRepository;
    private final StepRepository stepRepository;
    private final StepRelationRepository stepRelationRepository;

    @Autowired
    public ProblemController(
            ProblemRepository problemRepository,
            StepRepository stepRepository,
            StepRelationRepository stepRelationRepository) {
        this.problemRepository = problemRepository;
        this.stepRepository = stepRepository;
        this.stepRelationRepository = stepRelationRepository;
    }

    /**
     * 分页查询问题列表
     */
//    @SaCheckPermission("knowledge:problem:list")
    @GetMapping("/page")
    public TableDataInfo<Problem> page(Problem problem, PageQuery pageQuery) {
        // 使用 MyBatis-Plus 的 IPage 作为分页结果
        IPage<Problem> page = problemRepository.selectPage(
                pageQuery.build(), // 将 PageQuery 转换为 MyBatis-Plus 的 IPage
                new QueryWrapper<Problem>().lambda()
                        .eq(Problem::getIsActive, true)
                        .like(StringUtils.isNotEmpty(problem.getProblemId()), Problem::getProblemId, problem.getProblemId())
                        .like(StringUtils.isNotEmpty(problem.getProblemType()), Problem::getProblemType, problem.getProblemType())
        );
        // 将 IPage 转换为 Ruoyi-Plus 的 TableDataInfo
        return TableDataInfo.build(page);
    }


    /**
     * 获取问题详情
     */
//    @SaCheckPermission("knowledge:problem:query")
    @GetMapping("/{problemId}")
    public R<Problem> getInfo(@PathVariable String problemId) {
        return R.ok(problemRepository.findByProblemId(problemId));
    }

    /**
     * 获取问题步骤列表（不分页）
     */
//    @SaCheckPermission("knowledge:problem:query")
    @GetMapping("/{problemId}/steps")
    public R<List<Step>> getSteps(@PathVariable String problemId) {
        return R.ok(stepRepository.findByProblemId(problemId));
    }

    /**
     * 获取问题关系列表（不分页）
     */
//    @SaCheckPermission("knowledge:problem:query")
    @GetMapping("/{problemId}/relations")
    public R<List<StepRelation>> getRelations(@PathVariable String problemId) {
        return R.ok(stepRelationRepository.findByProblemId(problemId));
    }

    /**
     * 新增问题
     */
//    @SaCheckPermission("knowledge:problem:add")
    @Log(title = "问题管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody Problem problem) {
        return toAjax(problemRepository.insert(problem));
    }

    /**
     * 修改问题
     */
//    @SaCheckPermission("knowledge:problem:edit")
    @Log(title = "问题管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody Problem problem) {
        Problem existing = problemRepository.findByProblemId(problem.getProblemId());
        if (existing == null) {
            return R.fail("问题不存在");
        }
        problem.setId(existing.getId());
        return toAjax(problemRepository.updateById(problem));
    }

    /**
     * 删除问题（级联删除关联步骤和关系）
     */
//    @SaCheckPermission("knowledge:problem:remove")
    @Log(title = "问题管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{problemId}")
    public R<Void> remove(@PathVariable String problemId) {
        // 删除关联关系
        stepRelationRepository.delete(new QueryWrapper<StepRelation>()
                .lambda().eq(StepRelation::getProblemId, problemId));

        // 删除步骤
        stepRepository.delete(new QueryWrapper<Step>()
                .lambda().eq(Step::getProblemId, problemId));

        // 删除问题
        return toAjax(problemRepository.delete(new QueryWrapper<Problem>()
                .lambda().eq(Problem::getProblemId, problemId)));
    }
}
