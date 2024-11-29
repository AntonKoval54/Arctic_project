package org.uroran.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер файлов с температурой в скважинах
 */
public class PointParser {
    private PointParser() {}

    public static Map<LocalDate, Map<Double, Double>> parsePointFile(Path path) throws IOException {
        String text = Files.readString(path).trim();
        String[] monthsProfiles = text.split("\n\n");

        Map<LocalDate, Map<Double, Double>> map = new LinkedHashMap<>();
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})");

        Arrays.stream(monthsProfiles).toList().forEach(month -> fillMap(month, datePattern, map));

        return map;
    }

    private static void fillMap(String month, Pattern datePattern, Map<LocalDate, Map<Double, Double>> map) {
        Matcher matcher = datePattern.matcher(month);
        if (matcher.find()) {
            LocalDate keyDate = LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-M-d"));

            Map<Double, Double> depthToTemperature = new LinkedHashMap<>();
            String[] depths = month.split("\n")[2].trim().split(" ");
            String[] temps = month.split("\n")[3].trim().split(" ");

            for (int i = 0; i < depths.length; i++) {
                double depth = Double.parseDouble(depths[i]);
                double temp = Double.parseDouble(temps[i]);
                depthToTemperature.put(depth, temp);
            }

            map.put(keyDate, depthToTemperature);
        }
    }
}
