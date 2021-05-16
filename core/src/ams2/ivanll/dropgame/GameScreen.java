package ams2.ivanll.dropgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;

public class GameScreen implements Screen {

    final Drop drop;

    TextureRegion background;

    Texture dropImage;
    Texture bucketImage;
    Sound dropSound;
    Music rainMusic;
    OrthographicCamera camera;
    Rectangle bucket;
    Array<Rectangle> raindrops;

    boolean gameOver = false;
    long lastDropTime;
    int dropsGathered;

    private static final int FRAME_COLS = 4, FRAME_ROWS = 2;
    Animation<TextureRegion> splashAnimation;
    float splashAnimationRuntime = 0;
    boolean animationBool;

    public GameScreen(final Drop drop) {
        this.drop = drop;

        Texture backgroundImage = new Texture(Gdx.files.internal("background.jpg"));
        background = new TextureRegion(backgroundImage, 0, 0, 640, 426);

        // Load the sprite sheet as a Texture
        Texture splashSheet = new Texture(Gdx.files.internal("splash.png"));

        // Use the split utility method to create a 2D array of TextureRegions. This is
        // possible because this sprite sheet contains frames of equal size and they are
        // all aligned.
        TextureRegion[][] tmp = TextureRegion.split(splashSheet,
                splashSheet.getWidth() / FRAME_COLS,
                splashSheet.getHeight() / FRAME_ROWS);

        // Place the regions into a 1D array in the correct order, starting from the top
        // left, going across first. The Animation constructor requires a 1D array.
        TextureRegion[] splashFrames = new TextureRegion[FRAME_COLS * FRAME_ROWS];
        int index = 0;
        for (int i = 0; i < FRAME_ROWS; i++) {
            for (int j = 0; j < FRAME_COLS; j++) {
                splashFrames[index++] = tmp[i][j];
            }
        }

        // Initialize the Animation with the frame interval and array of frames
        splashAnimation = new Animation<TextureRegion>(0.05f, splashFrames);

        // load the images for the droplet and the bucket, 64x64 pixels each
        dropImage = new Texture(Gdx.files.internal("water-drop.png"));
        bucketImage = new Texture(Gdx.files.internal("bucket.png"));

        // load the drop sound effect and the rain background "music"
        dropSound = Gdx.audio.newSound(Gdx.files.internal("water-drop.mp3"));
        rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
        rainMusic.setLooping(true);

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        // create a Rectangle to logically represent the bucket
        bucket = new Rectangle();
        bucket.x = 800f / 2f - 64f / 2f; // center the bucket horizontally
        bucket.y = 20; // bottom left corner of the bucket is 20 pixels above
        // the bottom screen edge
        bucket.width = 64;
        bucket.height = 64;

        // create the raindrops array and spawn the first raindrop
        raindrops = new Array<Rectangle>();
        spawnRaindrop();
    }

    @Override
    public void render(float delta) {
        drop.batch.begin();
        drop.batch.draw(background, 0, 0, 800, 480);
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

        drop.font.draw(drop.batch, "Drops Collected: " + dropsGathered, 0, 480);
        drop.batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height);
        for (Rectangle raindrop : raindrops) {
            drop.batch.draw(dropImage, raindrop.x, raindrop.y);
        }

        // process user input
        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            bucket.x = touchPos.x - (64f / 2f);
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
        if ((TimeUtils.nanoTime() - lastDropTime > 1000000000) && !gameOver)
            spawnRaindrop();

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
                rainMusic.stop();
                gameOver = true;
                drop.gameOverSound.play();
                drop.setScreen(new GameOverScreen(drop));
                dispose();
            }

            // If the bucket grabs the raindrop
            if (raindrop.overlaps(bucket) && raindrop.getY() > bucket.getY()) {
                dropsGathered++;
                dropSound.play();
                splashAnimationRuntime = 0;
                animationBool = true;
                iter.remove();
            }
        }

        if (animationBool)
            playSplash(bucket.getX(), bucket.getY());

        drop.batch.end();
    }

    private void playSplash(final float x, final float y) {
        if (!splashAnimation.isAnimationFinished(splashAnimationRuntime)) {
            splashAnimationRuntime += Gdx.graphics.getDeltaTime();
            drop.batch.draw(splashAnimation.getKeyFrame(splashAnimationRuntime), x,  y);
        } else
            animationBool = false;
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
    public void resize(int width, int height) {}

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        rainMusic.play();
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
        rainMusic.dispose();
    }

}
