package model;

import socket.MySocket;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.function.Function;

public class User {
    private MySocket socket;
    private final String name;
    public static HashMap<String, User> list = new HashMap<>();
    private Room room;
    private UserState state;//0 - connected
    private PublicKey publicKey;

    public User(MySocket socket, String name) throws Exception {
        if (list.get(name) != null) {
            throw new Exception();
        }
        this.name = name;
        this.socket = socket;
        this.room = null;
        this.state = UserState.CONNECTED;
        list.put(name, this);
        initListeners();
    }

    int chatReplyError(Integer errorCode) {
        socket.emit(Identifier.CHAT_REPLY, errorCode);
        state = UserState.CONNECTED;
        room = null;
        return 0;
    }

    private void initListeners() {
        Function<Object, Object> chatListeneer = (username) -> {
            if (state != UserState.CONNECTED) return chatReplyError(-3);//Nije u lobiju
            if (!(username instanceof String otherUserName)) return chatReplyError(-1); //Lose prosledjen string
            User otherUser = list.get(otherUserName);
            if (otherUser == null || otherUser == this) return chatReplyError(-1);//Korisnik ne postoji
            if (otherUser.state != UserState.CONNECTED) return chatReplyError(-4);//Drugi korisnik zauzet
            Room room = new Room(this, otherUser);
            this.room = room;
            otherUser.state = UserState.INCOMING_REQUEST;
            state = UserState.SENT_REQUEST;
            otherUser.socket.emit(Identifier.CHAT_REQUEST, name);
            otherUser.socket.once(Identifier.CHAT_REQUEST_REPLY, (data) -> {
                if (!(data instanceof Boolean affirmative)) return chatReplyError(-2);//Nece da prihvati
                if (!affirmative) {
                    otherUser.state = UserState.CONNECTED;
                    return chatReplyError(-2);
                }
                System.out.println("SALJEM CHAT INIT");

                state = UserState.IN_CHAT;
                otherUser.room = room;
                otherUser.state = UserState.IN_CHAT;


                socket.emit(Identifier.CHAT_INIT, otherUser.getPublicKey());
                otherUser.socket.emit(Identifier.CHAT_INIT, getPublicKey());
                System.out.println("POSLAO CHAT INIT "+otherUser.getPublicKey()+" "+getPublicKey());
                return 0;
            });
            return 0;
        };
        Function<Object, Object> messageListener = (data) -> {
            if (state != UserState.IN_CHAT) return 0;
            //if (!(data instanceof String message)) return 0;
            room.sendMessage(this, (byte[]) data);
            return 0;
        };
        Function<Object, Object> cancelListener = (data) -> {
            if (state != UserState.SENT_REQUEST) return socket.emit(Identifier.CANCELED, -1);
            room.cancelRequest(this);
            socket.emit(Identifier.CANCELED, 0);
            return 0;
        };
        Function<Object, Object> userListListener = (data) -> {
            String userList = "";
            for (String s : list.keySet()) {
                userList += s + "\n";
            }
            userList = userList.substring(0, userList.length()-1);
            socket.emit(Identifier.USER_LIST, userList);
            return 0;
        };
        Function<Object, Object> endListener = (data) -> {
            room.end();
            return 0;
        };
        Function<Object, Object> exitListener = (data) -> {
            list.remove(name);
            return 0;
        };
        Function<Object, Object> disconnectListener = (data) -> {
            System.out.println("Disconnected " + name);
            list.remove(name);
            return 0;
        };
        Function<Object, Object> publicKeyListener = (data) -> {
            System.out.println("DATA JE "+data);
            if(!(data instanceof PublicKey publicKey)) return 0;
            setPublicKey(publicKey);
            return 0;
        };
        Function<Object, Object> emptyListener = (data) -> {

            return 0;
        };

        socket.on(Identifier.CHAT, chatListeneer);
        socket.on(Identifier.SEND_MESSAGE, messageListener);
        socket.on(Identifier.CANCEL, cancelListener);
        socket.on(Identifier.REQUEST_USER_LIST, userListListener);
        socket.on(Identifier.END, endListener);
        socket.on(Identifier.EXIT, exitListener);
        socket.on(Identifier.PUBLIC_KEY, publicKeyListener);
        socket.on(Identifier.DISCONNECT, disconnectListener);
    }

    public MySocket getSocket() {
        return socket;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public UserState getState() {
        return state;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setSocket(MySocket socket) {
        this.socket = socket;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
