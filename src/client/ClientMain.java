package client;

import model.Identifier;
import socket.MySocket;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;

public class ClientMain {
    private boolean connected = false;
    private boolean speaking = false;
    private boolean waiting = false;
    private boolean incoming = false;
    KeyPair keyPair;
    String otherUserName;
    PublicKey publicKey;

    public ClientMain() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        MySocket mySocket = new MySocket("localhost", 2011);
        init(mySocket);
        loadEvents(mySocket);
        start(mySocket);
    }

    private void login(MySocket socket) {
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();
        socket.emit(Identifier.CONNECT, name);
        socket.emit(Identifier.PUBLIC_KEY, keyPair.getPublic());
    }

    private void init(MySocket socket) {
        System.out.println("Please enter your name:");
        login(socket);
    }

    private void loadEvents(MySocket socket) {
        socket.on(Identifier.CONNECTED, (data) -> {
            if (data instanceof Boolean flag) {
                if (flag) {
                    connected = true;
                    System.out.println("You have been connected, check out the available commands with \"/help\".");
                } else {
                    System.out.println("The name you tried to enter already exists. Please try a different name:");
                    login(socket);
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
                    case -4: {
                        System.out.println("The prompted user is currently trying to start a conversation or has an incoming request, please try again later.");
                        waiting = false;
                        break;
                    }
                }
            }
            return 0;
        });

        socket.on(Identifier.CANCELED, (data) -> {
            if (incoming) {
                if (data instanceof Integer intt && intt == -1) {
                    System.out.println("Use /deny to deny a request.");
                    return 0;
                }
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
            try {
                Cipher decryptCipher = Cipher.getInstance("RSA");
                decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                byte[] decryptedMessageBytes = decryptCipher.doFinal((byte[]) data);
                String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
                System.out.println(otherUserName + ": " + decryptedMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        });

        socket.on(Identifier.CHAT_REQUEST, (data) -> {
            incoming = true;
            if (data instanceof String string)
                otherUserName = string;
            System.out.println(data + " wants to start a conversation (/accept or /deny)");
            return 0;
        });

        socket.on(Identifier.CHAT_INIT, (data) -> {
            System.out.println("Chat started with " + otherUserName + ", you can end it with \"/end\"");
            if (!(data instanceof PublicKey publicKey)) return 0;
            this.publicKey = publicKey;
            waiting = false;
            incoming = false;
            speaking = true;
            return 0;
        });

        socket.on(Identifier.CHAT_ENDED, (data) -> {
            if (data instanceof Boolean) {
                if ((Boolean) data) {
                    System.out.println("This conversation has ended.");
                } else {
                    System.out.println("This conversation was ended by the other user.");
                }
            }
            speaking = false;
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
                        System.out.println("/accept      - accepts an incoming chat request");
                        System.out.println("/deny        - denies an incoming chat request");
                        System.out.println("/cancel      - cancels a request");
                        break;
                    }
                    case "users": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }
                        socket.emit(Identifier.REQUEST_USER_LIST, 0);
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

                        System.out.println("Chat request successfully sent, you can cancel it with \"/cancel\".");
                        waiting = true;
                        otherUserName = args[1];
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

                        socket.emit(Identifier.CANCEL, 0);
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
                        break;
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
                        incoming = false;
                        System.out.println("Request denied");
                        break;
                    }
                    default: {
                        System.out.println("Please check out \"/help\" for a list of valid commands.");
                        break;
                    }
                    case "end": {
                        if (!connected) {
                            System.out.println("You do not have a name yet, please get one with \"/connect <name>\".");
                            break;
                        }

                        if (!speaking) {
                            System.out.println("You cannot end a conversation if you aren't in one.");
                            break;
                        }

                        socket.emit(Identifier.END, 0);
                        break;
                    }
                    case "exit": {
                        System.exit(0);
                        return;
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
                try {
                    String unencrypted = input;
                    Cipher encryptCipher = Cipher.getInstance("RSA");
                    encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    byte[] secretMessageBytes = unencrypted.getBytes(StandardCharsets.UTF_8);
                    byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
                    String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);
                    socket.emit(Identifier.SEND_MESSAGE, encryptedMessageBytes);
                } catch (Exception e) {

                }
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
