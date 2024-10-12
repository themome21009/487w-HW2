// MainMenu.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("Main Menu");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // Create buttons
        JButton driverButton = new JButton("Driver Reservation Form");
        JButton adminButton = new JButton("Admin View");

        // Add action listener for Driver Reservation Form button
        driverButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new DriverReservationForm();
                dispose(); // Close the Main Menu window
            }
        });

        // Add action listener for Admin View button
        adminButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new AdminView();
                dispose(); // Close the Main Menu window
            }
        });

        // Create a panel to hold the buttons
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1, 10, 10)); // 2 rows, 1 column, spacing
        panel.add(driverButton);
        panel.add(adminButton);

        // Add panel to the frame
        add(panel);

        // Make the window visible
        setVisible(true);
    }
}
