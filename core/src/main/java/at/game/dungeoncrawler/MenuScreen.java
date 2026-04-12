package at.game.dungeoncrawler;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Handles both the difficulty-selection and character-selection screens.
 * When the player has made both choices, it transitions to GameScreen.
 */
public class MenuScreen implements Screen {

    private static final float VIRTUAL_W = 800f;
    private static final float VIRTUAL_H = 480f;

    private final Game game;

    // Rendering
    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont    font;
    private Viewport      viewport;

    // Touch unprojection
    private final Vector3 worldTouch = new Vector3();

    // Difficulty buttons
    private Rectangle btnEasy, btnMedium, btnHard;

    // Character buttons
    private Rectangle btnWitch, btnAdventurer;

    // State
    private enum Phase { DIFFICULTY, CHARACTER }
    private Phase phase = Phase.DIFFICULTY;

    private GameScreen.Difficulty chosenDifficulty;
    private final AudioManager audio;

    public MenuScreen(Game game, AudioManager audio) {
        this.game  = game;
        this.audio = audio;
    }

    @Override
    public void show() {
        OrthographicCamera camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);
        camera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0);
        camera.update();

        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font          = new BitmapFont();
        font.getData().setScale(2f);

        // Difficulty buttons
        float dBtnW = 200, dBtnH = 50;
        float dBtnX = VIRTUAL_W / 2f - dBtnW / 2f;
        btnEasy   = new Rectangle(dBtnX, VIRTUAL_H / 2f + 60, dBtnW, dBtnH);
        btnMedium = new Rectangle(dBtnX, VIRTUAL_H / 2f,      dBtnW, dBtnH);
        btnHard   = new Rectangle(dBtnX, VIRTUAL_H / 2f - 60, dBtnW, dBtnH);

        // Character buttons
        float cBtnW = 250, cBtnH = 50;
        float cBtnX = VIRTUAL_W / 2f - cBtnW / 2f;
        btnWitch      = new Rectangle(cBtnX, VIRTUAL_H / 2f + 60, cBtnW, cBtnH);
        btnAdventurer = new Rectangle(cBtnX, VIRTUAL_H / 2f,      cBtnW, cBtnH);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        // Unproject on any touch
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isTouched()) {
            worldTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(worldTouch);
        }

        if (phase == Phase.DIFFICULTY) {
            handleDifficultyInput();
            drawDifficultyScreen();
        } else {
            handleCharacterInput();
            drawCharacterScreen();
        }
    }

    // -------------------------------------------------------------------------

    private void handleDifficultyInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        if (btnEasy.contains(worldTouch.x, worldTouch.y)) {
            chosenDifficulty = GameScreen.Difficulty.EASY;
            phase = Phase.CHARACTER;
        } else if (btnMedium.contains(worldTouch.x, worldTouch.y)) {
            chosenDifficulty = GameScreen.Difficulty.MEDIUM;
            phase = Phase.CHARACTER;
        } else if (btnHard.contains(worldTouch.x, worldTouch.y)) {
            chosenDifficulty = GameScreen.Difficulty.HARD;
            phase = Phase.CHARACTER;
        }
    }

    private void handleCharacterInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        Player.CharacterType type = null;
        if (btnWitch.contains(worldTouch.x, worldTouch.y)) {
            type = Player.CharacterType.WITCH;
        } else if (btnAdventurer.contains(worldTouch.x, worldTouch.y)) {
            type = Player.CharacterType.ADVENTURER;
        }

        if (type != null) {
            game.setScreen(new GameScreen(game, chosenDifficulty, type, audio));
        }
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    private void drawDifficultyScreen() {
        float mx = worldTouch.x;
        float my = worldTouch.y;

        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(btnEasy.contains(mx, my)
                ? new com.badlogic.gdx.graphics.Color(0.3f, 0.8f, 0.3f, 1f)
                : new com.badlogic.gdx.graphics.Color(0.2f, 0.5f, 0.2f, 1f));
        shapeRenderer.rect(btnEasy.x, btnEasy.y, btnEasy.width, btnEasy.height);

        shapeRenderer.setColor(btnMedium.contains(mx, my)
                ? new com.badlogic.gdx.graphics.Color(0.8f, 0.7f, 0.2f, 1f)
                : new com.badlogic.gdx.graphics.Color(0.5f, 0.4f, 0.1f, 1f));
        shapeRenderer.rect(btnMedium.x, btnMedium.y, btnMedium.width, btnMedium.height);

        shapeRenderer.setColor(btnHard.contains(mx, my)
                ? new com.badlogic.gdx.graphics.Color(0.9f, 0.2f, 0.2f, 1f)
                : new com.badlogic.gdx.graphics.Color(0.5f, 0.1f, 0.1f, 1f));
        shapeRenderer.rect(btnHard.x, btnHard.y, btnHard.width, btnHard.height);

        shapeRenderer.end();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.draw(batch, "SELECT DIFFICULTY", 240, 400);
        font.draw(batch, "EASY",   btnEasy.x   + 60, btnEasy.y   + 35);
        font.draw(batch, "MEDIUM", btnMedium.x + 45, btnMedium.y + 35);
        font.draw(batch, "HARD",   btnHard.x   + 60, btnHard.y   + 35);
        batch.end();
    }

    private void drawCharacterScreen() {
        float mx = worldTouch.x;
        float my = worldTouch.y;

        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(btnWitch.contains(mx, my)
                ? new com.badlogic.gdx.graphics.Color(0.6f, 0.2f, 0.8f, 1f)
                : new com.badlogic.gdx.graphics.Color(0.4f, 0.1f, 0.6f, 1f));
        shapeRenderer.rect(btnWitch.x, btnWitch.y, btnWitch.width, btnWitch.height);

        shapeRenderer.setColor(btnAdventurer.contains(mx, my)
                ? new com.badlogic.gdx.graphics.Color(0.2f, 0.7f, 0.3f, 1f)
                : new com.badlogic.gdx.graphics.Color(0.1f, 0.4f, 0.1f, 1f));
        shapeRenderer.rect(btnAdventurer.x, btnAdventurer.y, btnAdventurer.width, btnAdventurer.height);

        shapeRenderer.end();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.draw(batch, "SELECT CHARACTER", 250, 400);
        font.draw(batch, "WITCH",      btnWitch.x      + 45, btnWitch.y      + 35);
        font.draw(batch, "ADVENTURER", btnAdventurer.x + 20, btnAdventurer.y + 35);
        batch.end();
    }

    // -------------------------------------------------------------------------

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}