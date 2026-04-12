package at.game.dungeoncrawler;


import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;

public class SpawnManager {

    private static final long ENEMY_INTERVAL    = 2_000_000_000L;
    private static final long MATERIAL_INTERVAL = 2_500_000_000L;

    private final Texture enemyTexture;
    private final Texture enemyFastTexture;
    private final Texture enemyTankTexture;
    private final Texture materialTexture;
    private final Texture materialRareTexture;

    private final float borderSize;
    private GameScreen.Difficulty difficulty;
    private float enemySpeedMultiplier;

    private long lastEnemyTime;
    private long lastMaterialTime;

    public SpawnManager(Texture enemyTexture, Texture enemyFastTexture, Texture enemyTankTexture,
                        Texture materialTexture, Texture materialRareTexture,
                        float borderSize) {
        this.enemyTexture        = enemyTexture;
        this.enemyFastTexture    = enemyFastTexture;
        this.enemyTankTexture    = enemyTankTexture;
        this.materialTexture     = materialTexture;
        this.materialRareTexture = materialRareTexture;
        this.borderSize          = borderSize;

        lastEnemyTime    = TimeUtils.nanoTime();
        lastMaterialTime = TimeUtils.nanoTime();
    }

    public void setDifficulty(GameScreen.Difficulty difficulty, float speedMultiplier) {
        this.difficulty            = difficulty;
        this.enemySpeedMultiplier  = speedMultiplier;
    }

    // Called every frame from GameScreen.update()
    public void update(ArrayList<Enemy> enemies, ArrayList<Material> materials,
                       OrthographicCamera camera, Level level) {
        if (TimeUtils.nanoTime() - lastEnemyTime > ENEMY_INTERVAL) {
            trySpawnEnemy(enemies, camera, level);
        }
        if (TimeUtils.nanoTime() - lastMaterialTime > MATERIAL_INTERVAL) {
            trySpawnMaterial(materials, camera, level);
        }
    }

    public void resetTimers() {
        lastEnemyTime    = TimeUtils.nanoTime();
        lastMaterialTime = TimeUtils.nanoTime();
    }

    // -------------------------------------------------------------------------

    private void trySpawnEnemy(ArrayList<Enemy> enemies, OrthographicCamera camera, Level level) {
        Enemy enemy = buildEnemy();

        float camX  = camera.position.x;
        float camY  = camera.position.y;
        // Use a fixed virtual size so spawning is resolution-independent
        float halfW = 400f;
        float halfH = 240f;

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
            default -> { // top
                enemy.bounds.x = MathUtils.random(camX - halfW, camX + halfW - enemy.bounds.width);
                enemy.bounds.y = camY + halfH;
            }
        }

        if (level.overlapsCollision(enemy.bounds)) {
            lastEnemyTime = TimeUtils.nanoTime();
            return;
        }

        enemy.speed *= enemySpeedMultiplier;
        enemies.add(enemy);
        lastEnemyTime = TimeUtils.nanoTime();
    }

    private Enemy buildEnemy() {
        int type = MathUtils.random(2);
        return switch (difficulty) {
            case EASY -> switch (type) {
                case 1  -> Enemy.createFast(enemyFastTexture);
                case 2  -> Enemy.createTank(enemyTankTexture);
                default -> Enemy.createBasic(enemyTexture);
            };
            case MEDIUM -> switch (type) {
                case 0  -> Enemy.createFast(enemyFastTexture);
                default -> Enemy.createTank(enemyTankTexture);
            };
            case HARD -> MathUtils.randomBoolean()
                    ? Enemy.createFast(enemyFastTexture)
                    : Enemy.createTank(enemyTankTexture);
        };
    }

    private void trySpawnMaterial(ArrayList<Material> materials, OrthographicCamera camera, Level level) {
        float camX  = camera.position.x;
        float camY  = camera.position.y;
        float halfW = 400f;
        float halfH = 240f;

        float x = MathUtils.random(camX - halfW + borderSize, camX + halfW - 32 - borderSize);
        float y = MathUtils.random(camY - halfH + borderSize, camY + halfH - 32 - borderSize);

        float rareChance = switch (difficulty) {
            case EASY   -> 0.25f;
            case MEDIUM -> 0.10f;
            case HARD   -> 0.005f;
        };

        Material mat = MathUtils.randomBoolean(rareChance)
                ? Material.createRare(x, y, materialRareTexture)
                : Material.createCommon(x, y, materialTexture);

        if (level.overlapsCollision(mat.bounds)) {
            lastMaterialTime = TimeUtils.nanoTime();
            return;
        }

        materials.add(mat);
        lastMaterialTime = TimeUtils.nanoTime();
    }
}