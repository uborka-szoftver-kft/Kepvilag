package io.github.uborkaszoftver.menu1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Contains a collection of {@code Actor}s that may be too high to fit vertically, so it shows one at a time,
 * with horizontal transitions between them and vertical scroll for each.
 * The display area is known as <b>Sight</b>.
 * Each child {@code Actor} is called a <b>Ascender</b>, as its scrolls vertically.
 *
 *
 *
 * <h2>Resize</h2>
 * <p>
 *     The component defines its min/preferred/max size to the value passed to the constructor.
 *     But it may display inside a non-cooperating layout. For this reason, display relies on {@link Actor}'s size.
 * </p>
 *
 * <h2>Naming</h2>
 * <p>
 *
 * </p>
 */
public class FlingPane extends WidgetGroup {

    private final float preferredWidth ;
    private final float preferredHeight ;

    /**
     * The distance between {@link #currentRollIndex} and {@link #disappearingRollIndex}.
     */
    private final float interRollMargin ;

    private int currentRollIndex = 0 ;
    private int disappearingRollIndex = -1 ;

    private boolean currentRollIsCullable = false ;
    private boolean disappearingRollIsCullable = false ;

    /**
     * Gets incremented by {@link InputListener#touchDragged(InputEvent, float, float, int)},
     * gets zeroed by {@link Actor#act(float)}.
     */
    private float scrollAmount = 0 ;

    /**
     * Initial value to compute decreasing {@link #scrollVelocity} from.
     */
    private float initialScrollVelocity = 0 ;

    private float scrollVelocity = 0 ;

    private long inertialScrollingStartTime = 0 ;

    private static final float AXIS_DIFFERENCIATION_FACTOR = 1.3f ;
    private static final float FLING_DURATION_SECOND = 1f ;
    private static final float SCROLL_AMOUNT_CORRECTION = 8f ;
    private static final float SCROLL_VELOCITY_CORRECTION = SCROLL_AMOUNT_CORRECTION / 2 ;

    /**
     * The area in Sight for corresponding Ascender, in its own coordinates.
     * Value of {@link #currentAscenderCullingArea}, and scissors area are derived from it.
     */
    private final Rectangle currentAscenderAreaBounds = new Rectangle() ;

    private final Rectangle currentAscenderCullingArea = new Rectangle() ;
    private final Rectangle disappearingAscenderAreaBounds = new Rectangle() ;
    private final Rectangle disappearingAscenderCullingArea = new Rectangle() ;

    private final Rectangle sightBounds = new Rectangle() ;
    private final Rectangle scissorBounds = new Rectangle() ;

    //private ScrollDirection scrollDirection = null ;
    private OverscrollPhase overscrollPhase = null ;

    /**
     * Same as in {@link com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#flingTimer}.
     * Only mutate with {@link #prepareInertialScrolling(float)}, or by zeroing it.
     */
    private float flingTimer ;

    /**
     * Same as in {@link com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#flingTime}.
     */
    private final float flingDuration = FLING_DURATION_SECOND ;

    private Vector2 lastPoint = new Vector2() ;

    /**
     * {@code null} when no drag or other scroll operation.
     */
    private ScrollAxis scrollAxis = null ;


    private enum ScrollAxis { UNDEFINED, VERTICAL, HORIZONTAL }

    private enum OverscrollPhase {
        FORWARD,
        BACKWARD
    }

