package spf;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static spf.SpfConstants.*;

/**
 * Implementa un Listener de pulsaciones de teclado.
 * @author Sergio Paque Martin
 */
public class SpfKeyListener implements KeyListener, Runnable {
	private SpfCamera m_Camera;
	private boolean leftPressed = false;
	private boolean rightPressed = false;
	private boolean upPressed = false;
	private boolean downPressed = false;
	private boolean wPressed = false;
	private boolean sPressed = false;
	
	/**
	 * Contructor de SpfKeyListener.
	 * @param camera Camara asociada al listener.
	 */
	public SpfKeyListener (SpfCamera camera) {
		m_Camera = camera;
	}

	public void keyTyped (KeyEvent e) {	}

	/**
	 * Procesa la pulsacion de una tecla.
	 * @param e Evento de teclado.
	 */
	public void keyPressed (KeyEvent e) {
		int code = e.getKeyCode();
		if (code == KeyEvent.VK_Q ||
			code == KeyEvent.VK_ESCAPE)
			System.exit(0);
		
		if (code == KeyEvent.VK_LEFT)
			leftPressed = true;
		else if (code == KeyEvent.VK_RIGHT)
			rightPressed = true;
		else if (code == KeyEvent.VK_UP)
			upPressed = true;
		else if (code == KeyEvent.VK_DOWN)
			downPressed = true;
		else if (code == KeyEvent.VK_W)
			wPressed = true;
		else if (code == KeyEvent.VK_S)
			sPressed = true;
	}

	/**
	 * Procesa la liberacion de una tecla.
	 * @param e Evento de teclado.
	 */
	public void keyReleased (KeyEvent e) {
		int code = e.getKeyCode();
		
		if (code == KeyEvent.VK_LEFT)
			leftPressed = false;
		else if (code == KeyEvent.VK_RIGHT)
			rightPressed = false;
		else if (code == KeyEvent.VK_UP)
			upPressed = false;
		else if (code == KeyEvent.VK_DOWN)
			downPressed = false;
		else if (code == KeyEvent.VK_W)
			wPressed = false;
		else if (code == KeyEvent.VK_S)
			sPressed = false;
	}

	/**
	 * Bucle para la simulacion del movimiento
	 * de la camara.
	 */
	public void run() {
		while (true) {
			// Actualizar estado de teclas.
			m_Camera.moveRadius(wPressed, sPressed);
			m_Camera.moveAzimuth(rightPressed, leftPressed);
			m_Camera.moveElevation(upPressed, downPressed);
			// Actualizar estado de la camara.
			m_Camera.updateState();
			try {
				// 1/60th of a second
				Thread.sleep(TimeStep);
			} catch (InterruptedException e) {}
			Thread.yield();
		}
	}
}
