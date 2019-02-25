package com.github.uborkaszoftverkft.kepvilag.menu2.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.github.uborkaszoftverkft.kepvilag.menu2.Menu2;
import com.github.uborkaszoftverkft.kepvilag.menu2.Menu2K;

public class DesktopLauncher {
	public static void main (String[] arg) {
		System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new Menu2K(), config);
	}
}
