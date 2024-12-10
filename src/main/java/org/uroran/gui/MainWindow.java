package org.uroran.gui;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.uroran.models.SessionData;
import org.uroran.models.SftpEntry;
import org.uroran.models.TemperatureData;
import org.uroran.service.SessionDataService;
import org.uroran.service.SessionManager;
import org.uroran.service.SftpService;
import org.uroran.service.SshService;
import org.uroran.util.PointParser;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow extends JFrame {
    private static final SftpEntry emptyEntry = new SftpEntry("..", SftpEntry.EntryType.DIRECTORY, "");

    private JTextField commandInputField;
    private StyledDocument document;
    private DefaultTableModel tableModel;
    private boolean sync = true;

    private final SshService sshService;
    private final SftpService sftpService;

    public MainWindow(SessionData sessionData) {
        SessionManager sessionManager = new SessionManager(sessionData);

        try {
            sessionManager.connect();
        } catch (JSchException e) {
            throw new RuntimeException("Cannot connect to session", e);
        }

        this.sshService = new SshService(sessionManager);
        this.sftpService = new SftpService(sessionManager);

        try {
            sshService.connect();
            sftpService.connect();
            Thread.sleep(1000);
        } catch (IOException | JSchException | InterruptedException | SftpException e) {
            throw new RuntimeException("Cannot open to channel", e);
        }

        setTitle("СК 'УРАН'");
        setSize(1000, 600);
        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sshService.disconnect();
                sftpService.disconnect();
                dispose();
                System.exit(0);
            }
        });

        // Основной контейнер
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

        // Основной разделитель (левая и правая часть)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(500);
        container.add(mainSplitPane, BorderLayout.CENTER);

        // SSH терминал
        JPanel sshPanel = createSshPanel();
        mainSplitPane.setLeftComponent(sshPanel);

        // SFTP
        JPanel sftpPanel = createSftpPanel();
        mainSplitPane.setRightComponent(sftpPanel);

        sendCommand();
    }

    /**
     * Метод для создания SSH панели
     */
    private JPanel createSshPanel() {
        JPanel sshPanel = new JPanel(new BorderLayout());

        JTextPane terminalOutputPane = new JTextPane();
        terminalOutputPane.setEditable(false);
        terminalOutputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        document = terminalOutputPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(terminalOutputPane);
        sshPanel.add(scrollPane, BorderLayout.CENTER);

        commandInputField = new JTextField();
        commandInputField.addActionListener(e -> sendCommand());
        sshPanel.add(commandInputField, BorderLayout.SOUTH);

        return sshPanel;
    }

    private JPanel createSftpPanel() {
        {
            JPanel sftpPanel = new JPanel(new BorderLayout());

            // Создаем таблицу для отображения файлов
            String[] columnNames = {"Тип", "Имя", "Время изменения"};
            tableModel = new DefaultTableModel(columnNames, 0);
            JTable fileTable = new JTable(tableModel) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // Отключаем редактирование
                }
            };

            // Добавляем прокрутку
            JScrollPane scrollPane = new JScrollPane(fileTable);
            sftpPanel.add(scrollPane, BorderLayout.CENTER);

            // Слушатель для обновления текущей директории
            fileTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row < 0) return;

                    String fileName = (String) tableModel.getValueAt(row, 1);
                    String fileType = (String) tableModel.getValueAt(row, 0);

                    // ЛКМ: Переход в папку
                    if (e.getClickCount() == 2
                            && (fileType.equals(SftpEntry.EntryType.DIRECTORY.toString())
                            || fileType.equals(SftpEntry.EntryType.LINK.toString()))
                    ) {
                        try {
                            if (sync) {
                                commandInputField.setText("cd " + fileName);
                                sendCommand();
                            } else {
                                sftpService.changeCurrentRemoteDir(Path.of(fileName));
                            }

                            updateFileList(); // Обновление списка файлов
                        } catch (SftpException ex) {
                            JOptionPane.showMessageDialog(sftpPanel, "Не удалось открыть папку: " + ex.getMessage());
                        }
                    }

                    // ПКМ: Показать контекстное меню
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JPopupMenu contextMenu = createFileContextMenu(fileName);
                        contextMenu.show(fileTable, e.getX(), e.getY());
                    }
                }
            });

            fileTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel label = new JLabel();
                    label.setHorizontalAlignment(SwingConstants.CENTER);

                    if (value.toString().equals(SftpEntry.EntryType.DIRECTORY.toString()) || value.toString().equals(SftpEntry.EntryType.LINK.toString())) {
                        label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    } else {
                        label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                    }

                    if (isSelected) {
                        label.setBackground(table.getSelectionBackground());
                        label.setForeground(table.getSelectionForeground());
                        label.setOpaque(true);
                    }
                    return label;
                }
            });

            // Инициализация списка файлов
            updateFileList();

            return sftpPanel;
        }
    }

    private void updateFileList() {
        tableModel.setRowCount(0);
        try {
            tableModel.addRow(new Object[]{emptyEntry.getEntryType().name(), emptyEntry.getName(), emptyEntry.getMTime()});
            List<SftpEntry> files = sftpService.listFiles();
            for (SftpEntry entry : files) {
                tableModel.addRow(new Object[]{entry.getEntryType().name(), entry.getName(), entry.getMTime()});
            }
        } catch (SftpException e) {
            JOptionPane.showMessageDialog(this, "Не удалось загрузить список файлов: " + e.getMessage());
        }
    }

    private JPopupMenu createFileContextMenu(String fileName) {
        JPopupMenu contextMenu = new JPopupMenu();

        // Кнопка "Скачать"
        JMenuItem downloadItem = new JMenuItem("Скачать");
        downloadItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                Path localPath = fileChooser.getSelectedFile().toPath();
                try {
                    sftpService.downloadFile(Path.of(fileName), localPath);
                    JOptionPane.showMessageDialog(this, "Файл успешно скачан!");
                } catch (SftpException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при скачивании: " + ex.getMessage());
                }
            }
        });
        contextMenu.add(downloadItem);

        // Кнопка "Удалить"
        JMenuItem deleteItem = new JMenuItem("Удалить");
        deleteItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите удалить файл?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    sftpService.deleteFile(Path.of(fileName));
                    updateFileList();
                    JOptionPane.showMessageDialog(this, "Файл успешно удален!");
                } catch (SftpException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении: " + ex.getMessage());
                }
            }
        });
        contextMenu.add(deleteItem);

        // Кнопка "График"
        JMenuItem graphItem = new JMenuItem("График");
        graphItem.addActionListener(e -> {
            try {
                sftpService.downloadFile(Path.of(fileName), Path.of("src/main/resources/tempfiles"));
            } catch (SftpException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при открытии файла для построения графиков " + ex.getMessage());
            }

            Map<LocalDate, Map<Double, Double>> parsedData;
            try {
                Thread.sleep(2000);
                parsedData = PointParser.parsePointFile("src/main/resources/tempfiles/" + fileName);
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            TemperatureData data = new TemperatureData(15, parsedData);

            SwingUtilities.invokeLater(() -> {
                ChartWindow chartWindow = new ChartWindow(data);
                chartWindow.setVisible(true);
            });

        });
        contextMenu.add(graphItem);

        return contextMenu;
    }

    /**
     * Создаем меню-бар с кнопками "Настройки", "SFTP" и "Закрыть сессию".
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Меню "Настройки"
        JMenu settingsMenu = new JMenu("Настройки");
        JMenuItem settingsPlaceholder = new JMenuItem("Пусто");
        settingsPlaceholder.setEnabled(false);
        settingsMenu.add(settingsPlaceholder);
        menuBar.add(settingsMenu);

        // Кнопка "Закрыть сессию"
        JMenuItem closeSessionItem = new JMenuItem("Закрыть сессию");
        closeSessionItem.addActionListener(e -> {
            sshService.disconnect();
            new SessionManagerWindow(SessionDataService.getInstance()).setVisible(true);
            dispose();
        });
        menuBar.add(closeSessionItem);

        return menuBar;
    }

    /**
     * Метод для отправки команды из GUI по SSH и вывода ответа
     */
    private void sendCommand() {
        String command = commandInputField.getText();

        String output;
        try {
            if (command.startsWith("cd") && sync) {
                String path = command.split(" ")[1];
                sftpService.changeCurrentRemoteDir(Path.of(path));
                updateFileList();
            }
            output = sshService.sendCommand(command);
        } catch (Exception ignored) {
            output = "Failed";
        }

        //Раскрашиваем input
        appendColoredText(">>> " + command + "\n", Color.BLUE);
        //раскрашиваем output
        appendAnsiColoredText(output);
        commandInputField.setText("");
    }

    /**
     * Метод для добавления текста с указанным цветом (GPT, криво)
     */
    private void appendColoredText(String text, Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при раскрашивании вводимого текста", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Метод для парсинга и отображения текста с ANSI-кодами (GPT, вроде работает)
     */
    private void appendAnsiColoredText(String text) {
        // Регулярное выражение для ANSI-кодов
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+;?)*m");
        Matcher matcher = ansiPattern.matcher(text);
        int lastEnd = 0;

        SimpleAttributeSet attributes = new SimpleAttributeSet();

        while (matcher.find()) {
            // Добавляем текст до ANSI-кода
            if (matcher.start() > lastEnd) {
                try {
                    document.insertString(document.getLength(), text.substring(lastEnd, matcher.start()), attributes);
                } catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при раскрашивании выводимого текста", JOptionPane.ERROR_MESSAGE);
                }
            }

            // Обновляем стиль текста в зависимости от ANSI-кода
            String ansiCode = matcher.group();
            updateAttributesForAnsiCode(ansiCode, attributes);

            lastEnd = matcher.end();
        }

        // Добавляем оставшийся текст
        if (lastEnd < text.length()) {
            try {
                document.insertString(document.getLength(), text.substring(lastEnd), attributes);
            } catch (BadLocationException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка при раскрашивании выводимого текста", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Метод для получения цвета по ANSI коду (GPT, криво)
     */
    private void updateAttributesForAnsiCode(String ansiCode, SimpleAttributeSet attributes) {
        if (ansiCode.contains("31")) {
            StyleConstants.setForeground(attributes, Color.RED);
        } else if (ansiCode.contains("32")) {
            StyleConstants.setForeground(attributes, Color.GREEN);
        } else if (ansiCode.contains("33")) {
            StyleConstants.setForeground(attributes, Color.YELLOW);
        } else if (ansiCode.contains("34")) {
            StyleConstants.setForeground(attributes, Color.BLUE);
        } else if (ansiCode.contains("35")) {
            StyleConstants.setForeground(attributes, Color.MAGENTA);
        } else if (ansiCode.contains("36")) {
            StyleConstants.setForeground(attributes, Color.CYAN);
        } else if (ansiCode.contains("0")) {
            StyleConstants.setForeground(attributes, Color.BLACK);
        }
    }

}