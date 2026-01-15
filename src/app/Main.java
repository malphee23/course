package app;

import database.ReportService;
import database.ShiftService;
import database.UserSession;
import database.DBConnection;
import database.EditableTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class Main extends JFrame {

    private final UserSession user;
    private final ShiftService shiftService;
    private final ReportService reportService = new ReportService();
    private JPanel centerPanel;

    private Integer currentTicketNumber = null;
    private int currentCouponCount = 0;
    private DefaultListModel<String> couponListModel;
    
    // Поля для отчётов
    private JTable reportTable;
    private JButton btnPrintReport;
    private String currentReportType;
    private String currentReportTitle;
    private String currentReportParams;

    public Main(UserSession user, ShiftService shiftService) {
        this.user = user;
        this.shiftService = shiftService;

        setTitle("Авиакасса — Главное окно");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (shiftService != null) {
                    shiftService.closeShift();
                }
            }
        });
    }

    private JPanel createLeftPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setPreferredSize(new Dimension(260, 0));
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        panel.add(Box.createVerticalGlue());

        switch (user.getRole()) {
            case "dispatcher" -> addDispatcherButtons(panel);
            case "cashier"    -> addCashierButtons(panel);
        }
        
        panel.add(Box.createVerticalGlue());

        outerPanel.add(panel, BorderLayout.CENTER);

        return outerPanel;
    }

    private void addDispatcherButtons(JPanel panel) {
        addSection(panel, "Справочники",
                new String[]{"Авиакомпании", "Кассы", "Кассиры", "Клиенты", "Билеты", "Купоны"}
        );
        addSection(panel, "Отчёты",
                new String[]{"Билеты за месяц", "Сумма продаж", "Клиенты на дату"}
        );
    }

    private void addCashierButtons(JPanel panel) {
        addSection(panel, "Справочники",
                new String[]{"Клиенты", "Билеты", "Купоны"}
        );
        addSection(panel, "Операции",
                new String[]{"Продажа билета", "Печать билета", "Печать купона"}
        );
    }

    private void addSection(JPanel panel, String title, String[] buttons) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(10));

        for (String text : buttons) {
            JButton btn = createStyledButton(text);
            btn.addActionListener(e -> handleButtonClick(text));
            panel.add(btn);
            panel.add(Box.createVerticalStrut(10));
        }

        panel.add(Box.createVerticalStrut(20));
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(new Color(70, 130, 180));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(100, 149, 237));
                } else {
                    g2.setColor(new Color(135, 206, 250));
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                
                super.paintComponent(g);
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(70, 130, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        
        btn.setFont(new Font("Arial", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setMaximumSize(new Dimension(220, 50));
        btn.setPreferredSize(new Dimension(220, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        
        return btn;
    }

    private Font getModernFont(int style, int size) {
        // Пробуем использовать современные системные шрифты
        String[] fontNames = {"Segoe UI", "Calibri", "Tahoma", "Verdana", "Arial"};
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            
            for (String fontName : fontNames) {
                for (String availableFont : availableFonts) {
                    if (availableFont.equals(fontName)) {
                        return new Font(fontName, style, size);
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        // Если ничего не найдено, используем Arial
        return new Font("Arial", style, size);
    }

    private void styleFormLabel(JLabel label) {
        label.setFont(getModernFont(Font.PLAIN, 14));
        label.setForeground(new Color(60, 60, 80));
    }

    private void styleTextField(JTextField field) {
        field.setFont(getModernFont(Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(getModernFont(Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
    }

    private void styleTable(JTable table) {
        // Устанавливаем современный шрифт
        Font tableFont = getModernFont(Font.PLAIN, 13);
        table.setFont(tableFont);
        table.setRowHeight(28);
        
        // Стилизуем заголовки таблицы
        table.getTableHeader().setFont(getModernFont(Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getWidth(), 35));
        
        // Настройка отрисовки ячеек
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Проверяем, что компонент существует и строка валидна
                if (c == null || row < 0 || row >= table.getRowCount()) {
                    return c;
                }
                
                // Чередование цветов строк
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(new Color(245, 250, 255));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(new Color(100, 149, 237));
                    c.setForeground(Color.WHITE);
                }
                
                // Центрирование текста
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                
                // Границы ячеек
                if (c instanceof javax.swing.JComponent) {
                    ((javax.swing.JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(220, 220, 220)),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));
                }
                
                return c;
            }
        });
        
        // Отключаем стандартную отрисовку фокуса
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Настройка выбора
        table.setSelectionBackground(new Color(100, 149, 237));
        table.setSelectionForeground(Color.WHITE);
    }

    private JPanel createCenterPanel() {
        centerPanel = new JPanel(new BorderLayout());
        showPlaceholder("Выберите действие");
        return centerPanel;
    }

    private void showPlaceholder(String text) {
        centerPanel.removeAll();
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(getModernFont(Font.PLAIN, 20));
        centerPanel.add(label, BorderLayout.CENTER);
        refreshCenter();
    }

    private void showTable(String title, String sql) {
        showTable(title, sql, null);
    }

    private void showTable(String title, String sql, String[] columnNames) {
        centerPanel.removeAll();

        JLabel header = new JLabel(title, SwingConstants.CENTER);
        header.setFont(getModernFont(Font.BOLD, 20));

        // Используем EditableTableModel для таблиц справочников
        EditableTableModel model = new EditableTableModel(title, sql, columnNames);
        JTable table = new JTable(model);
        styleTable(table);
        
        // Настраиваем редактирование и удаление
        setupTableEditing(table, model, title);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(header, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        refreshCenter();
    }
    
    /**
     * Настраивает редактирование ячеек (двойной клик, Enter) и контекстное меню для удаления
     */
    private void setupTableEditing(JTable table, EditableTableModel model, String tableTitle) {
        // Кастомный редактор ячеек с автоматическим сохранением
        table.setDefaultEditor(Object.class, new javax.swing.DefaultCellEditor(new JTextField()) {
            private Object oldValue;
            private int editingRow = -1;
            private int editingColumn = -1;
            
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                // Сохраняем старое значение и координаты редактируемой ячейки
                oldValue = value;
                editingRow = row;
                editingColumn = column;
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
            
            @Override
            public boolean stopCellEditing() {
                boolean stopped = super.stopCellEditing();
                if (stopped && editingRow >= 0 && editingColumn >= 0 && editingRow < model.getRowCount()) {
                    // Получаем новое значение
                    Object newValue = getCellEditorValue();
                    
                    // Если значение изменилось, сохраняем в БД
                    if (oldValue != null && !oldValue.equals(newValue)) {
                        SwingUtilities.invokeLater(() -> {
                            // Проверяем, что строка все еще существует (на случай, если таблица была перезагружена)
                            if (editingRow < model.getRowCount()) {
                                boolean success = model.saveCellEdit(editingRow, editingColumn, oldValue);
                                if (!success) {
                                    JOptionPane.showMessageDialog(
                                        Main.this,
                                        "Ошибка при сохранении изменений. Проверьте правильность данных.",
                                        "Ошибка сохранения",
                                        JOptionPane.ERROR_MESSAGE
                                    );
                                } else {
                                    // Принудительно обновляем отображение таблицы
                                    table.repaint();
                                }
                            }
                        });
                    }
                }
                return stopped;
            }
        });
        
        // Контекстное меню для удаления
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Удалить");
        deleteItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < model.getRowCount()) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Вы уверены, что хотите удалить эту запись?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = model.deleteRow(selectedRow);
                    if (!success) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Ошибка при удалении записи. Возможно, запись используется в других таблицах.",
                            "Ошибка удаления",
                            JOptionPane.ERROR_MESSAGE
                        );
                    } else {
                        // Принудительно обновляем отображение таблицы
                        table.repaint();
                    }
                }
            }
        });
        popupMenu.add(deleteItem);
        
        // Добавляем обработчик правой кнопки мыши
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        popupMenu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
        
        // Обработчик Enter для завершения редактирования
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
            }
        });
    }

    /**
     * Создаёт панель с кнопками "Добавить/Изменить/Удалить" для справочников.
     * Для роли "кассир": только "Изменить" и "Удалить". Для роли "диспетчер": все три действия.
     */
    private JPanel createDirectoryActionsPanel(String tableTitle) {
        String role = user.getRole();
        List<String> actions = new ArrayList<>();

        if ("dispatcher".equals(role)) {
            // Для диспетчера: полный набор действий для всех справочников
            if (tableTitle.equals("Авиакомпании")
                    || tableTitle.equals("Кассы")
                    || tableTitle.equals("Кассиры")
                    || tableTitle.equals("Клиенты")
                    || tableTitle.equals("Билеты")
                    || tableTitle.equals("Купоны")) {
                actions.add("Добавить");
                actions.add("Изменить");
                actions.add("Удалить");
            }
        } else if ("cashier".equals(role)) {
            // Для кассира: только изменение/удаление для некоторых справочников
            if (tableTitle.equals("Клиенты")
                    || tableTitle.equals("Билеты")
                    || tableTitle.equals("Купоны")) {
                actions.add("Изменить");
                actions.add("Удалить");
            }
        }

        if (actions.isEmpty()) {
            return null;
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        for (String action : actions) {
            JButton btn = createStyledButton(action);
            btn.setPreferredSize(new Dimension(130, 36));
            btn.addActionListener(e ->
                    JOptionPane.showMessageDialog(
                            this,
                            "Действие \"" + action + "\" для таблицы \"" + tableTitle + "\" пока не реализовано.",
                            "Информация",
                            JOptionPane.INFORMATION_MESSAGE
                    )
            );
            panel.add(btn);
        }

        return panel;
    }

    private void refreshCenter() {
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void handleButtonClick(String action) {
        switch (action) {

            case "Авиакомпании" -> showTable(
                    "Авиакомпании",
                    "SELECT airline_code, name, city, street, house FROM \"Airlines\"",
                    new String[]{"Код авиакомпании", "Название", "Город", "Улица", "Дом"}
            );

            case "Кассы" -> showTable(
                    "Кассы",
                    "SELECT cashdesk_number, city, street, house FROM \"CashDesk\"",
                    new String[]{"Номер кассы", "Город", "Улица", "Дом"}
            );

            case "Кассиры" -> showTable(
                    "Кассиры",
                    "SELECT cashier_number, last_name, first_name, middle_name FROM \"Cashier\"",
                    new String[]{"Номер кассира", "Фамилия", "Имя", "Отчество"}
            );

            case "Клиенты" -> showTable(
                    "Клиенты",
                    "SELECT passport_series || ' ' || passport_number AS passport_data, last_name, first_name, middle_name FROM \"Client\"",
                    new String[]{"Паспорт", "Фамилия", "Имя", "Отчество"}
            );

            case "Билеты" -> showTable(
                    "Билеты",
                    """
                    SELECT ticket_number,
                           airline_code,
                           cashdesk_number,
                           cashier_number,
                           ticket_type,
                           sale_date,
                           price
                    FROM "Ticket"
                    """,
                    new String[]{"Номер билета", "Код авиакомпании", "Номер кассы", "Номер кассира", "Тип билета", "Дата продажи", "Цена"}
            );

            case "Купоны" -> showTable(
                    "Купоны",
                    """
                    SELECT coupon_number,
                           ticket_number,
                           passport_data,
                           departure_point,
                           arrival_point,
                           tariff
                    FROM "Coupon"
                    """,
                    new String[]{"Номер купона", "Номер билета", "Паспорт", "Пункт отправления", "Пункт назначения", "Тариф"}
            );

            case "Билеты за месяц" -> showTicketsByMonthForm();
            case "Сумма продаж" -> showTotalSalesForm();
            case "Клиенты на дату" -> showClientsByDateForm();

            case "Продажа билета" ->
                    showSaleForm();

            case "Печать билета", "Печать купона" ->
                    handleOperation(action);
        }
    }

    private void handleOperation(String action) {
        switch (action) {
            case "Печать билета" -> {
                String input = JOptionPane.showInputDialog(this, "Введите номер билета:");
                if (input == null || input.isBlank()) return;
                try {
                    int ticketNumber = Integer.parseInt(input.trim());
                    reportService.printTicket(ticketNumber);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Неверный номер билета", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
            case "Печать купона" -> {
                String input = JOptionPane.showInputDialog(this, "Введите номер купона:");
                if (input == null || input.isBlank()) return;
                try {
                    int couponNumber = Integer.parseInt(input.trim());
                    reportService.printCoupon(couponNumber);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Неверный номер купона", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
            default -> showPlaceholder("Операция: " + action);
        }
    }

    private void showSaleForm() {
        centerPanel.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setBackground(new Color(250, 252, 255));

        JLabel title = new JLabel("Продажа билета");
        title.setFont(getModernFont(Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> airlineBox = new JComboBox<>();
        loadAirlinesIntoCombo(airlineBox);

        JComboBox<String> ticketTypeBox = new JComboBox<>(new String[]{"электронный", "бумажный"});

        JTextField saleDateField = new JTextField("2025-12-15");
        JTextField priceField = new JTextField();

        Dimension smallFieldSize = new Dimension(260, 28);
        saleDateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, smallFieldSize.height));
        saleDateField.setPreferredSize(smallFieldSize);
        priceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, smallFieldSize.height));
        priceField.setPreferredSize(smallFieldSize);

        JButton addCouponsButton = createStyledButton("Добавить купоны к билету");
        JButton createTicketButton = createStyledButton("Создать билет");
        createTicketButton.setEnabled(false);

        couponListModel = new DefaultListModel<>();
        JList<String> couponList = new JList<>(couponListModel);
        JScrollPane couponScroll = new JScrollPane(couponList);
        couponScroll.setPreferredSize(new Dimension(400, 200));

        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JLabel airlineLabel = new JLabel("Шифр авиакомпании");
        styleFormLabel(airlineLabel);
        airlineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        airlineBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleComboBox(airlineBox);
        panel.add(airlineLabel);
        panel.add(airlineBox);
        panel.add(Box.createVerticalStrut(10));

        JLabel typeLabel = new JLabel("Тип билета");
        styleFormLabel(typeLabel);
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ticketTypeBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleComboBox(ticketTypeBox);
        panel.add(typeLabel);
        panel.add(ticketTypeBox);
        panel.add(Box.createVerticalStrut(10));

        JLabel dateLabel = new JLabel("Дата продажи (ГГГГ-ММ-ДД)");
        styleFormLabel(dateLabel);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        saleDateField.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleTextField(saleDateField);
        panel.add(dateLabel);
        panel.add(saleDateField);
        panel.add(Box.createVerticalStrut(10));

        JLabel priceLabel = new JLabel("Стоимость билета");
        styleFormLabel(priceLabel);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        priceField.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleTextField(priceField);
        panel.add(priceLabel);
        panel.add(priceField);
        panel.add(Box.createVerticalStrut(15));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(addCouponsButton);
        buttonsPanel.add(createTicketButton);
        panel.add(buttonsPanel);

        panel.add(Box.createVerticalStrut(10));
        JLabel addedCouponsLabel = new JLabel("Добавленные купоны");
        styleFormLabel(addedCouponsLabel);
        addedCouponsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        couponScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(addedCouponsLabel);
        panel.add(couponScroll);

        currentTicketNumber = null;
        currentCouponCount = 0;
        couponListModel.clear();

        addCouponsButton.addActionListener(e -> {
            String airline = (String) airlineBox.getSelectedItem();
            String saleDate = saleDateField.getText().trim();
            String priceText = priceField.getText().trim();

            if (airline == null || airline.isBlank()
                    || saleDate.isEmpty()
                    || priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Заполните все поля билета", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (currentTicketNumber == null) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("""
                             INSERT INTO "Ticket"(airline_code, cashdesk_number, cashier_number, ticket_type, sale_date, price)
                             VALUES (?, ?, ?, ?, ?, ?)
                             RETURNING ticket_number
                             """)) {

                    ps.setString(1, airline);
                    ps.setInt(2, user.getCashdeskNumber());
                    ps.setInt(3, user.getCashierNumber());
                    ps.setString(4, (String) ticketTypeBox.getSelectedItem());
                    LocalDate localDate = LocalDate.parse(saleDate);
                    ps.setDate(5, Date.valueOf(localDate));
                    ps.setBigDecimal(6, new java.math.BigDecimal(priceText));

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentTicketNumber = rs.getInt("ticket_number");
                        } else {
                            JOptionPane.showMessageDialog(this, "Не удалось создать билет", "Ошибка", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при создании билета: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    return;
                }
            }

            if (currentTicketNumber != null) {
                CouponDialog dialog = new CouponDialog(this, currentTicketNumber);
                dialog.setVisible(true);
                createTicketButton.setEnabled(currentCouponCount > 0);
            }
        });

        createTicketButton.addActionListener(e -> {
            handleButtonClick("Билеты");
        });

        centerPanel.add(panel, BorderLayout.CENTER);
        refreshCenter();
    }

    private void loadAirlinesIntoCombo(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT airline_code FROM \"Airlines\" ORDER BY airline_code");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                comboBox.addItem(rs.getString("airline_code"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки авиакомпаний: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private class CouponDialog extends JDialog {
        private final int ticketNumber;

        public CouponDialog(JFrame owner, int ticketNumber) {
            super(owner, "Купоны для билета № " + ticketNumber, true);
            this.ticketNumber = ticketNumber;

            setSize(300, 500);
            setLocationRelativeTo(owner);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel passportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            JTextField seriesField = new JTextField();
            JTextField numberField = new JTextField();
            seriesField.setColumns(6);
            numberField.setColumns(14);
            passportPanel.add(seriesField);
            passportPanel.add(numberField);

            JTextField lastNameField = new JTextField();
            JTextField firstNameField = new JTextField();
            JTextField middleNameField = new JTextField();

            JTextField departureField = new JTextField();
            JTextField arrivalField = new JTextField();

            JComboBox<String> tariffBox = new JComboBox<>(new String[]{"эконом", "бизнес", "первый класс"});

            JButton addCouponButton = new JButton("Добавить купон");
            JButton finishButton = new JButton("Завершить добавление купонов");

            JLabel passportLabel = new JLabel("Серия и номер паспорта");
            passportLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            passportPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(passportLabel);
            panel.add(passportPanel);
            panel.add(Box.createVerticalStrut(10));

            JLabel lastNameLabel = new JLabel("Фамилия");
            lastNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            lastNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(lastNameLabel);
            panel.add(lastNameField);
            panel.add(Box.createVerticalStrut(5));

            JLabel firstNameLabel = new JLabel("Имя");
            firstNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            firstNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(firstNameLabel);
            panel.add(firstNameField);
            panel.add(Box.createVerticalStrut(5));

            JLabel middleNameLabel = new JLabel("Отчество");
            middleNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            middleNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(middleNameLabel);
            panel.add(middleNameField);
            panel.add(Box.createVerticalStrut(5));

            JLabel depLabel = new JLabel("Пункт отправления");
            depLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            departureField.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(depLabel);
            panel.add(departureField);
            panel.add(Box.createVerticalStrut(5));

            JLabel arrLabel = new JLabel("Пункт назначения");
            arrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            arrivalField.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(arrLabel);
            panel.add(arrivalField);
            panel.add(Box.createVerticalStrut(5));

            JLabel tariffLabel = new JLabel("Тариф");
            tariffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tariffBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(tariffLabel);
            panel.add(tariffBox);
            panel.add(Box.createVerticalStrut(10));

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttons.add(addCouponButton);
            buttons.add(finishButton);
            panel.add(buttons);

            add(panel);

            finishButton.addActionListener(e -> dispose());

            addCouponButton.addActionListener(e -> {
                if (currentCouponCount >= 4) {
                    JOptionPane.showMessageDialog(this, "Максимум 4 купона на билет", "Ограничение", JOptionPane.WARNING_MESSAGE);
                    addCouponButton.setEnabled(false);
                    return;
                }

                String series = seriesField.getText().trim();
                String number = numberField.getText().trim();
                String last = lastNameField.getText().trim();
                String first = firstNameField.getText().trim();
                String middle = middleNameField.getText().trim();
                String departure = departureField.getText().trim();
                String arrival = arrivalField.getText().trim();
                String tariff = (String) tariffBox.getSelectedItem();

                if (series.isEmpty() || number.isEmpty() || last.isEmpty() || first.isEmpty()
                        || departure.isEmpty() || arrival.isEmpty() || tariff == null) {
                    JOptionPane.showMessageDialog(this, "Заполните все обязательные поля купона", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String passportData = series + " " + number;

                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    try (PreparedStatement psClient = conn.prepareStatement("""
                            INSERT INTO "Client"(passport_series, passport_number, last_name, first_name, middle_name)
                            VALUES (?, ?, ?, ?, ?)
                            ON CONFLICT (passport_series, passport_number)
                            DO UPDATE SET last_name = EXCLUDED.last_name,
                                          first_name = EXCLUDED.first_name,
                                          middle_name = EXCLUDED.middle_name
                            """)) {
                        psClient.setString(1, series);
                        psClient.setString(2, number);
                        psClient.setString(3, last);
                        psClient.setString(4, first);
                        psClient.setString(5, middle);
                        psClient.executeUpdate();
                    }

                    int newCouponNumber;
                    try (PreparedStatement psCoupon = conn.prepareStatement("""
                            INSERT INTO "Coupon"(ticket_number, passport_data, departure_point, arrival_point, tariff)
                            VALUES (?, ?, ?, ?, ?)
                            RETURNING coupon_number
                            """)) {
                        psCoupon.setInt(1, ticketNumber);
                        psCoupon.setString(2, passportData);
                        psCoupon.setString(3, departure);
                        psCoupon.setString(4, arrival);
                        psCoupon.setString(5, tariff);

                        try (ResultSet rs = psCoupon.executeQuery()) {
                            if (rs.next()) {
                                newCouponNumber = rs.getInt("coupon_number");
                            } else {
                                conn.rollback();
                                JOptionPane.showMessageDialog(this, "Не удалось создать купон", "Ошибка", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    }

                    conn.commit();

                    currentCouponCount++;
                    String summary = newCouponNumber + " - " + departure + " - " + arrival + " - " + tariff;
                    couponListModel.addElement(summary);

                    if (currentCouponCount >= 4) {
                        addCouponButton.setEnabled(false);
                        JOptionPane.showMessageDialog(this, "Максимум 4 купона на билет", "Ограничение", JOptionPane.INFORMATION_MESSAGE);
                    }

                    departureField.setText("");
                    arrivalField.setText("");

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при добавлении купона: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });
        }
    }

    // ========== Методы для отчётов ==========

    private void showTicketsByMonthForm() {
        centerPanel.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Отчёт: Билеты за месяц");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> monthBox = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"});
        JComboBox<String> yearBox = new JComboBox<>(new String[]{"2024", "2025", "2026"});
        JComboBox<String> airlineBox = new JComboBox<>();
        loadAirlinesIntoCombo(airlineBox);

        JButton btnGenerate = new JButton("Сформировать отчёт");

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        JLabel monthLabel = new JLabel("Месяц");
        monthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        monthBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(monthLabel);
        panel.add(monthBox);
        panel.add(Box.createVerticalStrut(10));

        JLabel yearLabel = new JLabel("Год");
        yearLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        yearBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(yearLabel);
        panel.add(yearBox);
        panel.add(Box.createVerticalStrut(10));

        JLabel airlineLabel = new JLabel("Авиакомпания");
        airlineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        airlineBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(airlineLabel);
        panel.add(airlineBox);
        panel.add(Box.createVerticalStrut(20));

        btnGenerate.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(btnGenerate);

        btnGenerate.addActionListener(e -> {
            int selectedMonth = Integer.parseInt((String) monthBox.getSelectedItem());
            int selectedYear = Integer.parseInt((String) yearBox.getSelectedItem());
            String selectedAirline = (String) airlineBox.getSelectedItem();

            if (selectedAirline == null || selectedAirline.isBlank()) {
                JOptionPane.showMessageDialog(this, "Выберите авиакомпанию", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            executeTicketsByMonthReport(selectedMonth, selectedYear, selectedAirline);
        });

        centerPanel.add(panel, BorderLayout.CENTER);
        refreshCenter();
    }

    private void showTotalSalesForm() {
        executeTotalSalesReport();
    }

    private void showClientsByDateForm() {
        centerPanel.removeAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Отчёт: Клиенты на дату");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField dateField = new JTextField(LocalDate.now().toString());
        dateField.setMaximumSize(new Dimension(200, 30));
        dateField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnGenerate = new JButton("Сформировать отчёт");

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        JLabel dateLabel = new JLabel("Дата (ГГГГ-ММ-ДД)");
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(dateLabel);
        panel.add(dateField);
        panel.add(Box.createVerticalStrut(20));

        btnGenerate.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(btnGenerate);

        btnGenerate.addActionListener(e -> {
            String dateText = dateField.getText().trim();
            if (dateText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Введите дату", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                LocalDate selectedDate = LocalDate.parse(dateText);
                executeClientsByDateReport(selectedDate);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Неверный формат даты. Используйте ГГГГ-ММ-ДД", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        centerPanel.add(panel, BorderLayout.CENTER);
        refreshCenter();
    }

    private void executeTicketsByMonthReport(int selectedMonth, int selectedYear, String selectedAirline) {
        String sql = """
                SELECT 
                    t.ticket_number,
                    a.name AS airline_name,
                    t.sale_date,
                    t.ticket_type,
                    t.price
                FROM "Ticket" t
                JOIN "Airlines" a ON a.airline_code = t.airline_code
                WHERE
                    EXTRACT(MONTH FROM t.sale_date) = ?
                    AND EXTRACT(YEAR FROM t.sale_date) = ?
                    AND t.airline_code = ?
                ORDER BY t.sale_date
                """;

        String[] columnNames = {"Номер билета", "Авиакомпания", "Дата продажи", "Тип билета", "Цена"};

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, selectedMonth);
            ps.setInt(2, selectedYear);
            ps.setString(3, selectedAirline);

            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("ticket_number"),
                            rs.getString("airline_name"),
                            rs.getDate("sale_date"),
                            rs.getString("ticket_type"),
                            rs.getBigDecimal("price")
                    };
                    model.addRow(row);
                }
            }

            currentReportType = "Билеты за месяц";
            currentReportTitle = "Билеты за месяц";
            currentReportParams = String.format("Месяц: %d, Год: %d, Авиакомпания: %s", selectedMonth, selectedYear, selectedAirline);
            showReportTable(currentReportTitle, model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при выполнении запроса: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void executeTotalSalesReport() {
        String sql = """
                SELECT
                    a.airline_code,
                    a.name AS airline_name,
                    SUM(t.price) AS total_sales
                FROM "Ticket" t
                JOIN "Airlines" a ON a.airline_code = t.airline_code
                GROUP BY a.airline_code, a.name
                ORDER BY total_sales DESC
                """;

        String[] columnNames = {"Код авиакомпании", "Название авиакомпании", "Сумма продаж"};

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            while (rs.next()) {
                Object[] row = {
                        rs.getString("airline_code"),
                        rs.getString("airline_name"),
                        rs.getBigDecimal("total_sales")
                };
                model.addRow(row);
            }

            currentReportType = "Сумма продаж";
            currentReportTitle = "Сумма продаж";
            currentReportParams = "";
            showReportTable(currentReportTitle, model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при выполнении запроса: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void executeClientsByDateReport(LocalDate selectedDate) {
        String sql = """
                SELECT DISTINCT
                    c.passport_data,
                    c.last_name,
                    c.first_name,
                    c.middle_name,
                    a.name AS airline_name
                FROM "Client" c
                JOIN "Coupon" cp ON cp.passport_data = c.passport_data
                JOIN "Ticket" t ON t.ticket_number = cp.ticket_number
                JOIN "Airlines" a ON a.airline_code = t.airline_code
                WHERE t.sale_date = ?
                ORDER BY c.last_name
                """;

        String[] columnNames = {"Паспорт", "Фамилия", "Имя", "Отчество", "Авиакомпания"};

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(selectedDate));

            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                            rs.getString("passport_data"),
                            rs.getString("last_name"),
                            rs.getString("first_name"),
                            rs.getString("middle_name"),
                            rs.getString("airline_name")
                    };
                    model.addRow(row);
                }
            }

            currentReportType = "Клиенты на дату";
            currentReportTitle = "Клиенты на дату";
            currentReportParams = "Дата: " + selectedDate.toString();
            showReportTable(currentReportTitle, model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при выполнении запроса: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void showReportTable(String title, DefaultTableModel model) {
        centerPanel.removeAll();

        JLabel header = new JLabel(title, SwingConstants.CENTER);
        header.setFont(getModernFont(Font.BOLD, 20));

        reportTable = new JTable(model);
        styleTable(reportTable);

        btnPrintReport = new JButton("Напечатать отчёт");
        btnPrintReport.setFont(new Font("Arial", Font.PLAIN, 14));
        btnPrintReport.addActionListener(e -> printReportToPdf());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btnPrintReport);

        centerPanel.add(header, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        refreshCenter();
    }

    private void printReportToPdf() {
        if (reportTable == null || reportTable.getModel().getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Нет данных для печати", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            reportService.printReportTable(
                    currentReportType,
                    currentReportTitle,
                    currentReportParams,
                    reportTable.getModel()
            );
            JOptionPane.showMessageDialog(this, "Отчёт успешно сохранён в папку Downloads", "Готово", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка при печати отчёта: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}