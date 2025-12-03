package com.floodresponse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

@Service
public class AIForecastService {

    @Value("${huggingface.api.key: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}")
    private String hfApiKey;

    @Value("${huggingface.api.url:https://api-inference.huggingface.co/models/SentientAGI/Dobby-Unhinged-Llama-3.3-70B}")
    private String hfApiUrl;

    @Value("${weather.api.key:5523bf8add464255b93210055252911}")
    private String weatherApiKey;

    @Value("${weather.api.url:https://api.weatherapi.com/v1}")
    private String weatherApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIForecastService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyze weekly forecast for a given location
     */
    public Map<String, Object> analyzeWeeklyForecast(String location) {
        try {
            // Step 1: Fetch 7-day weather data
            Map<String, Object> weatherData = fetchWeeklyWeather(location);

            // Step 2: Generate AI analysis
            Map<String, Object> aiAnalysis = generateWarningWithAI(weatherData);

            // Step 3: Return structured response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("location", weatherData.get("location"));
            response.put("forecast", weatherData.get("forecast"));
            response.put("analysis", aiAnalysis);
            response.put("timestamp", new Date().toString());

            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", new Date().toString());
            return errorResponse;
        }
    }

    /**
     * Fetch 7-day weather forecast from WeatherAPI
     */
    private Map<String, Object> fetchWeeklyWeather(String location) throws Exception {
        String url = String.format("%s/forecast.json?key=%s&q=%s&days=7&aqi=no&alerts=yes",
                weatherApiUrl, weatherApiKey, location);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Weather API error: " + response.getStatusCode());
        }

        JsonNode data = objectMapper.readTree(response.getBody());

        // Extract location info
        Map<String, Object> locationInfo = new HashMap<>();
        JsonNode locationNode = data.get("location");
        locationInfo.put("name", locationNode.get("name").asText());
        locationInfo.put("region", locationNode.get("region").asText());
        locationInfo.put("country", locationNode.get("country").asText());
        locationInfo.put("lat", locationNode.get("lat").asDouble());
        locationInfo.put("lon", locationNode.get("lon").asDouble());

        // Extract forecast data
        List<Map<String, Object>> forecastList = new ArrayList<>();
        JsonNode forecastDays = data.get("forecast").get("forecastday");
        for (JsonNode day : forecastDays) {
            Map<String, Object> dayData = new HashMap<>();
            JsonNode dayInfo = day.get("day");
            dayData.put("date", day.get("date").asText());
            dayData.put("maxTemp", dayInfo.get("maxtemp_c").asDouble());
            dayData.put("minTemp", dayInfo.get("mintemp_c").asDouble());
            dayData.put("avgTemp", dayInfo.get("avgtemp_c").asDouble());
            dayData.put("totalRainfall", dayInfo.get("totalprecip_mm").asDouble());
            dayData.put("maxWind", dayInfo.get("maxwind_kph").asDouble());
            dayData.put("avgHumidity", dayInfo.get("avghumidity").asInt());
            dayData.put("rainChance", dayInfo.get("daily_chance_of_rain").asInt());
            dayData.put("condition", dayInfo.get("condition").get("text").asText());
            dayData.put("uvIndex", dayInfo.get("uv").asDouble());
            forecastList.add(dayData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("location", locationInfo);
        result.put("forecast", forecastList);
        return result;
    }

    /**
     * Generate flood warning using Hugging Face LLM
     */
    private Map<String, Object> generateWarningWithAI(Map<String, Object> weatherData) throws Exception {
        String prompt = createAnalysisPrompt(weatherData);

        // Prepare request body for Hugging Face
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", "You are an expert meteorologist and flood risk analyst for Pakistan. " +
                "Analyze weather forecasts and provide accurate flood risk assessments with actionable recommendations. " +
                "Always respond with valid JSON only, no additional text.\n\n" + prompt);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_new_tokens", 1000);
        parameters.put("temperature", 0.3);
        parameters.put("return_full_text", false);
        requestBody.put("parameters", parameters);

        // Call Hugging Face API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + hfApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(hfApiUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Hugging Face API error: " + response.getStatusCode());
        }

        // Parse response
        JsonNode result = objectMapper.readTree(response.getBody());
        String aiResponse;

