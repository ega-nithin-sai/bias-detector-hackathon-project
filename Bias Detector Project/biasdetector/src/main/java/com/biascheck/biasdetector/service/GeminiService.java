// package com.biascheck.biasdetector.service;

// import com.biascheck.biasdetector.model.GeminiResponse;
// import okhttp3.*;
// import org.json.JSONArray;
// import org.json.JSONObject;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// @Service
// public class GeminiService {

//     @Value("${gemini.api.key}")
//     private String GEMINI_API_KEY;

//     private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

//     public GeminiResponse analyzeContent(String text) {
//         try {
//             OkHttpClient client = new OkHttpClient();

//             String prompt = """
//                     Analyze the following article text for bias and misinformation.
//                     Respond only in this exact format:
//                     Bias direction: [bias direction and confidence percentage]
//                     Fake news percentage: [percentage]
//                     Summary: [brief explanation of why you think this level of bias/fakeness exists]

//                     Article:
//                     """ + text;

//             JSONObject contentPart = new JSONObject().put("text", prompt);
//             JSONArray contents = new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(contentPart)));

//             JSONObject requestBody = new JSONObject().put("contents", contents);

//             Request request = new Request.Builder()
//                     .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
//                     .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
//                     .build();

//             Response response = client.newCall(request).execute();
//             if (!response.isSuccessful()) {
//                 return new GeminiResponse("Error: " + response.message());
//             }

//             String responseBody = response.body().string();
//             JSONObject json = new JSONObject(responseBody);
//             // -----------------------------------
//             // Response response = client.newCall(request).execute();
//             // String responseBody = response.body().string();

//             // System.out.println("=== Gemini Raw Response ===");
//             // System.out.println(responseBody);
//             // System.out.println("============================");

//             // if (!response.isSuccessful()) {
//             //     return new GeminiResponse("Error: " + responseBody);
//             // }

//             // JSONObject json = new JSONObject(responseBody);

//             // ----------------------------------

//             String output = json.getJSONArray("candidates")
//                     .getJSONObject(0)
//                     .getJSONObject("content")
//                     .getJSONArray("parts")
//                     .getJSONObject(0)
//                     .getString("text");

//             // Clean up output
//             String cleaned = output
//                     .replaceAll("(?i)```(?:json|text)?", "")
//                     .replaceAll("```", "")
//                     .trim();

//             return new GeminiResponse(cleaned);

//         } catch (Exception e) {
//             return new GeminiResponse("Error: " + e.getMessage());
//         }
//     }

    
// }


package com.biascheck.biasdetector.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.biascheck.biasdetector.model.GeminiResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String GEMINI_API_KEY;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final OkHttpClient client = new OkHttpClient();

    /**
     * Analyze raw text for bias and fake news probability
     */
    public Map<String, Object> analyzeText(String text) {
        Map<String, Object> response = new HashMap<>();
        try {
            // prompt for Gemini
            String prompt = "Analyze the following article or text for bias and fake news likelihood. "
                    + "Give your answer ONLY in JSON format as: "
                    + "{biasDirection: 'left/right/neutral (percentage%)', fakeNewsPercentage: 'number%', explanation: 'reasoning for these conclusions'}.\n\n"
                    + text;

            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray()
                    .put(new JSONObject().put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", prompt))));
            body.put("contents", contents);

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.get("application/json")))
                    .build();

            Response res = client.newCall(request).execute();

            if (!res.isSuccessful() || res.body() == null) {
                response.put("result", "Error: Gemini API call failed (" + res.code() + ")");
                return response;
            }

            String responseBody = res.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Parse Gemini response
            String modelOutput = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // Try parsing the model's text as JSON
            JSONObject parsed;
            try {
                parsed = new JSONObject(modelOutput);
            } catch (Exception e) {
                // fallback in case model didn't return pure JSON
                parsed = new JSONObject();
                parsed.put("biasDirection", extractField(modelOutput, "bias"));
                parsed.put("fakeNewsPercentage", extractField(modelOutput, "fake"));
                parsed.put("explanation", modelOutput);
            }

            response.put("biasDirection", parsed.optString("biasDirection", "Unknown"));
            response.put("fakeNewsPercentage", parsed.optString("fakeNewsPercentage", "Unknown"));
            response.put("explanation", parsed.optString("explanation", "No explanation provided."));

            // Extract numeric score from biasDirection if possible
            double score = 0;
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)%").matcher(parsed.optString("biasDirection", ""));
                if (m.find()) {
                    score = Double.parseDouble(m.group(1));
                }
            } catch (Exception e) {
                score = 0;
            }

            // Add score key to the response
            response.put("score", score);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("result", "Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Analyze an article from a given URL
     */
    public Map<String, Object> analyzeUrl(String url) {
        try {
            String text = Jsoup.connect(url).get().text();
            return analyzeText(text);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("result", "Error fetching URL: " + e.getMessage());
            return error;
        }
    }

    /**
     * Helper to extract simple values when Gemini response is not clean JSON
     */
    private String extractField(String text, String key) {
        String lower = text.toLowerCase();
        int idx = lower.indexOf(key);
        if (idx == -1) return "Unknown";
        int endIdx = Math.min(text.length(), idx + 100);
        return text.substring(idx, endIdx).replaceAll("[^a-zA-Z0-9%() ]", "").trim();
    }

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

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                return new GeminiResponse("Error: " + response.message());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            // -----------------------------------
            // Response response = client.newCall(request).execute();
            // String responseBody = response.body().string();

            // System.out.println("=== Gemini Raw Response ===");
            // System.out.println(responseBody);
            // System.out.println("============================");

            // if (!response.isSuccessful()) {
            //     return new GeminiResponse("Error: " + responseBody);
            // }

            // JSONObject json = new JSONObject(responseBody);

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
