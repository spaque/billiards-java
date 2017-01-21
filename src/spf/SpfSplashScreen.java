/*
 * SpfSplashScreen.java
 *
 * Created on 18 de mayo de 2008, 21:35
 */

package spf;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/**
 * Implementa un splash screen que se muestra
 * mientras se carga la aplicacion.
 * @author  Sergio Paque Martin
 */
public class SpfSplashScreen extends JFrame {
	
	/**
	 * Clase para detectar cuando se termina
	 * de cargar la aplicacion.
	 */
	class WindowListener extends WindowAdapter {
		public void windowOpened(WindowEvent we) {
			setVisible(false);
			dispose();
		}
	}
	
	/** Constructor de SpfSplashScreen */
	public SpfSplashScreen(JFrame owner) {
		initComponents();
		setLocationRelativeTo(null);
		setIconImage(new ImageIcon(SpfApplication.getResourcesPath() + "8ball.png").getImage());
		owner.addWindowListener(new WindowListener());
	}
	
	/* This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        imageLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Cargando...");
        setUndecorated(true);

        imageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spf/resources/loading.gif"))); // NOI18N
        getContentPane().add(imageLabel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    // End of variables declaration//GEN-END:variables
	
}
