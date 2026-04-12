package at.game.dungeoncrawler;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class StartScreen implements Screen {

    private static final float VIRTUAL_W = 800f;
    private static final float VIRTUAL_H = 480f;

    // How fast the "Press any key" text blinks (full cycle in seconds)
    private static final float BLINK_SPEED = 1.8f;

    private final Game         game;
    private final AudioManager audio;

    // Rendering
    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private Viewport      viewport;

    // Fonts — we use two sizes to fake a glow
    private BitmapFont titleFont;   // large title
    private BitmapFont promptFont;  // smaller "press any key"
    private BitmapFont winsFont;    // small win counter

    // Layout helper
    private final GlyphLayout layout = new GlyphLayout();

    // Animation
    private float elapsedTime = 0f;

    // Persistent win count
    private int totalWins;

    // Particle-like background dots
    private static final int DOT_COUNT = 60;
    private final float[] dotX    = new float[DOT_COUNT];
    private final float[] dotY    = new float[DOT_COUNT];
    private final float[] dotSpeed = new float[DOT_COUNT];
    private final float[] dotSize  = new float[DOT_COUNT];
    private final float[] dotAlpha = new float[DOT_COUNT];

    public StartScreen(Game game, AudioManager audio) {
        this.game  = game;
        this.audio = audio;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void show() {
        OrthographicCamera camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);
        camera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0);
        camera.update();

        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Title font — scaled up BitmapFont
        titleFont = new BitmapFont();
        titleFont.getData().setScale(5f);
        titleFont.setColor(Color.WHITE);

        promptFont = new BitmapFont();
        promptFont.getData().setScale(1.8f);
        promptFont.setColor(Color.WHITE);

        winsFont = new BitmapFont();
        winsFont.getData().setScale(1.4f);
        winsFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));

        // Load persistent win count
        Preferences prefs = Gdx.app.getPreferences("dungeon_crawler_prefs");
        totalWins = prefs.getInteger("total_wins", 0);

        // Initialise background dust dots
        for (int i = 0; i < DOT_COUNT; i++) {
            resetDot(i, true);
        }
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;

        handleInput();
        updateDots(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        drawBackground();
        drawDots();
        drawTitle();
        drawWinCounter();
        drawBlinkingPrompt();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        promptFont.dispose();
        winsFont.dispose();
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void handleInput() {
        boolean pressed =
                Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) ||
                Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) ||
                Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);

        if (pressed) {
            game.setScreen(new MenuScreen(game, audio));
        }
    }

    // -------------------------------------------------------------------------
    // Background dust dots
    // -------------------------------------------------------------------------

    private void resetDot(int i, boolean randomY) {
        dotX[i]     = (float) Math.random() * VIRTUAL_W;
        dotY[i]     = randomY ? (float) Math.random() * VIRTUAL_H : VIRTUAL_H + 4f;
        dotSpeed[i] = 8f  + (float) Math.random() * 25f;
        dotSize[i]  = 0.8f + (float) Math.random() * 2.4f;
        dotAlpha[i] = 0.15f + (float) Math.random() * 0.45f;
    }

    private void updateDots(float delta) {
        for (int i = 0; i < DOT_COUNT; i++) {
            dotY[i] -= dotSpeed[i] * delta;
            if (dotY[i] + dotSize[i] < 0) {
                resetDot(i, false); // re-enter from top
            }
        }
    }

    // -------------------------------------------------------------------------
    // Draw calls
    // -------------------------------------------------------------------------

    private void drawBackground() {
        // Very dark blue-grey gradient faked with two layered rects
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Bottom colour
        shapeRenderer.setColor(0.03f, 0.03f, 0.08f, 1f);
        shapeRenderer.rect(0, 0, VIRTUAL_W, VIRTUAL_H / 2f);

        // Top colour (slightly lighter)
        shapeRenderer.setColor(0.06f, 0.05f, 0.12f, 1f);
        shapeRenderer.rect(0, VIRTUAL_H / 2f, VIRTUAL_W, VIRTUAL_H / 2f);

        shapeRenderer.end();
    }

    private void drawDots() {
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < DOT_COUNT; i++) {
            shapeRenderer.setColor(0.55f, 0.45f, 0.85f, dotAlpha[i]);
            shapeRenderer.circle(dotX[i], dotY[i], dotSize[i], 6);
        }
        shapeRenderer.end();
    }

    private void drawTitle() {
        final String title = "DUNGEON CRAWLER";

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        // --- Glow layers: draw the title several times, offset & semi-transparent ---
        // Outer glow (purple, large offset)
        Color outerGlow = new Color(0.55f, 0.1f, 0.9f, 0.18f);
        drawTitleAt(title, titleFont, outerGlow, VIRTUAL_H * 0.72f, 4f);
        drawTitleAt(title, titleFont, outerGlow, VIRTUAL_H * 0.72f, -4f);

        // Mid glow (brighter purple, smaller offset)
        Color midGlow = new Color(0.7f, 0.3f, 1.0f, 0.35f);
        drawTitleAt(title, titleFont, midGlow, VIRTUAL_H * 0.72f, 2f);
        drawTitleAt(title, titleFont, midGlow, VIRTUAL_H * 0.72f, -2f);

        // Core text (white)
        titleFont.setColor(Color.WHITE);
        layout.setText(titleFont, title);
        float titleX = (VIRTUAL_W - layout.width) / 2f;
        titleFont.draw(batch, title, titleX, VIRTUAL_H * 0.72f);

        batch.end();
    }

    /**
     * Draws the title centred horizontally, offset vertically by {@code yOffset}
     * from {@code baseY}, in the given colour.
     */
    private void drawTitleAt(String text, BitmapFont font, Color color, float baseY, float yOffset) {
        font.setColor(color);
        layout.setText(font, text);
        float x = (VIRTUAL_W - layout.width) / 2f;
        font.draw(batch, text, x, baseY + yOffset);
    }

    private void drawWinCounter() {
        if (totalWins == 0) return; // nothing to show on first run

        String label = totalWins == 1
                ? "1 dungeon cleared"
                : totalWins + " dungeons cleared";

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layout.setText(winsFont, label);
        float x = (VIRTUAL_W - layout.width) / 2f;
        winsFont.setColor(0.55f, 0.45f, 0.85f, 1f);
        winsFont.draw(batch, label, x, VIRTUAL_H * 0.50f);
        batch.end();
    }

    private void drawBlinkingPrompt() {
        // Smooth sine-wave blink so it fades in/out rather than hard-cutting
        float alpha = (float) (0.5 + 0.5 * Math.sin(elapsedTime * (2 * Math.PI / BLINK_SPEED)));

        String prompt = "Press any key to start";

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layout.setText(promptFont, prompt);
        float x = (VIRTUAL_W - layout.width) / 2f;
        promptFont.setColor(0.85f, 0.75f, 1.0f, alpha);
        promptFont.draw(batch, prompt, x, VIRTUAL_H * 0.30f);
        batch.end();
    }
}