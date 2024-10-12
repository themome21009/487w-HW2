// DriverReservationForm.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class DriverReservationForm extends JFrame {
    // GUI Components for Reservation
    private JTextField nameField;
    private JComboBox<String> carTypeBox;
    private JSpinner checkOutSpinner;
    private JSpinner returnDateSpinner;
    private JButton submitButton;

    // GUI Components for Extension
    private JTextField reservationIdField;
    private JSpinner newReturnDateSpinner;
    private JButton extendButton;

    public DriverReservationForm() {
        setTitle("Driver Reservation Form");
        setSize(750, 750);
        setLayout(new GridLayout(10, 4));

        // Initialize components for reservation
        nameField = new JTextField();
        carTypeBox = new JComboBox<>(new String[]{"Sedan", "SUV", "Pick-up", "Van"});

        // Date spinners
        SpinnerDateModel checkOutModel = new SpinnerDateModel(new java.util.Date(), null, null, Calendar.HOUR_OF_DAY);
        checkOutSpinner = new JSpinner(checkOutModel);
        JSpinner.DateEditor checkOutEditor = new JSpinner.DateEditor(checkOutSpinner, "MM/dd/yyyy HH:mm");
        checkOutSpinner.setEditor(checkOutEditor);

        SpinnerDateModel returnDateModel = new SpinnerDateModel(new java.util.Date(), null, null, Calendar.HOUR_OF_DAY);
        returnDateSpinner = new JSpinner(returnDateModel);
        JSpinner.DateEditor returnDateEditor = new JSpinner.DateEditor(returnDateSpinner, "MM/dd/yyyy HH:mm");
        returnDateSpinner.setEditor(returnDateEditor);

        submitButton = new JButton("Submit Reservation");

        // Initialize components for extension
        reservationIdField = new JTextField();

        SpinnerDateModel newReturnDateModel = new SpinnerDateModel(new java.util.Date(), null, null, Calendar.HOUR_OF_DAY);
        newReturnDateSpinner = new JSpinner(newReturnDateModel);
        JSpinner.DateEditor newReturnDateEditor = new JSpinner.DateEditor(newReturnDateSpinner, "MM/dd/yyyy HH:mm");
        newReturnDateSpinner.setEditor(newReturnDateEditor);

        extendButton = new JButton("Request Extension");

        // Add components to frame
        add(new JLabel("Driver Name:"));
        add(nameField);
        add(new JLabel("Car Type:"));
        add(carTypeBox);
        add(new JLabel("Check-Out Date:"));
        add(checkOutSpinner);
        add(new JLabel("Return Date:"));
        add(returnDateSpinner);
        add(new JLabel());
        add(submitButton);

        // Separator
        add(new JLabel("----- Extension Request -----"));
        add(new JLabel());

        add(new JLabel("Reservation ID:"));
        add(reservationIdField);
        add(new JLabel("New Return Date:"));
        add(newReturnDateSpinner);
        add(new JLabel());
        add(extendButton);

        // Action listeners
        submitButton.addActionListener(e -> submitReservation());

        extendButton.addActionListener(e -> requestExtension());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void submitReservation() {
        try {
            String driverName = nameField.getText().trim();
            String carType = carTypeBox.getSelectedItem().toString();

            java.util.Date checkOutDateUtil = (java.util.Date) checkOutSpinner.getValue();
            java.util.Date returnDateUtil = (java.util.Date) returnDateSpinner.getValue();

            // Convert java.util.Date to LocalDateTime
            LocalDateTime checkOutDate = convertToLocalDateTimeViaInstant(checkOutDateUtil);
            LocalDateTime returnDate = convertToLocalDateTimeViaInstant(returnDateUtil);

            // Check if reservation is made at least 24 hours in advance
            if (Duration.between(LocalDateTime.now(), checkOutDate).toHours() < 24) {
                JOptionPane.showMessageDialog(this, "Reservation must be made at least 24 hours in advance.");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            // Find an available vehicle of the requested type
            String findVehicleSql = "SELECT vehicle_id FROM Vehicles WHERE car_type = ? AND vehicle_id NOT IN (" +
                    "SELECT vehicle_id FROM Reservations WHERE (" +
                    "(check_out <= ? AND return_date >= ?) OR " +
                    "(check_out <= ? AND return_date >= ?))) LIMIT 1";

            PreparedStatement findVehicleStmt = conn.prepareStatement(findVehicleSql);
            findVehicleStmt.setString(1, carType);
            findVehicleStmt.setTimestamp(2, Timestamp.valueOf(returnDate));
            findVehicleStmt.setTimestamp(3, Timestamp.valueOf(checkOutDate));
            findVehicleStmt.setTimestamp(4, Timestamp.valueOf(returnDate));
            findVehicleStmt.setTimestamp(5, Timestamp.valueOf(checkOutDate));

            ResultSet rs = findVehicleStmt.executeQuery();

            if (rs.next()) {
                int vehicleId = rs.getInt("vehicle_id");

                // Calculate total charge
                BigDecimal totalCharge = calculateCharge(carType, checkOutDate, returnDate);

                // Insert reservation into database
                String sql = "INSERT INTO Reservations (driver_name, car_type, vehicle_id, check_out, return_date, total_charge) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, driverName);
                stmt.setString(2, carType);
                stmt.setInt(3, vehicleId);
                stmt.setTimestamp(4, Timestamp.valueOf(checkOutDate));
                stmt.setTimestamp(5, Timestamp.valueOf(returnDate));
                stmt.setBigDecimal(6, totalCharge);
                stmt.executeUpdate();

                // Get the generated reservation ID
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                int reservationId = 0;
                if (generatedKeys.next()) {
                    reservationId = generatedKeys.getInt(1);
                }

                JOptionPane.showMessageDialog(this, "Reservation successful!\nReservation ID: " + reservationId +
                        "\nVehicle ID: " + vehicleId + "\nTotal Charge: $" + totalCharge);
            } else {
                JOptionPane.showMessageDialog(this, "No available vehicles of the requested type for the selected dates.");
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void requestExtension() {
        try {
            int reservationId = Integer.parseInt(reservationIdField.getText().trim());

            java.util.Date newReturnDateUtil = (java.util.Date) newReturnDateSpinner.getValue();
            LocalDateTime newReturnDate = convertToLocalDateTimeViaInstant(newReturnDateUtil);

            Connection conn = DatabaseConnection.getConnection();

            // Get the reservation details
            String getReservationSql = "SELECT vehicle_id, return_date FROM Reservations WHERE reservation_id = ?";
            PreparedStatement getReservationStmt = conn.prepareStatement(getReservationSql);
            getReservationStmt.setInt(1, reservationId);
            ResultSet rs = getReservationStmt.executeQuery();

            if (rs.next()) {
                int vehicleId = rs.getInt("vehicle_id");
                LocalDateTime currentReturnDate = rs.getTimestamp("return_date").toLocalDateTime();

                // Check if the new return date is after the current return date
                if (!newReturnDate.isAfter(currentReturnDate)) {
                    JOptionPane.showMessageDialog(this, "New return date must be after the current return date.");
                    return;
                }

                // Check for overlapping reservations
                String checkOverlapSql = "SELECT COUNT(*) FROM Reservations WHERE vehicle_id = ? AND reservation_id != ? AND " +
                        "((check_out <= ? AND return_date >= ?) OR (check_out <= ? AND return_date >= ?))";
                PreparedStatement checkOverlapStmt = conn.prepareStatement(checkOverlapSql);
                checkOverlapStmt.setInt(1, vehicleId);
                checkOverlapStmt.setInt(2, reservationId);
                checkOverlapStmt.setTimestamp(3, Timestamp.valueOf(newReturnDate));
                checkOverlapStmt.setTimestamp(4, rs.getTimestamp("return_date"));
                checkOverlapStmt.setTimestamp(5, Timestamp.valueOf(newReturnDate));
                checkOverlapStmt.setTimestamp(6, rs.getTimestamp("return_date"));

                ResultSet overlapRs = checkOverlapStmt.executeQuery();
                overlapRs.next();
                int overlapCount = overlapRs.getInt(1);

                if (overlapCount == 0) {
                    // Update the reservation
                    String updateSql = "UPDATE Reservations SET return_date = ?, total_charge = ? WHERE reservation_id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setTimestamp(1, Timestamp.valueOf(newReturnDate));

                    // Recalculate total charge
                    String carType = getCarType(vehicleId, conn);
                    LocalDateTime checkOutDate = getCheckOutDate(reservationId, conn);
                    BigDecimal newTotalCharge = calculateCharge(carType, checkOutDate, newReturnDate);
                    updateStmt.setBigDecimal(2, newTotalCharge);
                    updateStmt.setInt(3, reservationId);
                    updateStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Extension granted!\nNew total charge: $" + newTotalCharge);
                } else {
                    JOptionPane.showMessageDialog(this, "Extension denied. The vehicle is reserved during the requested period.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Reservation not found.");
            }

            conn.close();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Reservation ID.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private String getCarType(int vehicleId, Connection conn) throws SQLException {
        String sql = "SELECT car_type FROM Vehicles WHERE vehicle_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, vehicleId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getString("car_type");
        }
        return null;
    }

    private LocalDateTime getCheckOutDate(int reservationId, Connection conn) throws SQLException {
        String sql = "SELECT check_out FROM Reservations WHERE reservation_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, reservationId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getTimestamp("check_out").toLocalDateTime();
        }
        return null;
    }

    public BigDecimal calculateCharge(String carType, LocalDateTime checkOut, LocalDateTime returnDate) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();

        // Get the rate per day from the database
        String rateSql = "SELECT rate_per_day FROM RentalRates WHERE car_type = ?";
        PreparedStatement rateStmt = conn.prepareStatement(rateSql);
        rateStmt.setString(1, carType);
        ResultSet rateRs = rateStmt.executeQuery();

        BigDecimal ratePerDay;
        if (rateRs.next()) {
            ratePerDay = rateRs.getBigDecimal("rate_per_day");
        } else {
            ratePerDay = BigDecimal.ZERO;
        }

        // Calculate the number of days (at least 1)
        long days = Duration.between(checkOut, returnDate).toDays();
        if (days == 0) {
            days = 1; // Minimum one day charge
        }

        BigDecimal totalCharge = ratePerDay.multiply(new BigDecimal(days));

        // Apply discount if rental is a week or longer
        if (days >= 7) {
            totalCharge = totalCharge.multiply(new BigDecimal("0.90")); // 10% discount
        }

        conn.close();
        return totalCharge.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime convertToLocalDateTimeViaInstant(java.util.Date dateToConvert) {
        return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
