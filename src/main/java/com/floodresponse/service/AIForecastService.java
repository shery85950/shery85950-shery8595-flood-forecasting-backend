package com.floodresponse.service;

import com.floodresponse.dto.WeatherForecastRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class AIForecastService {

    private static final String HF_API_URL = "https://api.huggingface.co/v1/chat/completions";
    private static final String HF_API_KEY = "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";
    private static final String MODEL = "SentientAGI/Dobby-Unhinged-Llama-3.3-70B:fireworks-ai";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String analyzeForecast(WeatherForecastRequest request) {
        try {
            String prompt = createAnalysisPrompt(request);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1000);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are an expert meteorologist and flood risk analyst for Pakistan. Analyze weather forecasts and provide accurate flood risk assessments with actionable recommendations. Always respond with valid JSON only, no additional text.");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(HF_API_KEY);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(HF_API_URL, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            
            throw new RuntimeException("Failed to get valid response from AI API");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating AI analysis: " + e.getMessage());
        }
    }

    private String createAnalysisPrompt(WeatherForecastRequest request) {
        StringBuilder forecastText = new StringBuilder();
        int idx = 1;
        for (WeatherForecastRequest.ForecastDay day : request.getForecast()) {
            forecastText.append(String.format("Day %d (%s): %s, Rainfall: %.1fmm, Temp: %.1f°C - %.1f°C, Rain Chance: %.1f%%, Humidity: %.1f%%\n",
                    idx++, day.getDate(), day.getCondition(), day.getTotalRainfall(), day.getMinTemp(), day.getMaxTemp(), day.getRainChance(), day.getAvgHumidity()));
        }

        return String.format("""
                Analyze the following 7-day weather forecast for %s, %s, Pakistan and provide a flood risk assessment:

                %s

                Provide your analysis in the following JSON format (respond with ONLY valid JSON, no markdown or additional text):

                {
                  "warningLevel": "Low|Moderate|High|Critical",
                  "riskScore": 0-100,
                  "confidence": 0-100,
                  "summary": "Brief 1-2 sentence summary of the flood risk",
                  "keyFactors": ["factor1", "factor2", "factor3"],
                  "recommendations": ["recommendation1", "recommendation2", "recommendation3"],
                  "dailyRisks": [
                    {"date": "YYYY-MM-DD", "risk": "Low|Moderate|High|Critical", "reason": "brief reason"},
                    ...
                  ],
                  "peakRiskDays": ["YYYY-MM-DD", "YYYY-MM-DD"]
                }

                Consider:
                - Total rainfall accumulation over the week
                - Consecutive days of heavy rain
                - Regional flood history in Pakistan
                - Monsoon patterns
                - River basin proximity
                - Soil saturation potential
                """, request.getLocation().getName(), request.getLocation().getRegion(), forecastText.toString());
    }
}
