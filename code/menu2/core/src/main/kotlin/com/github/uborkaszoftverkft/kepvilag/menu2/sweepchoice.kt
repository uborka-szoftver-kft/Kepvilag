package com.github.uborkaszoftverkft.kepvilag.menu2

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
