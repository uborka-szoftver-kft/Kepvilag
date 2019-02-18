package io.github.uborkaszoftver.menu1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

/**
 * Contains a collection of {@code Actor}s that may be too high to fit vertically, so it shows one at a time,
 * with horizontal transitions between them and vertical scroll for each.
 * The display area is known as <b>Sight</b>.
 * Each child {@code Actor} is called a <b>Roll</b>, as its scrolls vertically.
 *
 *
 *
 * <h2>Resize</h2>
 * <p>
 *     The component defines its min/preferred/max size to the value passed to the constructor.
 *     But it may display inside a non-cooperating layout. For this reason, display relies on {@link Actor}'s size.
 * </p>
 *
 */
public class FlingPane extends WidgetGroup {

    private final float preferredWidth ;
    private final float preferredHeight ;

    /**
     * The distance between {@link #currentRollIndex} and {@link #disappearingRollIndex}.
     */
    private final float interRollMargin ;

    private int currentRollIndex = 2 ;
    private int disappearingRollIndex = -1 ;

    private boolean currentRollIsCullable = false ;
    private boolean disappearingRollIsCullable = false ;


    private float scrollVelocityX = 0 ;
    private float scrollVelocityY = 0 ;

    private float scrollAmountX = 0 ;

    /**
     * The area in Sight for corresponding Roll, in its own coordinates.
     * Value of {@link #currentRollCullingArea}, and scissors area are derived from it.
     */
    private final Rectangle currentRollAreaBounds = new Rectangle() ;

    private final Rectangle currentRollCullingArea = new Rectangle() ;
    private final Rectangle disappearingRollAreaBounds = new Rectangle() ;
    private final Rectangle disappearingRollCullingArea = new Rectangle() ;

    private final Rectangle sightBounds = new Rectangle() ;
    private final Rectangle scissorBounds = new Rectangle() ;

    private ScrollDirection scrollDirection = null ;
    private OverscrollPhase overscrollPhase = null ;

    private enum ScrollDirection {
        LEFT( false ),
        RIGHT( false ),
        UP( true ),
        DOWN( true ),
        ;

        ScrollDirection( boolean vertical ) {
            this.vertical = vertical ;
            this.horizontal = ! vertical ;
        }

        private final boolean vertical ;
        private final boolean horizontal ;
    }

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
    }

    /**
     * Caller must enforce every invariant. We expect {@link #scrollAmountX} to be consistent with
     * {@link #currentRollIndex} and {@link #disappearingRollIndex}.
     */
    private void applyCulling() {
        if( scrollDirection != null ) {
            if( scrollDirection.horizontal ) {
                // Scroll to the right, first on left is current.
                if( currentRollIsCullable ) {
                    final Actor currentRoll = getChildren().items[ currentRollIndex ] ;
                    currentRollCullingArea.x = scrollAmountX ;
                    currentRollCullingArea.y = currentRoll.getY() ;
                    currentRollCullingArea.height = preferredHeight ;
                    currentRollCullingArea.width = preferredWidth - scrollAmountX ;
                    ( ( Cullable ) currentRoll ).setCullingArea( currentRollCullingArea ) ;
                }
                if( disappearingRollIsCullable && disappearingRollIndex > -1 ) {  // There could be overscroll.
                    final Actor disappearingRoll = getChildren().items[disappearingRollIndex];
                    disappearingRollCullingArea.x = 0;
                    disappearingRollCullingArea.y = disappearingRoll.getY();
                    disappearingRollCullingArea.height = preferredWidth ;
                    disappearingRollCullingArea.width = preferredHeight + interRollMargin - scrollAmountX;
                    ((Cullable) disappearingRoll).setCullingArea(disappearingRollCullingArea);
                }
            } else {
                // Scroll to the left, first on left is disappearing.

            }


        } else {

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
        if( scrollDirection == null ) {
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
    }


// ======
// Scroll
// ======

    private void applyScroll() {

    }

// ======
// Layout
// ======

    @Override
    protected void sizeChanged() {
        super.sizeChanged() ;
    }


    private float previousHeight = -1 ;

    /**
     * Sets every Roll's coordinates according to {@link #getWidth()} and {@link #getHeight()}.
     * An unscrolled Roll has its top's y -- not its y -- equal to {@link FlingPane#getHeight()} .
     * For a given {@link #getWidth()} each Roll's horizontal width and height are constant.
     * Components' reference for coordinates is bottom-left corner.
     *
     * <pre>

    ^ x goes up
    |                 .....      -+
          .....       .   .        > A Roll may "go up" of this height
          .   .       .   .       |  (until its bottom hits Sight's bottom).
    +---+ +---+ +---+ +---+      -+
    |   | |   | |   | |   |        > This is Sight's height.
    |   | |   | +---+ |   |       |
y=0-+---+ |   |       |   |      -+
    |     |   |       |   |        > For the tallest Roll, this is the
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
        Gdx.app.debug(
                FlingPane.class.getSimpleName(),
                "Layout begins: w=" + getWidth() + ", h=" + getHeight() + ", " +
                        "currentRollIndex=" + currentRollIndex
        ) ;
        Actor previousRoll = null ;
        for( int rollIndex = 0 ; rollIndex < getChildren().size ; rollIndex ++  ) {
            final Actor roll = getChildren().get( rollIndex ) ;
            boolean isLayout = roll instanceof Layout ;
            final float newRollWidth, newRollHeight ;
            if( isLayout ) {
                final Layout rollAsLayout = ( Layout ) roll ;
                newRollWidth = rollAsLayout.getPrefWidth() > 0 ?
                        Math.min( rollAsLayout.getPrefWidth(), getWidth() ) : getWidth() ;
                newRollHeight = rollAsLayout.getPrefHeight() ;
            } else {
                newRollWidth = getWidth() ;
                newRollHeight = getHeight() ;
            }
            final float previousRollHeight = roll.getHeight() ;
            roll.setSize( newRollWidth, newRollHeight ) ;

            if( previousRoll == null ) {
                roll.setX( 0 ) ;
            } else {
                roll.setX( previousRoll.getX() + getWidth() + interRollMargin ) ;
            }
            float previousRollOffsetY = previousHeight - previousRollHeight;
            float newRollOffsetY = getHeight() - roll.getHeight();
            if( roll.getY() > previousRollOffsetY) {
                // The Roll was scrolled up, so we keep the scrolling ratio regarding new height.
                final float previousScrollRatio = previousRollOffsetY / roll.getY() ;
                roll.setY( newRollOffsetY * previousScrollRatio ) ;
            } else {
                roll.setY( newRollOffsetY ) ;
            }
            Gdx.app.debug(
                    FlingPane.class.getSimpleName(),
                    "Roll[" + rollIndex + "]: " +
                            "x=" + roll.getX() + ", " +
                            "y=" + roll.getY() + ", " +
                            "w=" + roll.getWidth() + ", " +
                            "h=" + roll.getHeight()
            ) ;
            previousRoll = roll ;
        }
        previousHeight = getHeight() ;

        sightBounds.set( getWidth() * currentRollIndex, 0, getWidth(), getHeight() ) ;

        Gdx.app.debug( FlingPane.class.getSimpleName(), "Layout ends." ) ;
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
}
