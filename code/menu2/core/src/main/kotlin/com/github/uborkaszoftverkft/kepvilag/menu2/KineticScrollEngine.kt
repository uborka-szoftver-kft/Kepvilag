package com.github.uborkaszoftverkft.kepvilag.menu2

import com.badlogic.gdx.Gdx


interface KineticScrollEngine {

  /**
   * The visible area on which scrolling happens.
   */
  val sightLength : Float

  /**
   * The length of the component to scroll, in the same unit as [sightLength].
   */
  val availableLength : Float

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
  fun forceScroll( scrollDelta : Float )

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
   * @throws IllegalStateException if [beginDrag] or [pursueDrag] was not called before.
   */
  fun pursueDrag( time : Long, position : Float )

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  fun endDrag( time : Long, position : Float )

  /**
   * Compute the scroll amount, in the same unit as [sightLength].
   *
   * @param elapsedTimeInSecond mirrors the [com.badlogic.gdx.scenes.scene2d.Actor.act] method.
   */
  fun scrollAmountAfter( elapsedTimeInSecond : Float ) : Float

}

class AbstractKineticScrollEngine(
    /**
     * Very suspicious. Should be 1000 because we convert delta time in milliseconds
     * to a speed in seconds.
     */
    private val velocityCorrection : Float = 100f
) : KineticScrollEngine {

  override var sightLength = 0f

  override var availableLength = 0f

  /**
   * The last time (as [System.currentTimeMillis]) set by [pursueDrag].
   */
  private var lastUpdateTime = 0L

  /**
   * Scroll amount of the component, in the same unit as [sightLength].
   */
  private var scrollAmount = 0f

  /**
   * Indicates that [beginDrag] got called, and [endDrag] has not been called yet.
   */
  private var dragging = false

  /**
   * Meaningful only when [dragging] is `true`. A value of -1 means the value is undefined.
   */
  private var lastPosition = -1f

  /**
   * Derived from [lastPosition], [lastUpdateTime], and from values passed to [pursueDrag].
   * Decreases with time during inertial scrolling.
   * The distance unit is the same unit as [sightLength]. The time unit is the second.
   */
  private var velocity = 0f

  private fun cancelScrolling() {
    velocity = 0f
    val scrollFreedom = availableLength - sightLength
    if( scrollAmount > scrollFreedom ) scrollAmount = scrollFreedom
    else if( scrollAmount < 0f ) scrollAmount = 0f
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


  override fun forceScroll(scrollDelta : Float) {
    scrollAmount += scrollDelta
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
    check( ! dragging )
    check( position >= 0 )
    checkUpdateTime(time)
    lastUpdateTime = time
    lastPosition = position
    dragging = true
  }

  /**
   * Indicates that some drag gesture is going on.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchDragged].
   *
   * @param position, in the same unit as [sightLength].
   * @throws IllegalStateException if [beginDrag] or [pursueDrag] was not called before.
   */
  override fun pursueDrag( time : Long, position : Float ) {
    doDrag( "#pursueDrag", time, position )
  }

  private fun doDrag( logCategory : String, time : Long, position : Float ) {
    check( dragging ) { "Not dragging" }
    checkUpdateTime(time)
//    check( position >= 0 ) { "Inconsistent position: $position" }
    val distance = position - lastPosition

    // The drag takes immediate effect.
    scrollAmount += distance

    val deltaTime = time - lastUpdateTime

    // We derive only the last value. It would be better to use a ring of position-time values.
    velocity = if( distance == 0f || deltaTime == 0L ) 0f else velocityCorrection * distance / deltaTime

    lastUpdateTime = time
    lastPosition = position

    logDebug( "$logCategory, scrollAmount=$scrollAmount, velocity=$velocity, lastPosition=$lastPosition" )
  }

  private fun checkUpdateTime(time : Long) {
    check( time >= lastUpdateTime ) { "Inconsistent time: $time, last update is $lastUpdateTime" }
  }

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  override fun endDrag( time : Long, position : Float ) {
    doDrag( "#endDrag", time, position )
    dragging = false
  }

  /**
   * Compute the scroll amount, in the same unit as [sightLength].
   *
   * @param elapsedTimeInSecond mirrors the [com.badlogic.gdx.scenes.scene2d.Actor.act] method.
   */
  override fun scrollAmountAfter( elapsedTimeInSecond : Float ) : Float {
    check( elapsedTimeInSecond >= 0f) { "Incorrect value for elapsedTimeInSecond: $elapsedTimeInSecond" }
    val displacement = velocity * elapsedTimeInSecond
    val calculatedScrollAmount = scrollAmount + displacement
    val velocityDelta = 0.1f * elapsedTimeInSecond
    velocity -= if( velocity > 0f ) velocityDelta else -velocityDelta
    if( Math.abs( velocity ) < 0.01f ) velocity = 0f
    scrollAmount = 0f
    return calculatedScrollAmount
  }

  private fun logDebug(message : String) {
    @Suppress("ConstantConditionIf")
    if( SweepChoice.DEBUG ) {
      Gdx.app.debug(KineticScrollEngine::class.java.simpleName, message)
    }
  }

}

/**
 * Returns a fresh [KineticScrollEngine] that scrolls continuously on
 * [KineticScrollEngine.availableLength].
 */
fun continuous() = AbstractKineticScrollEngine()

