package at.game.dungeoncrawler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;

public class Main extends ApplicationAdapter {

    // --- Rendering ---
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private OrthographicCamera camera;

    // --- Textures ---
    private Texture playerTexture;
    private Texture enemyTexture;
    private Texture materialTexture;
    private Texture backgroundTexture;
    private Texture enemyFastTexture;
    private Texture enemyTankTexture;
    private Texture materialRareTexture;
    private Texture adventurerTexture;
    // private Texture witchTexture; // falls in zukunft noch was dazukommt
    private Texture witchProjectile;
    private Texture adventurerProjectile;

    // --- Audio ---
    private Music backgroundMusic;
    private Sound hitSound;
    private Sound collectSound;

    // --- Game Objects ---
    private Player player;
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Material> materials = new ArrayList<>();
    private Level level;
    private Stage stage;
    private Viewport viewport;
    // Zentrales Vector-Objekt für Umrechnungen
    com.badlogic.gdx.math.Vector3 worldTouch = new com.badlogic.gdx.math.Vector3();

    // --- Timers ---
    private long lastEnemyTime;
    private long lastMaterialTime;

    // --- Game State ---
    // Difficulty Selection
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    private Difficulty difficulty = Difficulty.EASY;
    private boolean isSelectingDifficulty = true;
    private Rectangle btnEasy, btnMedium, btnHard;
    private float enemySpeedMultiplier = 1f;

    // Choosing Class
    private boolean isSelectingCharacter = false;
    private Rectangle btnWitch, btnAdventurer;

    // Other Game Stats
    private int score = 0;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private boolean isWon = false;
    private boolean isMoving = false;
    private int pointsToWin = 5;
    private int wins = 0;

    // Player Position
    private float dx = 0;
    private float dy = 0;

    // --- Layout ---
    private float borderSize = 20f;

    // --- Pause Slider ---
    private float musicVolume = 0.2f;
    private float sliderX;
    private float sliderWidth;

    // Attack
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private Sound shootSound;
    private long lastAttackTime;
    // -------------------------------------------------------------------------
    // LIFECYCLE
    // -------------------------------------------------------------------------