    public FlingPane(
            final float sightWidth,
            final float sightHeight,
            final float interRollMargin,
            final Actor... rolls
    ) {
        super( rolls ) ;
        preferredWidth = sightWidth ;
        preferredHeight = sightHeight ;
        this.interRollMargin = interRollMargin ;
        checkInvariants() ;

        addListener( new InputListener() {

            @Override
            public boolean touchDown(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final int button
            ) {
                logDebug( "#touchDown( event=" + event + ", x=" + x + ", y=" + y + ", " +
                        "pointer=" + pointer + ", button=" + button + " )" ) ;
                scrollAxis = null ;
                initialScrollVelocity = 0 ;
                scrollAmount = 0 ;
                scrollVelocity = 0 ;
                flingTimer = 0 ;
                overscrollPhase = null ;
                lastPoint.set( x, y ) ;
                inertialScrollingStartTime = TimeUtils.millis() ;
                getStage().setScrollFocus( FlingPane.this ) ;
                return true ;
            }

            @Override
            public void touchDragged(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer
            ) {
                logDebug( "#touchDragged( event=" + event + ", x=" + x + ", y=" + y + ", " +
                        "pointer=" + pointer + " )" ) ;
                final float deltaX = x - lastPoint.x ;
                final float deltaY = y - lastPoint.y ;
                final ScrollAxis previousScrollAxis = scrollAxis ;
                scrollAxis = resolveScrollAxis( deltaX, deltaY ) ;
                if( scrollAxis == previousScrollAxis || previousScrollAxis == null ) {
                    final float inertialScrollingLastDuration =
                            TimeUtils.timeSinceMillis( inertialScrollingStartTime ) ;
                    inertialScrollingStartTime = TimeUtils.millis() ;
                    final float velocityCorrection = inertialScrollingLastDuration ;
                    switch ( scrollAxis ) {
                        case VERTICAL :
                            scrollAmount = deltaY ;
                            prepareInertialScrolling(deltaY * velocityCorrection ) ;
                            break ;
                        case HORIZONTAL :
                            break ;
                        default : break ;
                    }
                } else {
                    logDebug( "No scroll initiated, scrollAxis=" + scrollAxis +
                            " and previousScrollAxis=" + previousScrollAxis + "." ) ;
                }

                lastPoint.set( x, y ) ;
            }

            @Override
            public void touchUp(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int pointer,
                    final int button
            ) {
                logDebug ( "#touchUp( event=" + event + ", x=" + x + ", y=" + y + ", " +
                        "pointer=" + pointer + ", button=" + button + " )" ) ;
            }

            @Override
            public boolean scrolled(
                    final InputEvent event,
                    final float x,
                    final float y,
                    final int amount
            ) {
                logDebug ( "#scrolled( event=" + event + ", x=" + x + ", y=" + y + ", " +
                        "amount=" + amount + " )" ) ;
                scrollAxis = ScrollAxis.VERTICAL ;
                // How to derive speed from this?
                initialScrollVelocity += amount * SCROLL_VELOCITY_CORRECTION ;
                scrollVelocity = initialScrollVelocity;
                flingTimer = 0 ;
                // scrollAmount += amount * SCROLL_AMOUNT_CORRECTION ;
                return true ;
            }


        } ) ;
/*
        addListener( new ActorGestureListener() {
            @Override
            public void fling(
                    final InputEvent event,
                    final float velocityX,
                    final float velocityY,
                    final int button
            ) {
                logDebug( "#fling( event=" + event + ", vx=" + velocityX + ", " +
                        "vy=" + velocityY + ", button=" + button + " )" ) ;
                final ScrollAxis resolved = resolveScrollAxis( velocityX, velocityY ) ;
                if( resolved == ScrollAxis.VERTICAL ) {
                    FlingPane.this.scrollAxis = ScrollAxis.VERTICAL ;
                    prepareInertialScrolling( velocityY ) ;
                }
            }
        } ) ;
*/
    }

    /**
     * Caller must enforce every invariant. We expect {@link #scrollAmount} to be consistent with
     * {@link #currentRollIndex} and {@link #disappearingRollIndex}.
     */
    private void applyCulling() {
        if( scrollAxis == ScrollAxis.HORIZONTAL ) {
            // Scroll to the right, first on left is current.
            if( currentRollIsCullable ) {
                final Actor currentRoll = getChildren().items[ currentRollIndex ] ;
                currentAscenderCullingArea.x = scrollAmount ;
                currentAscenderCullingArea.y = currentRoll.getY() ;
                currentAscenderCullingArea.height = preferredHeight ;
                currentAscenderCullingArea.width = preferredWidth - scrollAmount ;
                ( ( Cullable ) currentRoll ).setCullingArea( currentAscenderCullingArea ) ;
            }
            if( disappearingRollIsCullable && disappearingRollIndex > -1 ) {  // There could be overscroll.
                final Actor disappearingRoll = getChildren().items[disappearingRollIndex];
                disappearingAscenderCullingArea.x = 0;
                disappearingAscenderCullingArea.y = disappearingRoll.getY();
                disappearingAscenderCullingArea.height = preferredWidth ;
                disappearingAscenderCullingArea.width = preferredHeight + interRollMargin - scrollAmount ;
                ((Cullable) disappearingRoll).setCullingArea( disappearingAscenderCullingArea ) ;
            }
        } else {
            // Scroll to the left, first on left is disappearing.

        }


    }


    public interface RollListener {

        void rollSightChanged(
                int rollIndex,
                boolean mayScrollUp,
                boolean mayScrollLeft,
                boolean mayScrollDown,
                boolean mayScrollRight
        ) ;

        void rollSelected( int rollIndex ) ;

    }


// ==========
// Invariants
// ==========

    private static final boolean CHECK_INVARIANTS = true ;

