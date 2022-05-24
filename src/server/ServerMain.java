package server;

import model.User;
import socket.MyServerSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public ServerMain() throws IOException {
        MyServerSocket myServerSocket = new MyServerSocket(2011, (mySocket)->{

            mySocket.on("message", (data)->{
                System.out.println("MESSAGE GOT: " + data);
                return 0;
            });

            mySocket.once("message", (data)->{
                System.out.println("MESSAGE GOT: " + data);
                mySocket.emit("Reply", "Juhu");
                return 0;
            });

            return 0;
        });
    }

    public static void main(String[] args) {
        try {
            new ServerMain();
        }
        catch (Exception e){
            System.out.println("HERE ERROR");
        }
    }
}
