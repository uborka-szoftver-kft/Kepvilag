package io.github.kepvilag.fecskemese.graph

import java.io.File


/**
 * Where a [Story.Act.Stopover] can send to.
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


class Story {

    constructor( configurator : (Story ) -> Unit ) {
        configurator( this )
        /** TODO check consistency and [Holder.value]. */
    }

    private val _acts = mutableListOf< Act >()

    /**
     * TODO become a lazy property based on filtering of [_entities].
     */
    public val acts : List< Act > get() = _acts

    private val _entities = mutableMapOf< Key, Keyed >()
    public val entities : Map< Key, Keyed > get() = _entities

    @Suppress("LeakingThis")
    abstract inner class Keyed( mnemonic : Char ) {
        val key : Key = newKey( mnemonic )
        init { _entities[ key ] = this }
    }

    fun firstStopoverFor( place: Place ) : Act.Stopover {
        return acts.first { it.place == place }.stopovers.values.first()
    }

    inner class Act( val place : Place ) : Keyed( 'A' ) {

        constructor( place : Place, configurator : ( Act ) -> Unit ) : this( place ) {
            configurator( this )
        }

        init { this@Story._acts.add( this ) }

        private val _stopovers = LinkedHashMap< Key, Stopover > ()
        public val stopovers : Map< Key, Stopover > get() = _stopovers

        @Suppress("LeakingThis")
        abstract inner class Stopover( mnemonic: Char, val text : String ) : Keyed( mnemonic ){
            init { this@Act._stopovers[ key ] = this }
        }

        inner class Strophe(
            text : String,
            val choices : Map< Choice, Holder< Stopover > >
        ) : Stopover( 'S', text )

        inner class Trip(
            text : String,
            val destinations : Set< Place >
        ) : Stopover( 'T', text )

        inner class Jump(
            text : String,
            val destination : Holder< Act.Stopover >
        ) : Stopover( 'J', text )

    }

    private var keyGenerator = 0

    fun newKey( mnemonic : Char ) : Key {
        return Key( mnemonic, keyGenerator ++ )
    }

}

typealias Choice = String

/**
 * Not thread-safe.
 */
class Holder< OBJECT > {
    var current : OBJECT? = null
    var value : OBJECT
        get() { check( current != null ) ; return current !! }
        set( value ) { check( current == null ) ; current = value }
}

typealias StopoverHolder = Holder< Story.Act.Stopover >

/**
 * @param mnemonic TODO make it an enum.
 */
data class Key( val mnemonic : Char, val index : Int ) : Comparable< Key > {
    override fun compareTo( other: Key ): Int {
        return compareValuesBy(
            this,
            other,
            { it.mnemonic },
            { it.index }
        )
    }

    val identifier : String get() = "$mnemonic$index"
}


fun demo1(): Story {
    return Story { story ->
        val szia = StopoverHolder()
        story.Act( Place.KERTESZ_UTCA ) {
            val jol = StopoverHolder()
            val nemJol = StopoverHolder()
            szia.value = it.Strophe(
                "Szia, jól vagy? ",
                mapOf(
                    "Jól, és te?" to jol,
                    "Nem túl jól." to nemJol
                )
            )
            jol.value = it.Jump(
                "Akkor nagyon örülök Neked! Hát ezenkívül…",
                szia
            )
            nemJol.value = it.Trip(
                "Akkor menj a fürdőbe!",
                setOf( Place.GELLERT )
            )
        }
        story.Act( Place.GELLERT ) {
            val viszlat = StopoverHolder()
            it.Jump( "Élvezze a fürdőt úram!", viszlat )
            viszlat.value = it.Jump( "További szép napot! Jó pihenést otthonában!", szia )
        }
    }

}

fun main() {
    val story = demo1()
    println( "Current directory is '${System.getProperty( "user.dir" )}'." )
    val dot = DotRenderer().render( story )
    println( dot )

    File( "src/main/js/generated.js" ).writeText( dot )

}

