package database;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class tableLoad {

    public static DefaultTableModel load(String sql) {
        return load(sql, null);
    }

    public static DefaultTableModel load(String sql, String[] columnNames) {

        DefaultTableModel model = new DefaultTableModel();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Заголовки
            for (int i = 1; i <= columnCount; i++) {
                if (columnNames != null && i <= columnNames.length) {
                    model.addColumn(columnNames[i - 1]);
                } else {
                    model.addColumn(meta.getColumnLabel(i));
                }
            }

            // Данные
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при загрузке данных из БД: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем пустую модель с сообщением об ошибке
            model.addColumn("Ошибка");
            model.addRow(new Object[]{"Не удалось загрузить данные: " + e.getMessage()});
        }

        return model;
    }
}
