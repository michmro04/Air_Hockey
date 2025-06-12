package server;

public class ServerMain {
    public static void main(String[] args) {
        try {
            new GameServer().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