    void checkInvariants() {
        if( ! CHECK_INVARIANTS ) return ;
        if( scrollAxis == null ) {
            check( overscrollPhase == null, "overscrollPhase should be null" ) ;
            check( disappearingRollIndex < 0, "disappearingRoll should be undefined when not scrolling" ) ;
        } else {
            if( overscrollPhase != null ) {
                check( disappearingRollIndex < 0, "disappearingRoll should be undefined when overscrolling" ) ;
            }
        }

        final Actor currentRoll = getChildren().items[ currentRollIndex ] ;
        if( currentRollIsCullable ) {
            check( currentRoll instanceof Cullable, "currentRoll should be Cullable" ) ;
        }

        if( disappearingRollIndex >= 0 ) {
            final Actor disappearingRoll = getChildren().items[ disappearingRollIndex ] ;
            if( disappearingRollIsCullable ) {
                check( disappearingRoll instanceof Cullable, "disappearingRoll should be Cullable" ) ;
            }
        }
    }

    private void check( boolean expression, String message ) {
        if( ! expression ) {
            throw new AssertionError( message ) ;
        }
    }


// ===
// Act
// ===

    @Override
    public void act( float deltaSecond ) {
        super.act( deltaSecond ) ;
        applyScrolling( deltaSecond ) ;
    }


// ======
// Scroll
// ======

    private void prepareInertialScrolling( final float initialScrollVelocity ) {
        this.initialScrollVelocity = initialScrollVelocity ;
        scrollVelocity = initialScrollVelocity ;
        flingTimer = flingDuration ;
        logDebug( "#prepareInertialScrolling( " + initialScrollVelocity + " )" ) ;
        invalidate() ;
    }

    private void slowDownInertialScrolling( final float deltaSecond ) {
        flingTimer -= deltaSecond ;
        final float flingProgress = flingTimer / flingDuration ;
        scrollVelocity = initialScrollVelocity * flingProgress ;
        scrollAmount += scrollVelocity * ( deltaSecond / flingDuration ) ;
        if( Math.abs( scrollVelocity ) < 0.001 ) {
            scrollAmount = 0 ;
        }

        logDebug( "#slowDownInertialScrolling( " + deltaSecond + " ): " +
                " scrollVelocity=" + scrollVelocity + ", scrollAmount=" + scrollAmount ) ;
    }

    private void applyScrolling( float deltaSecond ) {
        if( scrollAxis != null && scrollAmount != 0 ) {
            final Actor Ascender = getChildren().get( currentRollIndex ) ;
            switch( scrollAxis ) {
                case VERTICAL :
                    logDebug(
                            "Applying scroll amount of " + scrollAmount + " on Y axis. " +
                            "Velocity updated to " + scrollVelocity + ". " +
                            "End of scroll in " + ( flingTimer ) + " s."
                    ) ;
                    Ascender.setY( Ascender.getY() + scrollAmount ) ;
                    break ;
                case HORIZONTAL :
                    break ;
                default :
                    break ;
            }
            scrollAmount = 0 ;
        }

        if( flingTimer <= 0 ) {
            initialScrollVelocity = 0 ;
            scrollVelocity = 0 ;
            scrollAxis = null ;
        } else {
            slowDownInertialScrolling( deltaSecond ) ;
        }
    }


// ======
// Layout
// ======

    private boolean sizeChanged = true ;

    @Override
    protected void sizeChanged() {
        sizeChanged = true ;
        super.sizeChanged() ;
    }


    /**
     * Detects vertical resize between two calls to {@link #layout()}.
     */
    private float previousHeight = -1 ;

