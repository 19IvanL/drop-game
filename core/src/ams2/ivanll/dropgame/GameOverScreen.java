package ams2.ivanll.dropgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class GameOverScreen implements Screen {

    final Drop drop;

    OrthographicCamera camera;
    Texture imposter;
    TextureRegion mainBackground;

    public GameOverScreen(final Drop drop) {
        this.drop = drop;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        imposter = new Texture(Gdx.files.internal("imposter.jpeg"));
        mainBackground = new TextureRegion(imposter, 0, 0, 320, 180);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        drop.batch.begin();
        drop.batch.draw(mainBackground, 0, 0, 800, 480);
        drop.font.draw(drop.batch, "Game over :( ", 100, 150);
        drop.font.draw(drop.batch, "Tap anywhere to try again!", 100, 100);
        drop.batch.end();

        if (Gdx.input.isTouched() && !drop.gameOverSound.isPlaying()) {
            drop.setScreen(new GameScreen(drop));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

}
