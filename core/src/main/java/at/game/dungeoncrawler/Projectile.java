package at.game.dungeoncrawler;

import com.badlogic.gdx.math.Rectangle;

public class Projectile {
    public float x, y;
    public float dx, dy;
    public float width, height;
    public float speed;

    public Projectile(float x, float y, float dx, float dy,
                      float width, float height, float speed) {
        this.x      = x - width  / 2f;
        this.y      = y - height / 2f;
        this.dx     = dx;
        this.dy     = dy;
        this.width  = width;
        this.height = height;
        this.speed  = speed;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}