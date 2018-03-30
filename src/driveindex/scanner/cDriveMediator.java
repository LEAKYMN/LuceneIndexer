package driveindex.scanner;

import driveindex.cConfig;
import driveindex.lucene.cLuceneIndexWriter;
import driveindex.ui.fx.cProgressPanelFx;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
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
  private HashMap<String, cDriveScanner> m_oDrives = new HashMap();
  private FileSystemView oFileSystemView = FileSystemView.getFileSystemView();
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
  
  public void scanComputer()
  {
    try
    {
      System.out.println("Scanning computer...");
      File[] paths = File.listRoots();
      for (File oRootFile: paths)
      {
        if (cConfig.instance().getScanDriveType(oFileSystemView.getSystemTypeDescription(oRootFile)))
        {
          cDriveScanner oDriveScanner = m_oDrives.get(oRootFile.getAbsolutePath());
          if (oDriveScanner == null)
          {
            oDriveScanner = new cDriveScanner();
            m_oDrives.put(oRootFile.getAbsolutePath(), oDriveScanner);
          }
          m_iOpenWriters.incrementAndGet();
          oDriveScanner.scanDrive(oRootFile);
        }
        else
        {
          System.out.println("Ignoring drive: " + oRootFile.getAbsolutePath() + 
                  " (Type " + oFileSystemView.getSystemTypeDescription(oRootFile) + " not configured to scan)");
        }
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cConfig.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void setStatus(String sStatus)
  {
    setChanged();
    notifyObservers(sStatus);
  }
  
  public cProgressPanelFx[] listDrives()
  {
    File[] paths = File.listRoots();
    cProgressPanelFx[] oReturn = new cProgressPanelFx[paths.length];
    int iCount = 0;
    for (File oRoot: paths)
    {
      cProgressPanelFx oPanel = cProgressPanelFx.get(oRoot.getAbsolutePath());
      oReturn[iCount++] = oPanel;
    }
    return oReturn;
  }
  
  public void stopScan()
  {
    System.out.println("Stop Scan");
    Collection<cDriveScanner> lsDrives = m_oDrives.values();
    Iterator<cDriveScanner> oIterator = lsDrives.iterator();
    while (oIterator.hasNext())
    {
      cDriveScanner oDrive = oIterator.next();
      oDrive.stopScan();
    }
  }
  
  public void startScan()
  {
    scanComputer();
  }

  public void scanDrive(File oRoot)
  {
    cDriveScanner oDriveScanner = m_oDrives.get(oRoot.getAbsolutePath());
    if (oDriveScanner == null)
    {
      oDriveScanner = new cDriveScanner();
      m_oDrives.put(oRoot.getAbsolutePath(), oDriveScanner);
    }
    m_iOpenWriters.incrementAndGet();
    oDriveScanner.scanDrive(oRoot);
  }
  
  public void closeIndexWriter()
  {
    int iOpen = m_iOpenWriters.decrementAndGet();
    
    if (iOpen == 0)
    {
      System.out.println("Closing index writer.");
      cLuceneIndexWriter.instance().close();
    }
    else
    {
      cLuceneIndexWriter.instance().commit();
    }
  }
}
