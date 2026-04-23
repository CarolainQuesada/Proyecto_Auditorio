package model;

import java.util.ArrayList;
import java.util.List;

public class Reservation {

    private int id;
    private String user;
    private String date;
    private String startTime;
    private String endTime;
    private int quantity;
    private String status;
    private long timestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getEquipmentType() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    private List<ReservationEquipment> equipments;
    
    public List<ReservationEquipment> getEquipments() {
        if (equipments == null) equipments = new ArrayList<>();
        return equipments;
    }
    
    public void setEquipments(List<ReservationEquipment> equipments) {
        this.equipments = equipments;
    }
    
    public void addEquipment(ReservationEquipment equipment) {
        getEquipments().add(equipment);
    }
}