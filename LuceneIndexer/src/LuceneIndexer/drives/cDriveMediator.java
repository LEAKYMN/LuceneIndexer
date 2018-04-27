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
package LuceneIndexer.drives;

import LuceneIndexer.cConfig;
import LuceneIndexer.linux.cLinux;
import LuceneIndexer.ui.fx.cProgressPanelFx;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileSystemView;


/**
 *
 * @author Philip M. Trenwith
 */
public class cDriveMediator extends Observable
{
  private AtomicInteger m_iOpenWriters = new AtomicInteger(0);
  private TreeMap<String, cDrive> m_oDrives = new TreeMap();
  private FileSystemView oFileSystemView = FileSystemView.getFileSystemView();
  private File[] lsRoots = null;
  private static volatile cDriveMediator m_oInstance = null;
 
  public static cDriveMediator instance()
  {
    cDriveMediator oInstance = cDriveMediator.m_oInstance;
    if (oInstance == null)
    {
      synchronized (cDriveMediator.class)
      {
        oInstance = cDriveMediator.m_oInstance;
        if (oInstance == null)
        {
          cDriveMediator.m_oInstance = oInstance = new cDriveMediator();
        }
      }
    }
    return oInstance;
  }
  
  private cDriveMediator()
  {
  }
  
  public void loadDrives()
  {
    File[] paths = getFileSystemRoots();
    for (File oRootFile: paths)
    {
      if (oFileSystemView.getSystemTypeDescription(oRootFile) == null || 
              cConfig.instance().getScanDriveType(oFileSystemView.getSystemTypeDescription(oRootFile)))
      {
        cDrive oDriveScanner = m_oDrives.get(oRootFile.getAbsolutePath());
        if (oDriveScanner == null)
        {
          oDriveScanner = new cDrive(oRootFile);
          m_oDrives.put(oRootFile.getAbsolutePath(), oDriveScanner);
        }
      }
      else
      {
        System.out.println("Ignoring drive: " + oRootFile.getAbsolutePath() + 
                " (Type " + oFileSystemView.getSystemTypeDescription(oRootFile) + " not configured to scan)");
      }
    }
  }
  
  
  public void scanComputer()
  {
    try
    {
      System.out.println("Scanning computer...");
      
      Iterator<cDrive> oIterator = m_oDrives.values().iterator();
      while (oIterator.hasNext())
      {
        cDrive oDriveScanner = oIterator.next();
        m_iOpenWriters.incrementAndGet();
        oDriveScanner.scanDrive();
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void scanDrive(File oRoot)
  {
    cDrive oDriveScanner = m_oDrives.get(oRoot.getAbsolutePath());
    if (oDriveScanner == null)
    {
      oDriveScanner = new cDrive(oRoot);
      m_oDrives.put(oRoot.getAbsolutePath(), oDriveScanner);
    }
    m_iOpenWriters.incrementAndGet();
    oDriveScanner.scanDrive();
  }
  
  public File[] getFileSystemRoots()
  {
    if (lsRoots == null)
    {
      String sOperatingSystem = System.getProperty("os.name");
      if (sOperatingSystem.equalsIgnoreCase("Windows"))
      {
        lsRoots = File.listRoots();
      }
      else if (sOperatingSystem.equalsIgnoreCase("Linux"))
      {
        lsRoots = cLinux.mountAllDrives(true);
      }
      else
      {
        lsRoots = File.listRoots();
      }
    }
    return lsRoots;
  }
  
  public cProgressPanelFx[] listDrives()
  {
    cProgressPanelFx[] oReturn = new cProgressPanelFx[m_oDrives.size()];
    Iterator<cDrive> oIterator = m_oDrives.values().iterator();
    int iCount = 0;
    while (oIterator.hasNext())
    {
      cDrive oDriveScanner = oIterator.next();
      cProgressPanelFx oPanel = cProgressPanelFx.get(oDriveScanner.getRoot());
      oReturn[iCount++] = oPanel;
    }
    
    return oReturn;
  }
  
  public void startScan()
  {
    scanComputer();
  }
  
  public void stopScan()
  {
    Collection<cDrive> lsDrives = m_oDrives.values();
    Iterator<cDrive> oIterator = lsDrives.iterator();
    while (oIterator.hasNext())
    {
      cDrive oDrive = oIterator.next();
      oDrive.stopScan();
    }
  }
  
  public void setStatus(String sStatus)
  {
    setChanged();
    notifyObservers(sStatus);
  }
  
  public cDrive getDrive(String sRoot)
  {
    return m_oDrives.get(sRoot);
  }
  
  public TreeMap<String, cDrive> getDrives()
  {
    return m_oDrives;
  }
}
