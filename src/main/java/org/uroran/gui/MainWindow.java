package org.uroran.gui;

import com.jcraft.jsch.JSchException;
import org.uroran.service.SshService;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow extends JFrame {
    private JTextPane terminalOutputPane;
    private JTextField commandInputField;
    private JScrollPane scrollPane;
    private StyledDocument document;

    private SshService sshService;

    public MainWindow(SshService sshService) {
        this.sshService = sshService;

        try {
            sshService.connect();
            Thread.sleep(2000);
        } catch (IOException | JSchException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Настройки окна
        setTitle("Удаленный терминал");
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Основной контейнер
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        // Создаем область вывода с JTextPane
        terminalOutputPane = new JTextPane();
        terminalOutputPane.setEditable(false); // Только для чтения
        terminalOutputPane.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Моноширинный шрифт
        document = terminalOutputPane.getStyledDocument();
        scrollPane = new JScrollPane(terminalOutputPane);
        container.add(scrollPane, BorderLayout.CENTER);

        // Создаем поле для ввода команд
        commandInputField = new JTextField();
        commandInputField.addActionListener(e -> sendCommand());
        container.add(commandInputField, BorderLayout.SOUTH);

        // Слушатель закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sshService.disconnect();
                dispose();
                System.exit(1);
            }
        });

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void sendCommand() {
        String command = commandInputField.getText();
        if (!command.trim().isEmpty()) {
            String output;
            try {
                output = sshService.sendCommand(command);
            } catch (Exception ignored) {
                output = "Failed";
            }

            appendColoredText(">>> " + command + "\n", Color.BLUE);
            appendAnsiColoredText(output);
            commandInputField.setText("");
        }
    }

    // Метод для добавления текста с указанным цветом
    private void appendColoredText(String text, Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Метод для парсинга и отображения текста с ANSI-кодами
    private void appendAnsiColoredText(String text) {
        // Регулярное выражение для ANSI-кодов
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+;?)*m");
        Matcher matcher = ansiPattern.matcher(text);
        int lastEnd = 0;

        SimpleAttributeSet attributes = new SimpleAttributeSet(); // Атрибуты текста

        while (matcher.find()) {
            // Добавляем текст до ANSI-кода
            if (matcher.start() > lastEnd) {
                try {
                    document.insertString(document.getLength(), text.substring(lastEnd, matcher.start()), attributes);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            // Обновляем стиль текста в зависимости от ANSI-кода
            String ansiCode = matcher.group();
            updateAttributesForAnsiCode(ansiCode, attributes);

            // Обновляем позицию конца
            lastEnd = matcher.end();
        }

        // Добавляем оставшийся текст
        if (lastEnd < text.length()) {
            try {
                document.insertString(document.getLength(), text.substring(lastEnd), attributes);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    // Метод для обновления атрибутов на основе ANSI-кода
    private void updateAttributesForAnsiCode(String ansiCode, SimpleAttributeSet attributes) {
        // Пример: обрабатываем цветовые коды (30–37 для текста)
        if (ansiCode.contains("31")) {
            StyleConstants.setForeground(attributes, Color.RED); // Красный
        } else if (ansiCode.contains("32")) {
            StyleConstants.setForeground(attributes, Color.GREEN); // Зеленый
        } else if (ansiCode.contains("33")) {
            StyleConstants.setForeground(attributes, Color.YELLOW); // Желтый
        } else if (ansiCode.contains("34")) {
            StyleConstants.setForeground(attributes, Color.BLUE); // Синий
        } else if (ansiCode.contains("35")) {
            StyleConstants.setForeground(attributes, Color.MAGENTA); // Магента
        } else if (ansiCode.contains("36")) {
            StyleConstants.setForeground(attributes, Color.CYAN); // Голубой
        } else if (ansiCode.contains("0")) {
            StyleConstants.setForeground(attributes, Color.BLACK); // Сброс цвета
        }
    }
}