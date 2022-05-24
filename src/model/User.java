package model;

import socket.MySocket;

import java.io.Serializable;
import java.util.HashMap;
import java.util.function.Function;

public class User {
    private MySocket socket;
    private final String name;
    public static HashMap<String, User> list = new HashMap<>();
    private Room room;
    private UserState state;//0 - connectedd

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
        Function<Serializable, Object> chatListeneer = (username) -> {
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
                if (!affirmative) return chatReplyError(-2);

                state = UserState.IN_CHAT;
                otherUser.room = room;
                otherUser.state = UserState.IN_CHAT;
                System.out.println("CET POCEO");

                socket.emit(Identifier.CHAT_INIT, otherUser.name);
                otherUser.socket.emit(Identifier.CHAT_INIT, name);
                return 0;
            });
            return 0;
        };
        Function<Serializable, Object> messageListener = (data) -> {
            if (state != UserState.IN_CHAT) return 0;
            if (!(data instanceof String message)) return 0;
            room.sendMessage(this, message);
            return 0;
        };
        Function<Serializable, Object> cancelListener = (data) -> {
            if (state != UserState.SENT_REQUEST) return socket.emit(Identifier.CANCELED, -1);
            room.cancelRequest(this);
            socket.emit(Identifier.CANCELED, 0);
            return 0;
        };
        Function<Serializable, Object> userListListener = (data) -> {
            //System.out.println("ZELI USERLIST");
            String userList = "";
            for (String s : list.keySet()) {
                userList += s + "\n";
            }
            //System.out.println("USERLIST JE NA SERVERU "+userList);
            socket.emit(Identifier.USER_LIST, userList);
            return 0;
        };
        Function<Serializable, Object> endListener = (data) -> {
            room.end();
            return 0;
        };
        Function<Serializable, Object> exitListener = (data) -> {
            list.remove(name);
            return 0;
        };
        Function<Serializable, Object> emptyListener = (data) -> {

            return 0;
        };

        socket.on(Identifier.CHAT, chatListeneer);
        socket.on(Identifier.SEND_MESSAGE, messageListener);
        socket.on(Identifier.CANCEL, cancelListener);
        socket.on(Identifier.REQUEST_USER_LIST, userListListener);
        socket.on(Identifier.END, endListener);
        socket.on(Identifier.EXIT, exitListener);
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
}
