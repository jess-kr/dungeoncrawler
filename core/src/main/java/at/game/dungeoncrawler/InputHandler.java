package at.game.dungeoncrawler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class InputHandler {

    // Shared unproject vector — reused every frame to avoid allocation
    private final Vector3 worldTouch = new Vector3();

    private final Viewport viewport;
    private final AudioManager audio;

    // Pause slider geometry (set once in GameScreen)
    private float sliderX;
    private float sliderWidth;

    public InputHandler(Viewport viewport, AudioManager audio, float sliderX, float sliderWidth) {
        this.viewport    = viewport;
        this.audio       = audio;
        this.sliderX     = sliderX;
        this.sliderWidth = sliderWidth;
    }

    // -------------------------------------------------------------------------
    // Called by MenuScreen / difficulty + character selection screens
    // -------------------------------------------------------------------------

    /** Returns true if a button was clicked and its rectangle matches. */
    public boolean justClickedIn(Rectangle rect) {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return false;
        unproject();
        return rect.contains(worldTouch.x, worldTouch.y);
    }

    // -------------------------------------------------------------------------
    // Called every frame by GameScreen
    // -------------------------------------------------------------------------

    public void processGameInput(GameState state, Player player,
                                 ArrayList<Projectile> projectiles,
                                 OrthographicCamera camera,
                                 long[] lastAttackTimeRef,
                                 AudioManager audioManager) {

        // Always unproject so worldTouch is fresh
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isTouched()) {
            unproject();
        }

        // Toggle pause
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && !state.isWon) {
            state.isPaused = !state.isPaused;
        }

        // Restart after game over / win
        if ((state.isGameOver || state.isWon) && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            state.restartRequested = true;
            return;
        }

        // Pause-screen volume slider
        if (state.isPaused) {
            handleVolumeSlider();
            return;
        }

        // Movement
        float dx = 0, dy = 0;
        if (!state.isWon && !state.isGameOver) {
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    dy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))   dy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))   dx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))  dx += 1;
        }

        boolean isMoving = (dx != 0 || dy != 0);
        if (isMoving) {
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= len;
            dy /= len;
        }

        state.dx       = dx;
        state.dy       = dy;
        state.isMoving = isMoving;

        // Attack
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            long now = TimeUtils.nanoTime();
            if (now - lastAttackTimeRef[0] > player.attackCooldown) {
                Projectile p = player.shoot(camera);
                if (p != null) {
                    projectiles.add(p);
                    audioManager.playShoot();
                }
                lastAttackTimeRef[0] = now;
            }
        }
    }

    // -------------------------------------------------------------------------

    private void unproject() {
        worldTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(worldTouch);
    }

    private void handleVolumeSlider() {
        if (!Gdx.input.isTouched()) return;

        float fixedSliderY = 200;
        float touchX = Gdx.input.getX();
        float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (touchX >= sliderX && touchX <= sliderX + sliderWidth
                && touchY >= fixedSliderY - 20 && touchY <= fixedSliderY + 20) {
            float volume = (touchX - sliderX) / sliderWidth;
            audio.setMusicVolume(MathUtils.clamp(volume, 0f, 1f));
        }
    }

    /** The last unprojected world-space touch position. */
    public Vector3 getWorldTouch() { return worldTouch; }
}
