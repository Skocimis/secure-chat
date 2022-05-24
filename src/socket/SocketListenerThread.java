package socket;

import model.Identifier;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;

public class SocketListenerThread implements Runnable {
    protected Socket socket;
    HashMap<Identifier, List<Function<Serializable, Object>>> callbacks;
    HashMap<Identifier, List<Function<Serializable, Object>>> callbacksOnce;
    PrintWriter printWriter;
    private boolean running;

    public SocketListenerThread(Socket socket) throws IOException {
        running = true;
        this.socket = socket;
        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        callbacks = new HashMap<>();
        callbacksOnce = new HashMap<>();
    }

    private void putInMap(HashMap<Identifier, List<Function<Serializable, Object>>> map, Identifier identifier, Function<Serializable, Object> callback) {
        List<Function<Serializable, Object>> callableList = map.get(identifier);
        if (callableList == null) {
            callableList = new ArrayList<>();
        }
        callableList.add(callback);
        map.put(identifier, callableList);
    }

    public int on(Identifier identifier, Function<Serializable, Object> callback) {
        putInMap(callbacks, identifier, callback);
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

    public int once(Identifier identifier, Function<Serializable, Object> callback) {
        putInMap(callbacksOnce, identifier, callback);
        return 0;
    }

    public int emit(Identifier identifier, Serializable data) {
        if (!isRunning()) return -1;
        printWriter.println(identifier.toString());
        try {
            printWriter.println(toStringS(data));
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
                System.out.println("DISCONNECTED");
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
            List<Function<Serializable, Object>> callableList = callbacks.get(identifier);
            List<Function<Serializable, Object>> callableListOnce = callbacksOnce.get(identifier);
            if (callableListOnce != null) {
                for (Function<Serializable, Object> func : callableListOnce) {
                    func.apply((Serializable) data);
                }
                callbacksOnce.remove(identifier);
            }
            if (callableList != null) {
                for (Function<Serializable, Object> func : callableList) {
                    func.apply((Serializable) data);
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
