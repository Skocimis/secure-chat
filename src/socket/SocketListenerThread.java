package socket;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class SocketListenerThread implements Runnable {
    protected Socket socket;
    HashMap<String, List<Function<Serializable, Object>>> callbacks;
    HashMap<String, List<Function<Serializable, Object>>> callbacksOnce;
    PrintWriter printWriter;
    private boolean running;

    public SocketListenerThread(Socket socket)  throws  IOException{
        running = true;
        this.socket = socket;
        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        callbacks = new HashMap<>();
        callbacksOnce = new HashMap<>();
    }
    private void putInMap(HashMap<String, List<Function<Serializable, Object>>> map, String identifier, Function<Serializable, Object> callback){
        List<Function<Serializable, Object>> callableList = map.get(identifier);
        if(callableList==null){
            callableList = new ArrayList<>();
        }
        callableList.add(callback);
        map.put(identifier, callableList);
    }
    public int on(String identifier, Function<Serializable, Object> callback){
        putInMap(callbacks, identifier, callback);
        return  0;
    }
    public int once(String identifier, Function<Serializable, Object> callback){
        putInMap(callbacksOnce, identifier, callback);
        return  0;
    }
    public int emit(String identifier, Serializable data){
        if(!isRunning()) return -1;
        printWriter.println(identifier);
        printWriter.println(data);
        //Acknowledge
        return  0;
    }
    @Override
    public void run() {
        try {
            runMain();
        }
        catch (Exception e){
            if(e.getMessage().equals("Connection reset")){
                System.out.println("DISCONNECTED");
                running = false;
            }
            else {
                e.printStackTrace();
            }
        }

    }
    protected void runMain() throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String identifier = "";
        String data = "";
        while (running&&!identifier.equalsIgnoreCase("disconnect")){
            try {
                identifier = bufferedReader.readLine();
                data = bufferedReader.readLine();
            }
            catch (NoSuchElementException e){
                System.out.println("Disconnected");
            }
            List<Function<Serializable, Object>> callableList = callbacks.get(identifier);
            List<Function<Serializable, Object>> callableListOnce = callbacksOnce.get(identifier);
            if(callableListOnce!=null){
                for(Function<Serializable, Object> func:callableListOnce){
                    func.apply(data);
                }
                callbacksOnce.remove(identifier);
            }
            if(callableList!=null){
                for(Function<Serializable, Object> func:callableList){
                    func.apply(data);
                }
            }

        }
    }

    public boolean isRunning() {
        return running;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
