package com.floodresponse.controller;

import com.floodresponse.dto.WeatherForecastRequest;
import com.floodresponse.service.AIForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Allow requests from frontend
public class AIForecastController {

    @Autowired
    private AIForecastService aiForecastService;

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeForecast(@RequestBody WeatherForecastRequest request) {
        try {
            String analysis = aiForecastService.analyzeForecast(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(analysis);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
