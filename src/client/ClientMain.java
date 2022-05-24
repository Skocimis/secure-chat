package client;

import model.Identifier;
import socket.MySocket;

import java.io.*;
import java.util.Scanner;

public class ClientMain {
    private String name = "";
    private boolean connected = false;
    private boolean speaking = false;
    private boolean waiting = false;
    private boolean incoming = false;

    public ClientMain() throws IOException {
        MySocket mySocket = new MySocket("localhost", 2011);
        loadEvents(mySocket);
        start(mySocket);
    }

    private void init(MySocket socket) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter your name:");
        String name = scanner.nextLine();
        this.name = name;
        socket.emit(Identifier.CONNECT, name);
    }

    private void loadEvents(MySocket socket) {
        socket.on(Identifier.CONNECTED, (data) -> {
            if (data instanceof Boolean flag) {
                if (flag) {
                    connected = true;
                    System.out.println("You have been connected, check out the available commands with \"/help\".");
                } else {
                    name = "";
                }
            }
            return 0;
        });

        socket.on(Identifier.CHAT_REPLY, (data) -> {
            if (data instanceof Number) {
                switch ((int) data) {
                    case 0: {
                        System.out.println("Connected to private conversation.");
                        speaking = true;
                        waiting = false;
                        break;
                    }
                    case -1: {
                        System.out.println("The prompted user does not exist.");
                        waiting = false;
                        break;
                    }
                    case -2: {
                        System.out.println("The prompted user refused the connection.");
                        waiting = false;
                        break;
                    }
                    case -3: {
                        System.out.println("The prompted user is currently in a conversation, please try again later.");
                        waiting = false;
                        break;
                    }
                }
            }
            return 0;
        });

        socket.on(Identifier.CANCELED, (data) -> {
            if (incoming) {
                incoming = false;
                System.out.println("Incoming request cancelled");
            }
            if (waiting) {
                waiting = false;
                System.out.println("Outgoing request cancelled");
            }
            return 0;
        });

        socket.on(Identifier.USER_LIST, (data) -> {
            System.out.println(data.toString());
            return 0;
        });

        socket.on(Identifier.NEW_MESSAGE, (data) -> {
            System.out.println(data);
            return 0;
        });

        socket.on(Identifier.CHAT_REQUEST, (data) -> {
            incoming = true;
            System.out.println(data + " wants to start a conversation (/accept or /deny)");
            return 0;
        });

        socket.on(Identifier.CHAT_INIT, (data) -> {
            waiting = false;
            incoming = false;
            speaking = true;
            return 0;
        });
    }

    private void start(MySocket socket) {
        Scanner scanner = new Scanner(System.in);
        String input = "";

        while (socket.isRunning() && !input.equalsIgnoreCase("kraj")) {
            input = scanner.nextLine();
            if (input.startsWith("/")) {
                String[] args = input.substring(1, input.length()).split(" ");
                String trigger = args[0].trim().toLowerCase();
                switch (trigger) {
                    case "help": {
                        System.out.println("General commands:");
                        System.out.println("/help        - shows this message");
                        System.out.println("/users       - lists currently active users");
                        System.out.println("Chat related commands:");
                        System.out.println("/chat <name> - sends a request to start a chat with somebody");
                        System.out.println("/accept>     - accepts an incoming chat request");
                        System.out.println("/deny        - denies an incoming chat request");
                        System.out.println("/cancel      - cancels a request");
                        break;
                    }
                    case "users": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        socket.emit(Identifier.REQUEST_USER_LIST, null);
                        break;
                    }
                    case "chat": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        if (incoming) {
                            System.out.println("You already have an incoming request, please accept it or deny it before proceeding.");
                            break;
                        }

                        if (waiting) {
                            System.out.println("You already have a pending request, please cancel it before proceeding.");
                            break;
                        }

                        if (speaking) {
                            System.out.println("You cannot speak with someone else until you \"/end\" the conversation.");
                            break;
                        }

                        if (args.length < 2) {
                            System.out.println("You did not enter a name.");
                            break;
                        }

                        waiting = true;
                        socket.emit(Identifier.CHAT, args[1]);
                        break;
                    }
                    case "cancel": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        if (!waiting) {
                            System.out.println("You haven't sent a conversation request, you can send one with \"/chat <name>\".");
                        }

                        socket.emit(Identifier.CANCEL, null);
                        break;
                    }
                    case "accept": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        if (!incoming) {
                            System.out.println("You do not have an incoming conversation request.");
                            break;
                        }

                        socket.emit(Identifier.CHAT_REQUEST_REPLY, true);
                    }
                    case "deny": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        if (!incoming) {
                            System.out.println("You do not have an incoming conversation request.");
                            break;
                        }

                        socket.emit(Identifier.CHAT_REQUEST_REPLY, false);
                    }
                }
            } else {
                if (!connected) {
                    System.out.println("You do not have a name, please use \"/connect <name>\" to assign one that's not taken.");
                    continue;
                }

                if (!speaking) {
                    System.out.println("You're not speaking with anybody, please use \"/chat <name>\" to send a request.");
                    continue;
                }

                socket.emit(Identifier.SEND_MESSAGE, input);
            }
        }
    }

    public static void main(String[] args) {
        try {
            new ClientMain();
        } catch (Exception e) {
            System.out.println("Desila se greska");
        }
    }
}
