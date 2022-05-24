package model;

public class Room {
    private User u1;
    private User u2;
    public Room(User u1, User u2){
        this.u1 = u1;
        this.u2 = u2;
    }

    public User getU1() {
        return u1;
    }

    public void setU1(User u1) {
        this.u1 = u1;
    }

    public User getU2() {
        return u2;
    }

    public void setU2(User u2) {
        this.u2 = u2;
    }
}
