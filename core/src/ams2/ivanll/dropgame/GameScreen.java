package ams2.ivanll.dropgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;

import v4lk.lwbd.util.Beat;

public class GameScreen implements Screen {

    final Drop drop;

    long timeStart;
    long time;

    Beat[] beats;
    float mean;
    int beatsIterator = 0;

    Texture dropImage;
    Texture bucketImage;
    Sound dropSound;
    Music mainMusic;
    OrthographicCamera camera;
    Rectangle bucket;
    Array<Rectangle> raindrops;

    boolean gameOver = false;
    long lastDropTime;
    int dropsGathered;

    public GameScreen(final Drop drop) {
        this.drop = drop;

        // Song BPM identification
        // TODO Impossible to implement lwbd's Beat Detector in real time in an Android system. Next solution? Generate and store the music data into a readable file externally and then add it to the game.
        // InputStream audioRealFile = Gdx.files.internal("technology.mp3").read();
        InputStream beatsFile = Gdx.files.internal("songs/technology/technology.mp3.sngtrck").read();
        try {
            ObjectInputStream ois = new ObjectInputStream(beatsFile);
            beats = (Beat[])ois.readObject();
            float sum = 0;
            for (Beat beat : beats)
                sum += beat.energy;
            mean = sum / beats.length;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        timeStart = System.currentTimeMillis();

        // load the images for the droplet and the bucket, 64x64 pixels each
        dropImage = new Texture(Gdx.files.internal("water-drop.png"));
        bucketImage = new Texture(Gdx.files.internal("bucket.png"));

        // load the drop sound effect and the rain background "music"
        dropSound = Gdx.audio.newSound(Gdx.files.internal("water-drop.mp3"));
        mainMusic = Gdx.audio.newMusic(Gdx.files.internal("songs/technology/technology.mp3"));
        mainMusic.setLooping(true);

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        // create a Rectangle to logically represent the bucket
        bucket = new Rectangle();
        bucket.x = 800 / 2 - 64 / 2; // center the bucket horizontally
        bucket.y = 20; // bottom left corner of the bucket is 20 pixels above
        // the bottom screen edge
        bucket.width = 64;
        bucket.height = 64;

        // create the raindrops array and spawn the first raindrop
        raindrops = new Array<Rectangle>();
        spawnRaindrop();

    }

    private void spawnRaindrop() {
        Rectangle raindrop = new Rectangle();
        raindrop.x = MathUtils.random(0, 800 - 64);
        raindrop.y = 480;
        raindrop.width = 64;
        raindrop.height = 64;
        raindrops.add(raindrop);
        lastDropTime = TimeUtils.nanoTime();
    }

    @Override
    public void render(float delta) {
        // clear the screen with a dark blue color. The
        // arguments to glClearColor are the red, green
        // blue and alpha component in the range [0,1]
        // of the color to be used to clear the screen.
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        drop.batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the bucket and
        // all drops
        drop.batch.begin();
        drop.font.draw(drop.batch, "Drops Collected: " + dropsGathered, 0, 480);
        drop.batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height);
        for (Rectangle raindrop : raindrops) {
            drop.batch.draw(dropImage, raindrop.x, raindrop.y);
        }
        drop.batch.end();

        // process user input
        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            bucket.x = touchPos.x - 64 / 2;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
            bucket.x -= 200 * Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            bucket.x += 200 * Gdx.graphics.getDeltaTime();

        // make sure the bucket stays within the screen bounds
        if (bucket.x < 0)
            bucket.x = 0;
        if (bucket.x > 800 - 64)
            bucket.x = 800 - 64;

        // Check if we need to create a new raindrop
        time = System.currentTimeMillis() - timeStart;
        System.out.println("Time: " + time + "\n" +
                "Beat time:" + beats[beatsIterator].timeMs);
        if (time >= beats[beatsIterator].timeMs) {
            if (beats[beatsIterator].energy >= (mean + 0.20))
                spawnRaindrop();
            beatsIterator++;
        }

        // move the raindrops, remove any that are beneath the bottom edge of
        // the screen or that hit the bucket. In the later case we increase the
        // value our drops counter and add a sound effect.
        Iterator<Rectangle> iter = raindrops.iterator();
        while (iter.hasNext()) {
            Rectangle raindrop = iter.next();
            raindrop.y -= 200 * Gdx.graphics.getDeltaTime();

            // Game over state
            if (raindrop.y + 64 < 0) {
                raindrops.clear();
                mainMusic.stop();
                gameOver = true;
                drop.gameOverSound.play();
                drop.setScreen(new GameOverScreen(drop));
                dispose();
            }

            // If the bucket grabs the raindrop
            if (raindrop.overlaps(bucket) && raindrop.y > bucket.y) {
                dropsGathered++;
                dropSound.play();
                iter.remove();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        mainMusic.play();
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        dropImage.dispose();
        bucketImage.dispose();
        dropSound.dispose();
        mainMusic.dispose();
    }

}
