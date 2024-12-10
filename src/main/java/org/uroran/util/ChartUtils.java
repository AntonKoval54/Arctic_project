package org.uroran.util;

import org.jfree.chart.JFreeChart;
import org.uroran.models.Season;
import org.uroran.models.TemperatureData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Collectors;

public final class ChartUtils {

    private ChartUtils() {
    }

    public static Month[] getAvailableMonthsForYear(TemperatureData data, int year) {
        return data.getData().keySet().stream()
                .filter(date -> date.getYear() == year)
                .map(LocalDate::getMonth)
                .toArray(java.time.Month[]::new);
    }

    public static Season[] getAvailableSeasonsForYear(TemperatureData data, int year) {
        return data.getData().keySet().stream()
                .filter(e -> (!e.getMonth().equals(Month.NOVEMBER) && e.getDayOfMonth() != 27) && e.getYear() == year)
                .collect(Collectors.groupingBy(
                        date -> Season.getSeason(date.getMonth())
                ))
                .keySet().toArray(Season[]::new);
    }

    public static Integer[] getAvailableYears(TemperatureData data) {
        return data.getData().keySet().stream()
                .map(LocalDate::getYear)
                .distinct()
                .toArray(Integer[]::new);
    }

    public static void exportChartAsPng(JFreeChart chart, File fileToSave) throws IOException {
        // Добавляем расширение, если пользователь его не указал
        if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
        }

        org.jfree.chart.ChartUtils.saveChartAsPNG(fileToSave, chart, 800, 600);
    }

    public static void exportDataAsXlsx(String scriptPath, String inputFilePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", // Должен быть доступен в PATH
                scriptPath,
                inputFilePath
        );

        // Перенаправляем ошибки в стандартный поток вывода
        processBuilder.redirectErrorStream(true);

        // Запускаем процесс
        Process process = processBuilder.start();

        // Читаем вывод скрипта
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        // Ожидаем завершения процесса
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Python script executed successfully.");
        } else {
            System.err.println("Python script failed with exit code " + exitCode);
        }
    }
}
