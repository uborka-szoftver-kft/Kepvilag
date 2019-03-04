package com.github.uborkaszoftverkft.kepvilag.menu2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VelocityRecorderTest {

  @Test
  internal fun initial() {
    val velocityRecorder = VelocityRecorder()
    assertEquals( velocityRecorder.average(), 0f )
  }

  @Test
  internal fun record() {
    val velocityRecorder = VelocityRecorder( 10, 1 )
    velocityRecorder.record( 10f, 1f )
    velocityRecorder.record( 70f, 3f )
    assertEquals( velocityRecorder.average(), 20f )
    velocityRecorder.clear()
    assertEquals( velocityRecorder.average(), 0f )
  }

  @Test
  internal fun recordWithCycle() {
    val velocityRecorder = VelocityRecorder( 3, 3 )
    velocityRecorder.record( 100f, 0f )
    velocityRecorder.record( 1f, 1f )
    velocityRecorder.record( 3f, 1f )
    velocityRecorder.record( 5f, 1f )
    // ( 1 + 3 + 5 ) / ( 1 + 1 + 1 ) = 9 / 3
    assertEquals( 3f, velocityRecorder.average() )
  }
}