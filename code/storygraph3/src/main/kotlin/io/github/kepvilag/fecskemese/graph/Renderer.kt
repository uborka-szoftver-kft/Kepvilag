package io.github.kepvilag.fecskemese.graph

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Not thread-safe, not reentrant.
 */
abstract class Renderer( private val setup : Setup ) : LineAccumulator {

    private val builder = StringBuilder()
    private var depth = 0

    private fun indent() { depth ++ }

    private fun outdent() { depth -- ; check( depth >= 0 ) }

    private fun printIndent() { for( i in 0..( depth - 1 ) ) builder.append( setup.indent ) }

    private fun printLineBreak() { builder.append( setup.lineBreak ) }

    override fun plusAssign( line: String ) {
        printIndent()
        builder.append( line )
        printLineBreak()
    }

    fun indented(lineBlock : (lineAccumulator : LineAccumulator ) -> Unit ) {
        indent()
        lineBlock( this )
        outdent()
    }

    fun block( lineBlock : ( lineAccumulator : LineAccumulator ) -> Unit ) {
        lineBlock( this )
    }

    constructor() : this( Setup() )

    data class Setup(
        val indent : String = "  ",
        val lineBreak : String = String( byteArrayOf( 10 ) ),
        val clock : () -> ZonedDateTime = { ZonedDateTime.now() },
        val renderingIdentifier : ( clock : () -> ZonedDateTime ) -> String = {
            "Created on " +
            DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss ZZZ" ).format( clock.invoke() )
        }
    ) {
        fun identifier() : String {
            return renderingIdentifier( clock )
        }
    }

    fun render( story: Story ) : String {
        builder.clear()
        doRender( story )
        return builder.toString()
    }

    abstract fun doRender( story: Story )


}

interface LineAccumulator {
    operator fun plusAssign( line : String )
}

class DotRenderer( private val setup : DotRenderer.Setup = DotRenderer.Setup() ) :
    Renderer( setup = setup.textSetup )
{
    data class Setup(
        val textSetup : Renderer.Setup = Renderer.Setup(),
        val fontSize : Int = 10
    )

    override fun doRender( story: Story ) {

        val proceedingMap : MutableMap< Int, Dialog< out Label >.Proceeding > = mutableMapOf()

        fun resolveIdentifier( proceeding : Dialog< out Label >.Proceeding ) : Int {
            val firstOrNull = proceedingMap.entries.firstOrNull { it.value == proceeding }
            return firstOrNull?.key ?: throw NoSuchElementException( "Unknown: '$proceeding'" )
        }

        fun proceedingIdentifier( i : Int ) : String {
            return "p$i"
        }

        block {
            it += "dot = `"
            it += "# " + setup.textSetup.identifier()
            it += "digraph G {"
            indented {
                it += "fontsize=10"
                it += "labeljust=left"
                it += "style=rounded"
                it += "node[shape=box]"
                it += "edge[arrowhead=vee]"

                var proceedingIdentifierGenerator = 0
                for( dialogEntry in story.dialogs.withIndex() ) {
                    it += "subgraph cluster_${dialogEntry.index} {"
                    indented {
                        it += "label=\"${dialogEntry.value.place.prettyName}\""
                        it += "node [ style=filled,color=white ]"
                        it += "style=filled"
                        it += "color=lightgrey"
                        for( proceeding in dialogEntry.value.proceedings.values ) {
                            proceedingMap[ ( proceedingIdentifierGenerator ) ] = proceeding
                            it += proceedingIdentifier( proceedingIdentifierGenerator ) + " ["
                            indented {
                                it += "label = <"
                                indented {
                                    it += "<table border='0' cellborder='0' cellspacing='1' style='rounded'>"
                                    indented {
                                        it += "<tr><td align='center'><b>${proceeding.label}</b></td></tr>"
                                        it += "<tr><td align='left'>${proceeding.text}</td></tr>"
                                    }
                                    it += "</table> >"

                                }
                            }
                            it += "]"
                            proceedingIdentifierGenerator ++
                        }
                    }
                    it += "}"
                }
                it += "start -> " + proceedingIdentifier( 0 )

                for( dialogEntry in story.dialogs.withIndex() ) {
                    for( proceeding in dialogEntry.value.proceedings.values ) {
                        val originProceedingIdentifier = resolveIdentifier( proceeding )
                        val origin = proceedingIdentifier( originProceedingIdentifier )
                        when( proceeding ) {

                            is Dialog< out Label >.Strophe -> {
                                for( choiceEntry in proceeding.choices ) {
                                    val targetProceeding: Dialog< out Label >.Proceeding =
                                        dialogEntry.value.proceedings[ choiceEntry.value ] ?:
                                            throw NoSuchElementException( "Unknown: '${choiceEntry.value}'" )
                                    val target = proceedingIdentifier( resolveIdentifier( targetProceeding ) )
                                    val choice = choiceEntry.key
                                    it += "$origin -> $target [ headlabel=\"$choice\" ]"
                                }
                            }

                            is Dialog< out Label >.DestinationChoice -> {
                                for( destination in proceeding.destinations ) {
                                    val targetDialog = story.dialogFor( destination )
                                    val targetProceeding = targetDialog.proceedings.values.first()
                                    val target = proceedingIdentifier( resolveIdentifier( targetProceeding ) )
                                    it += "$origin -> $target [ arrowhead=\"empty\" ]"
                                }
                            }

                            is Dialog< out Label >.Jump -> {
                                val targetProceeding = dialogEntry.value.proceedings[ proceeding.destination ] ?:
                                        throw NoSuchElementException( "Unknown: $proceeding.destination")
                                val target = proceedingIdentifier( resolveIdentifier( targetProceeding ) )
                                it += "$origin -> $target"

                            }
                        }
                    }
                }
            }
            it += "}"
            it += "`"

        }
    }
}