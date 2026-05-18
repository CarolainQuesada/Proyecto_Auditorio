CREATE DATABASE IF NOT EXISTS auditorio
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE auditorio;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('ADMIN', 'CLIENT') NOT NULL DEFAULT 'CLIENT'
);

CREATE TABLE IF NOT EXISTS equipment (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  total_quantity INT NOT NULL,
  available_quantity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user VARCHAR(150) NOT NULL,
  date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  quantity INT NOT NULL,
  status ENUM('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_reservations_user
    FOREIGN KEY (user) REFERENCES users(email)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS reservation_equipment (
  id INT AUTO_INCREMENT PRIMARY KEY,
  reservation_id INT NOT NULL,
  equipment_id INT NOT NULL,
  quantity INT NOT NULL,

  CONSTRAINT fk_reservation_equipment_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_reservation_equipment_equipment
    FOREIGN KEY (equipment_id) REFERENCES equipment(id)
    ON DELETE RESTRICT,

  CONSTRAINT uq_reservation_equipment
    UNIQUE (reservation_id, equipment_id)
);

CREATE TABLE IF NOT EXISTS blocked_days (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATE NOT NULL UNIQUE,
  motivo VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS log (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user VARCHAR(150) NOT NULL,
  action VARCHAR(100) NOT NULL,
  description TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO equipment (id, name, total_quantity, available_quantity) VALUES
  (1, 'PROYECTOR', 2, 2),
  (2, 'MICROFONO', 5, 5),
  (3, 'SONIDO', 3, 3)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  total_quantity = VALUES(total_quantity),
  available_quantity = VALUES(available_quantity);

INSERT INTO users (email, password, role) VALUES
  ('admin@una.ac.cr', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role);

CREATE INDEX idx_reservations_date ON reservations(date);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_user ON reservations(user);
CREATE INDEX idx_reservation_equipment_reservation_id ON reservation_equipment(reservation_id);
CREATE INDEX idx_log_created_at ON log(created_at);
CREATE INDEX idx_log_user ON log(user);
CREATE INDEX idx_log_action ON log(action);
