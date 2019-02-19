package io.github.uborkaszoftver.menu1;

/**
 * Contract for programmatically modifying Ascender
 * This is useful when using another controller than touch, e.g. keyboard or on-screen buttons.
 */
public interface ChoiceNavigator {
    /**
     * Triggers scrollup of current Ascender, or unveils of next Ascender if any.
     */
    void goForward() ;

    void goBackward() ;

    void selectCurrent() ;

    interface AscenderListener {

        void sightChanged(
            int ascenderIndex,
            boolean mayScrollUp,
            boolean mayScrollLeft,
            boolean mayScrollDown,
            boolean mayScrollRight
        ) ;

        void selected( int ascenderIndex ) ;

    }

}
