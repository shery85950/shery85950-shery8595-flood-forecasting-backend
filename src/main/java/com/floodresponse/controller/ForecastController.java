package com.floodresponse.controller;

import com.floodresponse.model.Forecast;
import com.floodresponse.service.ForecastService;
import com.floodresponse.service.AIForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    @Autowired
    private ForecastService forecastService;

    @Autowired
    private AIForecastService aiForecastService;

    @GetMapping
    public List<Forecast> getAllForecasts() {
        return forecastService.getAllForecasts();
    }

    @GetMapping("/latest")
    public List<Forecast> getLatestForecasts() {
        return forecastService.getLatestForecasts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Forecast> getForecastById(@PathVariable Long id) {
        return forecastService.getForecastById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Forecast createForecast(@RequestBody Forecast forecast) {
        return forecastService.createForecast(forecast);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Forecast> updateForecast(@PathVariable Long id, @RequestBody Forecast forecast) {
        try {
            return ResponseEntity.ok(forecastService.updateForecast(id, forecast));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForecast(@PathVariable Long id) {
        forecastService.deleteForecast(id);
        return ResponseEntity.ok().build();
    }

    /**
     * AI-powered weekly forecast analysis endpoint
     * @param location Location name or coordinates (default: Lahore)
     * @return AI-generated flood risk assessment
     */
    @GetMapping("/ai-analysis")
    public ResponseEntity<Map<String, Object>> getAIForecastAnalysis(
            @RequestParam(defaultValue = "Lahore") String location) {
        Map<String, Object> analysis = aiForecastService.analyzeWeeklyForecast(location);
        return ResponseEntity.ok(analysis);
    }
}
