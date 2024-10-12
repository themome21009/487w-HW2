CREATE DATABASE IF NOT EXISTS car_rental;
USE car_rental;

CREATE TABLE IF NOT EXISTS Reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    driver_name VARCHAR(100) NOT NULL,
    car_type ENUM('Sedan', 'SUV', 'Pick-up', 'Van') NOT NULL,
    check_out DATETIME NOT NULL,
    return_date DATETIME NOT NULL,
    extended_return_date DATETIME,
    total_charge DECIMAL(10,2),
    status ENUM('Reserved', 'Checked Out', 'Returned') DEFAULT 'Reserved'
);