package io.github.uborkaszoftver.scrollbardemo.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import io.github.uborkaszoftver.scrollbardemo.ScrollbarDemo;

public class DesktopLauncher {
	public static void main (String[] arg) {
		System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new ScrollbarDemo(), config);
	}
}
