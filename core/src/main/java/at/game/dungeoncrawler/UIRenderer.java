package at.game.dungeoncrawler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Responsible for all 2-D overlay rendering:
 * HUD (score / lives), pause overlay, game-over text, win text.
 *
 * Everything is drawn in screen-space (fixed 800×480 ortho).
 */
public class UIRenderer {

    private final SpriteBatch    batch;
    private final ShapeRenderer  shapeRenderer;
    private final BitmapFont     font;
    private final AudioManager   audio;

    private final float sliderX;
    private final float sliderWidth;

    /** Reusable screen-space projection matrix. */
    private final Matrix4 uiMatrix = new Matrix4();

    public UIRenderer(SpriteBatch batch, ShapeRenderer shapeRenderer,
                      BitmapFont font, AudioManager audio,
                      float sliderX, float sliderWidth) {
        this.batch         = batch;
        this.shapeRenderer = shapeRenderer;
        this.font          = font;
        this.audio         = audio;
        this.sliderX       = sliderX;
        this.sliderWidth   = sliderWidth;
    }

    // -------------------------------------------------------------------------
    // Public draw calls — GameScreen decides which ones to invoke
    // -------------------------------------------------------------------------

    public void drawHUD(GameState state, Player player) {
        applyUIMatrix();
        batch.begin();
        font.draw(batch, "Score: " + state.score,
                30, Gdx.graphics.getHeight() - 30);
        font.draw(batch, "Lives: " + player.lives,
                Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 30);
        batch.end();
    }

    public void drawGameOver() {
        applyUIMatrix();
        batch.begin();
        font.draw(batch, "GAME OVER",                    40, Gdx.graphics.getHeight() - 30);
        font.draw(batch, "Press ENTER to restart",       40, Gdx.graphics.getHeight() - 90);
        batch.end();
    }

    public void drawWin(int wins) {
        applyUIMatrix();
        batch.begin();
        font.draw(batch, "SUCCESS!",                     40, 280);
        font.draw(batch, "Amount of Wins: " + wins,      40, 240);
        font.draw(batch, "Press ENTER to play again",    40, 200);
        batch.end();
    }

    public void drawPause() {
        float fixedSliderY = 200;

        uiMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Slider track
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);
        shapeRenderer.rect(sliderX, fixedSliderY - 4, sliderWidth, 8);

        // Slider knob
        shapeRenderer.setColor(0.2f, 0.8f, 0.4f, 1f);
        shapeRenderer.circle(sliderX + audio.getMusicVolume() * sliderWidth, fixedSliderY, 12);

        shapeRenderer.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        font.draw(batch, "PAUSED",   Gdx.graphics.getWidth() / 2f - 50, 300);
        font.draw(batch, "Volume:",  sliderX,                            fixedSliderY + 40);
        batch.end();
    }

    // -------------------------------------------------------------------------

    private void applyUIMatrix() {
        uiMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(uiMatrix);
        shapeRenderer.setProjectionMatrix(uiMatrix);
    }
}