    /**
     * Sets every Ascender's coordinates according to {@link #getWidth()} and {@link #getHeight()}.
     * An unscrolled Ascender has its top's y -- not its y -- equal to {@link FlingPane#getHeight()} .
     * For a given {@link #getWidth()} each Ascender's horizontal width and height are constant.
     * Components' reference for coordinates is bottom-left corner.
     *
     * <pre>

    ^ x goes up
    |                 .....      -+
          .....       .   .        > A Ascender may "go up" of this height
          .   .       .   .       |  (until its bottom hits Sight's bottom).
    +---+ +---+ +---+ +---+      -+
    |   | |   | |   | |   |        > This is Sight's height.
    |   | |   | +---+ |   |       |
y=0-+---+ |   |       |   |      -+
    |     |   |       |   |        > For the tallest Ascender, this is the
    x=0   +---+       |   |       |  distance on which it can go "up".
                      +---+      -+
    |   |
    +- -+
      v
      This is Sight's width.

     </pre>
     */
    @Override
    public void layout() {
        logDebug( "#layout() begins: w=" + getWidth() + ", h=" + getHeight() + ", " +
                "currentRollIndex=" + currentRollIndex ) ;
        if ( sizeChanged ) {
            logDebug( "Applying full layout because of size change." ) ;
            Actor previousRoll = null ;
            for( int rollIndex = 0 ; rollIndex < getChildren().size ; rollIndex ++ ) {
                final Actor ascender = getChildren().get( rollIndex ) ;
                boolean isLayout = ascender instanceof Layout ;
                if( isLayout ) {
                    final Layout ascenderAsLayout = ( Layout ) ascender ;
                    ascenderAsLayout.layout() ;
                    // When word-wrapping, a Label returns 0 for its preferred size,
                    // which also depends on current width. So we set current width
                    // before calculating preferred size.
                    if( ascenderAsLayout.getPrefWidth() <= 0 ) {
                        ascender.setWidth( getWidth() ) ;
                    } else {
                        ascender.setWidth( Math.min( ascenderAsLayout.getPrefWidth(), getWidth() ) ) ;
                    }
                    ascender.setHeight( ascenderAsLayout.getPrefHeight() ) ;
                } else {
                    ascender.setSize( getWidth(), getHeight() ) ;
                }

                if( previousRoll == null ) {
                    ascender.setX( 0 ) ;
                } else {
                    ascender.setX( previousRoll.getX() + getWidth() + interRollMargin ) ;
                }

                ascender.setY( getHeight() - ascender.getHeight() ) ;
                if( previousHeight == getHeight() ) {
                } else {
                    // Some resize happened, so we want to maintain Y offset basing on a ratio.
                }

                previousRoll = ascender ;

                logDebug(
                        "Ascender[" + rollIndex + "]: " +
                                "x=" + ascender.getX() + ", " +
                                "y=" + ascender.getY() + ", " +
                                "w=" + ascender.getWidth() + ", " +
                                "h=" + ascender.getHeight()
                ) ;
            }
            sightBounds.set( getWidth() * currentRollIndex, 0, getWidth(), getHeight() ) ;
            previousHeight = getHeight() ;
            sizeChanged = false ;

        }

        logDebug( "#layout() ends." ) ;

    }



// ====
// Draw
// ====

    /**
     * Lot of code copied from {@link com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#draw(Batch, float)}.
     */
    @Override
    public void draw( final Batch batch, final float parentAlpha ) {
        // Triggers a call to layout().
        validate() ;

        final Matrix4 transform = computeTransform() ;
        transform.translate( - currentRollIndex * ( getWidth() + interRollMargin ), 0, 0 ) ;

        // Setup transform for this group.
        applyTransform( batch, transform ) ;

        // Caculate the scissor bounds based on the batch transform, the available widget area and the camera transform.
        // We need to project those to screen coordinates for OpenGL ES to consume.
        getStage().calculateScissors( sightBounds, scissorBounds ) ;

        // Enable scissors for widget area and draw the widget.
        batch.flush();
        if( ScissorStack.pushScissors( scissorBounds ) ) {
            drawChildren( batch, parentAlpha ) ;
            batch.flush() ;
            ScissorStack.popScissors() ;
        }
        resetTransform( batch ) ;

        // logDebug( "#draw(...) complete." );
    }


// ===============
// Layout contract
// ===============

    @Override
    public float getMinWidth() {
        return preferredWidth ;
    }

    @Override
    public float getMinHeight() {
        return preferredHeight ;
    }

    @Override
    public float getPrefWidth() {
        return preferredWidth ;
    }

    @Override
    public float getPrefHeight() {
        return preferredHeight ;
    }

    @Override
    public float getMaxWidth() {
        return preferredWidth ;
    }

    @Override
    public float getMaxHeight() {
        return preferredHeight ;
    }


// =============
// Miscellaneous
// =============

    private static void logDebug( final String message ) {
        Gdx.app.debug( FlingPane.class.getSimpleName(), message ) ;
    }

    private ScrollAxis resolveScrollAxis( float deltaX, float deltaY ) {
        ScrollAxis axis;
        if( Math.abs( deltaY ) > Math.abs( deltaX ) * AXIS_DIFFERENCIATION_FACTOR) {
            axis = ScrollAxis.VERTICAL ;
        } else if( Math.abs( deltaX ) > Math.abs( deltaY ) * AXIS_DIFFERENCIATION_FACTOR) {
            axis = ScrollAxis.HORIZONTAL ;
        } else {
            axis = ScrollAxis.UNDEFINED ;
        }
        return axis;
    }

}
