import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String EXIT_COMMAND = "/exit";

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            PrintWriter outputWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            // Solicitar el nickname al cliente
            System.out.print("Ingresa tu nombre: ");
            String nickname = consoleReader.readLine();
            outputWriter.println(nickname);

            // Recibir mensajes del servidor y mostrarlos en la consola
            Thread messageReceiverThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = inputReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            messageReceiverThread.start();

            // Leer mensajes del usuario y enviarlos al servidor
            String clientMessage;
            while ((clientMessage = consoleReader.readLine()) != null) {
                if (clientMessage.equalsIgnoreCase(EXIT_COMMAND)) {
                    outputWriter.println(EXIT_COMMAND);
                    break;
                } else if (clientMessage.startsWith("/file")) {
                    String[] fileCommand = clientMessage.split(" ");
                    if (fileCommand.length > 1) {
                        String filePath = fileCommand[1];
                        sendFile(outputWriter, filePath, nickname);
                    } else {
                        System.out.println("Comando inválido. Por favor, especifica la ruta del archivo a enviar.");
                    }
                } else {
                    outputWriter.println(clientMessage);
                }
            }

            // Cerrar la conexión y terminar el cliente
            socket.close();
            System.out.println("Conexión cerrada. Cliente finalizado.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(PrintWriter outputWriter, String filePath, String sender) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Enviar el comando de inicio de transferencia de archivo al servidor
                outputWriter.println("/file");

                // Enviar el nombre del archivo y el remitente al servidor
                outputWriter.println(file.getName());
                outputWriter.println(sender);

                // Leer y enviar el contenido del archivo al servidor
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputWriter.println(Base64.getEncoder().encodeToString(buffer, 0, bytesRead));
                }
                fileInputStream.close();

                // Enviar el comando de finalización de transferencia de archivo al servidor
                outputWriter.println("/endfile");
            } else {
                System.out.println("El archivo especificado no existe.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
