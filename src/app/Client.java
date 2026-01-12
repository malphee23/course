package app;

import database.ReportService;

import javax.swing.*;
import java.awt.*;

public class Client extends JFrame {

    private final ReportService reportService = new ReportService();

    public Client() {
        setTitle("Печать билетов по паспорту");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Введите серию и номер паспорта через пробел (например, 1234 555111)");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField passportField = new JTextField();
        passportField.setMaximumSize(new Dimension(300, 30));

        JButton printButton = new JButton("Печать PDF");
        printButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        printButton.addActionListener(e -> {
            String passport = passportField.getText().trim();
            if (passport.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Укажите паспортные данные", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            reportService.printByPassport(passport);
        });

        panel.add(Box.createVerticalGlue());
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(passportField);
        panel.add(Box.createVerticalStrut(20));
        panel.add(printButton);
        panel.add(Box.createVerticalGlue());

        add(panel);
    }
}