package chat;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

//ActionListener para poder dar enter 
public class Cliente extends Frame implements ActionListener {

    static Socket socket = null;
    static DataInputStream EntradaSocket;
    static DataOutputStream SalidaSocket;
    static TextField salida;
    static TextArea entrada;

    static JList<String> jList;
    static DefaultListModel<String> listModel;
    static String texto;
    static String name;

    public Cliente(String nombreUsuario) {
        name = nombreUsuario;
        setTitle("Chat - " + name);
        setSize(500, 300);
        setLayout(new BorderLayout());
        
        salida = new TextField(30);
        salida.addActionListener(this);

        entrada = new TextArea();
        entrada.setEditable(false);

        listModel = new DefaultListModel<>();
        jList = new JList<>(listModel);
        jList.setPreferredSize(new Dimension(120, 0));

    
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = jList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String selected = jList.getSelectedValue();
                    if (selected != null && selected.equals(name)) {
                        jList.clearSelection(); 
                    } else if (e.getClickCount() == 2) {
                        jList.clearSelection(); 
                        entrada.append("[Ahora estás escribiendo en el chat público]\n");
                    }
                }
            }
        });

        add(jList, BorderLayout.WEST);
        add(salida, BorderLayout.SOUTH);
        add(entrada, BorderLayout.CENTER);

        setVisible(true);
    }

    public static void main(String[] args) {
        try {
            String nombre = JOptionPane.showInputDialog(null, "Ingrese su nombre:", "Nombre de usuario", JOptionPane.PLAIN_MESSAGE);

            if (nombre == null || nombre.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Debe ingresar un nombre para conectarse.");
                System.exit(0);
            }

            Cliente cliente = new Cliente(nombre);
            socket = new Socket("10.251.48.105", 8000);
            EntradaSocket = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            SalidaSocket = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            SalidaSocket.writeUTF(nombre);
            SalidaSocket.flush();

            while (true) {
                String linea = EntradaSocket.readUTF();

                if (linea.startsWith("private:")) {
                    cliente.entrada.append("[Privado] " + linea.substring(8) + "\n");
                    continue;
                }

                if (linea.startsWith("message:")) {
                    cliente.entrada.append(linea.substring(8) + "\n");
                    continue;
                }

                if (linea.startsWith("[USERS]")) {
                    String[] users = linea.substring(8).split(",");
                    listModel.clear();
                    for (String user : users) {
                        listModel.addElement(user.trim());
                    }
                    continue;
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar al servidor.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        texto = salida.getText();
        salida.setText("");
        try {
            String selectedUser = jList.getSelectedValue();

            if (selectedUser != null && !selectedUser.equals(name)) {
                SalidaSocket.writeUTF("private:" + selectedUser + ":" + texto);
                entrada.append("[Privado a " + selectedUser + "] " + texto + "\n");
            } else {
                SalidaSocket.writeUTF("message:" + name + ": " + texto);
            }
            SalidaSocket.flush();
        } catch (IOException ioe) {
            entrada.append("Error al enviar mensaje.\n");
        }
    }

    @Override
    public boolean handleEvent(Event e) {
        if ((e.target == this) && (e.id == Event.WINDOW_DESTROY)) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    System.out.println("Error: " + ioe);
                }
                this.dispose();
            }
        }
        return true;
    }
}
