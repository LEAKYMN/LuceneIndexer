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
import LuceneIndexer.cryptopackage.cCryptographer;
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.lucene.cIndex;
import LuceneIndexer.ui.fx.cMainLayoutController;
import LuceneIndexer.ui.fx.cProgressPanelFx;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Philip M. Trenwith
 */
public class cDrive
{
  private cIndex m_oIndex;
  private final Object m_oLOCK = new Object();
  private File m_oRootFile;
  private ExecutorService m_oExecutorService;
  private Thread m_oProgressThread = null;
  private final Object m_oProgressLock = new Object();
  private boolean m_bDone = true;
  private boolean m_bCancel = false;
  private int m_iTOTAL_THREADS = 50;
  private AtomicInteger m_oAlive = new AtomicInteger(0);
  private static SimpleDateFormat g_DF = new SimpleDateFormat("HH:mm:ss");
  private long m_lScanStartTime = 0;
  private long m_lScanStopTime = 0;
  private cProgressPanelFx m_oStatusPanel;
  private String m_sLatestIndexedFile = "";
  
  public cDrive(File oRootFile)
  {
    g_DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    m_oRootFile = oRootFile;
    m_oIndex = new cIndex(m_oRootFile,this);
    m_oProgressThread = new Thread(() -> 
    {
      while (!m_bCancel)
      {
        if (m_bDone)
        {
          synchronized (m_oProgressLock)
          {
            try
            {
              m_oProgressLock.wait();
            }
            catch (InterruptedException ex)
            { }
          }
        }
        else
        {
          if (m_oStatusPanel != null && new GregorianCalendar().getTimeInMillis() - m_oStatusPanel.getLastStatusUpdateTime() > 1000)
          {
            m_oStatusPanel.setStatus("Scanning: " + m_sLatestIndexedFile);
          }
          try
          {
            Thread.sleep(100);
          }
          catch (InterruptedException ex)
          {
            Logger.getLogger(cDrive.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    });
    m_oProgressThread.setDaemon(true);
    m_oProgressThread.start();
    resetExecutor();
  }
  
  private void resetExecutor()
  {
    m_iTOTAL_THREADS = cConfig.instance().getScanThreads();
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
  
  public String getRoot()
  {
    return m_oRootFile.getPath();
  }
  
  public void scanDrive()
  {
    m_bCancel = false;
    m_bDone = false;
    synchronized (m_oProgressLock)
    {
      m_oProgressLock.notify();
    }
              
    Thread thread = new Thread(() -> 
    {
      m_lScanStartTime = new GregorianCalendar().getTimeInMillis();
      m_oStatusPanel = cProgressPanelFx.get(m_oRootFile.getAbsolutePath());
      try
      {
        System.out.println("Scanning drive: '" + m_oRootFile.getAbsolutePath() + "'");
        cDriveMediator.instance().setStatus("Scanning... (See Drive tab for detail)");
        m_oStatusPanel.resetProgress();
        m_oStatusPanel.setStatus("Scanning: " + m_oRootFile.getAbsolutePath());
        m_oIndex.openWriter();
        scanDirectory(m_oRootFile, m_oStatusPanel);
      }
      finally
      {
        while(m_oAlive.get() > 0)
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

        if (m_oAlive.get() <=0)
        {
          String sStatus;
          if (m_bCancel)
          {
            sStatus = "cancelled";
            m_oStatusPanel.cancel();
          }
          else
          {
            sStatus = "complete";
            m_oStatusPanel.complete();
          }

          System.out.println("Scanning drive: '" + m_oRootFile.getAbsolutePath() + "' " + sStatus + " (" + m_oAlive.get() + ")");
        
          m_bDone = true;
          m_oIndex.close();
          m_lScanStopTime = new GregorianCalendar().getTimeInMillis();
          cInjector.getInjector().getInstance(cMainLayoutController.class).scanComplete();
          m_oStatusPanel.setStatus("Scan Complete. Running Time: " + g_DF.format(new Date(m_lScanStopTime-m_lScanStartTime)));
        }
      }
    });
    thread.setDaemon(true);
    thread.setName("DriveScanner-Thread");
    thread.start();
  }
  
  private void scanDirectory(File oParentFile, cProgressPanelFx oStatusPanel) 
  {
    m_oStatusPanel = oStatusPanel;
    String[] list = oParentFile.list();
    if (list != null)
    {
      for (String sFile: list)
      {
        m_oAlive.getAndIncrement();
        m_oExecutorService.submit(() -> 
        {
          if (m_bCancel)
          {
            try
            {
              m_oExecutorService.awaitTermination(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException ex)
            {
              Logger.getLogger(cDrive.class.getName()).log(Level.SEVERE, null, ex);
            }
            resetExecutor();
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
              try
              {
                m_sLatestIndexedFile = oChildFile.getAbsolutePath();
                String sHash = "";
                if (cConfig.instance().getHashDocuments())
                {
                  sHash = cCryptographer.hash(oChildFile);
                }
                boolean bSuccess = m_oIndex.indexFile(oChildFile, sHash);
                oStatusPanel.appendIndexSize(oChildFile.getPath(), oChildFile.length());
              } 
              catch (Exception ex)
              {
                System.err.println("Error: " + ex.getMessage() + " m_oChildFile: " + oChildFile.getAbsolutePath());
                ex.printStackTrace();
              }
            }
          }
          m_oAlive.decrementAndGet();
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
    if (!m_bDone)
    {
      System.out.println("Stopping Scan : '" + m_oRootFile.getAbsolutePath() + "'");
    }
    m_bCancel = true;
    m_oProgressThread.interrupt();
  }

  public boolean isDone()
  {
    return m_bDone;
  }

  public cIndex getIndex()
  {
    return m_oIndex;
  }
}
