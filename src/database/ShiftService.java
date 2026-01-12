package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShiftService {
    private UserSession currentUser;

    public void openShift(UserSession user) {
        if (user == null || !"cashier".equals(user.getRole())) {
            return;
        }

        if (user.getCashdeskNumber() == null || user.getCashierNumber() == null) {
            System.err.println("Для кассира не заданы cashdesk_number или cashier_number.");
            return;
        }

        String insertSql = """
                INSERT INTO "Shift"(shift_number,
                                    cashdesk_number,
                                    cashier_number,
                                    shift_date,
                                    shift_time,
                                    status,
                                    event_type)
                VALUES (nextval('shift_number_seq'),
                        ?,
                        ?,
                        CURRENT_DATE,
                        CURRENT_TIME,
                        'открыта',
                        'start')
                RETURNING shift_number
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setInt(1, user.getCashdeskNumber());
            ps.setInt(2, user.getCashierNumber());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long shiftNumber = rs.getLong("shift_number");
                    user.setShiftNumber(shiftNumber);
                    this.currentUser = user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при открытии смены: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeShift() {
        if (currentUser == null || currentUser.getShiftNumber() == null) {
            return;
        }

        String insertSql = """
                INSERT INTO "Shift"(shift_number,
                                    cashdesk_number,
                                    cashier_number,
                                    shift_date,
                                    shift_time,
                                    status,
                                    event_type)
                SELECT ?, ?, ?, CURRENT_DATE, CURRENT_TIME, 'закрыта', 'end'
                WHERE NOT EXISTS (
                    SELECT 1 FROM "Shift"
                    WHERE shift_number = ? AND event_type = 'end'
                )
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setLong(1, currentUser.getShiftNumber());
            ps.setInt(2, currentUser.getCashdeskNumber());
            ps.setInt(3, currentUser.getCashierNumber());
            ps.setLong(4, currentUser.getShiftNumber());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии смены: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
