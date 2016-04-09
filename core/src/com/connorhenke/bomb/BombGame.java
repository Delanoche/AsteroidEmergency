package com.connorhenke.bomb;

import java.util.Iterator;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class BombGame extends ApplicationAdapter {

    private TextureAtlas textureAtlas;
    private Animation rotateAnimation;
    private Texture characterImage;
    private Texture background;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Rectangle character;
    private Array<Bomb> bombs;
    private long lastDropTime;
    private BitmapFont font;

    private int score;
    private int highScore;

    private static final int HEIGHT = 800;
    private static final int WIDTH = 480;

    private static final int LANE_1 = WIDTH / 2 - 64 - 32 - 64 / 2;
    private static final int LANE_2 = WIDTH / 2 - 64 / 2;
    private static final int LANE_3 = WIDTH / 2 + 64 + 32 - 64 / 2;

    private static final int FALLING_SPEED = 500;
    private static final long BOMBS_PER_SECOND = 5;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        // load the images for the droplet and the character, 64x64 pixels each
        textureAtlas = new TextureAtlas(Gdx.files.internal("spritesheet.atlas"));
        characterImage = new Texture(Gdx.files.internal("character.png"));
        background = new Texture(Gdx.files.internal("bg.png"));

        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, WIDTH, HEIGHT);
        batch = new SpriteBatch();

        // create a Rectangle to logically represent the character
        character = new Rectangle();
        character.x = WIDTH / 2 - 64 / 2; // center the character horizontally
        character.y = 20; // bottom left corner of the character is 20 pixels above the bottom screen edge
        character.width = 64;
        character.height = 64;

        font = new BitmapFont();
        score = 0;
        highScore = 0;

        rotateAnimation = new Animation(1/36f, textureAtlas.getRegions());

        // create the bombs array and spawn the first raindrop
        bombs = new Array<Bomb>();
        dropBomb();
    }

    private void dropBomb() {
        Bomb raindrop = new Bomb(rotateAnimation);
        int random = MathUtils.random(0, 3);
        if (random < 1) {
            raindrop.x = LANE_1;
        } else if (random < 2) {
            raindrop.x = LANE_2;
        } else {
            raindrop.x = LANE_3;
        }
        raindrop.y = HEIGHT;
        raindrop.width = 64;
        raindrop.height = 64;
        bombs.add(raindrop);
        lastDropTime = TimeUtils.millis();
    }

    @Override
    public void render() {
        // clear the screen with a dark blue color. The
        // arguments to glClearColor are the red, green
        // blue and alpha component in the range [0,1]
        // of the color to be used to clear the screen.
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // tell the camera to update its matrices.
        camera.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        batch.setProjectionMatrix(camera.combined);

        // begin a new batch and draw the character and
        // all drops
        batch.begin();
        batch.draw(background, 0, 0);
        batch.draw(characterImage, character.x, character.y);
        for(Bomb raindrop: bombs) {
            raindrop.elapsedTime += Gdx.graphics.getDeltaTime();
            batch.draw(raindrop.animation.getKeyFrame(raindrop.elapsedTime, true), raindrop.x, raindrop.y);
        }
        font.draw(batch, "Score: " + score, 10, HEIGHT - 10);
        font.draw(batch, "High Score: " + highScore, 10, HEIGHT - 30);
        batch.end();

        character.x = LANE_2;

        // process user input
        if(Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            if (touchPos.x < WIDTH / 2) {
                character.x = LANE_1;
            } else {
                character.x = LANE_3;
            }
        }

        // check if we need to create a new bomb
        if(TimeUtils.millis() - lastDropTime > 1000 / BOMBS_PER_SECOND) {
            dropBomb();
        }

        // move the bombs, remove any that are beneath the bottom edge of
        // the screen or that hit the character. In the later case we play back
        // a sound effect as well.
        Iterator<Bomb> iter = bombs.iterator();
        while(iter.hasNext()) {
            Bomb raindrop = iter.next();
            raindrop.y -= FALLING_SPEED * Gdx.graphics.getDeltaTime();
            if(raindrop.y + 64 < 0) {
                // Boom
                iter.remove();
                score = 0;
            } else if (raindrop.y - (20 + 64) < 0 && raindrop.y - (20 + 64) > -20) {
                if (isInLane(raindrop, getLane(character))) {
                    iter.remove();
                    score++;
                    if (score > highScore) {
                        highScore = score;
                    }
                }
            }
        }
    }

    private int getLane(Rectangle body) {
        if (body.x == LANE_1) {
            return 1;
        } else if (body.x == LANE_2) {
            return 2;
        } else if (body.x == LANE_3) {
            return 3;
        }
        return -1;
    }

    private boolean isInLane(Rectangle body, int lane) {
        if (lane == 1) {
            return body.x == LANE_1;
        } else if (lane == 2) {
            return body.x == LANE_2;
        } else if (lane == 3) {
            return body.x == LANE_3;
        }
        return false;
    }

    @Override
    public void dispose() {
        // dispose of all the native resources
        textureAtlas.dispose();
        characterImage.dispose();
        batch.dispose();
    }
}