package com.github.uborkaszoftverkft.kepvilag.menu2

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.TimeUtils
import java.util.*

class Menu2K : ApplicationAdapter(){

  private var stage : Stage? = null
  private var container : Table? = null

  override fun create() {
    Gdx.app.logLevel = Application.LOG_DEBUG
    stage = Stage()
    val skin = Skin(Gdx.files.internal("skin/dark-hdpi/Holo-dark-hdpi.json"))
    val inputMultiplexer = InputMultiplexer()
    inputMultiplexer.addProcessor( GestureDetector( object : GestureDetector.GestureAdapter() {
      override fun fling( velocityX : Float, velocityY : Float, button : Int ) : Boolean {
        Gdx.app.debug( "Menu2K", "#fling( vx=$velocityX, vy=$velocityY, time=${TimeUtils.millis()}" )
        return super.fling( velocityX, velocityY, button )
      }
    } ) )
    inputMultiplexer.addProcessor( stage )
//    Gdx.input.inputProcessor = inputMultiplexer
    Gdx.input.inputProcessor = stage

    // Gdx.graphics.setVSync(false);

    container = Table()
    stage !!.addActor(container)
    container !!.setFillParent(true)
    container !!.setDebug(true, true)

    val texts = newTextualEntries(5, 10, 200)
    val size = Vector2(400f, 500f)
    val choicePane = newSweepChoice(texts, size, skin)
    stage !!.scrollFocus = choicePane

    container !!.add(choicePane).size(size.x, size.y)
    container !!.row().space(10f).padBottom(10f)
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    stage !!.act(Gdx.graphics.deltaTime)
    stage !!.draw()
  }

  override fun resize(width : Int, height : Int) {
    stage !!.viewport.update(width, height, true)

    // Gdx.gl.glViewport(100, 100, width - 200, height - 200);
    // stage.setViewport(800, 600, false, 100, 100, width - 200, height - 200);
  }

  override fun dispose() {
    stage !!.dispose()
  }

  fun needsGL20() : Boolean {
    return false
  }


  private fun newTextualEntries(
      count : Int,
      minLength : Int,
      maxLength : Int
  ) : Array<String> {
    val random = Random(0)
    val builder = StringBuilder(maxLength)
    val entries = arrayOfNulls<String>(count)
    for(i in entries.indices) {
      builder.setLength(0)
      val wordCount = random.nextInt(maxLength)
      for(j in 0 until wordCount - minLength) {
        builder.append("word-").append(i).append('-').append(j).append(' ')
      }
      entries[i] = builder.toString()
    }
    return entries.requireNoNulls()
  }

  private fun newSweepChoice(texts : Array<String>, size : Vector2, skin : Skin) : Actor {
    val actors = arrayOfNulls<Actor>(texts.size)

    for(i in texts.indices) {
      val text = texts[i]
      val textWidget = newTextWidget(text, skin)
      actors[i] = textWidget
    }
    return SweepChoice( size.x, size.y, 10f, *actors.requireNoNulls() )
  }

  private fun newTextWidget(text : String, skin : Skin) : Actor {
    val multilineLabel = Label(text, skin)
    multilineLabel.setWrap(true)
    return multilineLabel
  }


}
