import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class ChatServer {
    private static final int PORT = 12345;
    private static List<Socket> clientSockets = new ArrayList<>();
    private static List<String> clientNicknames = new ArrayList<>();
    private static List<String> messages = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor de chat iniciado. Esperando conexiones en el puerto " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter outputWriter;
        private BufferedReader inputReader;
        private String clientNickname;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // Solicitar el nickname al cliente
                outputWriter.println("Por favor, ingresa tu nombre:");
                clientNickname = inputReader.readLine();
                clientNicknames.add(clientNickname);

                // Saludar al cliente
                outputWriter.println("Bienvenido al chat, " + clientNickname + "! Puedes empezar a enviar mensajes y archivos.");

                // Enviar mensajes previos y archivos a este cliente
                for (String message : messages) {
                    outputWriter.println(message);
                }

                // Leer mensajes y comandos del cliente y transmitirlos a todos los clientes conectados
                String clientMessage;
                boolean isFileTransfer = false;
                String fileName = "";
                String sender = "";
                FileOutputStream fileOutputStream = null;
                while ((clientMessage = inputReader.readLine()) != null) {
                    if (isFileTransfer) {
                        if (clientMessage.equals("/endfile")) {
                            isFileTransfer = false;
                            fileOutputStream.close();
                            broadcastFileTransferMessage(fileName, sender);
                            fileName = "";
                            sender = "";
                        } else {
                            byte[] fileBytes = Base64.getDecoder().decode(clientMessage);
                            fileOutputStream.write(fileBytes);
                        }
                    } else if (clientMessage.equals("/file")) {
                        isFileTransfer = true;
                        fileName = inputReader.readLine();
                        sender = inputReader.readLine();
                        fileOutputStream = new FileOutputStream(fileName);
                    } else {
                        String formattedMessage = formatMessage(clientNickname, clientMessage);
                        messages.add(formattedMessage);
                        System.out.println(formattedMessage);

                        // Transmitir el mensaje a todos los clientes
                        for (Socket socket : clientSockets) {
                            PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                            socketWriter.println(formattedMessage);
                        }

                        // Guardar la conversación en un archivo
                        saveConversation();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientSockets.remove(clientSocket);
                clientNicknames.remove(clientNickname);
                System.out.println("Cliente desconectado: " + clientSocket.getInetAddress().getHostAddress());
            }
        }

        private String formatMessage(String sender, String message) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            return "[" + timestamp + "] " + sender + ": " + message;
        }

        private void broadcastFileTransferMessage(String fileName, String sender) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            String message = "[" + timestamp + "] " + sender + " ha enviado un archivo: " + fileName;

            for (Socket socket : clientSockets) {
                try {
                    PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                    socketWriter.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void saveConversation() {
            try {
                PrintWriter writer = new PrintWriter("conversation.txt");
                for (String message : messages) {
                    writer.println(message);
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
