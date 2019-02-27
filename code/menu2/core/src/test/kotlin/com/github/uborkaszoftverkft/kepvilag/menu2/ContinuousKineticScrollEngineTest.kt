package com.github.uborkaszoftverkft.kepvilag.menu2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ContinuousKineticScrollEngineTest {
  @Test
  internal fun initial() {
    val scrollEngine = ContinuousKineticScrollEngine(
        logger = consoleLogger( "ScrollEngine" ),
        momentumDurationMs = 1000
    )

    assertEquals( 0f, scrollEngine.scrollAmountAt( 10 ) )
  }

  /**
   * Using a simple line of form `-2x + 10` as [reference](https://www.desmos.com/calculator).
   *
   */
  @Test
  internal fun simpleSlowdownWithUnrealisticValues() {
    val scrollEngine = ContinuousKineticScrollEngine(
        logger = consoleLogger( "ScrollEngine" ),
        momentumDurationMs = 3000
    )

    scrollEngine.beginDrag( 1900, 0f )
    scrollEngine.pursueDrag( 2000, 600f )
    scrollEngine.endDrag( 2000, 600f )  // Distance of 600 within 100 ms => velocity = 6.

    var previous = 0f
    for( t in 2000..5000 step 100 ) {
      val scrollAmount = scrollEngine.scrollAmountAt( t.toLong() )
      val scrollAmountString = floatFormat.format( scrollAmount )
      val incrementString = floatFormat.format( scrollAmount - previous )
      println( "At $t scrollAmount=$scrollAmountString, increment=$incrementString" )
      previous = scrollAmount

    }

    // Momentum ends after 1000 ms.
    assertEquals( 1100f, scrollEngine.scrollAmountAt( 1100 ) )
    assertEquals( 1200f, scrollEngine.scrollAmountAt( 1200 ) )
    assertEquals( 1300f, scrollEngine.scrollAmountAt( 1300 ) )
    assertEquals( 1400f, scrollEngine.scrollAmountAt( 1400 ) )
    assertEquals( 1500f, scrollEngine.scrollAmountAt( 1500 ) )
    assertEquals( 1600f, scrollEngine.scrollAmountAt( 1600 ) )
    assertEquals( 1700f, scrollEngine.scrollAmountAt( 1700 ) )
    assertEquals( 1800f, scrollEngine.scrollAmountAt( 1800 ) )
    assertEquals( 1900f, scrollEngine.scrollAmountAt( 1900 ) )
    assertEquals( 2000f, scrollEngine.scrollAmountAt( 2000 ) )

  }
  @Test
  internal fun simpleSlowdown() {
    val scrollEngine = ContinuousKineticScrollEngine(
        logger = consoleLogger( "ScrollEngine" ),
        momentumDurationMs = 1000
    )

    scrollEngine.beginDrag( 900, 800f )
    scrollEngine.pursueDrag( 910, 850f )
    scrollEngine.pursueDrag( 990, 900f )
    scrollEngine.pursueDrag( 1000, 1000f )
    scrollEngine.endDrag( 1000, 1000f )  // Distance of 200 within 100 ms => velocity = 2.

    var previous = 0f
    for( t in 1100..2000 step 100 ) {
      val scrollAmount = scrollEngine.scrollAmountAt( t.toLong() )
      val scrollAmountString = floatFormat.format( scrollAmount )
      val incrementString = floatFormat.format( scrollAmount - previous )
      println( "At $t scrollAmount=$scrollAmountString, increment=$incrementString" )
      previous = scrollAmount

    }

    // Momentum ends after 1000 ms.
    assertEquals( 1100f, scrollEngine.scrollAmountAt( 1100 ) )
    assertEquals( 1200f, scrollEngine.scrollAmountAt( 1200 ) )
    assertEquals( 1300f, scrollEngine.scrollAmountAt( 1300 ) )
    assertEquals( 1400f, scrollEngine.scrollAmountAt( 1400 ) )
    assertEquals( 1500f, scrollEngine.scrollAmountAt( 1500 ) )
    assertEquals( 1600f, scrollEngine.scrollAmountAt( 1600 ) )
    assertEquals( 1700f, scrollEngine.scrollAmountAt( 1700 ) )
    assertEquals( 1800f, scrollEngine.scrollAmountAt( 1800 ) )
    assertEquals( 1900f, scrollEngine.scrollAmountAt( 1900 ) )
    assertEquals( 2000f, scrollEngine.scrollAmountAt( 2000 ) )

  }


// =========
// Utilities
// =========

  companion object {
    private val floatFormat = "%8.2f"
  }


}