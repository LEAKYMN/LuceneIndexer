
package driveindex.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.logging.Level;
import driveindex.ui.fx.DriveIndexFx;
import driveindex.ui.fx.cMainLayoutController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip M. Trenwith
 */
public class cInjector extends AbstractModule
{
  private static final Logger g_oLog = LoggerFactory.getLogger(cInjector.class);
  private final DriveIndexFx m_oMainUI;
  private final cMainLayoutController m_oMainUIController;
  private static Injector g_oInjector;
  private static final Object m_oLock = new Object();
  private static boolean m_bInit = false;
  
  public cInjector(DriveIndexFx oUI, cMainLayoutController oUIController)
  {
    m_bInit = false;
    m_oMainUI = oUI;
    m_oMainUIController = oUIController;
    g_oInjector = Guice.createInjector(this);
    m_bInit = true;
    synchronized (m_oLock)
    {
      m_oLock.notifyAll();
    }
  }

  @Override
  protected void configure()
  {
    try
    {
      bind(DriveIndexFx.class).toInstance(m_oMainUI);
      bind(cMainLayoutController.class).toInstance(m_oMainUIController);
    } 
    catch (Exception ex)
    {
      g_oLog.error("Exception configuring injector.", ex);
    }
  }
 
  public static Injector getInjector()
  {
    if (!m_bInit)
    {
      synchronized (m_oLock)
      {
        try
        {
          m_oLock.wait(5000);
        }
        catch (InterruptedException ex)
        {
          java.util.logging.Logger.getLogger(cInjector.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return g_oInjector;
  }
}
