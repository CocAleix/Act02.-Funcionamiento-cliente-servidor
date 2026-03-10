package ACT02;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {

    // ── Configuración ──────────────────────────────────────
    static final int PUERTO = 5000;

    // Lista sincronizada de escritores activos (uno por cliente)
    private static final List<PrintWriter> clientesConectados =
            Collections.synchronizedList(new ArrayList<>());

    // ── Punto de entrada ───────────────────────────────────
    public static void main(String[] args) {
        System.out.println("║   Servidor de Chat Multicliente      ║");
        System.out.println("║   Puerto: " + PUERTO + "                       ║");

        // Pool de hilos ilimitado: crea uno nuevo por cada cliente
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[Servidor] Esperando conexiones...\n");

            // Bucle infinito: siempre listo para nuevos clientes
            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("[+] Nuevo cliente: " + socketCliente.getInetAddress()
                        + "  (clientes activos: " + (clientesConectados.size() + 1) + ")");
                pool.execute(new ManejadorCliente(socketCliente));
            }

        } catch (IOException e) {
            System.err.println("[ERROR Servidor] " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    // ── API pública (usada desde ManejadorCliente) ─────────

    /** Envía un mensaje a TODOS los clientes conectados. */
    static void broadcast(String mensaje) {
        synchronized (clientesConectados) {
            for (PrintWriter pw : clientesConectados) {
                pw.println(mensaje);
            }
        }
    }

    /** Añade un cliente a la lista de broadcast. */
    static void registrar(PrintWriter pw) {
        clientesConectados.add(pw);
    }

    /** Elimina un cliente de la lista cuando se desconecta. */
    static void desregistrar(PrintWriter pw) {
        clientesConectados.remove(pw);
    }

    // ══════════════════════════════════════════════════════════
    //  Hilo: gestiona la sesión completa de UN cliente
    // ══════════════════════════════════════════════════════════
    static class ManejadorCliente implements Runnable {

        private final Socket socket;

        ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            PrintWriter salida = null;

            try {
                // Flujos de comunicación con el cliente
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);

                // ── Registro del usuario ──────────────────────
                salida.println("  Bienvenido al Chat Multicliente ");
                salida.println("Introduce tu nombre de usuario:");

                String nombre = entrada.readLine();
                if (nombre == null || nombre.isBlank()) {
                    nombre = "Usuario-" + socket.getPort();
                }
                nombre = nombre.trim();

                // Añadir a la lista de broadcast
                Servidor.registrar(salida);

                System.out.println("[INFO] '" + nombre + "' se ha unido al chat.");
                Servidor.broadcast(">>> " + nombre + " se ha unido al chat <<<");
                salida.println("Escribe mensajes y pulsa ENTER. Escribe /salir para salir.");
                salida.println("──────────────────────────────────");

                // ── Bucle de mensajes ────────────────────────
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    linea = linea.trim();

                    if (linea.equalsIgnoreCase("/salir")) {
                        break;
                    }

                    if (linea.isEmpty()) continue;

                    // Comandos especiales
                    if (linea.equalsIgnoreCase("/usuarios")) {
                        salida.println("[Servidor] Clientes conectados: "
                                + clientesConectados.size());
                        continue;
                    }

                    String mensaje = "[" + nombre + "]: " + linea;
                    System.out.println(mensaje);
                    Servidor.broadcast(mensaje);
                }

                // ── Desconexión ──────────────────────────────
                Servidor.broadcast(">>> " + nombre + " ha abandonado el chat <<<");
                System.out.println("[-] '" + nombre + "' desconectado.");

            } catch (IOException e) {
                System.err.println("[ERROR cliente " + socket.getInetAddress() + "] " + e.getMessage());
            } finally {
                if (salida != null) Servidor.desregistrar(salida);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}