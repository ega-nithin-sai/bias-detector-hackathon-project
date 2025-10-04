// package com.biascheck.biasdetector.service;

// import com.biascheck.biasdetector.model.ArticleRequest;
// import com.biascheck.biasdetector.model.GeminiResponse;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import org.json.JSONObject;

// @Service
// public class GeminiService {

//     @Value("${gemini.api.key}")
//     private String geminiApiKey;

//     @Value("${gemini.model}")
//     private String geminiModel;

//     private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

//     public GeminiResponse analyzeArticle(ArticleRequest article) {
//         try {
//             RestTemplate restTemplate = new RestTemplate();
//             String url = GEMINI_API_URL + geminiModel + ":generateContent?key=" + geminiApiKey;

//             // --- Build the prompt dynamically ---
//             String prompt = buildPrompt(article);

//             JSONObject content = new JSONObject()
//                     .put("contents", new org.json.JSONArray()
//                             .put(new JSONObject()
//                                     .put("parts", new org.json.JSONArray()
//                                             .put(new JSONObject().put("text", prompt)))));

//             HttpHeaders headers = new HttpHeaders();
//             headers.setContentType(MediaType.APPLICATION_JSON);

//             HttpEntity<String> request = new HttpEntity<>(content.toString(), headers);
//             ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

//             // Extract the model text output
//             JSONObject jsonResponse = new JSONObject(response.getBody());
//             String output = jsonResponse
//                     .getJSONArray("candidates")
//                     .getJSONObject(0)
//                     .getJSONObject("content")
//                     .getJSONArray("parts")
//                     .getJSONObject(0)
//                     .getString("text");

//             return new GeminiResponse(output);
//         } catch (Exception e) {
//             e.printStackTrace();
//             return new GeminiResponse("Error: " + e.getMessage());
//         }
//     }

//     private String buildPrompt(ArticleRequest article) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("You are an expert research reviewer.\n");
//         sb.append("Analyze the following article for bias or manipulative framing.\n");
//         sb.append("Return concise JSON with keys: bias_score, flags, and evidence.\n\n");
//         sb.append("Title: ").append(article.getTitle()).append("\n");

//         if (article.getUrl() != null && !article.getUrl().isEmpty()) {
//             sb.append("URL: ").append(article.getUrl()).append("\n\n");
//             sb.append("If the URL is provided, analyze that article content.\n");
//         } else if (article.getText() != null && !article.getText().isEmpty()) {
//             sb.append("Text:\n").append(article.getText()).append("\n\n");
//         } else {
//             sb.append("No text or URL provided — return error JSON.\n");
//         }

//         return sb.toString();
//     }
// }
package com.biascheck.biasdetector.service;

import com.biascheck.biasdetector.model.GeminiResponse;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String GEMINI_API_KEY;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiResponse analyzeContent(String text) {
        try {
            OkHttpClient client = new OkHttpClient();

            String prompt = """
                    Analyze the following article text for bias and misinformation.
                    Respond only in this exact format:
                    Bias direction: [bias direction and confidence percentage]
                    Fake news percentage: [percentage]
                    Summary: [brief explanation of why you think this level of bias/fakeness exists]

                    Article:
                    """ + text;

            JSONObject contentPart = new JSONObject().put("text", prompt);
            JSONArray contents = new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(contentPart)));

            JSONObject requestBody = new JSONObject().put("contents", contents);

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                    .build();

            // Response response = client.newCall(request).execute();
            // if (!response.isSuccessful()) {
            //     return new GeminiResponse("Error: " + response.message());
            // }

            // String responseBody = response.body().string();
            // JSONObject json = new JSONObject(responseBody);
            // -----------------------------------
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();

            System.out.println("=== Gemini Raw Response ===");
            System.out.println(responseBody);
            System.out.println("============================");

            if (!response.isSuccessful()) {
                return new GeminiResponse("Error: " + responseBody);
            }

            JSONObject json = new JSONObject(responseBody);

            // ----------------------------------

            String output = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // Clean up output
            String cleaned = output
                    .replaceAll("(?i)```(?:json|text)?", "")
                    .replaceAll("```", "")
                    .trim();

            return new GeminiResponse(cleaned);

        } catch (Exception e) {
            return new GeminiResponse("Error: " + e.getMessage());
        }
    }
}
