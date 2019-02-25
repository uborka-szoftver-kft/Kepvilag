package com.github.uborkaszoftverkft.kepvilag.menu2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Cullable
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.TimeUtils

/**
 * Contains a collection of `Actor`s that may be too high to fit vertically, so it shows one at a time,
 * with horizontal transitions between them and vertical scroll for each.
 * The display area is known as **Sight**.
 * Each child `Actor` is called a **Panel**, as its scrolls vertically.
 *
 */
class SweepChoice(
    private val preferredWidth : Float,
    private val preferredHeight : Float,
    /**
     * The distance between [currentPanelIndex] and [disappearingPanelIndex].
     */
    private val interPanelMargin : Float,
    vararg panels : Actor
) : WidgetGroup( *panels ) {

  private val currentPanelIndex = 0
  private val disappearingPanelIndex = -1

  private val currentPanelIsCullable = false
  private val disappearingPanelIsCullable = false

  /**
   * Gets incremented by [InputListener.touchDragged],
   * gets zeroed by [Actor.act].
   */
//  private var scrollAmount = 0f

  /**
   * Initial value to compute decreasing [scrollVelocity] from.
   */
  private var initialScrollVelocity = 0f

  private var scrollVelocity = 0f

  private var touchDownTime:Long = 0
  private var lastMouseScrollTime:Long = 0

  /**
   * The area in Sight for corresponding Panel, in its own coordinates.
   * Value of [.currentPanelCullingArea], and scissors area are derived from it.
   */
  private val currentPanelAreaBounds = Rectangle()

  private val currentPanelCullingArea = Rectangle()
  private val disappearingPanelAreaBounds = Rectangle()
  private val disappearingPanelCullingArea = Rectangle()

  private val sightBounds = Rectangle()
  private val scissorBounds = Rectangle()

  //private ScrollDirection scrollDirection = null ;
  private var overscrollPhase:OverscrollPhase? = null

  /**
   * Same as in [com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.flingTimer].
   * Only mutate with [prepareInertialScrolling], or by zeroing it.
   */
  private var flingTimer:Float = 0.toFloat()

  /**
   * Same as in [com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.flingTime].
   */
  private val flingDuration = FLING_DURATION_SECOND

  private val lastPoint = Vector2()

  /**
   * `null` when no drag or other scroll operation.
   */
  private var scrollAxis:ScrollAxis? = null

  private val kineticScrollEngine = continuous()


// ======
// Layout
// ======

  private var sizeChanged = true


  /**
   * Detects vertical resize between two calls to [.layout].
   */
  private var previousHeight = -1f


  private enum class ScrollAxis {
    UNDEFINED, VERTICAL, HORIZONTAL
  }

  @Deprecated( "Done in KineticScrollEngine" )
  private enum class OverscrollPhase {
    FORWARD,
    BACKWARD
  }

  init{
    checkInvariants()

    addListener( object: InputListener() {

      override fun touchDown(
          event: InputEvent?,
          x:Float,
          y:Float,
          pointer:Int,
          button:Int
      ) : Boolean {
        logDebug("#touchDown( event=" + event + ", x=" + x + ", y=" + y + ", " +
        "pointer=" + pointer + ", button=" + button + " )")
        kineticScrollEngine.beginDrag( TimeUtils.millis(), y)
//        scrollAxis = null
//        initialScrollVelocity = 0f
//        scrollAmount = 0f
//        scrollVelocity = 0f
//        flingTimer = 0f
//        overscrollPhase = null
        lastPoint.set(x, y)
//        touchDownTime = TimeUtils.millis()
//        lastMouseScrollTime = 0
          stage.scrollFocus = this@SweepChoice
        return true
      }

      override fun touchDragged(
          event: InputEvent?,
          x:Float,
          y:Float,
          pointer:Int
      ) {
        logDebug("#touchDragged( event=" + event + ", x=" + x + ", y=" + y + ", " +
        "pointer=" + pointer + " )")
        val deltaX = x - lastPoint.x
        val deltaY = y - lastPoint.y
        val previousScrollAxis = scrollAxis
        scrollAxis = resolveScrollAxis(deltaX, deltaY)
        if( scrollAxis == previousScrollAxis ||
            previousScrollAxis == null ||
            previousScrollAxis == ScrollAxis.UNDEFINED
        ) {
          val inertialScrollingLastDuration = TimeUtils.timeSinceMillis(touchDownTime).toFloat()
          touchDownTime = TimeUtils.millis()
          // Very approximative: the longer this first gesture lasted,
          // the greater the scroll velocity.
          when (scrollAxis) {
            SweepChoice.ScrollAxis.VERTICAL -> {
              kineticScrollEngine.pursueDrag( TimeUtils.millis(), y)
            }
            SweepChoice.ScrollAxis.HORIZONTAL -> {}
            else -> {}
          }
        } else {
          logDebug("No scroll initiated, scrollAxis=$scrollAxis and " +
              "previousScrollAxis=$previousScrollAxis." )
        }

        lastPoint.set(x, y)
      }

      override fun touchUp(
          event: InputEvent?,
          x:Float,
          y:Float,
          pointer:Int,
          button:Int
      ) {
          logDebug("#touchUp( event=$event, x=$x, y=$y, pointer=$pointer, button=$button )")
        kineticScrollEngine.endDrag( TimeUtils.millis(), y)
      }

      override fun scrolled(
          event: InputEvent?,
          x:Float,
          y:Float,
          amount:Int
      ) : Boolean {
        logDebug( "#scrolled( event=$event, x=$x, y=$y, amount=$amount )" )
        scrollAxis = ScrollAxis.VERTICAL
        kineticScrollEngine.forceScroll(amount * SCROLL_AMOUNT_CORRECTION)
        return true
      }


    } )
  }


  private fun checkInvariants() {
    if(! CHECK_INVARIANTS) return
    if(scrollAxis == null) {
      check(overscrollPhase == null, "overscrollPhase should be null")
      check(disappearingPanelIndex < 0, "disappearingPane should be undefined when not scrolling")
    }
    else {
      if(overscrollPhase != null) {
        check(disappearingPanelIndex < 0, "disappearingPane should be undefined when overscrolling")
      }
    }

    val currentPane = children.items[currentPanelIndex]
    if(currentPanelIsCullable) {
      check(currentPane is Cullable, "currentPane should be Cullable")
    }

    if(disappearingPanelIndex >= 0) {
      val disappearingPane = children.items[disappearingPanelIndex]
      if(disappearingPanelIsCullable) {
        check(disappearingPane is Cullable, "disappearingPane should be Cullable")
      }
    }
  }

  private fun check(expression : Boolean, message : String) {
    if(! expression) {
      throw AssertionError(message)
    }
  }


// ===
// Act
// ===

  override fun act(deltaSecond : Float) {
    super.act(deltaSecond)
    applyScrolling(deltaSecond)
  }


// ======
// Scroll
// ======

  private fun prepareInertialScrolling(initialScrollVelocity : Float) {
    this.initialScrollVelocity = initialScrollVelocity
    scrollVelocity = initialScrollVelocity
    flingTimer = flingDuration
    logDebug("#prepareInertialScrolling( $initialScrollVelocity )")
  }


  private fun applyScrolling(deltaSecond : Float) {
    if(scrollAxis != null ) {
      val panel = children.get(currentPanelIndex)
      when(scrollAxis) {
        SweepChoice.ScrollAxis.VERTICAL -> {
          val scrollAmount = kineticScrollEngine.scrollAmountAfter( deltaSecond )
          if( scrollAmount != 0f ) {
            logDebug("Applying scroll amount of $scrollAmount on Y axis. ")
            panel.y = panel.y + scrollAmount
          }
        }
        SweepChoice.ScrollAxis.HORIZONTAL -> {
        }
        else -> {
        }
      }
    }

  }

  override fun sizeChanged() {
    sizeChanged = true
    super.sizeChanged()
  }

  /**
   * Sets every Panel's coordinates according to [.getWidth] and [.getHeight].
   * An unscrolled Panel has its top's y -- not its y -- equal to [SweepChoice.getHeight] .
   * For a given [.getWidth] each Panel's horizontal width and height are constant.
   * Components' reference for coordinates is bottom-left corner.
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
   *
   */
  override fun layout() {
    logDebug(("#layout() begins: w=" + width + ", h=" + height + ", " +
        "currentPanelIndex=" + currentPanelIndex))
    if(sizeChanged) {
      logDebug("Applying full layout because of size change.")
      var previousPanel : Actor? = null
      for(panelIndex in 0 until children.size) {
        val Panel = children.get(panelIndex)
        val isLayout = Panel is Layout
        if(isLayout) {
          val PanelAsLayout = Panel as Layout
          PanelAsLayout.layout()
          // When word-wrapping, a Label returns 0 for its preferred size,
          // which also depends on current width. So we set current width
          // before calculating preferred size.
          if(PanelAsLayout.prefWidth <= 0) {
            Panel.width = width
          }
          else {
            Panel.width = Math.min(PanelAsLayout.prefWidth, width)
          }
          Panel.height = PanelAsLayout.prefHeight
        }
        else {
          Panel.setSize(width, height)
        }

        if(previousPanel == null) {
          Panel.x = 0f
        }
        else {
          Panel.x = previousPanel !!.x + width + interPanelMargin
        }

        Panel.y = height - Panel.height
        if(previousHeight == height) {
        }
        else {
          // Some resize happened, so we want to maintain Y offset basing on a ratio.
        }

        previousPanel = Panel

        logDebug(
            ("Panel[" + panelIndex + "]: " +
                "x=" + Panel.x + ", " +
                "y=" + Panel.y + ", " +
                "w=" + Panel.width + ", " +
                "h=" + Panel.height)
        )
      }
      sightBounds.set(width * currentPanelIndex, 0f, width, height)
      previousHeight = height
      sizeChanged = false

    }

    logDebug("#layout() ends.")

  }


  // ====
  // Draw
  // ====

  /**
   * Lot of code copied from [com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.draw].
   */
  override fun draw(batch : Batch?, parentAlpha : Float) {
    // Triggers a call to layout().
    validate()

    val transform = computeTransform()
    transform.translate(- currentPanelIndex * (width + interPanelMargin), 0f, 0f)

    // Setup transform for this group.
    applyTransform(batch !!, transform)

    // Caculate the scissor bounds based on the batch transform, the available widget area and the camera transform.
    // We need to project those to screen coordinates for OpenGL ES to consume.
    stage.calculateScissors(sightBounds, scissorBounds)

    // Enable scissors for widget area and draw the widget.
    batch.flush()
    if(ScissorStack.pushScissors(scissorBounds)) {
      drawChildren(batch, parentAlpha)
      batch.flush()
      ScissorStack.popScissors()
    }
    resetTransform(batch)

    // logDebug( "#draw(...) complete." );
  }


  // ===============
  // Layout contract
  // ===============

  override fun getMinWidth() : Float {
    return preferredWidth
  }

  override fun getMinHeight() : Float {
    return preferredHeight
  }

  override fun getPrefWidth() : Float {
    return preferredWidth
  }

  override fun getPrefHeight() : Float {
    return preferredHeight
  }

  override fun getMaxWidth() : Float {
    return preferredWidth
  }

  override fun getMaxHeight() : Float {
    return preferredHeight
  }

  private fun resolveScrollAxis(deltaX : Float, deltaY : Float) : ScrollAxis {
    val axis : ScrollAxis
    if(Math.abs(deltaY) > Math.abs(deltaX) * AXIS_DIFFERENCIATION_FACTOR) {
      axis = ScrollAxis.VERTICAL
    }
    else if(Math.abs(deltaX) > Math.abs(deltaY) * AXIS_DIFFERENCIATION_FACTOR) {
      axis = ScrollAxis.HORIZONTAL
    }
    else {
      axis = ScrollAxis.UNDEFINED
    }
    return axis
  }

  companion object {

    private const val AXIS_DIFFERENCIATION_FACTOR = 1.3f
    private const val FLING_DURATION_SECOND = 1f
    private const val SCROLL_AMOUNT_CORRECTION = 10f


    // ==========
    // Invariants
    // ==========

    private const val CHECK_INVARIANTS = true


    // =============
    // Miscellaneous
    // =============

    private fun logDebug(message : String) {
      if( DEBUG ) {
        Gdx.app.debug(SweepChoice::class.java.simpleName, message)
      }
    }

    public const val DEBUG = true
  }

}

/**
 * Contract for programmatically modifying Panel
 * This is useful when using another controller than touch, e.g. keyboard or on-screen buttons.
 */
interface ChoiceNavigator {
  /**
   * Triggers scrollup of current Panel, or unveils of next Panel if any.
   */
  fun goForward()

  fun goBackward()

  fun selectCurrent()

}

interface ChoiceListener {

  fun sightChanged(
      choiceIndex : Int,
      mayScrollUp : Boolean,
      mayScrollLeft : Boolean,
      mayScrollDown : Boolean,
      mayScrollRight : Boolean
  )

  fun selected(choice : Int)

}
