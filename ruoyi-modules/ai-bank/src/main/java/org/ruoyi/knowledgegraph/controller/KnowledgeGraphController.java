package org.ruoyi.knowledgegraph.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.knowledgegraph.service.impl.KnowledgeGraphService;
import org.ruoyi.knowledgegraph.service.impl.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    @SaCheckPermission("knowledge:graph:create")
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
    @SaCheckPermission("knowledge:graph:clear")
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
    @SaCheckPermission("knowledge:graph:verify")
    @Log(title = "知识图谱", businessType = BusinessType.OTHER)
    @GetMapping("/verify")
    public R<String> verifyKnowledgeGraph() {
        boolean success = verificationService.verifyKnowledgeGraph();
        return success ?
                R.ok("知识图谱验证通过") :
                R.fail("知识图谱验证失败");
    }
}
