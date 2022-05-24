package model;

import java.io.Serializable;

public class Message implements Serializable {
    Integer counter;
    String name;

    public Message(Integer counter, String name) {
        this.counter = counter;
        this.name = name;
    }
}