        if (result.isArray() && result.size() > 0) {
            aiResponse = result.get(0).get("generated_text").asText();
        } else {
            aiResponse = result.get("generated_text").asText();
        }

        return parseAIResponse(aiResponse);
    }

    /**
     * Create analysis prompt for the AI
     */
    private String createAnalysisPrompt(Map<String, Object> weatherData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) weatherData.get("location");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forecast = (List<Map<String, Object>>) weatherData.get("forecast");

        StringBuilder forecastText = new StringBuilder();
        for (int i = 0; i < forecast.size(); i++) {
            Map<String, Object> day = forecast.get(i);
            forecastText.append(String.format("Day %d (%s): %s, Rainfall: %.1fmm, Temp: %.1f°C - %.1f°C, Rain Chance: %d%%, Humidity: %d%%\n",
                    i + 1, day.get("date"), day.get("condition"), day.get("totalRainfall"),
                    day.get("minTemp"), day.get("maxTemp"), day.get("rainChance"), day.get("avgHumidity")));
        }

        return String.format("Analyze the following 7-day weather forecast for %s, %s, Pakistan and provide a flood risk assessment:\n\n" +
                        "%s\n" +
                        "Provide your analysis in the following JSON format (respond with ONLY valid JSON, no markdown or additional text):\n\n" +
                        "{\n" +
                        "  \"warningLevel\": \"Low|Moderate|High|Critical\",\n" +
                        "  \"riskScore\": 0-100,\n" +
                        "  \"confidence\": 0-100,\n" +
                        "  \"summary\": \"Brief 1-2 sentence summary of the flood risk\",\n" +
                        "  \"keyFactors\": [\"factor1\", \"factor2\", \"factor3\"],\n" +
                        "  \"recommendations\": [\"recommendation1\", \"recommendation2\", \"recommendation3\"],\n" +
                        "  \"dailyRisks\": [\n" +
                        "    {\"date\": \"YYYY-MM-DD\", \"risk\": \"Low|Moderate|High|Critical\", \"reason\": \"brief reason\"},\n" +
                        "    ...\n" +
                        "  ],\n" +
                        "  \"peakRiskDays\": [\"YYYY-MM-DD\", \"YYYY-MM-DD\"]\n" +
                        "}\n\n" +
                        "Consider:\n" +
                        "- Total rainfall accumulation over the week\n" +
                        "- Consecutive days of heavy rain\n" +
                        "- Regional flood history in Pakistan\n" +
                        "- Monsoon patterns\n" +
                        "- River basin proximity\n" +
                        "- Soil saturation potential",
                location.get("name"), location.get("region"), forecastText.toString());
    }

    /**
     * Parse AI response and extract structured data
     */
    private Map<String, Object> parseAIResponse(String response) {
        try {
            // Remove markdown code blocks if present
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.replaceAll("```json\\n?", "").replaceAll("```\\n?", "");
            }

            JsonNode parsed = objectMapper.readTree(cleanResponse);

            // Convert to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.convertValue(parsed, Map.class);

            // Validate required fields
            if (!result.containsKey("warningLevel") || !result.containsKey("riskScore") || !result.containsKey("summary")) {
                throw new Exception("Missing required fields in AI response");
            }

            // Ensure warning level is valid
            List<String> validLevels = Arrays.asList("Low", "Moderate", "High", "Critical");
            if (!validLevels.contains(result.get("warningLevel"))) {
                result.put("warningLevel", "Moderate");
            }

            return result;
        } catch (Exception e) {
            // Return fallback analysis
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("warningLevel", "Moderate");
            fallback.put("riskScore", 50);
            fallback.put("confidence", 30);
            fallback.put("summary", "Unable to generate detailed analysis. Please check weather data manually.");
            fallback.put("keyFactors", Arrays.asList("Analysis error occurred"));
            fallback.put("recommendations", Arrays.asList("Monitor weather updates regularly", "Stay informed through official channels"));
            fallback.put("dailyRisks", new ArrayList<>());
            fallback.put("peakRiskDays", new ArrayList<>());
            fallback.put("error", "Failed to parse AI response: " + e.getMessage());
            return fallback;
        }
    }
}
