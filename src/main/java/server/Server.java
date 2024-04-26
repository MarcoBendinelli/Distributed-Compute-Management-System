package server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static Map<String, String> db_users;
    public static void main(String[] args) {
        //synchronizedMap might not be necessary
        db_users = Collections.synchronizedMap(new HashMap<>());
        db_users.put("matteo","matteo");
        db_users.put("marco","marco");
        db_users.put("simone","simone");
        ClientHandler handler = new ClientHandler(db_users);
        handler.start();
    }
}
