package at.game.dungeoncrawler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

import java.util.ArrayList;

public class Player {
    public enum CharacterType {
        WITCH, ADVENTURER
    }

    public Rectangle bounds;
    public float speed;
    public int lives;
    public Texture texture;
    public CharacterType characterType;
    public Level level;

    // Attack properties determined by character type
    public Texture projectileTexture;
    public long attackCooldown;
    public float projectileSpeed;
    public float projectileWidth;
    public float projectileHeight;

    // Animation
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private boolean facingRight = true;

    public Player(float x, float y, float width, float height,
            float speed, int lives, Texture texture, CharacterType characterType, Level level) {
        this.bounds = new Rectangle(x, y, width, height);
        this.speed = speed;
        this.lives = lives;
        this.texture = texture;
        this.characterType = characterType;
        this.level = level;
        setupAttack();
    }

    private void setupAttack() {
        switch (characterType) {
            case WITCH -> {
                attackCooldown = 500_000_000L;
                projectileSpeed = 600f;
                projectileWidth = 48f;
                projectileHeight = 12f;
            }
            case ADVENTURER -> {
                attackCooldown = 800_000_000L;
                projectileSpeed = 500f;
                projectileWidth = 32f;
                projectileHeight = 8f;
            }
        }
    }

    public void setProjectileTexture(Texture texture) {
        this.projectileTexture = texture;
    }

    // Returns a new projectile aimed at the mouse position
    public Projectile shoot(OrthographicCamera camera) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();

        Vector3 mouseWorld = new Vector3(mouseX, mouseY, 0);
        camera.unproject(mouseWorld);

        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;

        float dx = mouseWorld.x - centerX;
        float dy = mouseWorld.y - centerY;

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0)
            return null;
        dx /= len;
        dy /= len;

        return new Projectile(centerX, centerY, dx, dy,
                projectileWidth, projectileHeight, projectileSpeed);
    }

  public void draw(SpriteBatch batch) {
    if (animation != null) {
        TextureRegion frame = animation.getKeyFrame(stateTime, true);

        float frameW = 134f; // actual sprite width
        float frameH = 105f; // actual sprite height

        float drawX = bounds.x - (frameW - bounds.width) / 2f;
        float drawY = bounds.y - (frameH - bounds.height) / 2f;

        if (!facingRight) {
            batch.draw(frame, drawX + frameW, drawY, -frameW, frameH);
        } else {
            batch.draw(frame, drawX, drawY, frameW, frameH);
        }
    } else {
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
    }
}

    public boolean isDead() {
        return lives <= 0;
    }

    public void reset(float x, float y) {
        bounds.x = x;
        bounds.y = y;
        lives = 5;
    }

    public void move(float dx, float dy, float delta, ArrayList<Shape2D> collisions, boolean isMoving) {
        float oldX = bounds.x;
        float oldY = bounds.y;

        // normalize (prevents faster diagonal movement)
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len != 0) {
            dx /= len;
            dy /= len;
        }
        if (isMoving){
        // move X
        bounds.x += dx * speed * delta;
    if (level.overlapsCollision(bounds)) {
        bounds.x = oldX;
    }

        // move Y
        bounds.y += dy * speed * delta;
    if (level.overlapsCollision(bounds)) {
        bounds.y = oldY;
    }
    }
}

    public void createAnimation(Texture spritesheet) {
        TextureRegion[][] tmp = TextureRegion.split(spritesheet, 134, 105);
        TextureRegion[] frames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) {
            frames[i] = tmp[0][i];
        }
        animation = new Animation<>(0.1f, frames);
        stateTime = 0f;

    }

   public void update(float delta, boolean moving, float dx, float dy, Level level) {
    if (moving) {
        // 1. Animation & Blickrichtung aktualisieren
        if (animation != null) {
            stateTime += delta;
            if (dx < 0) facingRight = false;
            if (dx > 0) facingRight = true;
        }

        // 2. KOLLISIONSLOGIK
        float oldX = bounds.x;
        float oldY = bounds.y;
        float speed = this.speed;
        // Bewegung auf der X-Achse prüfen
        bounds.x += dx * speed * delta;
        if (level.overlapsCollision(bounds)) {
            bounds.x = oldX; // Bei Kollision zurücksetzen
        }

        // Bewegung auf der Y-Achse prüfen
        bounds.y += dy * speed * delta;
        if (level.overlapsCollision(bounds)) {
            bounds.y = oldY; // Bei Kollision zurücksetzen
        }
    }
}

}