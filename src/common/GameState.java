package common;

import java.io.Serializable;

public class GameState implements Serializable {
    public Vector2 puckPos;
    public Vector2 puckVel;
    public Vector2[] paddlePositions = new Vector2[2];
    public int[] scores = new int[2];
    public float timeLeft;

    public GameState() {
        puckPos = new Vector2(400, 300);
        puckVel = new Vector2(2, 2);
        paddlePositions[0] = new Vector2(200, 500);
        paddlePositions[1] = new Vector2(200, 100);
        scores[0] = scores[1] = 0;
        timeLeft = 60;
    }
}
