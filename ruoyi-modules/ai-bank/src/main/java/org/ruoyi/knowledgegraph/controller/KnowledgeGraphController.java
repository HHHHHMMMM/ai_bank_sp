package org.ruoyi.knowledgegraph.controller;


import org.ruoyi.knowledgegraph.service.impl.KnowledgeGraphService;
import org.ruoyi.knowledgegraph.service.impl.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for knowledge graph operations
 */
@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphController {

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
     * Create or recreate the entire knowledge graph
     * @return Response with success status
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createKnowledgeGraph() {
        boolean success = knowledgeGraphService.createKnowledgeGraph();

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Knowledge graph created successfully" : "Failed to create knowledge graph");

        return ResponseEntity.ok(response);
    }

    /**
     * Clear the entire knowledge graph
     * @return Response with success status
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearKnowledgeGraph() {
        boolean success = knowledgeGraphService.clearKnowledgeGraph();

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Knowledge graph cleared successfully" : "Failed to clear knowledge graph");

        return ResponseEntity.ok(response);
    }

    /**
     * Verify the knowledge graph structure
     * @return Response with verification results
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyKnowledgeGraph() {
        boolean success = verificationService.verifyKnowledgeGraph();

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Knowledge graph verification successful" : "Knowledge graph verification failed");

        return ResponseEntity.ok(response);
    }
}

