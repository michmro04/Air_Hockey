package client;

import common.*;
import utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ObjectOutputStream;

public class GamePanel extends JPanel implements MouseMotionListener {
    private ObjectOutputStream out;
    private GameState gameState;
    private Vector2 lastMousePos = null; // zmieniamy na null, ustawimy przy pierwszym ruchu myszy
    private Timer repaintTimer;

    public GamePanel(ObjectOutputStream out, GameState state) {
        this.out = out;
        this.gameState = state;
        addMouseMotionListener(this);

        // Timer do odświeżania panelu co tick (np. 16 ms ~ 60 FPS)
        repaintTimer = new Timer(Constants.TICK_RATE_MS, e -> repaint());
        repaintTimer.start();
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Tło boiska
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        // Linie boczne
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(0, 0, Constants.WIDTH - 1, Constants.HEIGHT - 1);

        // Linia środkowa
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, Constants.HEIGHT / 2, Constants.WIDTH, Constants.HEIGHT / 2);

        // Bramki (prostokąty pośrodku górnej i dolnej krawędzi)
        int goalWidth = 200;
        int goalHeight = 10;

        g2.setColor(Color.GREEN);
        g2.fillRect((Constants.WIDTH - goalWidth) / 2, 0, goalWidth, goalHeight); // Górna bramka

        g2.setColor(Color.RED);
        g2.fillRect((Constants.WIDTH - goalWidth) / 2, Constants.HEIGHT - goalHeight, goalWidth, goalHeight); // Dolna bramka

        // Wynik i czas
        g2.setColor(Color.WHITE);
        g2.drawString("Gracz 1: " + gameState.scores[0], 20, 20);
        g2.drawString("Gracz 2: " + gameState.scores[1], 20, 40);
        g2.drawString("Czas: " + String.format("%.1f", gameState.timeLeft), 700, 20);

        // Rakietki (pozycja x,y to środek rakietki, więc przesuwamy by rysować poprawnie)
        g2.setColor(Color.BLUE);
        g2.fillOval(
                (int)(gameState.paddlePositions[0].x - Constants.PADDLE_RADIUS / 2),
                (int)(gameState.paddlePositions[0].y - Constants.PADDLE_RADIUS / 2),
                (int) Constants.PADDLE_RADIUS,
                (int) Constants.PADDLE_RADIUS);

        g2.setColor(Color.RED);
        g2.fillOval(
                (int)(gameState.paddlePositions[1].x - Constants.PADDLE_RADIUS / 2),
                (int)(gameState.paddlePositions[1].y - Constants.PADDLE_RADIUS / 2),
                (int) Constants.PADDLE_RADIUS,
                (int) Constants.PADDLE_RADIUS);

        // Krążek (analogicznie - środek)
        g2.setColor(Color.MAGENTA);
        g2.fillOval(
                (int)(gameState.puckPos.x - Constants.PUCK_RADIUS / 2),
                (int)(gameState.puckPos.y - Constants.PUCK_RADIUS / 2),
                (int) Constants.PUCK_RADIUS,
                (int) Constants.PUCK_RADIUS);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Nie używamy, ale wymagana metoda interfejsu
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Vector2 current = new Vector2(e.getX(), e.getY());

        if (lastMousePos == null) {
            lastMousePos = current;
            return; // nie wysyłaj inputu przy pierwszym ruchu
        }

        // Ogranicz ruch myszy do połowy boiska gracza (tu dla gracza 0 - górna połowa)
        if (current.y > Constants.HEIGHT / 2) {
            current.y = Constants.HEIGHT / 2;
        }

        Vector2 delta = current.subtract(lastMousePos);

        if (delta.length() > Constants.MAX_MOUSE_SPEED) {
            delta = delta.normalize().scale(Constants.MAX_MOUSE_SPEED);
        }

        try {
            out.writeObject(new PlayerInput(lastMousePos, delta));
            out.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        lastMousePos = current;
    }
}
