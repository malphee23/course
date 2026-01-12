package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class authorization {
    public static UserSession authenticate(String login, String password) {

        String sql = """
                SELECT login,
                       role,
                       cashier_number,
                       cashdesk_number
                FROM "Users"
                WHERE login = ? AND password = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Number cashier = (Number) rs.getObject("cashier_number");
                Number cashdesk = (Number) rs.getObject("cashdesk_number");

                return new UserSession(
                        rs.getString("login"),
                        rs.getString("role"),
                        cashier != null ? cashier.intValue() : null,
                        cashdesk != null ? cashdesk.intValue() : null
                );
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при авторизации: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
