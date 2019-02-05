package io.github.kepvilag.fecskemese.graph

import java.io.File
import kotlin.io.writeText


/**
 * Where a [Dialog] can send to.
 */
enum class Place( val prettyName : String ) {
    PARLEMENT( "Parlement" ),
    NYUGATI_PALYAUDVAR( "Nyugati Palyaudvar"),
    GELLERT( "Gellért" ),
    KERTESZ_UTCA( "Kertész utca" ),
    VAR( "A Vár" ),
    SZABADSAG_HID( "Szabadság híd" ),

}

/**
 * Used only for special behaviors.
 */
enum class PrebuiltFicko {
    ORBAN_VIKTOR,
    VLADIMIR_PUTYIN,
    DONALD_TRUMP,
    ZAZIE
}

/**
 * Tagging interface for some kind of enums.
 */
interface Label

/**
 * There may be some localization to add, not even talking about some associated icon or whatever.
 */
typealias Choice = String

class Dialog< LABEL : Label >(
    val place : Place,
    configurator : ( dialog : Dialog< LABEL > ) -> Unit
) {
    private val _proceedings : MutableMap< LABEL, Proceeding > = LinkedHashMap()

    public val proceedings : Map< LABEL, Proceeding > get() { return _proceedings }

    init {
        configurator( this )
    }

    private fun add( proceeding : Proceeding ) {
        check( ! _proceedings.containsValue( proceeding ) ) { "Already present: $proceeding" }
        _proceedings[ proceeding.label ] = proceeding
    }

    abstract inner class Proceeding( val label : LABEL, val text : String )

    inner class Strophe(
        label : LABEL,
        text : String,
        val choices : Map< Choice, LABEL >
    ) : Proceeding( label, text ){
        init {
            add( this )  // Just to show that we can reference outer class private members.
        }
    }
    inner class Jump(
        label : LABEL,
        text : String,
        val destination : LABEL
    ) : Proceeding( label, text ) {
        init {
            add( this )
        }
    }

    inner class DestinationChoice(
        label : LABEL,
        text : String,
        val destinations : Set< Place >
    ) : Proceeding( label, text ) {

        constructor(
            label : LABEL,
            text : String,
            vararg destinations : Place
        ) : this( label, text, setOf( *destinations ) )
        init {
            destinations.forEach { check( it != place ) { "Destination $it same as $place" } }
            add( this )
        }
    }
}

/**
 * Contains the whole graph.
 * For now there can be only one [Dialog] per [Place] because the latter is given in
 * [Dialog.DestinationChoice] but we'll probably something more sophisticated.
 *
 * @param dialogs must be an ordered, non-empty [Set] with first element giving the initial [Place].
 */
class Story( val dialogs : Set< Dialog< out Label > > ) {
    constructor( vararg dialogs : Dialog< out Label > ) : this( setOf( *dialogs ) )

    init {
        check( dialogs.distinct().count() == dialogs.size ) {
            "Same " + Place::class.simpleName + " appears more than once in $dialogs" }
    }

    fun dialogFor( place : Place ) : Dialog< out Label > {
        return dialogs.firstOrNull { it.place == place } ?:
            throw NoSuchElementException( "Unknown: '$place'" )
    }
}

interface StropheLabel {
    enum class KerteszUtca1 : Label { K, JO, NEM }
    enum class Gellert1 : Label { UDV, VEGE }
}

/**
 * Not thread-safe.
 */
class Holder< OBJECT > {
    var current : OBJECT? = null
    var value : OBJECT
        get() { check( current != null ) ; return current !! }
        set( value ) { check( current == null ) ; current = value }
}

fun demo1() {

    val kerteszUtca1 : Dialog< StropheLabel.KerteszUtca1 > = Dialog( Place.KERTESZ_UTCA ) {
        val p = Holder< Dialog< StropheLabel.KerteszUtca1 >.Proceeding >()
        p.value = it.Strophe(
            StropheLabel.KerteszUtca1.K,
            "Hogy vagy?",
            mapOf(
                "Jól, és te?" to StropheLabel.KerteszUtca1.JO,
                "Nem túl jól" to StropheLabel.KerteszUtca1.NEM
            )
        )
        it.Jump(
            StropheLabel.KerteszUtca1.JO,
            "Akkor nagyon örülök Neked! Hát ezenkívül…",
            p.value.label
            // StropheLabel.KerteszUtca1.K
        )
        it.DestinationChoice(
            StropheLabel.KerteszUtca1.NEM,
            "Akkor menj a fürdőbe!",
            Place.GELLERT
        )
    }

    val gellert1 : Dialog< StropheLabel.Gellert1 > = Dialog( Place.GELLERT ) {
        it.Jump(
            StropheLabel.Gellert1.UDV,
            "Élvezze a fürdőt úram!",
            StropheLabel.Gellert1.VEGE
        )
        it.DestinationChoice(
            StropheLabel.Gellert1.VEGE,
            "További szép napot! Jó pihenést otthonában!",
            Place.KERTESZ_UTCA
        )
    }

    val story = Story( kerteszUtca1, gellert1 )

    println( "Current directory is '${System.getProperty( "user.dir" )}'." )
    val dot = DotRenderer().render( story )
    println( dot )

    File( "src/main/js/generated.js" ).writeText( dot )

}

fun main() {
    demo1()
}


