package io.github.kepvilag.fecskemese.graph

class Story2 {

    constructor( configurator : ( Story2 ) -> Unit ) {
        configurator( this )
    }

    private val _acts = mutableListOf< Act >()
    private val _entities = mutableMapOf< Key, Keyed >()

    @Suppress("LeakingThis")
    abstract inner class Keyed(mnemonic : Char ) {
        val key : Key = newKey( mnemonic )
        init { _entities[ key ] = this }
    }

    inner class Act( val place : Place ) : Keyed( 'A' ) {

        constructor( place : Place, configurator : ( Act ) -> Unit ) : this( place ) {
            configurator( this )
        }

        init { this@Story2._acts.add( this ) }

        private val _stopovers = mutableListOf< Stopover > ()
        public val stopovers : List< Stopover > get() = _stopovers

        @Suppress("LeakingThis")
        abstract inner class Stopover( mnemonic: Char, val text : String ) : Keyed( mnemonic ){
            init { this@Act._stopovers.add( this ) }
        }

        inner class Strophe(
            text : String,
            val choices : Map< Choice2, Holder< Stopover > >
        ) : Stopover( 'S', text )

        inner class Trip(
            text : String,
            val choices : Map< Choice2, Place >
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

typealias Choice2 = String

/**
 * Not thread-safe.
 */
class Holder< OBJECT > {
    var current : OBJECT? = null
    var value : OBJECT
        get() { check( current != null ) ; return current !! }
        set( value ) { check( current == null ) ; current = value }
}

typealias StopoverHolder = Holder< Story2.Act.Stopover >

class Key( val mnemonic : Char, val index : Int ) : Comparable< Key > {
    override fun compareTo( other: Key ): Int {
        return compareValuesBy(
            this,
            other,
            { it.mnemonic },
            { it.index }
        )
    }
}


fun demo2(): Story2 {
    return Story2 { story ->
        val szia = StopoverHolder()
        story.Act( Place.KERTESZ_UTCA ) {
            val jol = StopoverHolder()
            val nemJol = StopoverHolder()
            szia.value = it.Strophe(
                "Szia, jól vagy? ",
                mapOf(
                    "Jól, és te?" to szia,
                    "Nem túl jól." to nemJol
                )
            )
            jol.value = it.Jump(
                "Akkor nagyon örülök Neked! Hát ezenkívül…",
                szia
            )
            nemJol.value = it.Trip(
                "Akkor menj a fürdőbe!",
                mapOf(
                    "Rendben!" to Place.GELLERT
                )
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
    val demo2 = demo2()
    println( "Created: $demo2" )
}