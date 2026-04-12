package at.game.dungeoncrawler;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The main gameplay screen.
 * All heavy lifting is delegated to AudioManager, SpawnManager,
 * InputHandler, UIRenderer, and GameState.
 */
public class GameScreen implements Screen {

    // -------------------------------------------------------------------------
    // Difficulty enum (also used by SpawnManager)
    // -------------------------------------------------------------------------
    public enum Difficulty { EASY, MEDIUM, HARD }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final float VIRTUAL_W   = 800f;
    private static final float VIRTUAL_H   = 480f;
    private static final float BORDER_SIZE = 20f;

    // -------------------------------------------------------------------------
    // Core
    // -------------------------------------------------------------------------
    private final Game       game;
    private final Difficulty difficulty;

    // Rendering
    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont    font;
    private OrthographicCamera camera;
    private Viewport      viewport;

    // Textures
    private Texture playerTexture, adventurerTexture;
    private Texture enemyTexture, enemyFastTexture, enemyTankTexture;
    private Texture materialTexture, materialRareTexture;
    private Texture witchProjectile, adventurerProjectile;

    // Game objects
    private Player             player;
    private Level              level;
    private ArrayList<Enemy>      enemies    = new ArrayList<>();
    private ArrayList<Material>   materials  = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();

    // Systems
    private AudioManager  audio;
    private SpawnManager  spawner;
    private InputHandler  input;
    private UIRenderer    ui;
    private GameState     state;

    // Attack timer — wrapped in array so InputHandler can mutate it
    private final long[] lastAttackTime = { 0L };

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public GameScreen(Game game, Difficulty difficulty, Player.CharacterType characterType) {
        this.game       = game;
        this.difficulty = difficulty;
        // characterType is applied in show() once textures are loaded
        this.pendingCharacterType = characterType;
    }

