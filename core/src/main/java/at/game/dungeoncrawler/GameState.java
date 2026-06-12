package at.game.dungeoncrawler;

/**
 * Plain data bag that carries the mutable game state.
 * Passed by reference so all systems (input, update, draw) see the same values.
 */
public class GameState {

    public int score = 0;
    public int wins = 0;
    public int pointsToWin = 44;

    public boolean isPaused = false;
    public boolean isGameOver = false;
    public boolean isWon = false;
    public boolean isMoving = false;
    public boolean restartRequested = false;

    /**
     * Normalised movement direction set by InputHandler each frame.
     */
    public float dx = 0;
    public float dy = 0;

    public void reset() {
        score = 0;
        isPaused = false;
        isGameOver = false;
        isWon = false;
        isMoving = false;
        restartRequested = false;
        dx = 0;
        dy = 0;
    }
}
