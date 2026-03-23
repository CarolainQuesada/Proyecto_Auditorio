package chat;

import java.net.*;
import java.io.*;
import java.util.*;

public class Flujo extends Thread {

    Socket socket;
    DataInputStream FlujoLectura;
    DataOutputStream FlujoEscritura;
    private String nombre;

    public Flujo(Socket socketEntrada) {
        socket = socketEntrada;
        try {
            FlujoLectura = new DataInputStream(new BufferedInputStream(socketEntrada.getInputStream()));
            FlujoEscritura = new DataOutputStream(new BufferedOutputStream(socketEntrada.getOutputStream()));
        } catch (IOException ioe) {
            System.out.println("IOException(Flujo): " + ioe);
        }
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    public void run() {
        try {
            
            nombre = FlujoLectura.readUTF();
            Servidor.usuarios.add(this);
            enviarListaUsuarios(); 

            while (true) {
                String linea = FlujoLectura.readUTF();
                System.out.println("linea = " + linea);

                if (linea.startsWith("message:")) {
                    broadcast(linea);
                } else if (linea.startsWith("private:")) {
                    enviarMensajePrivado(linea); 
                }
            }

        } catch (IOException ioe) {
            Servidor.usuarios.removeElement(this);
            broadcast("message:" + getNombre() + " se ha desconectado.");
            enviarListaUsuarios(); 
        }
    }

    public void broadcast(String mensaje) {
        
        
        mensaje = mensaje.replace("liga", "********");
        
        
        synchronized (Servidor.usuarios) {
            for (Object o : Servidor.usuarios) {
                Flujo f = (Flujo) o;
                try {
                    synchronized (f.FlujoEscritura) {
                        f.FlujoEscritura.writeUTF(mensaje);
                        f.FlujoEscritura.flush();
                    }
                } catch (IOException ioe) {
                    System.out.println("Error al enviar mensaje: " + ioe);
                }
            }
        }
    }

    public void enviarListaUsuarios() {
        StringBuilder lista = new StringBuilder("[USERS],");
        synchronized (Servidor.usuarios) {
            for (Object o : Servidor.usuarios) {
                Flujo f = (Flujo) o;
                lista.append(f.getNombre()).append(",");
            }
        }

        
        if (lista.length() > 7) {
            lista.setLength(lista.length() - 1);
        }

        broadcast(lista.toString());
    }

    public void enviarMensajePrivado(String linea) {
        
        try {
            String[] partes = linea.split(":", 3);
            if (partes.length < 3) return;

            String destinatario = partes[1].trim();
            String mensaje = partes[2].trim();

            for (Object o : Servidor.usuarios) {
                Flujo f = (Flujo) o;
                if (f.getNombre().equals(destinatario)) {
                    f.FlujoEscritura.writeUTF("private:" + getNombre() + ": " + mensaje);
                    f.FlujoEscritura.flush();
                    break;
                }
            }

        } catch (IOException ioe) {
            System.out.println("Error al enviar mensaje privado: " + ioe);
        }
    }
}
