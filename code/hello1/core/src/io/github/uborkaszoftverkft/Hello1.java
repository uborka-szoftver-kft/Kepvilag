package io.github.uborkaszoftverkft;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Hello1 extends ApplicationAdapter {
    private Stage stage;

    @Override
    public void create() {
        stage = new Stage() ;
        final OrthographicCamera stageCamera = (OrthographicCamera) stage.getCamera() ;

        Texture sparrow1Texture = new Texture(Gdx.files.absolute("sparrow1.png"));
        Texture house1 = new Texture(Gdx.files.absolute("house1.png"));

        int X_left = Gdx.graphics.getWidth() / 3 - sparrow1Texture.getWidth() / 2;
        int X_right = Gdx.graphics.getWidth() * 2 / 3 - sparrow1Texture.getWidth() / 2;
        int Y_top = Gdx.graphics.getHeight() * 2 / 3 - sparrow1Texture.getHeight() / 2;
        int Y_bottom = Gdx.graphics.getHeight() / 3 - sparrow1Texture.getHeight() / 2;

        Image sparrow1Image = new Image(sparrow1Texture);
        sparrow1Image.setPosition(X_left, Y_top);
        sparrow1Image.setOrigin(sparrow1Image.getWidth() / 2, sparrow1Image.getHeight() / 2);

        stage.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                if( stageCamera.zoom > 3 ) {
                    return true ;
                } else {
                    stageCamera.zoom += delta ;
                    return false ;
                }
            }
        });
        stage.addActor(sparrow1Image);



    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
