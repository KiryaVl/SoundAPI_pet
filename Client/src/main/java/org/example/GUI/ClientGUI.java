package org.example.GUI;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/*
 * @author <KiryaVL>
 * @version 1.0
 */

public class ClientGUI extends JFrame {

    private Socket audioSocket;
    private Socket textSocket;
    private BufferedReader textBufferedReader;
    private BufferedWriter textBufferedWriter;
    private String name;

    private JTextArea chatArea;
    private JTextField messageField;

    private boolean isMicrophoneMuted;
    TargetDataLine line;
    private JButton muteButton;
    private static InetAddress inetAddress;

    public ClientGUI(String name, Socket audioSocket, Socket textSocket) {
        this.name = name;
        this.audioSocket = audioSocket;
        this.textSocket = textSocket;

        // Инициализация GUI
        initializeUI();

        try {
            textBufferedReader = new BufferedReader(new InputStreamReader(textSocket.getInputStream()));
            textBufferedWriter = new BufferedWriter(new OutputStreamWriter(textSocket.getOutputStream()));

            // Отправляем имя на сервер при подключении
            sendMessage(name);

            // Начинаем слушать входящие сообщения
            Thread messagesThread = new Thread(this::listenForMessages);
            messagesThread.start();

            // Начинаем слушать входящие аудио
            Thread listeningThread = new Thread(this::startAudioListening);
            listeningThread.start();

            // Начинаем отправлять аудио
            Thread sendingThread = new Thread(this::sendAudio);
            sendingThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.addActionListener(e -> {
            String message = messageField.getText();
            sendMessage(name + ": " + message);
            messageField.setText("");
        });

        muteButton = new JButton("Mute");
        muteButton.addActionListener(e -> toggleMicrophone());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(messageField, BorderLayout.SOUTH);
        getContentPane().add(muteButton, BorderLayout.NORTH);
    }

    /**
     * Метод отправки сообщения
     * @param message
     */
    private void sendMessage(String message) {
        try {
            textBufferedWriter.write(message + "\n");
            textBufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отображения сообщений
     */
    private void listenForMessages() {
        try {
            String message;
            while ((message = textBufferedReader.readLine()) != null) {
                chatArea.append(message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отключения микрофона
     */
    private void toggleMicrophone() {
        if (isMicrophoneMuted) {
            muteButton.setText("Unmuted");
            muteButton.setBackground(Color.RED);
            stopAudioRecording();
        } else {
            muteButton.setText("Mute");
            muteButton.setBackground(null);
            startAudioListening();
        }
        if (!isMicrophoneMuted) {
            isMicrophoneMuted = true;
        } else {
            isMicrophoneMuted = false;
        }
    }

    /**
     * Метод воспроизведения аудио
     */
    private void startAudioListening() {
        try {
            byte[] buffer = new byte[1024];
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            BufferedInputStream in = new BufferedInputStream(audioSocket.getInputStream());
            line.open(format);
            line.start();
            int count;
            while ((count = in.read(buffer)) != -1) {
                System.out.println(this);
                line.write(buffer, 0, count);
            }
            line.drain();
            line.close();
        } catch (LineUnavailableException |IOException e) {
            line.drain();
            line.close();
        }
    }

    /**
     * Метод записи аудио
     */
    private void sendAudio() {
        try {
            byte[] buffer = new byte[1024];
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            try (Socket socket = audioSocket;
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
                while (true) {
                    int count = line.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        out.write(buffer, 0, count);
                    }
                }

            }
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Метод остановки записи аудио
     */
    private void stopAudioRecording() {
        if (line != null) {
            line.close();
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Enter your name:");
        String address = JOptionPane.showInputDialog("Enter address:");
        try {
            inetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        try {
            Socket audioSocket = new Socket(inetAddress, 12345); // Подключение к серверу для аудио
            Socket textSocket = new Socket(inetAddress, 4500); // Подключение к серверу для текста
            SwingUtilities.invokeLater(() -> new ClientGUI(name, audioSocket, textSocket).setVisible(true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
