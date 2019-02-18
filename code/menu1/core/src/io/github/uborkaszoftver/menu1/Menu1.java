package io.github.uborkaszoftver.menu1;


import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
        Actor choicePane = newFlingPane( texts, size, skin ) ;

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

    private Actor newChoicePane( String[] texts, Vector2 size, Skin skin  ) {
        final HorizontalGroup group = new HorizontalGroup() ;
        Vector2 innerSize = new Vector2(size.x - 50, size.y - 50 ) ;

        for( final String text : texts ) {
            final Actor textPane = newTextWidget2( text, innerSize, skin ) ;
            group.addActor( textPane ) ;
        }
        final ScrollPane scrollPane = new ScrollPane( group, skin ) ;
        scrollPane.setScrollingDisabled( false, true ) ;
        return scrollPane ;
    }

    private Actor newFlingPane( String[] texts, Vector2 size, Skin skin  ) {
        final Actor[] actors = new Actor[ texts.length ] ;

        for( int i = 0 ; i < texts.length ; i ++ ) {
            final String text = texts[ i ] ;
            final Actor textWidget = newTextWidget( text, skin ) ;
            actors[ i ] = textWidget ;
        }
        final FlingPane flingPane = new FlingPane( size.x, size.y, 10, actors ) ;
        return flingPane ;
    }

    private Actor newTextWidget( String text, Skin skin  ) {
        final Label multilineLabel = new Label( text, skin ) ;
        multilineLabel.setWrap( true ) ;
        return multilineLabel ;
    }

    private Actor newTextWidget2( String text, final Vector2 size, Skin skin  ) {
        final Label multilineLabel = new Label( text, skin ) ;
        multilineLabel.setWrap( true ) ;
        final ScrollPane scrollPane = new ScrollPane( multilineLabel, skin ) ;
        scrollPane.setScrollingDisabled( true, false );
        scrollPane.setFillParent( true ) ;
        final Container< Actor > container = new Container< Actor >( scrollPane ) {
            @Override
            public float getMinWidth() {
                return size.x ;
            }

            @Override
            public float getMinHeight() {
                return size.y ;
            }

            @Override
            public float getPrefWidth() {
                return size.x ;
            }

            @Override
            public float getPrefHeight() {
                return size.y ;
            }

            @Override
            public float getMaxWidth() {
                return size.x ;
            }

            @Override
            public float getMaxHeight() {
                return size.y ;
            }
        };
        return container ;
    }

}