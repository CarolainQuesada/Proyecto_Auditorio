CREATE INDEX idx_reservations_date ON reservations(date);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_user ON reservations(user);
CREATE INDEX idx_reservation_equipment_reservation_id ON reservation_equipment(reservation_id);
CREATE INDEX idx_log_created_at ON log(created_at);
CREATE INDEX idx_log_user ON log(user);
CREATE INDEX idx_log_action ON log(action);
