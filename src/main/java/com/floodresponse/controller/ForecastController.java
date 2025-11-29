package com.floodresponse.controller;

import com.floodresponse.model.Forecast;
import com.floodresponse.service.ForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {

    @Autowired
    private ForecastService forecastService;

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
}
