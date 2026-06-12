package at.game.dungeoncrawler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;

import java.util.ArrayList;


public class Level {
    private TiledMap map;
    private TiledMapRenderer renderer;
    private int[] bgLayerIndices;
    private int[] fgLayerIndices;
    public ArrayList<Shape2D> collisions = new ArrayList<>();
    private final Polygon playerHelperPoly = new Polygon(new float[8]);
    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private static final float SCALE = 5f;

    public Level(String tmxPath) {
        TmxMapLoader.Parameters params = new TmxMapLoader.Parameters();
        params.convertObjectToTileSpace = false;
        map = new TmxMapLoader().load(tmxPath, params);
        renderer = new OrthogonalTiledMapRenderer(map, SCALE);
        buildCollisions();
        buildLayerIndices();
    }

    private void buildCollisions() {
        if (map.getLayers().get("collisions") == null)
            return;

        MapObjects objects = map.getLayers().get("collisions").getObjects();

        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                collisions.add(new Rectangle(
                        rect.x * SCALE,
                        rect.y * SCALE,
                        rect.width * SCALE,
                        rect.height * SCALE));
            } else if (obj instanceof EllipseMapObject) {
                Ellipse e = ((EllipseMapObject) obj).getEllipse();

                // 1. Alle Rohdaten skalieren
                float x = e.x * SCALE;
                float y = e.y * SCALE;
                float width = e.width * SCALE;
                float height = e.height * SCALE;

                int segments = 12;
                float[] vertices = new float[segments * 2];


                float centerX = x + width / 2f;
                float centerY = y + height / 2f;
                float radiusX = width / 2f;
                float radiusY = height / 2f;

                for (int i = 0; i < segments; i++) {
                    float angle = (float) (i * 2 * Math.PI / segments);
                    vertices[i * 2] = centerX + radiusX * (float) Math.cos(angle);
                    vertices[i * 2 + 1] = centerY + radiusY * (float) Math.sin(angle);
                }
                collisions.add(new Polygon(vertices));
            }
        }
    }

    public float getScale() {
        return SCALE;
    }

    public void update(float delta) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
    }

    public void renderBackground(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(bgLayerIndices);
    }

    public void renderForeground(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(fgLayerIndices);
    }


    public void dispose() {
        map.dispose();
        shapeRenderer.dispose();
    }

    public float getMapWidth() {
    if (map.getLayers().getCount() > 0 && map.getLayers().get(0) instanceof TiledMapTileLayer) {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
        return layer.getWidth() * layer.getTileWidth() * SCALE;
    }
    return 0;
}
    public float getMapHeight() {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(0);
        return layer.getHeight() * layer.getTileHeight() * SCALE;
    }

    private void buildLayerIndices() {
        // Layers that render BEHIND the player
        String[] bgNames = {
            "water_floor3",
            "walls_under_water",
            "water_detailization2",
            "Floor2_pool",
            "water_detailization",
            "Floor2_darker_surface",
            "Floor",
            "Floor_darker_surface",
            "Objects_under_wall",
            "Walls",
            "Windows",
            "Lights"};

        // Layers that render IN FRONT of the player
        String[] fgNames = { "Objects", "Objects2" };

        bgLayerIndices = resolveLayerIndices(bgNames);
        fgLayerIndices = resolveLayerIndices(fgNames);
    }

    private int[] resolveLayerIndices(String[] names) {
        int[] result = new int[names.length];
        int count = 0;
        for (String name : names) {
            if (map.getLayers().get(name) != null) {
                result[count++] = map.getLayers().getIndex(name);
            }
        }
        return java.util.Arrays.copyOf(result, count);
    }

    public boolean overlapsCollision(Rectangle rect) {
        for (Shape2D wall : collisions) {
            // 1. RECHTECK GEGEN RECHTECK
            if (wall instanceof Rectangle) {
                if (rect.overlaps((Rectangle) wall)) {
                    return true;
                }
            }
            // 2. RECHTECK GEGEN POLYGON
            else if (wall instanceof Polygon) {
                Polygon polyWall = (Polygon) wall;


                if (rect.overlaps(polyWall.getBoundingRectangle())) {

                    float[] v = playerHelperPoly.getVertices();
                    v[0] = rect.x;
                    v[1] = rect.y;
                    v[2] = rect.x + rect.width;
                    v[3] = rect.y;
                    v[4] = rect.x + rect.width;
                    v[5] = rect.y + rect.height;
                    v[6] = rect.x;
                    v[7] = rect.y + rect.height;

                    playerHelperPoly.setPosition(0, 0);
                    if (Intersector.overlapConvexPolygons(polyWall, playerHelperPoly)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void renderDebug(OrthographicCamera camera) {
    shapeRenderer.setProjectionMatrix(camera.combined);

    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

    for (Shape2D wall : collisions) {
        if (wall instanceof Rectangle) {
            Rectangle r = (Rectangle) wall;
            shapeRenderer.setColor(1, 0, 0, 1); // red
            shapeRenderer.rect(r.x, r.y, r.width, r.height);

        } else if (wall instanceof Polygon) {
            Polygon p = (Polygon) wall;
            shapeRenderer.setColor(0, 1, 0, 1); // green
            float[] verts = p.getTransformedVertices();
            for (int i = 0; i < verts.length; i += 2) {
                int next = (i + 2) % verts.length;
                shapeRenderer.line(verts[i], verts[i+1], verts[next], verts[next+1]);
            }
        }
    }

    shapeRenderer.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
}
}
