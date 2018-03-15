

package driveindex.scanner;

import driveindex.injection.cInjector;
import driveindex.lucene.cLuceneIndexWriter;
import driveindex.ui.fx.cMainLayoutController;
import driveindex.ui.fx.cProgressPanelFx;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Philip M. Trenwith
 */
public class cDriveScanner
{
  private final Object m_oLOCK = new Object();
  
  private cLuceneIndexWriter oWriter = cLuceneIndexWriter.instance();
  private ExecutorService m_oExecutorService;
  private boolean bDone = true;
  private boolean bCancel = false;
  private final int m_iTOTAL_THREADS = 10;
  private AtomicInteger oAlive = new AtomicInteger(0);
  
  public cDriveScanner()
  {
    ThreadFactory tFactory = (Runnable runnable) ->
    {
      Thread thread = new Thread(runnable);
      thread.setName("Scan-Thread");
      thread.setDaemon(true);
      thread.setPriority(3);
      return thread;
    };
    m_oExecutorService = Executors.newFixedThreadPool(m_iTOTAL_THREADS, tFactory);
  }
  
  public boolean indexFile(File oFile)
  {
    return oWriter.indexFile(oFile);
  }
  
  public boolean deleteFile(File oFile)
  {
    return oWriter.deleteFile(oFile);
  }  
  
  public void scanDrive(File oRootFile)
  {
    bCancel = false;
    bDone = false;
    Thread thread = new Thread(() -> 
    {
      
      cProgressPanelFx oStatusPanel = cProgressPanelFx.get(oRootFile.getAbsolutePath());
      try
      {
        System.out.println("Scanning drive: '" + oRootFile.getAbsolutePath());
        cDriveMediator.instance().setStatus("Scanning... (See Drive tab for detail)");
        oStatusPanel.resetProgress();
        oStatusPanel.setStatus("Scanning: " + oRootFile.getAbsolutePath());
        scanDirectory(oRootFile, oStatusPanel);
      }
      finally
      {
        while(oAlive.get() > 0)
        {
          synchronized (m_oLOCK)
          {
            try
            {
              m_oLOCK.wait();
            }
            catch (Exception ex)
            { }
          }
        }

        if (oAlive.get() <=0)
        {
          String sStatus;
          if (bCancel)
          {
            sStatus = "cancelled";
            oStatusPanel.cancel();
          }
          else
          {
            sStatus = "complete";
            oStatusPanel.complete();
          }

          System.out.println("Scanning drive: '" + oRootFile.getAbsolutePath() + "' " + sStatus + " (" + oAlive.get() + ")");
        
          bDone = true;
          cDriveMediator.instance().closeIndexWriter();
          cInjector.getInjector().getInstance(cMainLayoutController.class).scanComplete();
          oStatusPanel.setStatus("Scan Complete");
        }
      }
    });
    thread.setDaemon(true);
    thread.setName("DriveScanner-Thread");
    thread.start();
  }
  
  private void scanDirectory(File oParentFile, cProgressPanelFx oStatusPanel) 
  {
    String[] list = oParentFile.list();
    if (list != null)
    {
      for (String sFile: list)
      {
        oAlive.getAndIncrement();
        m_oExecutorService.submit(() -> 
        {
          if (bCancel)
          {
            m_oExecutorService.shutdownNow();
          }

          File oChildFile = new File(oParentFile.getAbsolutePath() + File.separator + sFile);
          String sFilePath = oChildFile.getAbsolutePath().toUpperCase();
          if (!(sFilePath.contains("RECYCLE.BIN")))
          {
            if (oChildFile.isDirectory())
            {
              oStatusPanel.setStatus("Scanning: " + oChildFile.getAbsolutePath());
              scanDirectory(oChildFile, oStatusPanel);
            }
            else
            {
              boolean bSuccess = indexFile(oChildFile);
              oStatusPanel.appendIndexSize(oChildFile.length());
            }
          }
          oAlive.decrementAndGet();
          synchronized (m_oLOCK)
          {
            m_oLOCK.notify();
          }
        });
      }
    }
  }

//  public void startFolderWatcher()
//  {
//    bRunning = true;
//    if (oDriveRoot.toPath() != null)
//    {
//      oFolderWatcher = new cFolderWatcher(oDriveRoot.toPath(), 500, true, true, false, true);
//      oFolderWatcher.start();
//      lblFolderWatcher.setText("FolderWatcher: Running");
//
//      Thread oAddQueueThread = new Thread(new Runnable() 
//      {
//        @Override
//        public void run()
//        {
//          while (bRunning)
//          {
//            if (oFolderWatcher != null)
//            {
//              String sFile = oFolderWatcher.getFileFromProcessQueue();
//              boolean bSuccess = cDriveMediator.instance().indexFile(new File(sFile));
//              setStatus("Status: Indexing: " + sFile);
//            }
//          }
//        }
//      });
//      oAddQueueThread.setDaemon(true);
//      oAddQueueThread.setName("FolderWatcher-AddQueueThread");
//      oAddQueueThread.start();
//
//      Thread oDeleteQueueThread = new Thread(()-> 
//      {
//        while (bRunning)
//        {
//          if (oFolderWatcher != null)
//          {
//            String sFile = oFolderWatcher.getFileFromDeleteQueue();
//            boolean bSuccess = cDriveMediator.instance().deleteFile(new File(sFile));
//            setStatus("Status: Deleting from index: " + sFile);
//          }
//        }
//      });
//      oDeleteQueueThread.setDaemon(true);
//      oDeleteQueueThread.setName("FolderWatcher-DeleteQueueThread");
//      oDeleteQueueThread.start();
//    }
//  }
  
  public void stopScan()
  {
    System.out.println("Stop Scan");
    bCancel = true;
  }

  public boolean isDone()
  {
    return bDone;
  }
}