    @Override
    public void create() {
        // Rendering setup
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        camera.position.set(400, 240, 0);
        stage = new Stage(viewport);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        font = new BitmapFont();
        font.getData().setScale(2f);
        level = new Level("maps/Dungeon1.tmx");

        // Textures
        materialTexture = new Texture(Gdx.files.internal("blueFlower.png"));
        materialRareTexture = new Texture(Gdx.files.internal("material_rare.png"));

        backgroundTexture = new Texture(Gdx.files.internal("background.png"));

        enemyTexture = new Texture(Gdx.files.internal("goblin_spritesheet.png"));
        enemyFastTexture = new Texture(Gdx.files.internal("slime_spritesheet.png"));
        enemyTankTexture = new Texture(Gdx.files.internal("goblin_spritesheet.png"));

        witchProjectile = new Texture(Gdx.files.internal("laser.png"));
        adventurerProjectile = new Texture(Gdx.files.internal("arrow.png"));

        playerTexture = new Texture(Gdx.files.internal("player.png"));
        adventurerTexture = new Texture(Gdx.files.internal("adventurer.png"));
        // witchTexture = new Texture(Gdx.files.internal("player.png"));

        // Default Player so Game can start lol
        player = new Player(
                800,
                800,
                30, 30,
                400, 5,
                playerTexture,
                Player.CharacterType.WITCH,
                level);
        player.setProjectileTexture(witchProjectile);
        lastAttackTime = TimeUtils.nanoTime();

        // Focus Cam on Player
        camera.position.set(player.bounds.x, player.bounds.y, 0);

        // Fixes wegen Viewport
        float virtualW = 800f;
        float virtualH = 480f;

        // Buttons for Class-Choice
        float ClassbtnW = 250, ClassBtnH = 50;
        float ClassbtnX = virtualW / 2f - ClassbtnW / 2f;
        btnWitch = new Rectangle(ClassbtnX, virtualH / 2f + 60, ClassbtnW, ClassBtnH);
        btnAdventurer = new Rectangle(ClassbtnX, virtualH / 2f, ClassbtnW, ClassBtnH);

        // Audio
        hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));
        collectSound = Gdx.audio.newSound(Gdx.files.internal("collect.wav"));
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.wav"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(musicVolume);
        backgroundMusic.play();
        shootSound = Gdx.audio.newSound(Gdx.files.internal("shoot.wav"));

        // Pause slider
        sliderWidth = virtualW * 0.4f;
        sliderX = virtualW / 2f - sliderWidth / 2f;

        // Kick off timers so nothing spawns instantly
        lastEnemyTime = TimeUtils.nanoTime();
        lastMaterialTime = TimeUtils.nanoTime();

        // Difficulty Buttons
        float DifficultybtnW = 200, DifficultybtnH = 50;
        float DifficultybtnX = virtualW / 2f - DifficultybtnW / 2f;
        btnEasy = new Rectangle(DifficultybtnX, virtualH / 2f + 60, DifficultybtnW, DifficultybtnH);
        btnMedium = new Rectangle(DifficultybtnX, virtualH / 2f, DifficultybtnW, DifficultybtnH);
        btnHard = new Rectangle(DifficultybtnX, virtualH / 2f - 60, DifficultybtnW, DifficultybtnH);
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        input();
        if (!isPaused && !isWon && !isGameOver
                && !isSelectingDifficulty && !isSelectingCharacter) {
            update();
        }
        draw();
    }

    private void updateCamera() {
        float targetX = player.bounds.x + player.bounds.width / 2f;
        float targetY = player.bounds.y + player.bounds.height / 2f;

        camera.position.x += (targetX - camera.position.x) * 0.1f;
        camera.position.y += (targetY - camera.position.y) * 0.1f;

        // Clamp camera to map bounds
        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;

        camera.position.x = Math.max(halfW, Math.min(camera.position.x, level.getMapWidth() - halfW));
        camera.position.y = Math.max(halfH, Math.min(camera.position.y, level.getMapHeight() - halfH));

        camera.update();
    }
    // -------------------------------------------------------------------------
    // INPUT
    // -------------------------------------------------------------------------

    private void input() {

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || Gdx.input.isTouched()) {
            worldTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(worldTouch);
        }

        // --- Difficulty selection ---
        if (isSelectingDifficulty) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                if (btnEasy.contains(worldTouch.x, worldTouch.y)) {
                    setDifficulty(Difficulty.EASY);
                } else if (btnMedium.contains(worldTouch.x, worldTouch.y)) {
                    setDifficulty(Difficulty.MEDIUM);
                } else if (btnHard.contains(worldTouch.x, worldTouch.y)) {
                    setDifficulty(Difficulty.HARD);
                }
            }
            return;
        }

        // --- Character Choice ---
        if (isSelectingCharacter) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                if (btnWitch.contains(worldTouch.x, worldTouch.y)) {
                    createPlayer(Player.CharacterType.WITCH);
                    isSelectingCharacter = false;
                } else if (btnAdventurer.contains(worldTouch.x, worldTouch.y)) {
                    createPlayer(Player.CharacterType.ADVENTURER);
                    isSelectingCharacter = false;
                }
            }
            return;
        }

        // Toggle pause
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) && !isWon) {
            isPaused = !isPaused;
        }

        // Restart after game over or win
        if ((isGameOver || isWon) && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            restart();
            return;
        }

        // --- Pause-screen volume slider ---
        if (isPaused) {
            if (Gdx.input.isTouched()) {

                float fixedSliderY = 200;

                float touchX = Gdx.input.getX();
                float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();

                if (touchX >= sliderX && touchX <= sliderX + sliderWidth
                        && touchY >= fixedSliderY - 20 && touchY <= fixedSliderY + 20) {

                    musicVolume = (touchX - sliderX) / sliderWidth;
                    musicVolume = MathUtils.clamp(musicVolume, 0f, 1f);
                    backgroundMusic.setVolume(musicVolume);
                }
            }
            return;
        }

        // --- Actually Moving the Character ---
        dx = 0;
        dy = 0;

        if (!isWon && !isGameOver) {
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
                dy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))
                dy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
                dx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                dx += 1;
        }

        isMoving = (dx != 0 || dy != 0);

        // Diagonal-Speed-Fix
        if (isMoving) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        // --- Attack Mechanic ---
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (TimeUtils.nanoTime() - lastAttackTime > player.attackCooldown) {
                // Wir geben die Kamera mit, damit das Projektil weiß, wo "vorne" ist
                Projectile p = player.shoot(camera);
                if (p != null) {
                    projectiles.add(p);
                    if (shootSound != null)
                        shootSound.play();
                }
                lastAttackTime = TimeUtils.nanoTime();
            }
        }
    }
    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    private void update() {
        level.update(Gdx.graphics.getDeltaTime());
        player.update(Gdx.graphics.getDeltaTime(), isMoving, dx, dy, level);
        updateEnemies();
        updateMaterials();
        updateProjectiles();
        updateCamera();
    }

    // -------------------------------------------------------------------------
    // ENEMIES
    // -------------------------------------------------------------------------

    private void spawnEnemy() {
        int type = MathUtils.random(2);
        Enemy enemy = switch (difficulty) {
            case EASY -> switch (type) {
                case 1 -> Enemy.createFast(enemyFastTexture);
                case 2 -> Enemy.createTank(enemyTankTexture);
                default -> Enemy.createBasic(enemyTexture);
            };
            case MEDIUM -> switch (type) {
                case 0 -> Enemy.createFast(enemyFastTexture);
                default -> Enemy.createTank(enemyTankTexture);
            };
            case HARD -> MathUtils.randomBoolean()
                    ? Enemy.createFast(enemyFastTexture)
                    : Enemy.createTank(enemyTankTexture);
        };

        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = Gdx.graphics.getWidth() / 2f;
        float halfH = Gdx.graphics.getHeight() / 2f;

        int edge = MathUtils.random(3);
        switch (edge) {
            case 0 -> { // left
                enemy.bounds.x = camX - halfW - enemy.bounds.width;
                enemy.bounds.y = MathUtils.random(camY - halfH, camY + halfH - enemy.bounds.height);
            }
            case 1 -> { // right
                enemy.bounds.x = camX + halfW;
                enemy.bounds.y = MathUtils.random(camY - halfH, camY + halfH - enemy.bounds.height);
            }
            case 2 -> { // bottom
                enemy.bounds.x = MathUtils.random(camX - halfW, camX + halfW - enemy.bounds.width);
                enemy.bounds.y = camY - halfH - enemy.bounds.height;
            }
            case 3 -> { // top
                enemy.bounds.x = MathUtils.random(camX - halfW, camX + halfW - enemy.bounds.width);
                enemy.bounds.y = camY + halfH;
            }
        }

        if (level.overlapsCollision(enemy.bounds)) {
            lastEnemyTime = TimeUtils.nanoTime(); // reset timer
            return;
        }
        enemy.speed = enemySpeedMultiplier * enemy.speed;
        enemies.add(enemy);
        lastEnemyTime = TimeUtils.nanoTime();
    }

    private void updateEnemies() {
        // Spawn a new enemy every 2 seconds
        if (TimeUtils.nanoTime() - lastEnemyTime > 2_000_000_000L) {
            spawnEnemy();
        }

        Iterator<Enemy> iter = enemies.iterator();
        while (iter.hasNext()) {
            Enemy enemy = iter.next();
            enemy.update(Gdx.graphics.getDeltaTime());

            // Calculate direction vector toward player center
            float dx = (player.bounds.x + player.bounds.width / 2f) - (enemy.bounds.x + enemy.bounds.width / 2f);
            float dy = (player.bounds.y + player.bounds.height / 2f) - (enemy.bounds.y + enemy.bounds.height / 2f);

            // Normalize so diagonal movement isn't faster
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                dx /= len;
                dy /= len;
            }

            enemy.move(dx, dy, enemy.speed, Gdx.graphics.getDeltaTime(), level);

            // Hit player
            if (enemy.bounds.overlaps(player.bounds)) {
                player.lives -= enemy.damage;
                iter.remove();
                if (hitSound != null)
                    hitSound.play();
                if (player.isDead())
                    isGameOver = true;
                continue;
            }

            // Remove if way off screen (shouldn't happen but safety net)
            float camX = camera.position.x;
            float camY = camera.position.y;
            float cullDist = Gdx.graphics.getWidth();
            if (enemy.bounds.x + enemy.bounds.width < camX - cullDist ||
                    enemy.bounds.x > camX + cullDist ||
                    enemy.bounds.y + enemy.bounds.height < camY - cullDist ||
                    enemy.bounds.y > camY + cullDist) {
                iter.remove();
            }
        }
    }

    // -------------------------------------------------------------------------
    // MATERIALS
    // -------------------------------------------------------------------------

    private void spawnMaterial() {
        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = Gdx.graphics.getWidth() / 2f;
        float halfH = Gdx.graphics.getHeight() / 2f;

        float x = MathUtils.random(camX - halfW + borderSize, camX + halfW - 32 - borderSize);
        float y = MathUtils.random(camY - halfH + borderSize, camY + halfH - 32 - borderSize);

        float rareChance = switch (difficulty) {
            case EASY -> 0.25f;
            case MEDIUM -> 0.10f;
            case HARD -> 0.005f;
        };

        Material mat = MathUtils.randomBoolean(rareChance)
                ? Material.createRare(x, y, materialRareTexture)
                : Material.createCommon(x, y, materialTexture);

        if (level.overlapsCollision(mat.bounds)) {
            lastMaterialTime = TimeUtils.nanoTime(); // reset timer, try again next cycle
            return;
        }
        materials.add(mat);
        lastMaterialTime = TimeUtils.nanoTime();
    }

    private void updateMaterials() {
        // Spawn a new material every 1.5 seconds
        if (TimeUtils.nanoTime() - lastMaterialTime > 2_500_000_000L) {
            spawnMaterial();
        }

        Iterator<Material> iter = materials.iterator();
        while (iter.hasNext()) {
            Material mat = iter.next();

            // Player collects material
            if (mat.bounds.overlaps(player.bounds)) {
                score = score + 3;
                iter.remove();
                collectSound.play();
                continue;
            }
            // Checks if
            if (score >= pointsToWin) {
                isWon = true;
                wins++;
            }

            // Left the screen at the bottom — missed it
            if (mat.bounds.y + mat.bounds.height < borderSize) {
                iter.remove();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Attack
    // -------------------------------------------------------------------------

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
                    score++;
                    hit = true;
                    break;
                }
            }

            if (hit) {
                iter.remove();
                continue;
            }

            if (level.overlapsCollision(p.getBounds())) {
                iter.remove();
                hit = true;
                break;
            }

            if (hit)
                continue;

            float distX = p.x - player.bounds.x;
            float distY = p.y - player.bounds.y;
            if (distX * distX + distY * distY > 1500f * 1500f) {
                iter.remove();
            }
        }
    }

    // -------------------------------------------------------------------------
    // DRAW
    // -------------------------------------------------------------------------

    private void draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f);
        viewport.apply();

        if (!isSelectingDifficulty && !isSelectingCharacter) {
            level.render(camera);

            // level.renderDebug(camera);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            player.draw(batch);
            for (Enemy enemy : enemies)
                enemy.draw(batch);
            for (Material mat : materials)
                batch.draw(mat.texture, mat.bounds.x, mat.bounds.y, mat.bounds.width, mat.bounds.height);

            for (Projectile p : projectiles) {
                boolean flipX = true;
                float SCALE = level.getScale();
                float angle = (float) Math.toDegrees(Math.atan2(p.dy, p.dx));
                batch.draw(player.projectileTexture, p.x, p.y, p.width / 2f, p.height / 2f,
                        p.width, p.height, SCALE, SCALE, angle, 0, 0,
                        player.projectileTexture.getWidth(), player.projectileTexture.getHeight(),
                        flipX, false);
            }
            batch.end();
        }

        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        if (isSelectingDifficulty) {
            drawDifficultyScreen();
        } else if (isSelectingCharacter) {
            drawCharacterScreen();
        } else {

            batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(
                    0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            batch.begin();

            font.draw(batch, "Score: " + score, 30, Gdx.graphics.getHeight() - 30);
            font.draw(batch, "Lives: " + player.lives, Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 30);

            if (isGameOver) {
                font.draw(batch, "GAME OVER", 40, Gdx.graphics.getHeight() - 30);
                font.draw(batch, "Press ENTER to restart", 40, Gdx.graphics.getHeight() - 90);
            }
            if (isWon) {
                font.draw(batch, "SUCCESS!", 40 , 280 );
                font.draw(batch, "Amount of Wins: " + wins, 40,  240 );
                font.draw(batch, "Press ENTER to play again", 40 , 200 );
            }
            batch.end();

            if (isPaused) {
                drawPauseUI();
            }
        }

        // Border (ebenfalls fest auf 800x480)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(0, 0, 800, borderSize); // Unten
        shapeRenderer.rect(0, 480 - borderSize, 800, borderSize); // Oben
        shapeRenderer.rect(0, 0, borderSize, 480); // Links
        shapeRenderer.rect(800 - borderSize, 0, borderSize, 480); // Rechts
        shapeRenderer.end();
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void setDifficulty(Difficulty d) {
        difficulty = d;
        pointsToWin = switch (d) {
            case EASY -> 44;
            case MEDIUM -> 50;
            case HARD -> 100;
        };
        enemySpeedMultiplier = switch (d) {
            case EASY -> 1f;
            case MEDIUM -> 1.5f;
            case HARD -> 2f;
        };
        isSelectingDifficulty = false;
        isSelectingCharacter = true;
    }

    private void createPlayer(Player.CharacterType type) {
        Texture tex = type == Player.CharacterType.WITCH ? playerTexture : adventurerTexture;
        Texture projectileTex = type == Player.CharacterType.WITCH ? witchProjectile : adventurerProjectile;

        int startLives = type == Player.CharacterType.WITCH ? 5 : 6;
        float startSpeed = type == Player.CharacterType.WITCH ? 450f : 400f;

        player = new Player(
                1600,
                1100,
                40, 40,
                startSpeed,
                startLives,
                tex,
                type,
                level);

        Texture aTexture = new Texture(Gdx.files.internal("adventurer-Sheet.png"));
        Texture wTexture = new Texture(Gdx.files.internal("witch-Sheet.png"));
        player.setProjectileTexture(projectileTex);
        lastAttackTime = TimeUtils.nanoTime();
        player.createAnimation(type == Player.CharacterType.WITCH ? wTexture : aTexture);
    }

    private void drawDifficultyScreen() {
        float mx = worldTouch.x;
        float my = worldTouch.y - Gdx.input.getY();

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

        batch.begin();
        font.draw(batch, "SELECT DIFFICULTY", 270, 400);
        font.draw(batch, "EASY", btnEasy.x + 60, btnEasy.y + 35);
        font.draw(batch, "MEDIUM", btnMedium.x + 45, btnMedium.y + 35);
        font.draw(batch, "HARD", btnHard.x + 60, btnHard.y + 35);
        batch.end();
    }

    private void drawPauseUI() {
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float fixedSliderY = 200;

        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);
        shapeRenderer.rect(sliderX, fixedSliderY - 4, sliderWidth, 8);

        shapeRenderer.setColor(0.2f, 0.8f, 0.4f, 1f);
        shapeRenderer.circle(sliderX + musicVolume * sliderWidth, fixedSliderY, 12);

        shapeRenderer.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        font.draw(batch, "PAUSED", Gdx.graphics.getWidth() / 2f - 50, 300);
        font.draw(batch, "Volume:", sliderX, fixedSliderY + 40);
        batch.end();
    }

    private void drawCharacterScreen() {
        float mx = worldTouch.x;
        float my = worldTouch.y;

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

        batch.begin();
        font.draw(batch, "SELECT CHARACTER", 270, 400);
        font.draw(batch, "WITCH", btnWitch.x + 45, btnWitch.y + 35);
        font.draw(batch, "ADVENTURER", btnAdventurer.x + 20, btnAdventurer.y + 35);
        batch.end();
    }

    private void restart() {
        score = 0;
        isGameOver = false;
        isWon = false;
        enemies.clear();
        materials.clear();
        projectiles.clear();
        player.reset(Gdx.graphics.getWidth() / 2f - player.bounds.width / 2f, borderSize + 10);
        lastEnemyTime = TimeUtils.nanoTime();
        lastMaterialTime = TimeUtils.nanoTime();
        isSelectingDifficulty = true;
        isSelectingCharacter = false;
        drawDifficultyScreen();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

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
        enemyTankTexture.dispose();
        enemyFastTexture.dispose();
        materialTexture.dispose();
        materialRareTexture.dispose();
        backgroundTexture.dispose();
        backgroundMusic.dispose();
        hitSound.dispose();
        collectSound.dispose();
        level.dispose();
        if (shootSound != null)
            shootSound.dispose();
    }
}