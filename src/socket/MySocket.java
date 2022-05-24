package socket;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Function;

public class MySocket {
    SocketListenerThread socketListenerThread;

    public MySocket(String address, int port) throws IOException {
        Socket socket = new Socket(address, port);
        this.socketListenerThread = new SocketListenerThread(socket);
        Thread listenerThread = new Thread(socketListenerThread);
        listenerThread.start();
    }
    public MySocket(SocketListenerThread socketListenerThread){
        this.socketListenerThread = socketListenerThread;
        Thread listenerThread = new Thread(socketListenerThread);
        listenerThread.start();
    }
    public int on(String identifier, Function<Serializable, Object> callback){
        return socketListenerThread.on(identifier, callback);
    }
    public int once(String identifier, Function<Serializable, Object> callback){
        return socketListenerThread.once(identifier, callback);
    }
    public int emit(String identifier, Serializable data){
        return socketListenerThread.emit(identifier, data);
    }


    public boolean isRunning() {
        return socketListenerThread.isRunning();
    }
}
