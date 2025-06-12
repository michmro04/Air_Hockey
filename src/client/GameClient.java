package client;

import common.*;
        import utils.Constants;

import javax.swing.*;
        import java.awt.*;
        import java.io.*;
        import java.net.Socket;

public class GameClient {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameState gameState = new GameState();
    private GamePanel panel;

    public GameClient(String host) {
        try {
            Socket socket = new Socket(host, Constants.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            panel = new GamePanel(out, gameState);
            panel.setPreferredSize(new Dimension(Constants.WIDTH, Constants.HEIGHT));

            JFrame frame = new JFrame("Air Hockey");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();                    // Dopasowuje rozmiar okna do panelu
            frame.setLocationRelativeTo(null); // Wyśrodkowanie na ekranie
            frame.setVisible(true);

            new Thread(this::listenLoop).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenLoop() {
        try {
            while (true) {
                GameState newState = (GameState) in.readObject();
                this.gameState = newState;
                panel.setGameState(newState);
                panel.repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
