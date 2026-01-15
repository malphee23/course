package database;

import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Класс для выполнения операций обновления и удаления записей в таблицах БД
 */
public class TableOperations {
    
    /**
     * Обновляет запись в таблице на основе названия таблицы и данных строки
     */
    public static boolean updateRecord(String tableName, DefaultTableModel model, int rowIndex, Object[] oldValues) {
        try (Connection conn = DBConnection.getConnection()) {
            switch (tableName) {
                case "Авиакомпании" -> {
                    return updateAirlines(conn, model, rowIndex, oldValues);
                }
                case "Кассы" -> {
                    return updateCashDesk(conn, model, rowIndex, oldValues);
                }
                case "Кассиры" -> {
                    return updateCashier(conn, model, rowIndex, oldValues);
                }
                case "Клиенты" -> {
                    return updateClient(conn, model, rowIndex, oldValues);
                }
                case "Билеты" -> {
                    return updateTicket(conn, model, rowIndex, oldValues);
                }
                case "Купоны" -> {
                    return updateCoupon(conn, model, rowIndex, oldValues);
                }
                default -> {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении записи: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Удаляет запись из таблицы на основе названия таблицы и данных строки
     */
    public static boolean deleteRecord(String tableName, DefaultTableModel model, int rowIndex) {
        try (Connection conn = DBConnection.getConnection()) {
            switch (tableName) {
                case "Авиакомпании" -> {
                    return deleteAirlines(conn, model, rowIndex);
                }
                case "Кассы" -> {
                    return deleteCashDesk(conn, model, rowIndex);
                }
                case "Кассиры" -> {
                    Integer cashierNumber = (Integer) model.getValueAt(rowIndex, 0);
                    if (cashierNumber == null) return false;
                    return deleteCashier(conn, cashierNumber);
                }
                case "Клиенты" -> {
                    return deleteClient(conn, model, rowIndex);
                }
                case "Билеты" -> {
                    return deleteTicket(conn, model, rowIndex);
                }
                case "Купоны" -> {
                    return deleteCoupon(conn, model, rowIndex);
                }
                default -> {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении записи: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ========== Методы обновления для каждой таблицы ==========
    
    private static boolean updateAirlines(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        // Получаем значения с правильным преобразованием типов
        Object airlineCodeObj = model.getValueAt(rowIndex, 0);
        String airlineCode = airlineCodeObj != null ? airlineCodeObj.toString().trim() : null;
        
        Object nameObj = model.getValueAt(rowIndex, 1);
        String name = nameObj != null ? nameObj.toString().trim() : null;
        
        Object cityObj = model.getValueAt(rowIndex, 2);
        String city = cityObj != null ? cityObj.toString().trim() : null;
        
        Object streetObj = model.getValueAt(rowIndex, 3);
        String street = streetObj != null ? streetObj.toString().trim() : null;
        
        Object houseObj = model.getValueAt(rowIndex, 4);
        String house = houseObj != null ? houseObj.toString().trim() : null;
        
        // Валидация
        if (airlineCode == null || airlineCode.isEmpty() ||
            name == null || name.isEmpty() ||
            city == null || city.isEmpty() ||
            street == null || street.isEmpty() ||
            house == null || house.isEmpty()) {
            throw new SQLException("Все поля должны быть заполнены");
        }
        
        String sql = "UPDATE \"Airlines\" SET name = ?, city = ?, street = ?, house = ? WHERE airline_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, city.trim());
            ps.setString(3, street.trim());
            ps.setString(4, house.trim());
            ps.setString(5, airlineCode.trim());
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean updateCashDesk(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        Integer cashdeskNumber = convertToInteger(model.getValueAt(rowIndex, 0));
        Object cityObj = model.getValueAt(rowIndex, 1);
        String city = cityObj != null ? cityObj.toString().trim() : null;
        Object streetObj = model.getValueAt(rowIndex, 2);
        String street = streetObj != null ? streetObj.toString().trim() : null;
        Object houseObj = model.getValueAt(rowIndex, 3);
        String house = houseObj != null ? houseObj.toString().trim() : null;
        
        // Валидация
        if (cashdeskNumber == null ||
            city == null || city.isEmpty() ||
            street == null || street.isEmpty() ||
            house == null || house.isEmpty()) {
            throw new SQLException("Все поля должны быть заполнены");
        }
        
        String sql = "UPDATE \"CashDesk\" SET city = ?, street = ?, house = ? WHERE cashdesk_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, city.trim());
            ps.setString(2, street.trim());
            ps.setString(3, house.trim());
            ps.setInt(4, cashdeskNumber);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean updateCashier(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        Integer cashierNumber = convertToInteger(model.getValueAt(rowIndex, 0));
        Object lastNameObj = model.getValueAt(rowIndex, 1);
        String lastName = lastNameObj != null ? lastNameObj.toString().trim() : null;
        Object firstNameObj = model.getValueAt(rowIndex, 2);
        String firstName = firstNameObj != null ? firstNameObj.toString().trim() : null;
        Object middleNameObj = model.getValueAt(rowIndex, 3);
        String middleName = middleNameObj != null ? middleNameObj.toString().trim() : null;
        
        // Валидация
        if (cashierNumber == null ||
            lastName == null || lastName.isEmpty() ||
            firstName == null || firstName.isEmpty()) {
            throw new SQLException("Фамилия и имя должны быть заполнены");
        }
        
        String sql = "UPDATE \"Cashier\" SET last_name = ?, first_name = ?, middle_name = ? WHERE cashier_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lastName.trim());
            ps.setString(2, firstName.trim());
            ps.setString(3, middleName != null ? middleName.trim() : null);
            ps.setInt(4, cashierNumber);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean updateClient(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        // Для клиентов паспорт - вычисляемое поле, нужно разобрать его
        String passportData = (String) model.getValueAt(rowIndex, 0);
        String lastName = (String) model.getValueAt(rowIndex, 1);
        String firstName = (String) model.getValueAt(rowIndex, 2);
        String middleName = (String) model.getValueAt(rowIndex, 3);
        
        // Валидация
        if (passportData == null || passportData.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty() ||
            firstName == null || firstName.trim().isEmpty()) {
            throw new SQLException("Паспорт, фамилия и имя должны быть заполнены");
        }
        
        // Разбираем паспорт (формат: "серия номер")
        String[] passportParts = passportData.trim().split("\\s+", 2);
        if (passportParts.length != 2) {
            throw new SQLException("Неверный формат паспорта. Используйте формат: серия номер");
        }
        
        String passportSeries = passportParts[0].trim();
        String passportNumber = passportParts[1].trim();
        
        // Получаем старые значения паспорта из oldValues
        String oldPassportData = (String) oldValues[0];
        String[] oldPassportParts = oldPassportData.trim().split("\\s+", 2);
        String oldPassportSeries = oldPassportParts[0].trim();
        String oldPassportNumber = oldPassportParts[1].trim();
        
        String sql = "UPDATE \"Client\" SET passport_series = ?, passport_number = ?, last_name = ?, first_name = ?, middle_name = ? WHERE passport_series = ? AND passport_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passportSeries);
            ps.setString(2, passportNumber);
            ps.setString(3, lastName.trim());
            ps.setString(4, firstName.trim());
            ps.setString(5, middleName != null ? middleName.trim() : null);
            ps.setString(6, oldPassportSeries);
            ps.setString(7, oldPassportNumber);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean updateTicket(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        // Получаем значения с правильным преобразованием типов
        Object ticketNumberObj = model.getValueAt(rowIndex, 0);
        Integer ticketNumber = convertToInteger(ticketNumberObj);
        
        Object airlineCodeObj = model.getValueAt(rowIndex, 1);
        String airlineCode = airlineCodeObj != null ? airlineCodeObj.toString().trim() : null;
        
        Object cashdeskNumberObj = model.getValueAt(rowIndex, 2);
        Integer cashdeskNumber = convertToInteger(cashdeskNumberObj);
        
        Object cashierNumberObj = model.getValueAt(rowIndex, 3);
        Integer cashierNumber = convertToInteger(cashierNumberObj);
        
        Object ticketTypeObj = model.getValueAt(rowIndex, 4);
        String ticketType = ticketTypeObj != null ? ticketTypeObj.toString().trim() : null;
        
        Object saleDateObj = model.getValueAt(rowIndex, 5);
        Object priceObj = model.getValueAt(rowIndex, 6);
        
        // Валидация
        if (ticketNumber == null || airlineCode == null || airlineCode.isEmpty() ||
            cashdeskNumber == null || cashierNumber == null ||
            ticketType == null || ticketType.isEmpty() ||
            saleDateObj == null || priceObj == null) {
            throw new SQLException("Все поля должны быть заполнены");
        }
        
        Date saleDate;
        if (saleDateObj instanceof Date) {
            saleDate = (Date) saleDateObj;
        } else if (saleDateObj instanceof String) {
            try {
                saleDate = Date.valueOf(LocalDate.parse((String) saleDateObj));
            } catch (Exception e) {
                throw new SQLException("Неверный формат даты. Используйте формат: ГГГГ-ММ-ДД");
            }
        } else if (saleDateObj instanceof java.util.Date) {
            saleDate = new Date(((java.util.Date) saleDateObj).getTime());
        } else {
            throw new SQLException("Неверный формат даты");
        }
        
        java.math.BigDecimal price;
        if (priceObj instanceof java.math.BigDecimal) {
            price = (java.math.BigDecimal) priceObj;
        } else if (priceObj instanceof Number) {
            price = java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue());
        } else {
            try {
                price = new java.math.BigDecimal(priceObj.toString().trim());
            } catch (NumberFormatException e) {
                throw new SQLException("Неверный формат цены");
            }
        }
        
        String sql = "UPDATE \"Ticket\" SET airline_code = ?, cashdesk_number = ?, cashier_number = ?, ticket_type = ?, sale_date = ?, price = ? WHERE ticket_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, airlineCode.trim());
            ps.setInt(2, cashdeskNumber);
            ps.setInt(3, cashierNumber);
            ps.setString(4, ticketType.trim());
            ps.setDate(5, saleDate);
            ps.setBigDecimal(6, price);
            ps.setInt(7, ticketNumber);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean updateCoupon(Connection conn, DefaultTableModel model, int rowIndex, Object[] oldValues) throws SQLException {
        // Получаем значения с правильным преобразованием типов
        Object couponNumberObj = model.getValueAt(rowIndex, 0);
        Integer couponNumber = convertToInteger(couponNumberObj);
        
        Object ticketNumberObj = model.getValueAt(rowIndex, 1);
        Integer ticketNumber = convertToInteger(ticketNumberObj);
        
        Object passportDataObj = model.getValueAt(rowIndex, 2);
        String passportData = passportDataObj != null ? passportDataObj.toString().trim() : null;
        
        Object departurePointObj = model.getValueAt(rowIndex, 3);
        String departurePoint = departurePointObj != null ? departurePointObj.toString().trim() : null;
        
        Object arrivalPointObj = model.getValueAt(rowIndex, 4);
        String arrivalPoint = arrivalPointObj != null ? arrivalPointObj.toString().trim() : null;
        
        Object tariffObj = model.getValueAt(rowIndex, 5);
        String tariff = tariffObj != null ? tariffObj.toString().trim() : null;
        
        // Валидация
        if (couponNumber == null || ticketNumber == null ||
            passportData == null || passportData.isEmpty() ||
            departurePoint == null || departurePoint.isEmpty() ||
            arrivalPoint == null || arrivalPoint.isEmpty() ||
            tariff == null || tariff.isEmpty()) {
            throw new SQLException("Все поля должны быть заполнены");
        }
        
        String sql = "UPDATE \"Coupon\" SET ticket_number = ?, passport_data = ?, departure_point = ?, arrival_point = ?, tariff = ? WHERE coupon_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketNumber);
            ps.setString(2, passportData.trim());
            ps.setString(3, departurePoint.trim());
            ps.setString(4, arrivalPoint.trim());
            ps.setString(5, tariff.trim());
            ps.setInt(6, couponNumber);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    // ========== Методы удаления для каждой таблицы ==========
    
    private static boolean deleteAirlines(Connection conn, DefaultTableModel model, int rowIndex) throws SQLException {
        Object airlineCodeObj = model.getValueAt(rowIndex, 0);
        String airlineCode = airlineCodeObj != null ? airlineCodeObj.toString().trim() : null;
        if (airlineCode == null || airlineCode.isEmpty()) return false;
        
        String sql = "DELETE FROM \"Airlines\" WHERE airline_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, airlineCode);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean deleteCashDesk(Connection conn, DefaultTableModel model, int rowIndex) throws SQLException {
        Integer cashdeskNumber = convertToInteger(model.getValueAt(rowIndex, 0));
        if (cashdeskNumber == null) return false;
        
        String sql = "DELETE FROM \"CashDesk\" WHERE cashdesk_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cashdeskNumber);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean deleteCashier(Connection conn, int cashierNumber) throws SQLException {
        String sql = "DELETE FROM \"Cashier\" WHERE cashier_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cashierNumber);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean deleteClient(Connection conn, DefaultTableModel model, int rowIndex) throws SQLException {
        String passportData = (String) model.getValueAt(rowIndex, 0);
        if (passportData == null || passportData.trim().isEmpty()) return false;
        
        // Разбираем паспорт
        String[] passportParts = passportData.trim().split("\\s+", 2);
        if (passportParts.length != 2) return false;
        
        String passportSeries = passportParts[0].trim();
        String passportNumber = passportParts[1].trim();
        
        String sql = "DELETE FROM \"Client\" WHERE passport_series = ? AND passport_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passportSeries);
            ps.setString(2, passportNumber);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean deleteTicket(Connection conn, DefaultTableModel model, int rowIndex) throws SQLException {
        Integer ticketNumber = convertToInteger(model.getValueAt(rowIndex, 0));
        if (ticketNumber == null) return false;
        
        String sql = "DELETE FROM \"Ticket\" WHERE ticket_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketNumber);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    private static boolean deleteCoupon(Connection conn, DefaultTableModel model, int rowIndex) throws SQLException {
        Integer couponNumber = convertToInteger(model.getValueAt(rowIndex, 0));
        if (couponNumber == null) return false;
        
        String sql = "DELETE FROM \"Coupon\" WHERE coupon_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponNumber);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Преобразует объект в Integer, обрабатывая различные типы данных
     */
    private static Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
