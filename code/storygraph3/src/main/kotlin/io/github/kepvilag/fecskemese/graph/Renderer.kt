package io.github.kepvilag.fecskemese.graph

import java.awt.Color
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Not thread-safe, not reentrant.
 *
 */
abstract class Renderer< OBJECT >( private val setup : Setup = Setup() ) : LineAccumulator {

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

    fun render( subject : OBJECT ) : String {
        builder.clear()
        doRender( subject )
        return builder.toString()
    }

    abstract fun doRender( subject : OBJECT )


}

interface LineAccumulator {

    fun addLine(line : String )

    operator fun String.unaryPlus() {
        addLine( this )
    }
}

/**
 * TODO: <a href="https://stackoverflow.com/questions/2012036/graphviz-how-to-connect-subgraphs">connect subgraphs</a>
 */
class DotRenderer( private val setup : DotRenderer.Setup = DotRenderer.Setup() ) :
    Renderer< Story >( setup = setup.textSetup )
{
    data class Setup(
        val textSetup : Renderer.Setup = Renderer.Setup(),
        val fontSize : Int = 10,
        val backgroundColor : Color = Color.DARK_GRAY,
        val clusterBackgroundColor : Color = Color.GRAY,
        val clusterStrokeColor : Color = clusterBackgroundColor,
        val nodeBackgroundColor : Color = Color.LIGHT_GRAY,
        val nodeStrokeColor : Color = nodeBackgroundColor
    )

    override fun doRender( story : Story ) {

        fun Color.toDot() : String {
            fun format( i : Int ) : String = String.format( "%02x", i )
            return "\"#" + format( this.red ) + format( this.green ) + format( this.blue ) + "\""
        }

        block {
            + "dot = `"  // Importing the .dot file as JavaScript requires this (and the closing one).
            + "# ${setup.textSetup.identifier()}"
            + "digraph G {"
            block {
                + "compound=true"  // Enables edges from/to subgraphs.
                + "overlap=scale"
                + "fontsize=${setup.fontSize}"
                + "bgcolor=${setup.backgroundColor.toDot()}"
                + "labeljust=left"
                + "style=rounded"
                + "node["
                block {
                    + "shape=Mrecord,"
                    + "style=\"filled,solid\","  // 'rounded' disables fill.
                    + "color=${setup.nodeStrokeColor.toDot()},"
                    + "fillcolor=${setup.nodeBackgroundColor.toDot()}"
                }
                + "]"
                + "edge[arrowhead=vee]"

                for( act in story.acts ) {
                    + "subgraph cluster_${act.key.index} {"
                    block {
                        + "label=\"${act.place.prettyName}\""
                        + "style=filled"  // 'rounded' disables fill.
                        + "color=${setup.clusterStrokeColor.toDot()}"
                        + "fillcolor=${setup.clusterBackgroundColor.toDot()}"
                        for( stopover in act.stopovers.values ) {
                            + "${stopover.key.identifier} ["
                            block {
                                + "label = <"
                                block {
                                    + "<table border='0' cellborder='0' cellspacing='1' style='rounded'>"
                                    block {
                                        + "<tr><td align='center'><b>${stopover.key.identifier}</b></td></tr>"
                                        + "<tr><td align='left'>${stopover.text}</td></tr>"
                                    }
                                    + "</table> >"

                                }
                            }
                            + "]"
                        }
                    }
                    + "}"
                }
                + "start -> ${story.acts.first().stopovers.values.first().key.identifier}"

                for( act in story.acts ) {
                    for( stopover in act.stopovers.values ) {
                        when( stopover ) {

                            is Story.Act.Strophe -> {
                                for( choiceEntry in stopover.choices ) {
                                    val target = choiceEntry.value.value
                                    val choice = choiceEntry.key
                                    + "${stopover.key.identifier} -> ${target.key.identifier} [ headlabel=\"$choice\" ]"
                                }
                            }

                            is Story.Act.Trip -> {
                                for( destination in stopover.destinations ) {
                                    val target = story.firstStopoverFor( destination )
                                    + "${stopover.key.identifier} -> ${target.key.identifier} [ arrowhead=\"empty\" ]"
                                }
                            }

                            is Story.Act.Jump -> {
                                val target = stopover.destination.value
                                + "${stopover.key.identifier} -> ${target.key.identifier}"

                            }
                        }
                    }
                }
            }
            + "}"
            + "`"  // Closing multiline string for JavaScript import.

        }
    }
}