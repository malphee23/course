package database;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Модель таблицы с поддержкой редактирования и автоматического сохранения в БД
 */
public class EditableTableModel extends DefaultTableModel {
    
    private final String tableName;
    private final String sqlQuery;
    private final String[] columnNames;
    private Object[][] originalData; // Сохраняем оригинальные данные для отката при ошибке
    
    public EditableTableModel(String tableName, String sqlQuery, String[] columnNames) {
        this.tableName = tableName;
        this.sqlQuery = sqlQuery;
        this.columnNames = columnNames;
        loadData();
    }
    
    /**
     * Загружает данные из БД в модель
     */
    private void loadData() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            // Заголовки
            for (int i = 1; i <= columnCount; i++) {
                if (columnNames != null && i <= columnNames.length) {
                    addColumn(columnNames[i - 1]);
                } else {
                    addColumn(meta.getColumnLabel(i));
                }
            }
            
            // Данные
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            
            // Сохраняем оригинальные данные
            originalData = rows.toArray(new Object[0][]);
            
            // Добавляем строки в модель
            for (Object[] row : rows) {
                addRow(row);
            }
            
        } catch (SQLException e) {
            System.err.println("Ошибка при загрузке данных из БД: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем пустую модель с сообщением об ошибке
            addColumn("Ошибка");
            addRow(new Object[]{"Не удалось загрузить данные: " + e.getMessage()});
        }
    }
    
    /**
     * Переопределяем метод для определения редактируемости ячеек
     * Первая колонка (ключ) всегда нередактируема
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        // Первая колонка (ключ) нередактируема
        return column > 0;
    }
    
    /**
     * Сохраняет изменения в БД при редактировании ячейки
     */
    public boolean saveCellEdit(int row, int column, Object oldValue) {
        // Получаем все значения строки до изменения
        Object[] oldValues = new Object[getColumnCount()];
        for (int i = 0; i < getColumnCount(); i++) {
            if (i == column) {
                oldValues[i] = oldValue;
            } else {
                oldValues[i] = getValueAt(row, i);
            }
        }
        
        // Сохраняем изменения в БД
        boolean success = TableOperations.updateRecord(tableName, this, row, oldValues);
        
        if (!success) {
            // Если сохранение не удалось, восстанавливаем старое значение
            setValueAt(oldValue, row, column);
            return false;
        }
        
        return true;
    }
    
    /**
     * Удаляет строку из модели и БД
     */
    public boolean deleteRow(int rowIndex) {
        boolean success = TableOperations.deleteRecord(tableName, this, rowIndex);
        if (success) {
            removeRow(rowIndex);
        }
        return success;
    }
    
    /**
     * Перезагружает данные из БД
     */
    public void reload() {
        // Сохраняем количество колонок
        int columnCount = getColumnCount();
        
        // Очищаем только строки, не трогая колонки
        setRowCount(0);
        
        // Загружаем данные заново без повторного добавления колонок
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            
            // Данные
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            
            // Сохраняем оригинальные данные
            originalData = rows.toArray(new Object[0][]);
            
            // Добавляем строки в модель
            for (Object[] row : rows) {
                addRow(row);
            }
            
        } catch (SQLException e) {
            System.err.println("Ошибка при перезагрузке данных из БД: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обновляет конкретную ячейку в таблице (для вычисляемых полей)
     */
    public void updateCell(int row, int column, Object value) {
        if (row >= 0 && row < getRowCount() && column >= 0 && column < getColumnCount()) {
            setValueAt(value, row, column);
        }
    }
}
