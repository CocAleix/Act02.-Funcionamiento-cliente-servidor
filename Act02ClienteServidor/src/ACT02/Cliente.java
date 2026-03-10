package ACT02;

import java.io.*;
import java.net.*;

public class Cliente {

    private static final String HOST  = "127.0.0.1";
    private static final int    PUERTO = 5000;

    public static void main(String[] args) {
        System.out.println("Cliente de Chat Multiusuario");

        System.out.println("Conectando a " + HOST + ":" + PUERTO + " ...");

        try (Socket socket = new Socket(HOST, PUERTO)) {

            System.out.println("¡Conectado!\n");

            PrintWriter    salida  = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

            // ── Hilo receptor ─────────────────────────────────────
            // Escucha los mensajes del servidor e imprime en pantalla.
            // Es un hilo daemon: se detiene automáticamente al cerrar el programa.
            Thread receptor = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        System.out.println(linea);
                    }
                } catch (IOException e) {
                    System.out.println("\n[Cliente] Conexión cerrada por el servidor.");
                }
            });
            receptor.setDaemon(true);
            receptor.start();

            // ── Hilo principal: lectura del teclado ───────────────
            // Lee lo que escribe el usuario y lo envía al servidor.
            String linea;
            while ((linea = teclado.readLine()) != null) {
                salida.println(linea);
                if (linea.trim().equalsIgnoreCase("/salir")) {
                    break;
                }
            }

            System.out.println("\n[Cliente] Desconectado. ¡Hasta pronto!");

        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo conectar al servidor en "
                    + HOST + ":" + PUERTO);
            System.err.println("        Asegúrate de que el servidor está en marcha.");
        }
    }
}