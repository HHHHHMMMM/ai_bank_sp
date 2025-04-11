package org.ruoyi.knowledgegraph.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.knowledgegraph.domain.Problem;
import org.ruoyi.knowledgegraph.domain.Step;
import org.ruoyi.knowledgegraph.domain.StepRelation;
import org.ruoyi.knowledgegraph.service.impl.KnowledgeGraphService;
import org.ruoyi.knowledgegraph.service.impl.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 知识图谱管理控制器
 */
@RestController
@RequestMapping("/knowledge/graph")
public class KnowledgeGraphController extends BaseController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final VerificationService verificationService;

    @Autowired
    public KnowledgeGraphController(
            KnowledgeGraphService knowledgeGraphService,
            VerificationService verificationService) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.verificationService = verificationService;
    }

    /**
     * 创建/重建知识图谱
     */
    @Log(title = "知识图谱", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public R<String> createKnowledgeGraph() {
        boolean success = knowledgeGraphService.createKnowledgeGraph();
        return success ?
                R.ok("知识图谱创建成功") :
                R.fail("知识图谱创建失败");
    }

    /**
     * 清空知识图谱
     */
    @Log(title = "知识图谱", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clear")
    public R<String> clearKnowledgeGraph() {
        boolean success = knowledgeGraphService.clearKnowledgeGraph();
        return success ?
                R.ok("知识图谱已清空") :
                R.fail("知识图谱清空失败");
    }

    /**
     * 验证知识图谱结构
     */
    @Log(title = "知识图谱", businessType = BusinessType.OTHER)
    @GetMapping("/verify")
    public R<String> verifyKnowledgeGraph() {
        boolean success = verificationService.verifyKnowledgeGraph();
        return success ?
                R.ok("知识图谱验证通过") :
                R.fail("知识图谱验证失败");
    }

    /**
     * 创建问题节点
     */
    @Log(title = "知识图谱", businessType = BusinessType.INSERT)
    @PostMapping("/node")
    public R<String> createNode(@RequestBody Problem problem) {
        boolean success = knowledgeGraphService.createProblemGraph(problem);
        return success ?
                R.ok("节点创建成功") :
                R.fail("节点创建失败");
    }

    /**
     * 创建步骤节点
     */
    @Log(title = "知识图谱", businessType = BusinessType.INSERT)
    @PostMapping("/stepNode")
    public R<String> createStepNode(@RequestBody Step step) {
        boolean success = knowledgeGraphService.createStepNode(step);
        return success ?
                R.ok("step节点创建成功") :
                R.fail("step节点创建失败");
    }

    /**
     * 创建关系
     */
    @Log(title = "知识图谱", businessType = BusinessType.INSERT)
    @PostMapping("/relation")
    public R<String> createRelation(@RequestBody StepRelation stepRelation) {
        boolean success = knowledgeGraphService.createStepRelation(stepRelation);
        return success ?
                R.ok("关系创建成功") :
                R.fail("关系创建失败");
    }

    /**
     * 获取图谱数据
     */
    @GetMapping("/data")
    public R<Map<String, Object>> getGraphData() {
        Map<String, Object> graphData = knowledgeGraphService.getGraphData();
        return R.ok(graphData);
    }

    /**
     * 删除节点
     */
    @Log(title = "知识图谱", businessType = BusinessType.DELETE)
    @DeleteMapping("/node/{nodeId}")
    public R<String> deleteNode(@PathVariable String nodeId) {
        boolean success = knowledgeGraphService.deleteNode(nodeId);
        return success ?
                R.ok("节点删除成功") :
                R.fail("节点删除失败");
    }

    /**
     * 删除关系
     */
    @Log(title = "知识图谱", businessType = BusinessType.DELETE)
    @DeleteMapping("/relation/{relationId}")
    public R<String> deleteRelation(@PathVariable String relationId) {
        boolean success = knowledgeGraphService.deleteRelation(relationId);
        return success ?
                R.ok("关系删除成功") :
                R.fail("关系删除失败");
    }



}
