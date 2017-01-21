package spf;

import java.util.Enumeration;
import javax.media.j3d.*;

/**
 * Implementa un comportamiento para obtener
 * los frames por segundo.
 * @author Sergio Paque Martin
 */
public class SpfFpsBehavior extends Behavior {
    protected WakeupCondition m_WakeupCond = null;
    protected long m_StartTime = 0;
    protected long m_Fps = 0;
    
    private final int REPORT_INTERVAL = 100;
    
	/**
	 * Contructor de SpfFpsBehavior.
	 */
    public SpfFpsBehavior () {
        m_WakeupCond = new WakeupOnElapsedFrames(REPORT_INTERVAL);
    }
    
	/**
	 * @return Devuelve los frames por segundo.
	 */
    public long getFps() {
        return m_Fps;
    }

	/**
	 * Inicializa el comportamiento.
	 */
    public void initialize() {
        wakeupOn(m_WakeupCond);
    }

	/**
	 * Procesa los eventos recibidos.
	 * @param criteria Eventos recibidos.
	 */
    public void processStimulus(Enumeration criteria) {
        while (criteria.hasMoreElements()){
            WakeupCriterion wakeUp = (WakeupCriterion)criteria.nextElement();
            if (wakeUp instanceof WakeupOnElapsedFrames) {
                if (m_StartTime > 0){
                    final long interval = System.currentTimeMillis() - m_StartTime;
                    m_Fps = REPORT_INTERVAL * 1000 / interval;
                    //System.out.println("FPS: "+ m_Fps);
                }
                m_StartTime = System.currentTimeMillis();
            }
        }
        wakeupOn(m_WakeupCond);
    }

}
