package io.github.uborkaszoftver.menu1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

/**
 * Contains a collection of {@code Actor}s that may be too high to fit vertically, so it shows one at a time,
 * with horizontal transitions between them and vertical scroll for each.
 * The display area is known as <b>Sight</b>.
 * Each child {@code Actor} is called a <b>Lane</b>, as its scrolls vertically.
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

    /**
     * Should not mutate.
     * For computing display values use {@link Actor#getWidth()} and {@link Actor#getHeight()}.
     */
    private final Vector2 preferredSightSize ;

    /**
     * The distance between {@link #currentLaneIndex} and {@link #disappearingLaneIndex}.
     */
    private final float interLaneMargin ;

    private int currentLaneIndex = 0 ;
    private int disappearingLaneIndex = -1 ;

    private boolean currentLaneIsCullable = false ;
    private boolean disappearingLaneIsCullable = false ;


    private float scrollVelocityX = 0 ;
    private float scrollVelocityY = 0 ;

    private float scrollAmountX = 0 ;

    /**
     * The area in Sight for corresponding Lane, in its own coordinates.
     * Value of {@link #currentLaneCullingArea}, and scissors area are derived from it.
     */
    private final Rectangle currentLaneAreaBounds = new Rectangle() ;

    private final Rectangle currentLaneCullingArea = new Rectangle() ;
    private final Rectangle disappearingLaneAreaBounds = new Rectangle() ;
    private final Rectangle disappearingLaneCullingArea = new Rectangle() ;

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
            final float interLaneMargin,
            final Actor... lanes
    ) {
        super( lanes ) ;
        preferredSightSize = new Vector2( sightWidth, sightHeight ) ;
        this.interLaneMargin = interLaneMargin ;
    }

    /**
     * Caller must enforce every invariant. We expect {@link #scrollAmountX} to be consistent with
     * {@link #currentLaneIndex} and {@link #disappearingLaneIndex}.
     */
    private void applyCulling() {
        if( scrollDirection != null ) {
            if( scrollDirection.horizontal ) {
                // Scroll to the right, first on left is current.
                if( currentLaneIsCullable ) {
                    final Actor currentLane = getChildren().items[ currentLaneIndex ] ;
                    currentLaneCullingArea.x = scrollAmountX ;
                    currentLaneCullingArea.y = currentLane.getY() ;
                    currentLaneCullingArea.height = preferredSightSize.x ;
                    currentLaneCullingArea.width = preferredSightSize.y - scrollAmountX ;
                    ( ( Cullable ) currentLane ).setCullingArea( currentLaneCullingArea ) ;
                }
                if( disappearingLaneIsCullable && disappearingLaneIndex > -1 ) {  // There could be overscroll.
                    final Actor disappearingLane = getChildren().items[disappearingLaneIndex];
                    disappearingLaneCullingArea.x = 0;
                    disappearingLaneCullingArea.y = disappearingLane.getY();
                    disappearingLaneCullingArea.height = preferredSightSize.x ;
                    disappearingLaneCullingArea.width = preferredSightSize.y + interLaneMargin - scrollAmountX;
                    ((Cullable) disappearingLane).setCullingArea(disappearingLaneCullingArea);
                }
            } else {
                // Scroll to the left, first on left is disappearing.

            }


        } else {

        }


    }


    public interface LaneListener {

        void laneSightChanged(
                int laneIndex,
                boolean mayScrollUp,
                boolean mayScrollLeft,
                boolean mayScrollDown,
                boolean mayScrollRight
        ) ;

        void laneSelected( int laneIndex ) ;

    }


