package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

public class MyServerSocket {
    Function<MySocket, Object> onConnectionListener;

    public MyServerSocket(int port, Function<MySocket, Object> onConnectionListener) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        this.onConnectionListener = onConnectionListener;
        while (true){
            Socket a = serverSocket.accept();
            onConnectionListener.apply(new MySocket(new SocketListenerThread(a)));
        }
    }
}
