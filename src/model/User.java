package model;

import java.net.Socket;

public class User {
    private Socket socket;

    public User(Socket socket){
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
