package org.uroran.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер файлов с температурой в скважинах
 */
public final class PointParser {
    private PointParser() {
    }

    public static Map<LocalDate, Map<Double, Double>> parsePointFile(String path) throws IOException {
        Path pathToFile = Path.of(path);
        String text = Files.readString(pathToFile).trim();
        String[] monthsProfiles = text.split("\n\n");

        monthsProfiles = Arrays.stream(monthsProfiles)
                .toArray(String[]::new);

        Map<LocalDate, Map<Double, Double>> map = new LinkedHashMap<>();
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})");

        Arrays.stream(monthsProfiles).toList().forEach(profile -> fillMap(profile, datePattern, map));

        return map;
    }

    private static void fillMap(String profile, Pattern datePattern, Map<LocalDate, Map<Double, Double>> map) {
        Matcher matcher = datePattern.matcher(profile);
        if (matcher.find()) {
            LocalDate keyDate = LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-M-d"));

            Map<Double, Double> depthToTemperature = new LinkedHashMap<>();
            String[] depths = profile.split("\n")[2].trim().split(" ");
            String[] temps = profile.split("\n")[3].trim().split(" ");

            for (int i = 0; i < depths.length; i++) {
                double depth = Double.parseDouble(depths[i]);
                double temp = Double.parseDouble(temps[i]);
                depthToTemperature.put(depth, temp);
            }

            map.put(keyDate, depthToTemperature);
        }
    }
}
