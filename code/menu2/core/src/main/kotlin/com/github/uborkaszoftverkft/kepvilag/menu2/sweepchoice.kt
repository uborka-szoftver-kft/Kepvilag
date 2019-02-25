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
 * Each child `Actor` is called a **Ascender**, as its scrolls vertically.
 *
 */
class SweepChoice(
    private val preferredWidth : Float,
    private val preferredHeight : Float,
    /**
     * The distance between [.currentRollIndex] and [.disappearingRollIndex].
     */
    private val interRollMargin : Float,
    vararg slabs : Actor
) : WidgetGroup( *slabs ) {

  private val currentRollIndex = 0
  private val disappearingRollIndex = -1

  private val currentRollIsCullable = false
  private val disappearingRollIsCullable = false

  /**
   * Gets incremented by [InputListener.touchDragged],
   * gets zeroed by [Actor.act].
   */
  private var scrollAmount = 0f

  /**
   * Initial value to compute decreasing [.scrollVelocity] from.
   */
  private var initialScrollVelocity = 0f

  private var scrollVelocity = 0f

  private var touchDownTime:Long = 0
  private var lastMouseScrollTime:Long = 0

  /**
   * The area in Sight for corresponding Ascender, in its own coordinates.
   * Value of [.currentAscenderCullingArea], and scissors area are derived from it.
   */
  private val currentAscenderAreaBounds = Rectangle()

  private val currentAscenderCullingArea = Rectangle()
  private val disappearingAscenderAreaBounds = Rectangle()
  private val disappearingAscenderCullingArea = Rectangle()

  private val sightBounds = Rectangle()
  private val scissorBounds = Rectangle()

  //private ScrollDirection scrollDirection = null ;
  private var overscrollPhase:OverscrollPhase? = null

  /**
   * Same as in [com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.flingTimer].
   * Only mutate with [.prepareInertialScrolling], or by zeroing it.
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
        scrollAxis = null
        initialScrollVelocity = 0f
        scrollAmount = 0f
        scrollVelocity = 0f
        flingTimer = 0f
        overscrollPhase = null
        lastPoint.set(x, y)
        touchDownTime = TimeUtils.millis()
        lastMouseScrollTime = 0
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
             // Forcing amount to some reasonable value avoids delay
                                        // before scroll begins.
                                        scrollAmount = deltaY
              prepareInertialScrolling(deltaY * inertialScrollingLastDuration)
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
      }

      override fun scrolled(
          event: InputEvent?,
          x:Float,
          y:Float,
          amount:Int
      ) : Boolean {
        logDebug( "#scrolled( event=$event, x=$x, y=$y, amount=$amount )" )
        scrollAxis = ScrollAxis.VERTICAL
        scrollAmount = amount * SCROLL_AMOUNT_CORRECTION
        return true
      }


    } )
  }


  private fun checkInvariants() {
    if(! CHECK_INVARIANTS) return
    if(scrollAxis == null) {
      check(overscrollPhase == null, "overscrollPhase should be null")
      check(disappearingRollIndex < 0, "disappearingRoll should be undefined when not scrolling")
    }
    else {
      if(overscrollPhase != null) {
        check(disappearingRollIndex < 0, "disappearingRoll should be undefined when overscrolling")
      }
    }

    val currentRoll = children.items[currentRollIndex]
    if(currentRollIsCullable) {
      check(currentRoll is Cullable, "currentRoll should be Cullable")
    }

    if(disappearingRollIndex >= 0) {
      val disappearingRoll = children.items[disappearingRollIndex]
      if(disappearingRollIsCullable) {
        check(disappearingRoll is Cullable, "disappearingRoll should be Cullable")
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

  private fun slowDownInertialScrolling(deltaSecond : Float) {
    if(flingTimer <= 0) {
      initialScrollVelocity = 0f
      scrollVelocity = 0f
      scrollAxis = null
    }
    else {
      flingTimer -= deltaSecond
      val flingProgress = flingTimer / flingDuration
      scrollVelocity = initialScrollVelocity * flingProgress
      scrollAmount += scrollVelocity * (deltaSecond / flingDuration)
      if(Math.abs(scrollVelocity) < 0.1) {
        scrollAmount = 0f
      }

      logDebug("#slowDownInertialScrolling( $deltaSecond ): scrollVelocity=$scrollVelocity, " +
          "scrollAmount=$scrollAmount")
    }
  }

  private fun applyScrolling(deltaSecond : Float) {
    if(scrollAxis != null && scrollAmount != 0f) {
      val Ascender = children.get(currentRollIndex)
      when(scrollAxis) {
        SweepChoice.ScrollAxis.VERTICAL -> {
          logDebug(
              "Applying scroll amount of " + scrollAmount + " on Y axis. " +
                  "Velocity updated to " + scrollVelocity + ". " +
                  "End of scroll in " + flingTimer + " s."
          )
          Ascender.y = Ascender.y + scrollAmount
        }
        SweepChoice.ScrollAxis.HORIZONTAL -> {
        }
        else -> {
        }
      }
      scrollAmount = 0f
    }

    slowDownInertialScrolling(deltaSecond)
  }

  override fun sizeChanged() {
    sizeChanged = true
    super.sizeChanged()
  }

  /**
   * Sets every Ascender's coordinates according to [.getWidth] and [.getHeight].
   * An unscrolled Ascender has its top's y -- not its y -- equal to [SweepChoice.getHeight] .
   * For a given [.getWidth] each Ascender's horizontal width and height are constant.
   * Components' reference for coordinates is bottom-left corner.
   *
   * <pre>
   *
   * ^ x goes up
   * |                 .....      -+
   * .....       .   .        > A Ascender may "go up" of this height
   * .   .       .   .       |  (until its bottom hits Sight's bottom).
   * +---+ +---+ +---+ +---+      -+
   * |   | |   | |   | |   |        > This is Sight's height.
   * |   | |   | +---+ |   |       |
   * y=0-+---+ |   |       |   |      -+
   * |     |   |       |   |        > For the tallest Ascender, this is the
   * x=0   +---+       |   |       |  distance on which it can go "up".
   * +---+      -+
   * |   |
   * +- -+
   * v
   * This is Sight's width.
   *
  </pre> *
   */
  override fun layout() {
    logDebug(("#layout() begins: w=" + width + ", h=" + height + ", " +
        "currentRollIndex=" + currentRollIndex))
    if(sizeChanged) {
      logDebug("Applying full layout because of size change.")
      var previousRoll : Actor? = null
      for(rollIndex in 0 until children.size) {
        val ascender = children.get(rollIndex)
        val isLayout = ascender is Layout
        if(isLayout) {
          val ascenderAsLayout = ascender as Layout
          ascenderAsLayout.layout()
          // When word-wrapping, a Label returns 0 for its preferred size,
          // which also depends on current width. So we set current width
          // before calculating preferred size.
          if(ascenderAsLayout.prefWidth <= 0) {
            ascender.width = width
          }
          else {
            ascender.width = Math.min(ascenderAsLayout.prefWidth, width)
          }
          ascender.height = ascenderAsLayout.prefHeight
        }
        else {
          ascender.setSize(width, height)
        }

        if(previousRoll == null) {
          ascender.x = 0f
        }
        else {
          ascender.x = previousRoll !!.x + width + interRollMargin
        }

        ascender.y = height - ascender.height
        if(previousHeight == height) {
        }
        else {
          // Some resize happened, so we want to maintain Y offset basing on a ratio.
        }

        previousRoll = ascender

        logDebug(
            ("Ascender[" + rollIndex + "]: " +
                "x=" + ascender.x + ", " +
                "y=" + ascender.y + ", " +
                "w=" + ascender.width + ", " +
                "h=" + ascender.height)
        )
      }
      sightBounds.set(width * currentRollIndex, 0f, width, height)
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
    transform.translate(- currentRollIndex * (width + interRollMargin), 0f, 0f)

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
      Gdx.app.debug(SweepChoice::class.java.simpleName, message)
    }
  }

}

/**
 * Contract for programmatically modifying Ascender
 * This is useful when using another controller than touch, e.g. keyboard or on-screen buttons.
 */
interface ChoiceNavigator {
  /**
   * Triggers scrollup of current Ascender, or unveils of next Ascender if any.
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
