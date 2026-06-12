package at.game.dungeoncrawler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

public class AudioManager {

    private Music backgroundMusic;
    private Sound hitSound;
    private Sound collectSound;
    private Sound shootSound;

    private float musicVolume = 0.2f;

    public AudioManager() {
        hitSound       = Gdx.audio.newSound(Gdx.files.internal("audio/hit.wav"));
        collectSound   = Gdx.audio.newSound(Gdx.files.internal("audio/collect.wav"));
        shootSound     = Gdx.audio.newSound(Gdx.files.internal("audio/shoot.wav"));
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/background.wav"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(musicVolume);
        backgroundMusic.play();
    }

    public void playHit()     { if (hitSound     != null) hitSound.play(); }
    public void playCollect() { if (collectSound != null) collectSound.play(); }
    public void playShoot()   { if (shootSound   != null) shootSound.play(); }

    public float getMusicVolume() { return musicVolume; }

    public void setMusicVolume(float volume) {
        musicVolume = MathUtils.clamp(volume, 0f, 1f);
        backgroundMusic.setVolume(musicVolume);
    }

    public void dispose() {
        backgroundMusic.dispose();
        hitSound.dispose();
        collectSound.dispose();
        shootSound.dispose();
    }
}
