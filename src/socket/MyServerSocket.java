package socket;

import model.User;
import server.ServerSocketListenerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

public class MyServerSocket {
    Function<MySocket, Object> onConnectionListener;
    ServerSocketListenerThread serverSocketListenerThread;


    public MyServerSocket(int port, Function<MySocket, Object> onConnectionListener) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        this.onConnectionListener = onConnectionListener;
        while (true){
            Socket a = serverSocket.accept();
            System.out.println("POVEZAN");
            onConnectionListener.apply(new MySocket(new SocketListenerThread(a)));
        }
    }
    /*public void onConnection(Function<MySocket, Object> onConnectionListener){
        System.out.println("MENJAM LIST");
        this.onConnectionListener = onConnectionListener;
    }*/

    public void setOnConnectionListener(Function<MySocket, Object> onConnectionListener) {
        System.out.println("SETOVAN ONCONNECTION LISTENER");
        this.onConnectionListener = onConnectionListener;
    }
}
