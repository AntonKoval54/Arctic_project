package org.uroran.gui;

import org.jfree.chart.ChartPanel;
import org.uroran.models.Season;
import org.uroran.models.TemperatureData;
import org.uroran.util.ChartDrawer;
import org.uroran.util.ChartUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

public class ChartWindow extends JFrame {
    private final static String XLSX_SCRIPT_PATH = "script_txt_xslx.py";
    private final TemperatureData temperatureData;
    private ChartPanel currentChart;
    private final JPanel chartPanel;

    public ChartWindow(TemperatureData temperatureData) {
        this.temperatureData = temperatureData;

        setTitle("Скважина №" + temperatureData.getPointNumber());
        setSize(1000, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        JPanel managingPanel = createManagingPanel();
        add(managingPanel, BorderLayout.WEST);

        chartPanel = new JPanel(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);
    }

    private JPanel createManagingPanel() {
        JPanel managingPanel = new JPanel(new BorderLayout());
        managingPanel.setPreferredSize(new Dimension(350, this.getHeight()));
        managingPanel.setBorder(BorderFactory.createSoftBevelBorder(0));

        //Панель с выпадающими списками
        managingPanel.add(getTopManagingPanel());

        // Панель с кнопками экспорта
        managingPanel.add(getBottomManagingPanel(), BorderLayout.SOUTH);

        return managingPanel;
    }

    private JPanel getTopManagingPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> chartTypes = new JComboBox<>(new String[]{"", "Месяц", "Сезон", "Всё"});
        JComboBox<String> valueSelector = new JComboBox<>();
        JComboBox<String> yearSelector = new JComboBox<>();
        valueSelector.setEnabled(false);
        yearSelector.setEnabled(false);

        chartTypes.addActionListener(e -> listenChartTypes(chartTypes, yearSelector, valueSelector));
        yearSelector.addActionListener(e -> listenYearSelector(yearSelector, valueSelector, chartTypes));
        valueSelector.addActionListener(e -> listenValueSelector(valueSelector, yearSelector, chartTypes));

        Dimension comboBoxSize = new Dimension(150, 30);

        topPanel.add(createComboBoxPanel(chartTypes, new JLabel("Выберите тип:"), comboBoxSize));
        topPanel.add(createComboBoxPanel(yearSelector, new JLabel("Выберите год:"), comboBoxSize));
        topPanel.add(createComboBoxPanel(valueSelector, new JLabel("Выберите значение:"), comboBoxSize));
        return topPanel;
    }

    private JPanel getBottomManagingPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton exportImageButton = new JButton("Экспорт графика");
        JButton exportXlsxButton = new JButton("Экспорт данных");

        Dimension buttonSize = new Dimension(150, 30); // Размер кнопок
        exportImageButton.setPreferredSize(buttonSize);
        exportXlsxButton.setPreferredSize(buttonSize);

        exportImageButton.addActionListener(e -> exportChartAsPng());
        exportXlsxButton.addActionListener(e -> exportChartAsXLSX());

        bottomPanel.add(exportImageButton);
        bottomPanel.add(exportXlsxButton);
        return bottomPanel;
    }

    private JPanel createComboBoxPanel(JComboBox<String> comboBox, JLabel label, Dimension comboBoxDimension) {
        JPanel topPanelElement = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        comboBox.setPreferredSize(comboBoxDimension);
        topPanelElement.add(label);
        topPanelElement.add(comboBox);

        return topPanelElement;
    }

    private void listenChartTypes(JComboBox<String> chartTypes, JComboBox<String> yearSelector, JComboBox<String> valueSelector) {
        String selectedType = (String) chartTypes.getSelectedItem();
        yearSelector.removeAllItems();
        valueSelector.removeAllItems();
        valueSelector.setEnabled(false);

        if ("Месяц".equals(selectedType) || "Сезон".equals(selectedType)) {
            Integer[] availableYears = ChartUtils.getAvailableYears(this.temperatureData);
            yearSelector.addItem("");
            for (int year : availableYears) {
                yearSelector.addItem(String.valueOf(year));
            }
            yearSelector.setEnabled(true);
        } else if ("Все".equals(selectedType)) {
            updateChart(ChartDrawer.drawFullChart(temperatureData));
            yearSelector.setEnabled(false);
        }
    }

    private void listenYearSelector(JComboBox<String> yearSelector, JComboBox<String> valueSelector, JComboBox<String> chartTypes) {
        if (!yearSelector.isEnabled()) return;

        String selectedYear = (String) yearSelector.getSelectedItem();
        String selectedType = (String) chartTypes.getSelectedItem();

        valueSelector.removeAllItems();
        valueSelector.setEnabled(false);

        if (selectedYear == null || selectedYear.isEmpty()) {
            return;
        }

        int year = Integer.parseInt(selectedYear);

        if ("Месяц".equals(selectedType)) {
            valueSelector.addItem("");
            for (Month month : ChartUtils.getAvailableMonthsForYear(this.temperatureData, year)) {
                valueSelector.addItem(month.toString());
            }
            valueSelector.setEnabled(true);
        } else if ("Сезон".equals(selectedType)) {
            valueSelector.addItem("");
            for (Season season : ChartUtils.getAvailableSeasonsForYear(this.temperatureData, year)) {
                valueSelector.addItem(season.toString());
            }
            valueSelector.setEnabled(true);
        }
    }

    private void listenValueSelector(JComboBox<String> valueSelector, JComboBox<String> yearSelector, JComboBox<String> chartTypes) {
        if (!valueSelector.isEnabled()) return;

        String selectedType = (String) chartTypes.getSelectedItem();
        String selectedValue = (String) valueSelector.getSelectedItem();
        String selectedYear = (String) yearSelector.getSelectedItem();

        if (selectedValue == null || selectedValue.isEmpty() || selectedYear == null || selectedYear.isEmpty()) {
            return;
        }

        int year = Integer.parseInt(selectedYear);

        switch (selectedType) {
            case "Месяц": {
                Month selectedMonth = Month.valueOf(selectedValue.toUpperCase());
                updateChart(ChartDrawer.drawMonthChart(LocalDate.of(year, selectedMonth, 1), temperatureData));
                break;
            }

            case "Сезон": {
                Season selectedSeason = Season.valueOf(selectedValue.toUpperCase());
                updateChart(ChartDrawer.drawSeasonChart(selectedSeason, year, temperatureData));
                break;
            }

            case "Всё": {
                updateChart(ChartDrawer.drawFullChart(temperatureData));
                break;
            }

            case null, default:
                break;
        }
    }

    private void updateChart(ChartPanel newChartPanel) {
        currentChart = newChartPanel;

        chartPanel.removeAll();
        chartPanel.add(newChartPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void exportChartAsPng() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить график как изображение");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ChartUtils.exportChartAsPng(currentChart.getChart(), fileToSave);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка сохранения графика: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(null, "График успешно сохранён в " + fileToSave.getAbsolutePath());
        }
    }

    private void exportChartAsXLSX() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить данные как XLSX");
        fileChooser.setFileFilter(new FileNameExtensionFilter("XLSX", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                ChartUtils.exportDataAsXlsx(XLSX_SCRIPT_PATH, String.valueOf(fileToSave));
            } catch (IOException | InterruptedException e) {
                JOptionPane.showMessageDialog(null, "Ошибка сохранения данных: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(null, "Данные успешно сохранены в " + fileToSave.getAbsolutePath());
        }
    }
}