    private final Player.CharacterType pendingCharacterType;

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void show() {
        // Rendering
        camera   = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);
        camera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0);
        camera.update();

        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        font = new BitmapFont();
        font.getData().setScale(2f);

        level = new Level("maps/Dungeon1.tmx");

        // Textures
        playerTexture      = new Texture(Gdx.files.internal("player.png"));
        adventurerTexture  = new Texture(Gdx.files.internal("adventurer.png"));
        enemyTexture       = new Texture(Gdx.files.internal("goblin_spritesheet.png"));
        enemyFastTexture   = new Texture(Gdx.files.internal("slime_spritesheet.png"));
        enemyTankTexture   = new Texture(Gdx.files.internal("goblin_spritesheet.png"));
        materialTexture     = new Texture(Gdx.files.internal("blueFlower.png"));
        materialRareTexture = new Texture(Gdx.files.internal("material_rare.png"));
        witchProjectile    = new Texture(Gdx.files.internal("laser.png"));
        adventurerProjectile = new Texture(Gdx.files.internal("arrow.png"));

        // Game state
        state = new GameState();
        state.pointsToWin = switch (difficulty) {
            case EASY   -> 44;
            case MEDIUM -> 50;
            case HARD   -> 100;
        };

        float speedMult = switch (difficulty) {
            case EASY   -> 1f;
            case MEDIUM -> 1.5f;
            case HARD   -> 2f;
        };

        // Audio
        audio = new AudioManager();

        // Pause slider geometry
        float sliderWidth = VIRTUAL_W * 0.4f;
        float sliderX     = VIRTUAL_W / 2f - sliderWidth / 2f;

        // Systems
        spawner = new SpawnManager(enemyTexture, enemyFastTexture, enemyTankTexture,
                                   materialTexture, materialRareTexture, BORDER_SIZE);
        spawner.setDifficulty(difficulty, speedMult);

        input = new InputHandler(viewport, audio, sliderX, sliderWidth);
        ui    = new UIRenderer(batch, shapeRenderer, font, audio, sliderX, sliderWidth);

        // Create player
        createPlayer(pendingCharacterType);
        lastAttackTime[0] = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();

        // Input always runs
        input.processGameInput(state, player, projectiles, camera, lastAttackTime, audio);

        // Restart goes back to menu
        if (state.restartRequested) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        // Update only when playing
        if (!state.isPaused && !state.isWon && !state.isGameOver) {
            update(delta);
        }

        draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   { dispose(); }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        playerTexture.dispose();
        adventurerTexture.dispose();
        witchProjectile.dispose();
        adventurerProjectile.dispose();
        enemyTexture.dispose();
        enemyFastTexture.dispose();
        enemyTankTexture.dispose();
        materialTexture.dispose();
        materialRareTexture.dispose();
        level.dispose();
        audio.dispose();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    private void update(float delta) {
        level.update(delta);
        player.update(delta, state.isMoving, state.dx, state.dy, level);
        spawner.update(enemies, materials, camera, level);
        updateEnemies(delta);
        updateMaterials();
        updateProjectiles();
        updateCamera();
    }

    private void updateCamera() {
        float targetX = player.bounds.x + player.bounds.width  / 2f;
        float targetY = player.bounds.y + player.bounds.height / 2f;

        camera.position.x += (targetX - camera.position.x) * 0.1f;
        camera.position.y += (targetY - camera.position.y) * 0.1f;

        float halfW = viewport.getWorldWidth()  / 2f;
        float halfH = viewport.getWorldHeight() / 2f;
        camera.position.x = java.lang.Math.max(halfW, java.lang.Math.min(camera.position.x, level.getMapWidth()  - halfW));
        camera.position.y = java.lang.Math.max(halfH, java.lang.Math.min(camera.position.y, level.getMapHeight() - halfH));

        camera.update();
    }

    private void updateEnemies(float delta) {
        Iterator<Enemy> iter = enemies.iterator();
        while (iter.hasNext()) {
            Enemy enemy = iter.next();
            enemy.update(delta);

            float dx = (player.bounds.x + player.bounds.width  / 2f) - (enemy.bounds.x + enemy.bounds.width  / 2f);
            float dy = (player.bounds.y + player.bounds.height / 2f) - (enemy.bounds.y + enemy.bounds.height / 2f);
            float len = (float) java.lang.Math.sqrt(dx * dx + dy * dy);
            if (len != 0) { dx /= len; dy /= len; }

            enemy.move(dx, dy, enemy.speed, delta, level);

            if (enemy.bounds.overlaps(player.bounds)) {
                player.lives -= enemy.damage;
                iter.remove();
                audio.playHit();
                if (player.isDead()) state.isGameOver = true;
                continue;
            }

            // Cull far-away enemies
            float camX    = camera.position.x;
            float camY    = camera.position.y;
            float cullDst = Gdx.graphics.getWidth();
            if (enemy.bounds.x + enemy.bounds.width  < camX - cullDst ||
                enemy.bounds.x                        > camX + cullDst ||
                enemy.bounds.y + enemy.bounds.height  < camY - cullDst ||
                enemy.bounds.y                        > camY + cullDst) {
                iter.remove();
            }
        }
    }

    private void updateMaterials() {
        Iterator<Material> iter = materials.iterator();
        while (iter.hasNext()) {
            Material mat = iter.next();

            if (mat.bounds.overlaps(player.bounds)) {
                state.score += 3;
                iter.remove();
                audio.playCollect();
                continue;
            }

            if (state.score >= state.pointsToWin) {
                state.isWon = true;
                state.wins++;
            }

            if (mat.bounds.y + mat.bounds.height < BORDER_SIZE) {
                iter.remove();
            }
        }
    }

    private void updateProjectiles() {
        Iterator<Projectile> iter = projectiles.iterator();
        while (iter.hasNext()) {
            Projectile p = iter.next();
            p.x += p.dx * p.speed * Gdx.graphics.getDeltaTime();
            p.y += p.dy * p.speed * Gdx.graphics.getDeltaTime();

            boolean hit = false;
            Iterator<Enemy> enemyIter = enemies.iterator();
            while (enemyIter.hasNext()) {
                Enemy enemy = enemyIter.next();
                if (p.getBounds().overlaps(enemy.bounds)) {
                    enemyIter.remove();
                    state.score++;
                    hit = true;
                    break;
                }
            }

            if (hit) { iter.remove(); continue; }

            if (level.overlapsCollision(p.getBounds())) { iter.remove(); continue; }

            float distX = p.x - player.bounds.x;
            float distY = p.y - player.bounds.y;
            if (distX * distX + distY * distY > 1500f * 1500f) {
                iter.remove();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Draw
    // -------------------------------------------------------------------------

    private void draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        viewport.apply();

        // World-space rendering
        level.render(camera);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.draw(batch);
        for (Enemy e    : enemies)   e.draw(batch);
        for (Material m : materials) batch.draw(m.texture, m.bounds.x, m.bounds.y, m.bounds.width, m.bounds.height);
        drawProjectiles();
        batch.end();

        // Screen-space HUD
        ui.drawHUD(state, player);

        if (state.isGameOver) ui.drawGameOver();
        if (state.isWon)      ui.drawWin(state.wins);
        if (state.isPaused)   ui.drawPause();

        // Border
        drawBorder();
    }

    private void drawProjectiles() {
        float scale = level.getScale();
        for (Projectile p : projectiles) {
            float angle = (float) java.lang.Math.toDegrees(java.lang.Math.atan2(p.dy, p.dx));
            batch.draw(player.projectileTexture,
                    p.x, p.y, p.width / 2f, p.height / 2f,
                    p.width, p.height, scale, scale, angle,
                    0, 0,
                    player.projectileTexture.getWidth(),
                    player.projectileTexture.getHeight(),
                    true, false);
        }
    }

    private void drawBorder() {
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(0,                    0,                    VIRTUAL_W, BORDER_SIZE);  // bottom
        shapeRenderer.rect(0,                    VIRTUAL_H - BORDER_SIZE, VIRTUAL_W, BORDER_SIZE);  // top
        shapeRenderer.rect(0,                    0,                    BORDER_SIZE, VIRTUAL_H);  // left
        shapeRenderer.rect(VIRTUAL_W - BORDER_SIZE, 0,                 BORDER_SIZE, VIRTUAL_H);  // right
        shapeRenderer.end();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void createPlayer(Player.CharacterType type) {
        Texture tex          = type == Player.CharacterType.WITCH ? playerTexture : adventurerTexture;
        Texture projectileTex = type == Player.CharacterType.WITCH ? witchProjectile : adventurerProjectile;
        int   startLives     = type == Player.CharacterType.WITCH ? 5 : 6;
        float startSpeed     = type == Player.CharacterType.WITCH ? 450f : 400f;

        player = new Player(1600, 1100, 40, 40, startSpeed, startLives, tex, type, level);

        Texture sheetTex = new Texture(Gdx.files.internal(
                type == Player.CharacterType.WITCH ? "witch-Sheet.png" : "adventurer-Sheet.png"));
        player.setProjectileTexture(projectileTex);
        player.createAnimation(sheetTex);

        camera.position.set(player.bounds.x, player.bounds.y, 0);
    }
}