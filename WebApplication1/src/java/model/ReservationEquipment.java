package model;

public class ReservationEquipment {
    private int id;
    private int reservationId;
    private int equipmentId;
    private int quantity;
    
    public ReservationEquipment() {}
    
    public ReservationEquipment(int reservationId, int equipmentId, int quantity) {
        this.reservationId = reservationId;
        this.equipmentId = equipmentId;
        this.quantity = quantity;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    
    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}