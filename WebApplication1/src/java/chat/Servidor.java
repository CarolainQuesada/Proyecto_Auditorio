package chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor extends Thread {

    public static Vector usuarios = new Vector<>();

    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(8000);
            System.out.println("Servidor Activo...");
        } catch (IOException ioe) {
            System.out.println("Puerto ocupado: " + ioe);
            System.exit(1);
        }
   
        JFramePanelControl jfpc = new JFramePanelControl();
        jfpc.setVisible(true);
        
        System.out.println("Esperando conexiones...");
       
        while (true) {
            try {
                Socket socket = server.accept();
                Flujo flujo = new Flujo(socket);
                Thread t = new Thread(flujo);
                t.start();
            } catch (IOException ioe) {
                System.out.println("Error al aceptar conexión: " + ioe);
            }
        }
    }
    
    public static void eliminarUsuario(){
        System.out.println("Usuarios: ");
        for (int i = 0; i < usuarios.size(); i++) {
            Flujo f = (Flujo) usuarios.get(i);
            System.out.println("Nombre: "+ f.getNombre());
            
        }
    
    }
}
