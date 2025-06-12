package common;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    public Vector2 startPos;
    public Vector2 delta;

    public PlayerInput(Vector2 startPos, Vector2 delta) {
        this.startPos = startPos;
        this.delta = delta;
    }
}
