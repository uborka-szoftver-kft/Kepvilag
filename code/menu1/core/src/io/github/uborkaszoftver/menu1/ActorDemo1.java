package io.github.uborkaszoftver.menu1;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * https://stackoverflow.com/a/18134946/1923328
 */
public class ActorDemo1 implements ApplicationListener {
    private Stage stage;
    private Skin skin;

    @Override public void create() {
        Gdx.app.log("CREATE", "App Opening");

        this.stage = new Stage();
        Gdx.input.setInputProcessor(this.stage);

        Skin skin = new Skin(Gdx.files.internal("skin/dark-hdpi/Holo-dark-hdpi.json"));

        final Table table = new Table();
        table.setFillParent(true);

        final Button btnLab = new TextButton("Lab", skin);
        final Button btnSea = new TextButton("Sea", skin);


        setupButton(table, btnLab);
        setupButton(table, btnSea);

        final Button btn3 = new TextButton("B3", skin);
        final Button btn4 = new TextButton("B4", skin);
        addClickListenerTo(btn3);
        addClickListenerTo(btn4);
        final Group horizontalGroup = new VerticalGroup() ;
        horizontalGroup.addActor( btn3 ) ;
        horizontalGroup.addActor( btn4 ) ;
        table.row() ;
        table.add( horizontalGroup ) ;


        this.stage.addActor(table);

        Gdx.gl20.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void setupButton(Table table, final Button button) {
        table.add(button).expandX().center();

        addClickListenerTo(button) ;
    }

    private void addClickListenerTo(final Button button ) {
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("XY", "[" + x + ", " + y + "]");
                Gdx.app.log("Event", "[" + event.getStageX() + ", " + event.getStageY() + "]");
                Gdx.app.log("Actor", "[" + button.getX() + ", " + button.getY() + "]");

                Vector2 loc = new Vector2(button.getX(), button.getY());
                Vector2 stageLoc = button.localToStageCoordinates(loc);
                Gdx.app.log("ActorStage", "[" + stageLoc.x + ", " + stageLoc.y + "]");

                Vector2 zeroLoc = button.localToStageCoordinates(new Vector2());
                Gdx.app.log("ZeroStage", "[" + zeroLoc.x + ", " + zeroLoc.y + "]");
            }
        });
    }

    @Override public void render() {
        this.stage.act();

        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl20.glEnable(GL20.GL_BLEND);

        this.stage.draw();
    }

    @Override public void dispose() {
        Gdx.app.log("DISPOSE", "App Closing");
    }

    @Override public void resize(final int width, final int height) {
        Gdx.app.log("RESIZE", width + "x" + height);
        Gdx.gl20.glViewport(0, 0, width, height);
        //this.stage.setViewport(width, height, false);
    }

    @Override public void pause() {}

    @Override public void resume() {}
}