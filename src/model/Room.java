package model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private List<User> users;

    public Room(User u1, User u2) {
        users = new ArrayList<>();
        users.add(u1);
        users.add(u2);
    }

    public void sendMessage(User sender, byte[] message) {
        for (User user : users) {
            if (user == sender) continue;
            user.getSocket().emit(Identifier.NEW_MESSAGE, message);
        }
    }

    public void end() {
        for (User user : users) {
            user.setState(UserState.CONNECTED);
            user.setRoom(null);
            user.getSocket().emit(Identifier.CHAT_ENDED, 0);
        }
    }

    public void cancelRequest(User sender) {
        for (User user : users) {
            user.setState(UserState.CONNECTED);
            user.setRoom(null);
            if (user != sender) {
                user.getSocket().emit(Identifier.CANCELED, 0);
            }
        }
    }

    public User getU1() {

        return users.get(0);
    }

    public void setU1(User u1) {
        users.set(0, u1);
    }

    public User getU2() {
        return users.get(1);
    }

    public void setU2(User u2) {
        users.set(1, u2);
    }
}
