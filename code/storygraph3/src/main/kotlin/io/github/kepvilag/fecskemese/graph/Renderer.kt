package io.github.kepvilag.fecskemese.graph

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Not thread-safe, not reentrant.
 */
abstract class Renderer( private val setup : Setup = Setup() ) : LineAccumulator {

    private val builder = StringBuilder()
    private var depth = -1

    private fun indent() { depth ++ }

    private fun outdent() { check( depth >= 0 ) ; depth -- }

    private fun printIndent() { for( i in 0..( depth - 1 ) ) builder.append( setup.indent ) }

    private fun printLineBreak() { builder.append( setup.lineBreak ) }

    override fun addLine( line: String ) {
        printIndent()
        builder.append( line )
        printLineBreak()
    }

    protected fun block( lineBlock : ( lineAccumulator : LineAccumulator ) -> Unit ) {
        indent()
        lineBlock( this )
        outdent()
    }

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

    fun addLine(line : String )

    operator fun String.unaryPlus() {
        addLine( this )
    }
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
            return "node_$i"
        }

        block {
            + "dot = `"  // Importing the .dot file as JavaScript requires this (and the closing one).
            + "# ${setup.textSetup.identifier()}"
            + "digraph G {"
            block {
                + "fontsize=${setup.fontSize}"
                + "labeljust=left"
                + "style=rounded"
                + "node[shape=box]"
                + "edge[arrowhead=vee]"

                var proceedingIdentifierGenerator = 0
                for( dialogEntry in story.dialogs.withIndex() ) {
                    + "subgraph cluster_${dialogEntry.index} {"
                    block {
                        + "label=\"${dialogEntry.value.place.prettyName}\""
                        + "node [ style=filled,color=white ]"
                        + "style=filled"
                        + "color=lightgrey"
                        for( proceeding in dialogEntry.value.proceedings.values ) {
                            proceedingMap[ ( proceedingIdentifierGenerator ) ] = proceeding
                            + "${proceedingIdentifier( proceedingIdentifierGenerator ) } ["
                            block {
                                + "label = <"
                                block {
                                    + "<table border='0' cellborder='0' cellspacing='1' style='rounded'>"
                                    block {
                                        + "<tr><td align='center'><b>${proceeding.label}</b></td></tr>"
                                        + "<tr><td align='left'>${proceeding.text}</td></tr>"
                                    }
                                    + "</table> >"

                                }
                            }
                            + "]"
                            proceedingIdentifierGenerator ++
                        }
                    }
                    + "}"
                }
                + "start -> ${proceedingIdentifier(0)}"

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
                                    + "$origin -> $target [ headlabel=\"$choice\" ]"
                                }
                            }

                            is Dialog< out Label >.DestinationChoice -> {
                                for( destination in proceeding.destinations ) {
                                    val targetDialog = story.dialogFor( destination )
                                    val targetProceeding = targetDialog.proceedings.values.first()
                                    val target = proceedingIdentifier( resolveIdentifier( targetProceeding ) )
                                    + "$origin -> $target [ arrowhead=\"empty\" ]"
                                }
                            }

                            is Dialog< out Label >.Jump -> {
                                val targetProceeding = dialogEntry.value.proceedings[ proceeding.destination ] ?:
                                        throw NoSuchElementException( "Unknown: $proceeding.destination")
                                val target = proceedingIdentifier( resolveIdentifier( targetProceeding ) )
                                + "$origin -> $target"

                            }
                        }
                    }
                }
            }
            + "}"
            + "`"

        }
    }
}