package io.github.uborkaszoftver.menu1.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import io.github.uborkaszoftver.menu1.Menu1;

public class DesktopLauncher {
	public static void main (String[] arg) {
		System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new Menu1(), config);
	}
}
