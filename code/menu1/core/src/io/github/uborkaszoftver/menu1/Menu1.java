package io.github.uborkaszoftver.menu1;


import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import java.util.Random;


public class Menu1 extends ApplicationAdapter {
    private Stage stage;
    private Table container;

    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG) ;
        stage = new Stage();
        Skin skin = new Skin(Gdx.files.internal("skin/dark-hdpi/Holo-dark-hdpi.json"));
        Gdx.input.setInputProcessor(stage);

        // Gdx.graphics.setVSync(false);

        container = new Table();
        stage.addActor(container);
        container.setFillParent(true);
        container.setDebug( true, true);

        final String[] texts = newTextualEntries( 5, 10, 200 ) ;
        Vector2 size = new Vector2(400, 500) ;
        Actor choicePane = newSweepChoice( texts, size, skin ) ;
        stage.setScrollFocus( choicePane ) ;

        container.add( choicePane ).size( size.x, size.y );
        container.row().space(10).padBottom(10);
    }

    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Gdx.gl.glViewport(100, 100, width - 200, height - 200);
        // stage.setViewport(800, 600, false, 100, 100, width - 200, height - 200);
    }

    public void dispose() {
        stage.dispose();
    }

    public boolean needsGL20() {
        return false;
    }


    private String[] newTextualEntries(
            final int count,
            final int minLength,
            final int maxLength
    ) {
        final Random random = new Random( 0 ) ;
        final StringBuilder builder = new StringBuilder( maxLength ) ;
        final String[] entries = new String[ count ] ;
        for( int i = 0 ; i < entries.length ; i ++ ) {
            builder.setLength( 0 ) ;
            final int wordCount = random.nextInt( maxLength ) ;
            for( int j = 0 ; j < wordCount - minLength ; j ++ ) {
                builder.append( "word-" ).append( i ).append( '-' ).append( j ).append( ' ' ) ;
            }
            entries[ i ] = builder.toString() ;
        }
        return entries ;
    }

    private Actor newSweepChoice( String[] texts, Vector2 size, Skin skin  ) {
        final Actor[] actors = new Actor[ texts.length ] ;

        for( int i = 0 ; i < texts.length ; i ++ ) {
            final String text = texts[ i ] ;
            final Actor textWidget = newTextWidget( text, skin ) ;
            actors[ i ] = textWidget ;
        }
        final SweepChoice sweepChoice = new SweepChoice( size.x, size.y, 10, actors ) ;
        return sweepChoice;
    }

    private Actor newTextWidget( String text, Skin skin  ) {
        final Label multilineLabel = new Label( text, skin ) ;
        multilineLabel.setWrap( true ) ;
        return multilineLabel ;
    }

}