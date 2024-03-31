package org.example.Models;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.example.Services.AudioClientHandler;
import org.example.Services.ClientManager;

public class Server {

    //region Поля

    /**
     * Серверный сокет
     */
    private final ServerSocket serverSocket;

    //endregion


    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void runServer(){

        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("Подключен новый клиент!");
                AudioClientHandler audioClientHandler = new AudioClientHandler(socket);
                ClientManager clientManager = new ClientManager(socket);
                Thread thread = new Thread(clientManager);
                Thread secondaryThread = new Thread(audioClientHandler);
                thread.start();
                secondaryThread.start();
            }
        }
        catch (IOException e){
            closeSocket();
        }

    }

    private void closeSocket(){
        try{
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