// ==========
// Invariants
// ==========

    private static final boolean CHECK_INVARIANTS = true ;

    void checkInvariants() {
        if( ! CHECK_INVARIANTS ) return ;
        if( scrollDirection == null ) {
            check( overscrollPhase == null, "overscrollPhase should be null" ) ;
            check( disappearingLaneIndex < 0, "disappearingLane should be undefined when not scrolling" ) ;
        } else {
            if( overscrollPhase != null ) {
                check( disappearingLaneIndex < 0, "disappearingLane should be undefined when overscrolling" ) ;
            }
        }

        final Actor currentLane = getChildren().items[ currentLaneIndex ] ;
        if( currentLaneIsCullable ) {
            check( currentLane instanceof Cullable, "currentLane should be Cullable" ) ;
        }

        if( disappearingLaneIndex >= 0 ) {
            final Actor disappearingLane = getChildren().items[ disappearingLaneIndex ] ;
            if( disappearingLaneIsCullable ) {
                check( disappearingLane instanceof Cullable, "disappearingLane should be Cullable" ) ;
            }
        }
    }

    private void check( boolean expression, String message ) {
        if( ! expression ) {
            throw new AssertionError( message ) ;
        }
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
     * Sets every Lane's coordinates.
     * An unscrolled Lane has its top's y -- not its y -- equal to {@link FlingPane#getHeight()} .
     * Components' reference for coordinates is bottom-left corner.
     *
     * <pre>

    ^ x goes up
    |                 .....      -+
          .....       .   .        > A Lane may "go up" of this height
          .   .       .   .       |  (until its bottom hits Sight's bottom).
    +---+ +---+ +---+ +---+      -+
    |   | |   | |   | |   |        > This is Sight's height.
    |   | |   | +---+ |   |       |
y=0-+---+ |   |       |   |      -+
    |     |   |       |   |        > For the tallest Lane, this is the
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
                "Layout begins: w=" + getWidth() + ", h=" + getHeight()
        ) ;
        Actor previousLane = null ;
        for( int laneIndex = 0 ; laneIndex < getChildren().size ; laneIndex ++  ) {
            final Actor lane = getChildren().get( laneIndex ) ;
            boolean isLayout = lane instanceof Layout ;
            final float newLaneWidth, newLaneHeight ;
            if( isLayout ) {
                Layout laneAsLayout = ( Layout ) lane ;
                newLaneWidth = laneAsLayout.getPrefWidth() > 0 ?
                        Math.min( laneAsLayout.getPrefWidth(), getWidth() ) : getWidth() ;
                newLaneHeight = laneAsLayout.getPrefHeight() ;
            } else {
                newLaneWidth = getWidth() ;
                newLaneHeight = getHeight() ;
            }
            final float previousLaneHeight = lane.getHeight() ;
            lane.setSize( newLaneWidth, newLaneHeight ) ;

            if( previousLane == null ) {
                lane.setX( 0 ) ;
            } else {
                lane.setX( previousLane.getX() + getWidth() + interLaneMargin ) ;
            }
            float previousLaneOffsetY = previousHeight - previousLaneHeight;
            float newLaneOffsetY = getHeight() - lane.getHeight();
            if( lane.getY() > previousLaneOffsetY) {
                // The Lane was scrolled up, so we keep the scrolling ratio regarding new height.
                final float previousScrollRatio = previousLaneOffsetY / lane.getY() ;
                lane.setY( newLaneOffsetY * previousScrollRatio ) ;
            } else {
                lane.setY( newLaneOffsetY ) ;
            }
            Gdx.app.debug(
                    FlingPane.class.getSimpleName(),
                    "Lane[" + laneIndex + "]: " +
                            "x=" + lane.getX() + ", " +
                            "y=" + lane.getY() + ", " +
                            "w=" + lane.getWidth() + ", " +
                            "h=" + lane.getHeight()
            ); ;
            previousLane = lane ;
        }
        previousHeight = getHeight() ;

        Gdx.app.debug(
                FlingPane.class.getSimpleName(),
                "Layout ends."
        ); ;

    }

// ===============
// Layout contract
// ===============

    @Override
    public float getMinWidth() {
        return preferredSightSize.x ;
    }

    @Override
    public float getMinHeight() {
        return preferredSightSize.y ;
    }

    @Override
    public float getPrefWidth() {
        return preferredSightSize.x ;
    }

    @Override
    public float getPrefHeight() {
        return preferredSightSize.y ;
    }

    @Override
    public float getMaxWidth() {
        return preferredSightSize.x ;
    }

    @Override
    public float getMaxHeight() {
        return preferredSightSize.y ;
    }
}
