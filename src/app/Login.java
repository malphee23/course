package app;

import database.ShiftService;
import database.UserSession;
import database.authorization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Login extends JFrame {
    private final JTextField loginField;
    private final JPasswordField passwordField;

    public Login() {
        setTitle("Авторизация");
        setSize(520, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        panel.setBackground(new Color(250, 252, 255));

        JLabel title = new JLabel("Вход в систему");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginField = new JTextField();
        passwordField = new JPasswordField();

        Dimension fieldSize = new Dimension(280, 32);
        loginField.setMaximumSize(fieldSize);
        passwordField.setMaximumSize(fieldSize);

        styleTextField(loginField);
        styleTextField(passwordField);

        JButton loginButton = createStyledButton("Вход");
        JButton guestButton = createStyledButton("Я гость");

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        guestButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton.addActionListener(e -> login());
        guestButton.addActionListener(e -> openClient());

        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        };
        loginField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);

        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JLabel loginLabel = new JLabel("Логин");
        styleFormLabel(loginLabel);
        loginLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(loginLabel);
        panel.add(loginField);
        panel.add(Box.createVerticalStrut(10));

        JLabel passwordLabel = new JLabel("Пароль");
        styleFormLabel(passwordLabel);
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(20));
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(guestButton);
        panel.add(Box.createVerticalGlue());
        
        add(panel);
    }

    private void login() {
        String login = loginField.getText();
        String password = new String(passwordField.getPassword());

        UserSession user = authorization.authenticate(login, password);

        if (user == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Неверный логин или пароль",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        ShiftService shiftService = new ShiftService();

        if ("cashier".equals(user.getRole())) {
            shiftService.openShift(user);
        }

        dispose();

        switch (user.getRole()) {
            case "dispatcher" -> new Main(user, shiftService).setVisible(true);
            case "cashier"    -> new Main(user, shiftService).setVisible(true);
            case "client"     -> new Client().setVisible(true);
            default -> JOptionPane.showMessageDialog(
                    null,
                    "Неизвестная роль пользователя"
            );
        }
    }

    private void openClient() {
        dispose();
        new Client().setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }

    // Стилизация элементов формы (повторяем подход из Main)
    private void styleFormLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(60, 60, 80));
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
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

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setMaximumSize(new Dimension(160, 38));
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);

        return btn;
    }
}
