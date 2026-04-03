package service;

import concurrency.Calendario;
import concurrency.CapacidadControl;
import java.util.List;
import model.Reserva;

public class ReservaService {

    private ReservaDAO dao = new ReservaDAO();

    public String crearReserva(String fecha, String horaInicio, String horaFin, int cantidad) {

        if (fecha == null || fecha.isEmpty()) return "error_fecha";
        if (horaInicio.compareTo(horaFin) >= 0) return "hora";
        if (cantidad <= 0) return "cantidad";

        Calendario.lock(); 

        try {

           
            List<Reserva> existentes = dao.obtenerPorFecha(fecha);

            for (Reserva r : existentes) {

                if (horaInicio.compareTo(r.getHoraFin()) < 0 &&
                    horaFin.compareTo(r.getHoraInicio()) > 0) {
                    return "ocupado";
                }
            }

          
            if (!CapacidadControl.adquirir(cantidad)) {
                return "ocupado";
            }

         
            dao.crear(fecha, horaInicio, horaFin, cantidad);

            return "Reserva creada";

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        } finally {
            Calendario.unlock();
        }
    }

    public String confirmarReserva(int id) {

        dao.confirmar(id);

        return "confirmada";
    }

    public String eliminarReserva(int id) {

        Calendario.lock();

        try {

            Reserva r = dao.obtenerPorId(id);

            if (r != null) {
                CapacidadControl.liberar(r.getCantidad());
                dao.eliminar(id);
            }

            return "eliminada";

        } finally {
            Calendario.unlock();
        }
    }

  public String listarReservas() {

    StringBuilder sb = new StringBuilder();

    List<Reserva> lista = dao.obtenerTodas();

    for (Reserva r : lista) {

        sb.append(r.getId()).append(",")
          .append(r.getFecha()).append(",")
          .append(r.getHoraInicio()).append(",")
          .append(r.getHoraFin()).append(",")
          .append(r.getCantidad()).append(",")
          .append(r.getEstado())
          .append("|");
    }

    return sb.toString();
}
}