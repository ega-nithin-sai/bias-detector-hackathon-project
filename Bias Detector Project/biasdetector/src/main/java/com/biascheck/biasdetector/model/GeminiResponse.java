// package com.biascheck.biasdetector.model;

// public class GeminiResponse {
//     private final String rawResponse;

//     public GeminiResponse(String rawResponse) {
//         this.rawResponse = rawResponse;
//     }

//     public String getRawResponse() {
//         return rawResponse;
//     }
// }

package com.biascheck.biasdetector.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GeminiResponse {
    private String result;
}
