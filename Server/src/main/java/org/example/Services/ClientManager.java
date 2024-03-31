package org.example.Services;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
/*
Класс для прослушивания и отображения текстовых сообщения в чате
 */

public class ClientManager implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    public boolean flag = true;
    public static ArrayList<ClientManager> clients = new ArrayList<>();

    /**
     * Конструктор Менеджера текстовых клиентов
     *
     * @param socket
     * @throws IOException
     */
    public ClientManager(Socket socket) throws IOException {
        this.socket = socket;
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        name = bufferedReader.readLine();
        clients.add(this);
        System.out.println(name + " подключился к чату.");
        broadcastMessage("Server: " + name + " подключился к чату.");
    }

    /*
     * Метод удаление клиента из списка
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    @Override
    public void run() {
        String messageFromClient;
        try {
            while (socket.isConnected()) {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    closeEverything();
                    break;
                }
                if (isPrivateMessage(messageFromClient)) {
                    processPrivateMessage(messageFromClient);
                } else {
                    broadcastMessage(messageFromClient);
                }
            }
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Метод трансляции текстовых сообщений в чат все кроме отправителя
     *
     * @param message
     */
    private void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            try {
                client.bufferedWriter.write(message);
                client.bufferedWriter.newLine();
                client.bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything();
            }
        }
    }

    /**
     * Метод проверки сообщения на "приватность"
     * @param message
     * @return
     */
    private boolean isPrivateMessage(String message) {
        return message.contains("/");
    }

    /**
     * Метод обработки приватных сообщений
     * @param message
     */
    private void processPrivateMessage(String message) {
        String[] parts1 = message.split("/", 2);
        String[] parts2 = parts1[1].split(" ");
        if (parts2.length > 1) {
            String recipient = parts2[0];
            StringBuffer messageBuffer = new StringBuffer();
            for (int i = 1; i < parts2.length; i++) {
                messageBuffer.append(parts2[i] + " ");
            }
            String content = String.valueOf(messageBuffer);
            for (ClientManager client : clients) {
                if (client.name.equals(recipient)) {
                    try {
                        client.bufferedWriter.write(name + " (private): " + content);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    } catch (IOException e) {
                        closeEverything();
                    }
                    return;
                }
            }
            broadcastMessage(message);
        }
    }

    /**
     * Метод удаления клиента из списка
     */
    private void closeEverything() {
        for (ClientManager client : clients) {
            if (client.name.equals(name)) {
                client.removeClient();
            }
        }
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public static ArrayList<ClientManager> getClients() {
        return clients;
    }
}