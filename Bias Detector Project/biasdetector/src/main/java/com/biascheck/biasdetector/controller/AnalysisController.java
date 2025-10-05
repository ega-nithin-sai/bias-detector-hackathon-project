package com.biascheck.biasdetector.controller;

import com.biascheck.biasdetector.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analyze")
@CrossOrigin(origins = "*")  // allow extension access
public class AnalysisController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> analyzeText(@RequestBody Map<String, String> req) {
        String text = req.get("text");
        Map<String, Object> result = geminiService.analyzeText(text);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/url")
    public ResponseEntity<Map<String, Object>> analyzeUrl(@RequestBody Map<String, String> req) {
        String url = req.get("url");
        Map<String, Object> result = geminiService.analyzeUrl(url);
        return ResponseEntity.ok(result);
    }
}
