package com.floodresponse.dto;

import lombok.Data;
import java.util.List;

@Data
public class WeatherForecastRequest {
    private Location location;
    private List<ForecastDay> forecast;

    @Data
    public static class Location {
        private String name;
        private String region;
        private String country;
        private double lat;
        private double lon;
    }

    @Data
    public static class ForecastDay {
        private String date;
        private double maxTemp;
        private double minTemp;
        private double avgTemp;
        private double totalRainfall;
        private double maxWind;
        private double avgHumidity;
        private double rainChance;
        private String condition;
        private double uvIndex;
    }
}
