package spf;

import javax.vecmath.Point3d;

/**
 * Clase que contiene constantes utilizadas en el juego.
 * @author Sergio Paque Martin
 */
public final class SpfConstants {
	public static final double BackClipDistance = 100.0;
	public static final double PiOver4 = Math.PI / 4;
	public static final double PiOver2 = Math.PI / 2;
	public static final double Pi = Math.PI;
	public static final float KeystrokeForce = 5.0f;
	public static final float Drag = 8.0f;
	public static final Point3d Origin = new Point3d();
	public static final float G = 9.8f;
	public static final float Friction = 0.01f;
	
	public static final long TimeStep = 17;
	public static final int GameBalls = 15;
	
	private SpfConstants () {}
}
