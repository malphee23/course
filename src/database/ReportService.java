package database;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    private final Path downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads");

    public void printTicket(int ticketNumber) {
        String sql = """
                SELECT t.ticket_number,
                       t.airline_code,
                       t.cashdesk_number,
                       t.cashier_number,
                       t.ticket_type,
                       t.sale_date,
                       t.price,
                       c.passport_data,
                       cl.last_name,
                       cl.first_name,
                       cl.middle_name
                FROM "Ticket" t
                LEFT JOIN "Coupon" c ON c.ticket_number = t.ticket_number
                LEFT JOIN "Client" cl ON cl.passport_data = c.passport_data
                WHERE t.ticket_number = ?
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ticketNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showError("Билет №" + ticketNumber + " не найден");
                    return;
                }

                List<String> lines = new ArrayList<>();
                lines.add("Номер билета: " + rs.getInt("ticket_number"));
                lines.add("Паспортные данные: " + safe(rs.getString("passport_data")));
                lines.add("Владелец: " + fio(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")));
                lines.add("Шифр авиакомпании: " + safe(rs.getString("airline_code")));
                lines.add("Номер кассы: " + rs.getInt("cashdesk_number"));
                lines.add("Табельный номер кассира: " + rs.getInt("cashier_number"));
                lines.add("Тип билета: " + safe(rs.getString("ticket_type")));
                lines.add("Дата продажи: " + safe(rs.getString("sale_date")));
                lines.add("Стоимость: " + format(rs.getBigDecimal("price")));

                Path file = downloadsDir.resolve("ticket_" + ticketNumber + ".pdf");
                createPdf(file, "Билет №" + ticketNumber, lines);
                showInfo("PDF сохранён: " + file);
            }

        } catch (SQLException | IOException e) {
            showError("Не удалось сформировать билет: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void printCoupon(int couponNumber) {
        String sql = """
                SELECT cp.coupon_number,
                       cp.ticket_number,
                       cp.passport_data,
                       cp.flight_direction,
                       cp.tariff,
                       cl.last_name,
                       cl.first_name,
                       cl.middle_name
                FROM "Coupon" cp
                LEFT JOIN "Client" cl ON cl.passport_data = cp.passport_data
                WHERE cp.coupon_number = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, couponNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showError("Купон №" + couponNumber + " не найден");
                    return;
                }

                List<String> lines = new ArrayList<>();
                lines.add("Номер купона: " + rs.getInt("coupon_number"));
                lines.add("Номер билета: " + rs.getInt("ticket_number"));
                lines.add("Паспортные данные: " + safe(rs.getString("passport_data")));
                lines.add("Владелец: " + fio(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name")));
                lines.add("Направление полета: " + safe(rs.getString("flight_direction")));
                lines.add("Тариф: " + safe(rs.getString("tariff")));

                Path file = downloadsDir.resolve("coupon_" + couponNumber + ".pdf");
                createPdf(file, "Купон №" + couponNumber, lines);
                showInfo("PDF сохранён: " + file);
            }

        } catch (SQLException | IOException e) {
            showError("Не удалось сформировать купон: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void printByPassport(String passportData) {
        String sql = """
                SELECT t.ticket_number,
                       t.airline_code,
                       t.cashdesk_number,
                       t.cashier_number,
                       t.ticket_type,
                       t.sale_date,
                       t.price,
                       cp.coupon_number,
                       cp.departure_point,
                       cp.arrival_point,
                       cp.tariff,
                       cl.last_name,
                       cl.first_name,
                       cl.middle_name
                FROM "Coupon" cp
                JOIN "Ticket" t ON t.ticket_number = cp.ticket_number
                JOIN "Client" cl ON cl.passport_data = cp.passport_data
                WHERE cp.passport_data = ?
                ORDER BY t.ticket_number
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passportData);

            try (ResultSet rs = ps.executeQuery()) {
                List<String> lines = new ArrayList<>();
                String fio = null;

                while (rs.next()) {
                    if (fio == null) {
                        fio = fio(rs.getString("last_name"), rs.getString("first_name"), rs.getString("middle_name"));
                        lines.add("Паспорт: " + passportData);
                        lines.add("Владелец: " + fio);
                        lines.add("------------------------------------");
                    }

                    lines.add("Билет №: " + rs.getInt("ticket_number"));
                    lines.add("Купон №: " + rs.getInt("coupon_number"));
                    lines.add("Шифр авиакомпании: " + safe(rs.getString("airline_code")));
                    lines.add("Номер кассы: " + rs.getInt("cashdesk_number"));
                    lines.add("Табельный номер кассира: " + rs.getInt("cashier_number"));
                    lines.add("Тип билета: " + safe(rs.getString("ticket_type")));
                    lines.add("Дата продажи: " + safe(rs.getString("sale_date")));
                    lines.add("Стоимость: " + format(rs.getBigDecimal("price")));
                    lines.add("Маршрут: " + safe(rs.getString("departure_point")) + " → " + safe(rs.getString("arrival_point")));
                    lines.add("Тариф: " + safe(rs.getString("tariff")));
                    lines.add("------------------------------------");
                }

                if (lines.isEmpty()) {
                    showError("По паспорту " + passportData + " ничего не найдено");
                    return;
                }

                Path file = downloadsDir.resolve("tickets_" + passportData.replace(" ", "_") + ".pdf");
                createPdf(file, "Билеты по паспорту", lines);
                showInfo("PDF сохранён: " + file);
            }

        } catch (SQLException | IOException e) {
            showError("Не удалось сформировать PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createPdf(Path file, String title, List<String> lines) throws IOException {
        Files.createDirectories(file.getParent());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDType0Font font = loadFont(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(font, 14);
                cs.beginText();
                cs.newLineAtOffset(50, 750);
                cs.showText(title);
                cs.endText();

                cs.setFont(font, 12);
                float y = 720;
                for (String line : lines) {
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 18;
                }
            }

            doc.save(file.toFile());
        }
    }

    private PDType0Font loadFont(PDDocument doc) throws IOException {
        String[] candidates = {
                "C:\\\\Windows\\\\Fonts\\\\arial.ttf",
                "C:\\\\Windows\\\\Fonts\\\\tahoma.ttf",
                "C:\\\\Windows\\\\Fonts\\\\times.ttf"
        };
        for (String path : candidates) {
            Path p = Paths.get(path);
            if (Files.exists(p)) {
                return PDType0Font.load(doc, p.toFile());
            }
        }
        // Резерв: встроенный LiberationSans из ресурсов pdfbox
        var stream = ReportService.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");
        if (stream != null) {
            return PDType0Font.load(doc, stream);
        }
        throw new IOException("Не удалось загрузить шрифт для PDF");
    }

    private String fio(String last, String first, String middle) {
        StringBuilder sb = new StringBuilder();
        if (last != null && !last.isBlank()) sb.append(last).append(" ");
        if (first != null && !first.isBlank()) sb.append(first).append(" ");
        if (middle != null && !middle.isBlank()) sb.append(middle);
        return sb.toString().trim();
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }

    private String format(BigDecimal val) {
        return val == null ? "" : val.toPlainString();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Готово", JOptionPane.INFORMATION_MESSAGE);
    }

    public void printReportTable(String reportType, String reportTitle, String reportParams, TableModel model) throws IOException {
        Path file = downloadsDir.resolve("report_" + reportType.replace(" ", "_") + "_" + 
                System.currentTimeMillis() + ".pdf");

        Files.createDirectories(file.getParent());

        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = loadFont(doc);
            
            float margin = 50;
            float y = 750;
            float lineHeight = 18;
            int columnCount = model.getColumnCount();
            int rowCount = model.getRowCount();
            float totalWidth = 500;
            float columnWidth = totalWidth / columnCount;
            float[] columnWidths = new float[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnWidths[i] = columnWidth;
            }

            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream cs = null;

            try {
                cs = new PDPageContentStream(doc, page);

                // Заголовок отчёта
                cs.setFont(font, 16);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(reportTitle);
                cs.endText();
                y -= lineHeight * 1.5f;

                // Параметры фильтра
                if (reportParams != null && !reportParams.isEmpty()) {
                    cs.setFont(font, 12);
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Параметры: " + reportParams);
                    cs.endText();
                    y -= lineHeight * 1.5f;
                }

                // Дата генерации
                String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                cs.setFont(font, 10);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Дата создания: " + genDate);
                cs.endText();
                y -= lineHeight * 1;

                // Рисуем заголовки таблицы
                float x = margin;
                cs.setFont(font, 11);
                cs.setLineWidth(1);
                for (int col = 0; col < columnCount; col++) {
                    String header = model.getColumnName(col);
                    cs.beginText();
                    cs.newLineAtOffset(x + 5, y - 12);
                    cs.showText(truncateText(header, columnWidth - 10));
                    cs.endText();
                    
                    // Рамка ячейки
                    cs.moveTo(x, y);
                    cs.lineTo(x + columnWidths[col], y);
                    cs.lineTo(x + columnWidths[col], y - lineHeight);
                    cs.lineTo(x, y - lineHeight);
                    cs.closePath();
                    cs.stroke();
                    
                    x += columnWidths[col];
                }
                y -= lineHeight;

                // Рисуем данные
                cs.setFont(font, 10);
                for (int row = 0; row < rowCount; row++) {
                    if (y < 100) {
                        // Новая страница
                        if (cs != null) {
                            cs.close();
                        }
                        page = new PDPage();
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(font, 10);
                        y = 750;
                    }

                    x = margin;
                    for (int col = 0; col < columnCount; col++) {
                        Object value = model.getValueAt(row, col);
                        String cellText = value != null ? value.toString() : "";
                        
                        cs.beginText();
                        cs.newLineAtOffset(x + 5, y - 12);
                        cs.showText(truncateText(cellText, columnWidth - 10));
                        cs.endText();
                        
                        // Рамка ячейки
                        cs.setLineWidth(0.5f);
                        cs.moveTo(x, y);
                        cs.lineTo(x + columnWidths[col], y);
                        cs.lineTo(x + columnWidths[col], y - lineHeight);
                        cs.lineTo(x, y - lineHeight);
                        cs.closePath();
                        cs.stroke();
                        
                        x += columnWidths[col];
                    }
                    y -= lineHeight;
                }
            } finally {
                if (cs != null) {
                    cs.close();
                }
            }
            
            doc.save(file.toFile());
        }

        showInfo("PDF сохранён: " + file);
    }

    private String truncateText(String text, float maxWidth) {
        if (text == null) return "";
        // Простое усечение текста (в реальном приложении можно использовать более сложную логику)
        if (text.length() > 30) {
            return text.substring(0, 27) + "...";
        }
        return text;
    }
}
