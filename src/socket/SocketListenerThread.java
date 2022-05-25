package socket;

import model.Identifier;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;

public class SocketListenerThread implements Runnable {
    protected Socket socket;
    HashMap<Identifier, List<Function<Object, Object>>> callbacks;
    HashMap<Identifier, List<Function<Object, Object>>> callbacksOnce;
    List<Function<Object, Object>> onDisconnect;
    PrintWriter printWriter;
    private boolean running;
    private Object disconnectData;

    public SocketListenerThread(Socket socket) throws IOException {
        running = true;
        this.socket = socket;
        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        onDisconnect = new ArrayList<>();
        callbacks = new HashMap<>();
        callbacksOnce = new HashMap<>();
        disconnectData = null;
    }

    private void putInMap(HashMap<Identifier, List<Function<Object, Object>>> map, Identifier identifier, Function<Object, Object> callback) {
        List<Function<Object, Object>> callableList = map.get(identifier);
        if (callableList == null) {
            callableList = new ArrayList<>();
        }
        callableList.add(callback);
        map.put(identifier, callableList);
    }

    public int on(Identifier identifier, Function<Object, Object> callback) {
        if(identifier==Identifier.DISCONNECT){
            onDisconnect.add(callback);
        }
        else{
            putInMap(callbacks, identifier, callback);
        }
        return 0;
    }

    public int removeAllListeners(Identifier identifier) {
        callbacksOnce.remove(identifier);
        callbacks.remove(identifier);
        return 0;
    }

    private static Object fromStringS(String s) throws IOException,
            ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    private static String toStringS(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public int once(Identifier identifier, Function<Object, Object> callback) {
        putInMap(callbacksOnce, identifier, callback);
        return 0;
    }

    public int emit(Identifier identifier, Object data) {
        if (!isRunning()) return -1;
        printWriter.println(identifier.toString());
        if(!(data instanceof Serializable serializable)) return -1;
        try {
            printWriter.println(toStringS(serializable));
        } catch (Exception e) {
            System.out.println("GRESKA PRILIKOM SERIJALIZACIJE");
        }
        //Acknowledge
        return 0;
    }

    @Override
    public void run() {
        try {
            runMain();
        } catch (Exception e) {
            if (e.getMessage().equals("Connection reset")) {
                for(Function<Object, Object> onDisconnectFunction : onDisconnect){
                    onDisconnectFunction.apply(disconnectData);
                }
                running = false;
            } else {
                e.printStackTrace();
            }
        }
    }

    protected void runMain() throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String identifierString = "";
        Identifier identifier = Identifier.EMPTY;
        String dataString = "";
        Object data = null;
        while (running && identifier != Identifier.DISCONNECT) {
            try {
                identifierString = bufferedReader.readLine();
                identifier = Identifier.valueOf(identifierString);
                dataString = bufferedReader.readLine();
                data = fromStringS(dataString);
            } catch (NoSuchElementException e) {
                System.out.println("Disconnected");
            }
            List<Function<Object, Object>> callableList = callbacks.get(identifier);
            List<Function<Object, Object>> callableListOnce = callbacksOnce.get(identifier);
            if (callableListOnce != null) {
                for (Function<Object, Object> func : callableListOnce) {
                    func.apply(data);
                }
                callbacksOnce.remove(identifier);
            }
            if (callableList != null) {
                for (Function<Object, Object> func : callableList) {
                    func.apply(data);
                }
            }

        }
    }

    public void setDisconnectData(Object disconnectData) {
        this.disconnectData = disconnectData;
    }

    public Object getDisconnectData() {
        return disconnectData;
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
