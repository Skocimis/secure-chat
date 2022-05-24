package server;

import model.User;
import socket.SocketListenerThread;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class ServerSocketListenerThread extends SocketListenerThread {
    private User user;

    public ServerSocketListenerThread(User user) throws IOException {
        super(user.getSocket());
        this.user = user;
    }
}
