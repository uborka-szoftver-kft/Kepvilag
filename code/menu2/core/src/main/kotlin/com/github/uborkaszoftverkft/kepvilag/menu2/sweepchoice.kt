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
 * <h2>About time</h2>
 * The time source mentioned here is [System.currentTimeMillis] but we could use [System.nanoTime]
 * with a right shift of 20 bits (error is
 * [said to be 5 %](https://stackoverflow.com/a/22494671/1923328))
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
   * The y value for a Panel showing its topmost area.
   */
  private val panelBaseY : Array< Float > = Array( children.size ) { 0f }


  /**
   * The area in Sight for corresponding Panel, in its own coordinates.
   * Value of [currentPanelCullingArea], and scissors area are derived from it.
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
   * Keep track of last point hit by [InputListener.touchDragged] so we can derive [ScrollAxis].
   */
  private val lastPoint = Vector2()

  /**
   * `null` when no drag or other scroll operation.
   */
  private var scrollAxis:ScrollAxis? = null

//  private val kineticScrollEngine = PassiveScrollEngine()
  private val kineticScrollEngine = ContinuousKineticScrollEngine()


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
        logDebug( "#touchDown( x=$x, y=$y ), time=${TimeUtils.millis()}" )
        kineticScrollEngine.beginDrag( TimeUtils.millis(), y)
        lastPoint.set(x, y)
        stage.scrollFocus = this@SweepChoice
        return true
      }

      override fun touchDragged(
          event : InputEvent?,
          x : Float,
          y : Float,
          pointer : Int
      ) {
        logDebug("#touchDragged( event=$event, x=$x, y=$y ), time=${TimeUtils.millis()}")
        val deltaX = x - lastPoint.x
        val deltaY = y - lastPoint.y
        val previousScrollAxis = scrollAxis
        scrollAxis = resolveScrollAxis( deltaX, deltaY )
//        if( scrollAxis == previousScrollAxis ||
//            previousScrollAxis == null ||
//            previousScrollAxis == ScrollAxis.UNDEFINED
//        ) {
          // Very approximative: the longer this first gesture lasted,
          // the greater the scroll velocity.
          when ( scrollAxis ) {
            SweepChoice.ScrollAxis.VERTICAL -> {
              kineticScrollEngine.pursueDrag( TimeUtils.millis(), y )
            }
            SweepChoice.ScrollAxis.HORIZONTAL -> {}
            else -> {}
          }
//        } else {
//          logDebug("No scroll initiated, scrollAxis=$scrollAxis and " +
//              "previousScrollAxis=$previousScrollAxis." )
//        }

        lastPoint.set( x, y )
      }

      override fun touchUp(
          event: InputEvent?,
          x:Float,
          y:Float,
          pointer:Int,
          button:Int
      ) {
        logDebug( "#touchUp( x=$x, y=$y ), time=${TimeUtils.millis()}" )
        kineticScrollEngine.endDrag( TimeUtils.millis(), y )
      }

      override fun scrolled(
          event: InputEvent?,
          x:Float,
          y:Float,
          amount:Int
      ) : Boolean {
        logDebug( "#scrolled( event=$event, x=$x, y=$y, amount=$amount )" )
        scrollAxis = ScrollAxis.VERTICAL
        kineticScrollEngine.forceRelativeScroll( amount * SCROLL_AMOUNT_CORRECTION )
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
    super.act( deltaSecond )  // TODO: skip children which do not appear within Sight.
    applyScrolling()
  }


// ======
// Scroll
// ======

  private fun applyScrolling() {
    if(scrollAxis != null ) {
      val panel = children.get(currentPanelIndex)
      when(scrollAxis) {
        SweepChoice.ScrollAxis.VERTICAL -> {
          val time = TimeUtils.millis()
          if( kineticScrollEngine.animatingScroll( time ) ){
            val scrollAmount = kineticScrollEngine.scrollAmountAt( time )
            if( scrollAmount != 0f ) {
              logDebug( "Applying scroll amount of $scrollAmount on Y axis. " )
              panel.y = panelBaseY[ currentPanelIndex ] + scrollAmount
            }
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
        } else {
          Panel.setSize(width, height)
        }

        if(previousPanel == null) {
          Panel.x = 0f
        } else {
          Panel.x = previousPanel !!.x + width + interPanelMargin
        }

        Panel.y = height - Panel.height
        panelBaseY[ panelIndex ] = Panel.y
        if(previousHeight == height) {

        } else {
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



interface KineticScrollEngine {

  /**
   * The visible area on which scrolling happens.
   */
  val sightLength : Float

  /**
   * The length of the component to scroll, in the same unit as [sightLength].
   *
   *
   */
  val availableLength : Float

  /**
   * @param newScrollAmount 0-based, a value of 0 means no scroll.
   */
  fun setGeometry(
      newSightLength : Float,
      newAvailableLength : Float,
      newScrollAmount : Float = 0f,
      cancelScrolling : Boolean = false
  )

  /**
   * Use this method to resize while keeping the scroll ratio.
   * Typical use is window resize, or change of inner component.
   */
  fun resize( newSightLength : Float, newAvailableLength : Float )

  /**
   *
   * @param scrollDelta The increase of the scroll, in the same unit as [sightLength].
   */
  fun forceRelativeScroll( scrollDelta : Float )

  /**
   * Indicates that some drag gesture has begun.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchDown].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if not the first `*Drag` method called, or if
   *     [endDrag] was not called before.
   */
  fun beginDrag( time : Long, position : Float )

  /**
   * Indicates that some drag gesture is going on.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchDragged].
   *
   * @param position, in the same unit as [sightLength].
   * @return the distance since last call to [beginDrag] or [pursueDrag].
   * @throws IllegalStateException if [beginDrag] or [pursueDrag] was not called before.
   */
  fun pursueDrag( time : Long, position : Float ) : Float

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  fun endDrag( time : Long, position : Float )

  /**
   * Compute the scroll amount, in the same unit as [sightLength], _and_ sets the time.
   *
   * @param time as returned by [System.currentTimeMillis].
   */
  fun scrollAmountAt( time : Long ) : Float

  /**
   * @param time as returned by [System.currentTimeMillis].
   * @return `true` is scrolling has started, and has not ended yet.
   */
  fun animatingScroll( time : Long ) : Boolean

}

abstract class AbstractKineticScrollEngine(
    private val logger : Logger = gdxLogger( "ScrollEngine")
) : KineticScrollEngine {

  override var sightLength = 0f

  /**
   * The overall length of what can be scrolled, in the same unit as [sightLength].
   */
  override var availableLength = 0f

  /**
   * The last time (as [System.currentTimeMillis]) set by [pursueDrag].
   */
  private var lastUpdateTime = 0L

  protected fun lastUpdateTime() = lastUpdateTime

  /**
   * Scroll amount, in the same unit as [sightLength].
   * A value of 0 means no scroll (panel position has the value kept in [SweepChoice.panelBaseY]).
   */
  protected var scrollAmount = 0f

  /**
   * Indicates that [beginDrag] got called, and [endDrag] has not been called yet.
   */
  private var dragging = false


  /**
   * Meaningful only when [dragging] is `true`. A value of -1 means the value is undefined.
   */
  protected var lastPosition = -1f


  protected fun dragging() : Boolean = dragging

  protected open fun cancelScrolling() {
    dragging = false
  }

  override fun setGeometry(
      newSightLength : Float,
      newAvailableLength : Float,
      newScrollAmount : Float,
      cancelScrolling : Boolean
  ) {
    check( newSightLength >= 0 )
    check( newAvailableLength >= 0 )
    check( newScrollAmount < newAvailableLength - newSightLength )
    sightLength = newSightLength
    availableLength = newAvailableLength
    scrollAmount = newScrollAmount
    if( cancelScrolling ) cancelScrolling()
  }

  override fun resize( newSightLength : Float, newAvailableLength : Float ) {
    val oldRatio = sightLength / availableLength
    setGeometry( newSightLength, newAvailableLength, scrollAmount * oldRatio )
  }


  override fun forceRelativeScroll( scrollDelta : Float ) {
    scrollAmount += scrollDelta
  }

  override fun beginDrag( time : Long, position : Float ) {
    check( ! dragging )
    check( position >= 0 )
    checkUpdateTime(time)
    lastUpdateTime = time
    lastPosition = position
    dragging = true
  }

  override fun pursueDrag( time : Long, position : Float ) : Float {
    check( dragging ) { "Not dragging" }
    checkUpdateTime( time )
    val distance = position - lastPosition
    lastPosition = position
    lastUpdateTime = time
    return distance
  }

  private fun checkUpdateTime(time : Long) {
    check( time >= lastUpdateTime ) { "Inconsistent time: $time, last update is $lastUpdateTime" }
  }

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength]. Happens to be the same value as in last
   *     call to [pursueDrag] so it cannot be used for speed derivation at this point.
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  override fun endDrag( time : Long, position : Float ) {
    dragging = false
    lastPosition = -1f
  }


  protected fun logDebug( message : String ) {
    @Suppress("ConstantConditionIf")
    if( SweepChoice.DEBUG ) {
      logger.logDebug( message )
    }
  }

}
/**
 * Scrolls upon drag, when drag is release the scroll pursues during a given time, with a speed
 * decreasing linearly.
 *
 * Velocity can be written as:
 * ```
 * v = at + b
 * ```
 * where `t` is the time, `a` the slope, and `b` the y-intercept.
 * `v0` is the initial velocity (calculated from drag gesture by [velocityRecorder]),
 * `t0` the time at which momentum begins, and `d` the [momentumDurationS]. So far we can write:
 * ```
 * a ​= ​-v0/d
 * b = v0 + (v0/d)t0
 * ```
 *
 * By integrating velocity we obtain the position as:
 * ```
 * p = ((a/2) * t^2) + bt + p0
 * ```
 * In the code `a/2` is the [velocityHalfSlope]. `b` is the [velocityYIntercept].
 * `t0` is [momentumStartTime]. `p0` is [momentumStartPosition].
 *
 * Unit for distance is the same as [sightLength]'.
 *
 * Unit for [velocityRecorder] is millisecond, unit for [scrollAmount] is second.
 * This avoids some rounding problems.
 *
 * Momentum calculation translates [momentumStartTime] to 0, so [velocityYIntercept] calculation
 * skips the `t0` member of the equation shown above.

 */
class ContinuousKineticScrollEngine(
    private val momentumDurationS : Float = 1f,
    logger : Logger = gdxLogger( "ScrollEngine")
) : AbstractKineticScrollEngine( logger ) {


  /**
   * The time at which momentum begins (as [System.currentTimeMillis]), set by [endDrag].
   */
  private var momentumStartTime = 0L

  /**
   * The time at which momentum ends (as [System.currentTimeMillis]), set by [endDrag].
   */
  private var momentumEndTime = 0L

  /**
   * The position at which momentum begins (in the same unit as [sightLength]), set by [endDrag].
   */
  private var momentumStartPosition = 0f


  /**
   * Describes the speed at which velocity decreases, for a time given in seconds.
   */
  private var velocityHalfSlope = 0f

  /**
   * Describes the speed at which velocity decreases, for a time given in seconds.
   */
  private var velocityYIntercept = 0f

  private val velocityRecorder = VelocityRecorder()

  override fun cancelScrolling() {
    velocityRecorder.clear()
    super.cancelScrolling()
  }



  /**
   * Indicates that some drag gesture has begun.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchDown].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if not the first `*Drag` method called, or if
   *     [endDrag] was not called before.
   */
  override fun beginDrag( time : Long, position : Float ) {
    super.beginDrag( time, position )
    velocityRecorder.clear()
    momentumStartTime = 0L
    momentumEndTime = 0L
  }

  override fun pursueDrag( time : Long, position : Float ) : Float {
    val lastUpdateTimeBeforeUpdate = lastUpdateTime()
    val distance = super.pursueDrag( time, position )
    val deltaTime = time - lastUpdateTimeBeforeUpdate
    velocityRecorder.record( distance, deltaTime.toFloat() )
    logDebug( "#pursueDrag, scrollAmount=$scrollAmount, lastPosition=$lastPosition, " +
        "distance=$distance, time=$time, deltaTime=$deltaTime" )
    return distance
  }

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength]. Happens to be the same value as in last
   *     call to [pursueDrag] so it cannot be used for speed derivation at this point.
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  override fun endDrag( time : Long, position : Float ) {
    super.endDrag( time, position )
    momentumStartTime = time
    momentumEndTime = time + ( momentumDurationS * 1000f ).toLong()
    momentumStartPosition = position
    val velocity = velocityRecorder.average() * 1000  // Converting ms to s.
    velocityHalfSlope = ( - velocity / momentumDurationS ) / 2
    velocityYIntercept = velocity  // Skipping some other terms from original equation.
    logDebug(
        "#endDrag, velocity=$velocity, " +
        "velocityHalfSlope=$velocityHalfSlope, velocityYIntercept=$velocityYIntercept, " +
        "momentumStartPosition=$momentumStartPosition."
    )
    velocityRecorder.clear()
  }

  override fun scrollAmountAt( time : Long ) : Float {
    if( ! dragging() ) {
      check( time >= momentumStartTime ) {
        "Incorrect value for time: $time, greater than $momentumStartTime" }
      if( animatingScroll( time ) ) {
        val elapsedTimeSeconds = ( time - momentumStartTime ) / 1000f
        scrollAmount = ( velocityHalfSlope * elapsedTimeSeconds * elapsedTimeSeconds) +
            ( velocityYIntercept * elapsedTimeSeconds ) + momentumStartPosition
      }
    }
    return scrollAmount
  }

  override fun animatingScroll( time : Long ) : Boolean {
    return time <= momentumEndTime
  }

}

/**
 * Scrolls only upon User drag, no momentum effect.
 */
class PassiveScrollEngine : AbstractKineticScrollEngine() {

  override fun pursueDrag( time : Long, position : Float ) : Float {
    val distance = super.pursueDrag( time, position )

    scrollAmount += distance
    logDebug( "#pursueDrag, scrollAmount=$scrollAmount, lastPosition=$lastPosition, " +
        "distance=$distance, time=$time" )
    scrollAmountChanged = true
    return distance
  }

  var scrollAmountChanged = false

  /**
   * Twisting the contract a bit: we don't take time in account.
   */
  override fun scrollAmountAt( time : Long ) : Float {
    scrollAmountChanged = false
    return scrollAmount
  }

  override fun forceRelativeScroll(scrollDelta : Float) {
    super.forceRelativeScroll( scrollDelta )
    scrollAmountChanged = true
  }

  override fun animatingScroll( time : Long ) : Boolean = scrollAmountChanged

}


/**
 * Records distances and the time it took, and produces a moving average of the recorded speeds,
 * weighted by durations.
 *
 * @see com.badlogic.gdx.input.GestureDetector.VelocityTracker
 */
class VelocityRecorder( private val capacity : Int = 10 ) {

  /**
   * Holds couples of value: distance, duration.
   */
  private val records : Array< Float >

  private var size : Int = 0

  /**
   * Indice of next element to [record].
   */
  private var next : Int = 0

  init {
    check( capacity > 0 )
    records = Array( capacity * 2 ) { 0f }
  }

  fun record( distance : Float, duration : Float ) {
    if( next > records.size - 1 ) { next = 0 }
    records[ next ++ ] = distance
    records[ next ++ ] = duration
    if( size < capacity ) size ++
  }

  /**
   * Calculates the average speed weighted by measurement duration.
   *
   * <pre>
  v1.t1 + v2.t2 + v3.t3   d1 + d2 + d3
  --------------------- = ------------
      t1 + t2 + t3        t1 + t2 + t3

  v = d/t => vt = d

   * </pre>
   */
  fun average() : Float {
    var readIndex = next - 1
    var readCount = 0
    var distanceSum = 0f
    var durationSum = 0f
    while( readCount < size ) {
      if( readIndex > records.size ) readIndex = 0
      else if( readIndex < 0 ) readIndex = records.size - 1
      durationSum += records[ readIndex -- ]
      distanceSum += records[ readIndex -- ]
      readCount ++
    }
    return if( durationSum == 0f ) 0f else distanceSum / durationSum
  }

  fun clear() {
    next = 0
    size = 0
  }

}

interface Logger {
  fun logDebug( message : String )

}

fun gdxLogger( tag : String ) : Logger {
  return object: Logger {
    override fun logDebug(message : String) {
      Gdx.app.debug( tag, message )
    }
  }
}

fun consoleLogger( tag : String ) : Logger {
  return object : Logger {
    override fun logDebug(message : String) {
      println( "[$tag] $message" )
    }

  }
}
