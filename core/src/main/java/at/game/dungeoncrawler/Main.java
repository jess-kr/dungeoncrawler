package at.game.dungeoncrawler;

import com.badlogic.gdx.Game;

public class Main extends Game {

    private AudioManager audio;

    @Override
    public void create() {
        audio = new AudioManager();
        setScreen(new StartScreen(this, audio));
    }

    @Override
    public void dispose() {
        super.dispose();
        audio.dispose();
    }
}