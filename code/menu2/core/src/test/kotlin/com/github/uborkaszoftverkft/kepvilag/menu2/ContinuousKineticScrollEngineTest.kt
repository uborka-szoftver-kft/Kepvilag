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
        scrollAmount( 2000,    16.00f ),
        scrollAmount( 2500,    18.75f ),
        scrollAmount( 3000,    21.00f ),
        scrollAmount( 3500,    22.75f ),
        scrollAmount( 4000,    24.00f ),
        scrollAmount( 4500,    24.75f ),
        scrollAmount( 5000,    25.00f ),
        scrollAmount( 5500,    25.00f )
    )

  }


// =========
// Utilities
// =========

  companion object {
    private const val floatFormat = "%8.2f"
  }


}