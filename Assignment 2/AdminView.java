// AdminView.java
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class AdminView extends JFrame {
    // DateTimeFormatter with new date format
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    public AdminView() {
        setTitle("Admin View - Reservation List");
        setSize(900, 400);

        // Fetch reservation data
        String[] columnNames = {"ID", "Driver Name", "Car Type", "Vehicle ID", "Check-Out", "Return Date", "Total Charge", "Status"};
        Object[][] data = getReservationData();

        // Create table
        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Set column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Driver Name
        table.getColumnModel().getColumn(2).setPreferredWidth(80);  // Car Type
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Vehicle ID
        table.getColumnModel().getColumn(4).setPreferredWidth(150); // Check-Out
        table.getColumnModel().getColumn(5).setPreferredWidth(150); // Return Date
        table.getColumnModel().getColumn(6).setPreferredWidth(100); // Total Charge
        table.getColumnModel().getColumn(7).setPreferredWidth(80);  // Status

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private Object[][] getReservationData() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT reservation_id, driver_name, car_type, vehicle_id, check_out, return_date, total_charge, status FROM Reservations";
            PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery();

            rs.last();
            int rowCount = rs.getRow();
            rs.beforeFirst();

            Object[][] data = new Object[rowCount][8];
            int i = 0;

            while (rs.next()) {
                data[i][0] = rs.getInt("reservation_id");
                data[i][1] = rs.getString("driver_name");
                data[i][2] = rs.getString("car_type");
                data[i][3] = rs.getInt("vehicle_id");
                data[i][4] = rs.getTimestamp("check_out").toLocalDateTime().format(DATE_TIME_FORMATTER);
                data[i][5] = rs.getTimestamp("return_date").toLocalDateTime().format(DATE_TIME_FORMATTER);
                data[i][6] = rs.getBigDecimal("total_charge");
                data[i][7] = rs.getString("status");
                i++;
            }

            conn.close();
            return data;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Object[0][0];
        }
    }
}
