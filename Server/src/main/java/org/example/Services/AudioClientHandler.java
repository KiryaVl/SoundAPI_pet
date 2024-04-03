package org.example.Services;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/*
Класс для прослушивания и записи аудио потока
 */
public class AudioClientHandler implements Runnable {

    /*
     * Сокет подключение аудио клиента
     */
    private Socket socket;
    /*
     * Список подключенных клиентов
     */
    public static ArrayList<AudioClientHandler> clients = new ArrayList<>();
    /*
     * Выходящий аудио поток
     */
    private OutputStream outputStream;
    /*
     * Входящий аудио поток
     */
    private InputStream inputStream;

    /**
     * Конструктор класса с добавлением каждого аудио клиента в список
     *
     * @param socket Сокет подключение клиента
     * @throws IOException Ошибка ввода/вывода
     */
    public AudioClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        inputStream = new BufferedInputStream(socket.getInputStream());
        clients.add(this);
    }

    /**
     * Удаление клиента из списка подключенных клиентов
     */
    private void removeClient() {
        clients.remove(this);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запуск клиента
     */

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                line.write(buffer, 0,  bytesRead);
                broadcastAudio(buffer, bytesRead);
            }
            line.drain();
            line.close();
        } catch (IOException | LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            removeClient();
        }
    }

//            while (socket.isConnected()) {
//                // Создание и запуск аудио линии
//                byte[] buffer = new byte[1024];
//                AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
//                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
//                line.open(format);
//                line.start();
//                /*
//                 * Прослушивание линии входящего потока и запись данных в буфер
//                 */
//                int count;
//                while ((count = inputStream.read(buffer)) != -1) {
//                    line.write(buffer, 0, count);
//                    broadcastAudio(buffer);
//                }
//                line.drain();
//                line.close();
//            }
//        } catch (LineUnavailableException | IOException e) {
//            closeEverything();
//        }
    //   }

    //
//    /**
//     * Метод вещания, запись аудио в буфер и передача в OutputStream
//     * @param buffer
//     */
//    private void broadcastAudio(byte[] buffer) {
//        for (AudioClientHandler client : clients) {
//            if (!client.socket.equals(socket)) {
//                try {
//                    AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
//                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
//                    line.open(format);
//                    line.start();
//                    while (true) {
//                        int count = line.read(buffer, 0, buffer.length);
//                        if (count > 0) {
//                            client.outputStream.write(buffer, 0, count);
//                        }
//                    }
//                }catch (IOException | LineUnavailableException e) {
//                    closeEverything();
//                }
//            }
//        }
//    }

    /**
     * Метод вещания, запись аудио в буфер и передача в OutputStream
     * @param data
     * @param bytesRead
     */
    private void broadcastAudio(byte[] data, int bytesRead) {
        for (AudioClientHandler client : clients) {
            if (client != this) {
                try {
                    client.outputStream.write(data, 0, bytesRead);
                } catch (IOException e) {
                    closeEverything();
                }
            }
        }
    }

    /**
     * Закрытие всех подключенных клиентов
     */
    private void closeEverything() {
        for (AudioClientHandler client : clients) {
            if (client.equals(this)) {
                client.removeClient();
            }
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
