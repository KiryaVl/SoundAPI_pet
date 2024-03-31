package org.example.GUI;

import org.example.Services.AudioClientHandler;
import org.example.Services.ClientManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/*
 * @author <KiryaVL>
 * @version 1.0
 */
public class ServerGUI extends JFrame {

    private ServerSocket audioServerSocket;
    private ServerSocket textServerSocket;
    private ArrayList<ClientManager> clients = new ArrayList<>();
    private ArrayList<AudioClientHandler> audioClients = new ArrayList<>();
    private JTextArea logArea;

    public ServerGUI() {
        initializeUI();
        try {
            audioServerSocket = new ServerSocket(12345); // Аудио-сокет
            textServerSocket = new ServerSocket(4500);  // Текстовый сокет
            log("Server started. Waiting for clients...");

            // Начинаем слушать подключения клиентов для аудио
            new Thread(this::acceptAudioClients).start();

            // Начинаем слушать подключения клиентов для текста
            new Thread(this::acceptTextClients).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Инициализация графической составляющей
     */
    private void initializeUI() {
        setTitle("Chat Server");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    /*
        Принимаем подключение клиентов для аудио
     */
    private void acceptAudioClients() {
        try {
            while (true) {
                Socket audioSocket = audioServerSocket.accept();
                log("New audio client connected: " + audioSocket.getInetAddress());
                System.out.println(audioSocket);

                // Создаем обработчик аудио и добавляем его в список
                AudioClientHandler audioHandler = new AudioClientHandler(audioSocket);
                audioClients.add(audioHandler);

                // Запускаем обработчик аудио в отдельном потоке
                new Thread(audioHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        Принимаем подключение клиентов для текста
     */
    private void acceptTextClients() {
        try {
            while (true) {
                Socket textSocket = textServerSocket.accept();
                log("New text client connected: " + textSocket.getInetAddress());

                // Создаем обработчик клиента и добавляем его в список
                ClientManager clientHandler = new ClientManager(textSocket);
                clients.add(clientHandler);

                // Запускаем обработчик клиента в отдельном потоке
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Запускаем сервер
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
    }
}
