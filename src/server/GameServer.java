package server;

import common.*;
import utils.Constants;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GameServer {
    private boolean testMode = true; // true dla testów lokalnych
    private Socket[] players = new Socket[2];
    private ObjectInputStream[] inputs = new ObjectInputStream[2];
    private ObjectOutputStream[] outputs = new ObjectOutputStream[2];
    private GameState gameState = new GameState();
    private volatile boolean running = true; // volatile, bo wątek tick i główny mogą działać równolegle
    private ScheduledExecutorService executor;

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
        System.out.println("Serwer oczekuje na graczy...");

        if (testMode) {
            players[0] = serverSocket.accept();
            outputs[0] = new ObjectOutputStream(players[0].getOutputStream());
            inputs[0] = new ObjectInputStream(players[0].getInputStream());
            System.out.println("Połączono gracza 0 (tryb testowy)");
        } else {
            for (int i = 0; i < 2; i++) {
                players[i] = serverSocket.accept();
                outputs[i] = new ObjectOutputStream(players[i].getOutputStream());
                inputs[i] = new ObjectInputStream(players[i].getInputStream());
                System.out.println("Połączono gracza " + i);
            }
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::tick, 0, Constants.TICK_RATE_MS, TimeUnit.MILLISECONDS);

        // Wątek główny czeka na zakończenie serwera (np. przez running = false)
        while (running) {
            try {
                Thread.sleep(100); // odczekaj trochę, nie spinaj CPU
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Serwer kończy działanie...");
        shutdown(serverSocket);
    }

    private void tick() {
        if (!running) return;

        try {
            PlayerInput[] playerInputs = new PlayerInput[2];

            if (testMode) {
                if (inputs[0] != null && inputs[0].available() > 0) {
                    playerInputs[0] = (PlayerInput) inputs[0].readObject();
                }
                playerInputs[1] = simulateSecondPlayerInput();
            } else {
                for (int i = 0; i < 2; i++) {
                    if (inputs[i].available() > 0) {
                        playerInputs[i] = (PlayerInput) inputs[i].readObject();
                    }
                }
            }

            updateGame(playerInputs);

            if (testMode) {
                if (outputs[0] != null)
                    outputs[0].writeObject(gameState);
            } else {
                for (int i = 0; i < 2; i++) {
                    if (outputs[i] != null)
                        outputs[i].writeObject(gameState);
                }
            }

        } catch (IOException e) {
            System.out.println("Połączenie zostało zamknięte lub wystąpił błąd IO: " + e.getMessage());
            running = false;  // ustalamy, że mamy zakończyć serwer
        } catch (ClassNotFoundException e) {
            System.out.println("Błąd deserializacji: " + e.getMessage());
            running = false;
        } catch (Exception e) {
            System.out.println("Nieoczekiwany błąd: " + e.getMessage());
            running = false;
        }
    }

    private PlayerInput simulateSecondPlayerInput() {
        Vector2 target = gameState.puckPos;
        Vector2 current = gameState.paddlePositions[1];
        Vector2 direction = target.subtract(current);

        if (direction.length() > 0) {
            direction = direction.normalize().scale(2);
        }
        return new PlayerInput(current, direction);
    }

    private void updateGame(PlayerInput[] inputs) {
        for (int i = 0; i < 2; i++) {
            if (inputs[i] != null) {
                Vector2 newPos = inputs[i].startPos.add(inputs[i].delta);
                float newY = Math.max(0, Math.min(Constants.HEIGHT, newPos.y));
                float half = Constants.HEIGHT / 2f;

                if ((i == 0 && newY > half) || (i == 1 && newY < half)) continue;

                gameState.paddlePositions[i] = newPos;
            }
        }

        gameState.puckPos = gameState.puckPos.add(gameState.puckVel);

        if (gameState.puckPos.x <= 0 || gameState.puckPos.x >= Constants.WIDTH) {
            gameState.puckVel.x *= -1;
        }

        if (gameState.puckPos.y <= 0) {
            gameState.scores[1]++;
            resetPuck();
        } else if (gameState.puckPos.y >= Constants.HEIGHT) {
            gameState.scores[0]++;
            resetPuck();
        }

        gameState.timeLeft -= Constants.TICK_RATE_MS / 1000.0;
        if (gameState.timeLeft <= 0) {
            running = false;
            System.out.println("Koniec gry!");
        }
    }

    private void resetPuck() {
        gameState.puckPos = new Vector2(Constants.WIDTH / 2f, Constants.HEIGHT / 2f);
        gameState.puckVel = new Vector2(2, 2);
    }

    private void shutdown(ServerSocket serverSocket) {
        try {
            if (executor != null) executor.shutdownNow();

            for (int i = 0; i < players.length; i++) {
                if (inputs[i] != null) inputs[i].close();
                if (outputs[i] != null) outputs[i].close();
                if (players[i] != null && !players[i].isClosed()) players[i].close();
            }

            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.out.println("Błąd podczas zamykania zasobów: " + e.getMessage());
        }
    }
}
