package io.github.kepvilag.fecskemese.graph


/**
 * Where a [Dialog] can send to.
 */
enum class Place {
    PARLEMENT,
    NYUGATI_PALYAUDVAR,
    GELLERT,
    KERTESZ_UTCA,
    VAR,
    SZABADSAG_HID,
    ;
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
    private val proceedings : MutableMap< LABEL, Proceeding > = mutableMapOf()

    init {
        configurator( this )
    }

    private fun add( proceeding : Proceeding ) {
        proceedings[ proceeding.label ] = proceeding
    }

    abstract inner class Proceeding( val label : LABEL )

    inner class Strophe(
        label : LABEL,
        val text : String,
        val choices : Map< Choice, LABEL >
    ) : Proceeding( label ){
        init {
            add( this )  // Just to show that we can reference outer class private members.
        }
    }
    inner class Jump(
        label : LABEL,
        val text : String,
        val destination : LABEL
    ) : Proceeding( label ) {
        init {
            add( this )
        }

    }

    inner class DestinationChoice(
        label : LABEL,
        val text : String,
        vararg destination : Place
    ) : Proceeding( label ){
        init {
            destination.forEach { check( it != place ) { "Destination $destination same as $it" } }
            add( this )
        }
    }
}

/**
 * @param dialogs must be an ordered, non-empty [Set] with first element giving the initial [Place].
 */
class Story( val dialogs : Set< Dialog< * > > ) {
    constructor( vararg dialogs : Dialog< * > ) : this( setOf( *dialogs ) )
}

interface StropheLabel {
    enum class KerteszUtca1 : Label { K, JO, NEM }
    enum class Gellert1 : Label { UDV, VEGE }
}


fun demo1() {

    val kerteszUtca1 : Dialog<StropheLabel.KerteszUtca1> = Dialog( Place.KERTESZ_UTCA ) {
        it.Strophe(
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
            StropheLabel.KerteszUtca1.K
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
            "További szép napot! Jó pihenés otthon!",
            Place.KERTESZ_UTCA
        )
    }

    val story = Story( kerteszUtca1, gellert1 )

}

fun main() {
    demo1()
}


