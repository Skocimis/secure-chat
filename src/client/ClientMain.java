package client;

import socket.MySocket;
import socket.SocketListenerThread;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public ClientMain() throws IOException {
        MySocket mySocket = new MySocket("localhost", 2011);
        mySocket.once("Reply", (data)->{
            System.out.println("Got reply" + data);
            return 0;
        });
        Scanner scanner = new Scanner(System.in);
        String input = "";
        while (mySocket.isRunning()&&!input.equalsIgnoreCase("kraj")){
            input = scanner.nextLine();
            System.out.println("SALJEM "+input);
            mySocket.emit("message", input);
        }
    }

    public static void main(String[] args) {
        try{
            new ClientMain();
        }
        catch (Exception e){

        }
    }
}
