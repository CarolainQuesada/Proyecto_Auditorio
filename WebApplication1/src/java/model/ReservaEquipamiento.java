package model;

public class ReservaEquipamiento {

    private int id;

    private int idReserva;
    private int idEquipamiento;

    private int cantidad;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }

    public int getIdEquipamiento() { return idEquipamiento; }
    public void setIdEquipamiento(int idEquipamiento) { this.idEquipamiento = idEquipamiento; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}