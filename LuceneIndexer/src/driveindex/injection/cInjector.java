/*
 * Copyright (C) 2018 Philip M. Trenwith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
