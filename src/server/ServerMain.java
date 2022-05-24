package server;

import model.Identifier;
import model.User;
import socket.MyServerSocket;

import java.io.IOException;

public class ServerMain {
    public ServerMain() throws IOException {
        MyServerSocket myServerSocket = new MyServerSocket(2011, (mySocket) -> {
            //mySocket.emit("askForUsername", null);
            mySocket.once(Identifier.CONNECT, (data) -> {
                if (!(data instanceof String name)) {
                    return mySocket.emit(Identifier.CONNECTED, false);
                }
                try {
                    System.out.println("TRUE");
                    new User(mySocket, name);
                    return mySocket.emit(Identifier.CONNECTED, true);
                } catch (Exception e) {
                    System.out.println("FALSE");
                    return mySocket.emit(Identifier.CONNECTED, false);
                }
            });
            return 0;
        });
    }

    public static void main(String[] args) {
        try {
            new ServerMain();
        } catch (Exception e) {
            System.out.println("HERE ERROR");
        }
    }
}
