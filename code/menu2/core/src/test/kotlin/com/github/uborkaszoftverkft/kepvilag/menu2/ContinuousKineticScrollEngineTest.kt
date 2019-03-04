package com.github.uborkaszoftverkft.kepvilag.menu2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class ContinuousKineticScrollEngineTest {
  @Test
  internal fun initial() {
    val scrollEngine = ContinuousKineticScrollEngine(
        logger = consoleLogger( "ScrollEngine" ),
        momentumDurationS = 1f
    )

    assertEquals( 0f, scrollEngine.scrollAmountAt( 10 ) )
  }

  /**
   * Using a simple line of form `-2t + 10` as [reference](https://www.desmos.com/calculator).
   * Momentum starts arbitrarily at `t=2` with a velocity of 6.
   * A Momentum duration of 3 brings the velocity to 0 at `t=5`.
   *
   */
  @Test
  internal fun simpleSlowdownWithUnrealisticValues() {
    val scrollEngine = ContinuousKineticScrollEngine(
        logger = consoleLogger( "ScrollEngine" ),
        momentumDurationS = 3f
    )

    scrollEngine.beginDrag( 1000, 10f )
    scrollEngine.pursueDrag( 2000, 16f )
    scrollEngine.endDrag( 2000, 16f )

    for( t in 2000..5500 step 500 ) {
      val scrollAmount = scrollEngine.scrollAmountAt( t.toLong() )
      val scrollAmountString = floatFormat.format( scrollAmount )
      println( "scrollAmount( $t, ${scrollAmountString}f )," )
    }

    fun scrollAmount( time : Long, expected : Float ) = {
      assertEquals( expected, scrollEngine.scrollAmountAt( time ) )
    }

    assertAll(
        scrollAmount( 2000,  6.00f ),
        scrollAmount( 2500,  8.75f ),
        scrollAmount( 3000, 11.00f ),
        scrollAmount( 3500, 12.75f ),
        scrollAmount( 4000, 14.00f ),
        scrollAmount( 4500, 14.75f ),
        scrollAmount( 5000, 15.00f ),
        scrollAmount( 5500, 15.00f )
    )

  }


// =========
// Utilities
// =========

  companion object {
    private const val floatFormat = "%8.2f"
  }


}