package at.game.dungeoncrawler;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Enemy {
    public Rectangle bounds;
    public float speed;
    public int damage;

    private Animation<TextureRegion> animation;
    private float stateTime = 0f;

    public Enemy(float x, float y, float width, float height, float speed, int damage, Texture spritesheet) {
        this.bounds  = new Rectangle(x, y, width, height);
        this.speed   = speed;
        this.damage  = damage;
        this.animation = createAnimation(spritesheet);
    }

    private Animation<TextureRegion> createAnimation(Texture spritesheet) {
        // Split the 16x16 spritesheet into 6 frames (all in one row)
        TextureRegion[][] tmp = TextureRegion.split(spritesheet, 16, 16);
        TextureRegion[] frames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            frames[i] = tmp[0][i];
        }
        return new Animation<>(0.1f, frames); // 0.1f = time per frame
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = animation.getKeyFrame(stateTime, true); // true = looping
        batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public static Enemy createBasic(Texture spritesheet) {
        return new Enemy(0, 0, 48, 48, 150, 1, spritesheet);
    }

    public static Enemy createFast(Texture spritesheet) {
        return new Enemy(0, 0, 32, 32, 280, 1, spritesheet);
    }

    public static Enemy createTank(Texture spritesheet) {
        return new Enemy(0, 0, 72, 72, 80, 2, spritesheet);
    }

    public void move(float dx, float dy, float speed, float delta, Level level) {
    float oldX = bounds.x;
        float oldY = bounds.y;

        // X-Achse separat prüfen (für Wall-Sliding)
        bounds.x += dx * speed * delta;
        if (level.overlapsCollision(bounds)) {
            bounds.x = oldX;
        }

        // Y-Achse separat prüfen
        bounds.y += dy * speed * delta;
        if (level.overlapsCollision(bounds)) {
            bounds.y = oldY;
        }
}
}