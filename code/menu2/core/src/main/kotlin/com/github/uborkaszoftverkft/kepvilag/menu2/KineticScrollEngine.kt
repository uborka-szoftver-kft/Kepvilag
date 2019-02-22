package com.github.uborkaszoftverkft.kepvilag.menu2


interface KineticScrollEngine {

  /**
   * The visible area on which scrolling happens.
   */
  val sightLength : Float

  /**
   * The length of the component to scroll, in the same unit as [sightLength].
   */
  val availableLength : Float

  fun resize( newSightLength : Float, newAvailableLength : Float)

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

class AbstractKineticScrollEngine : KineticScrollEngine {

  override var sightLength = 0f

  override var availableLength = 0f

  /**
   * The distance recently set by [pursueDrag].
   */
  private var recentDragDistance = 0f

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

  override fun resize( newSightLength : Float, newAvailableLength : Float ) {
    check( newSightLength >= 0 )
    check( newAvailableLength >= 0 )
    val oldRatio = sightLength / availableLength
    this.sightLength = newSightLength
    this.availableLength = newAvailableLength
    scrollAmount *= oldRatio
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
    lastUpdateTime = time
    lastPosition = position
  }

  /**
   * Indicates that some drag gesture is going on.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchDragged].
   *
   * @param position, in the same unit as [sightLength].
   * @throws IllegalStateException if [beginDrag] or [pursueDrag] was not called before.
   */
  override fun pursueDrag( time : Long, position : Float ) {
    check( dragging )
    check( time > lastUpdateTime )
    check( position >= 0 )
    val distance = lastPosition - position

    // The drag takes immediate effect.
    scrollAmount += distance

    // We derive only the last value. It would be better to use a ring of position-time values.
    velocity = distance * 1000 / time - lastUpdateTime  // We have milliseconds here.

    lastUpdateTime = time
    lastPosition = position
  }

  /**
   * Indicates that some drag gesture ended.
   * This method mirrors [com.badlogic.gdx.scenes.scene2d.InputListener.touchUp].
   *
   * @param position in the same unit as [sightLength].
   * @throws IllegalStateException if [pursueDrag] was not called before.
   */
  override fun endDrag( time : Long, position : Float ) {
    pursueDrag( time, position )
    dragging = false
    lastUpdateTime = -1
    lastPosition = -1f
  }

  /**
   * Compute the scroll amount, in the same unit as [sightLength].
   *
   * @param elapsedTimeInSecond mirrors the [com.badlogic.gdx.scenes.scene2d.Actor.act] method.
   */
  override fun scrollAmountAfter( elapsedTimeInSecond : Float ) : Float {
    val displacement = velocity * elapsedTimeInSecond
    scrollAmount += displacement
    velocity -= 0.1f * elapsedTimeInSecond
    if( Math.abs( velocity ) < 0.01f ) velocity = 0f
    return scrollAmount
  }

}

