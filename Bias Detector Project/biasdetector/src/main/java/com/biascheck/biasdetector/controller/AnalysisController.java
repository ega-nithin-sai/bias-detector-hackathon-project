// package com.biascheck.biasdetector.controller;

// import com.biascheck.biasdetector.service.GeminiService;
// import com.biascheck.biasdetector.model.ArticleRequest;
// import com.biascheck.biasdetector.model. GeminiResponse;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.*;

// @CrossOrigin(origins = "*")
// @RestController
// @RequestMapping("/api")
// public class AnalysisController {
//     @Autowired
//     private GeminiService geminiService;

//     @PostMapping("/analyze")
//     public GeminiResponse analyzeArticle(@RequestBody ArticleRequest article) {
//         return geminiService.analyzeArticle(article);
//     }
// }

package com.biascheck.biasdetector.controller;

import com.biascheck.biasdetector.model.ArticleRequest;
import com.biascheck.biasdetector.model.GeminiResponse;
import com.biascheck.biasdetector.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/analyze")
    public GeminiResponse analyzeArticle(@RequestBody ArticleRequest request) {
        try {
            String content;

            if (request.getUrl() != null && !request.getUrl().isEmpty()) {
                Document doc = Jsoup.connect(request.getUrl()).get();
                content = doc.body().text();
            } else if (request.getText() != null && !request.getText().isEmpty()) {
                content = request.getText();
            } else {
                return new GeminiResponse("Error: Please provide either a URL or text.");
            }

            return geminiService.analyzeContent(content);

        } catch (Exception e) {
            return new GeminiResponse("Error: " + e.getMessage());
        }
    }
}
