package at.game.dungeoncrawler;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;

public class Material {
    public Rectangle bounds;
    public int value;       // how many points it gives
    public Texture texture;

    public Material(float x, float y, float width, float height, int value, Texture texture) {
        this.bounds  = new Rectangle(x, y, width, height);
        this.value   = value;
        this.texture = texture;
    }

    // Factory methods for different material types
    public static Material createCommon(float x, float y, Texture texture) {
        return new Material(x, y, 32, 32, 1, texture);
    }

    public static Material createRare(float x, float y, Texture texture) {
        return new Material(x, y, 32, 32, 3, texture);
    }
}