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
package LuceneIndexer.drives.folderwatcher;

import LuceneIndexer.lucene.cLuceneIndexReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class watches changes to a specified directory. When new files or
 * changes to existing files are detected, the file is scheduled to be added to
 * a queue for further processing.
 *
 * @author Philip M. Trenwith
 */
public class cFolderWatcher 
{

  /**
   * Is the folder watcher running
   */
  private boolean m_bFolderWatcherRunning = false;
  /**
   * Keep the folder watcher alive
   */
  private boolean m_bKeepAlive = true;

  /**
   * The thread that watches the directory
   */
  private Thread m_tWatcher = null;

  /**
   * The queue containing new or modified files
   */
  private Queue<String> m_oProcessQueue = new LinkedList<>();

  /**
   * The queue containing files that has been deleted from the directory.
   */
  private Queue<String> m_oDeleteQueue = new LinkedList<>();

  /**
   * The schedule for new or modified files
   */
  private HashMap<String, cFileTask> m_oProcessSchedule = new HashMap();

  /**
   * The directory being watched
   */
  private String m_sDirectory;

  /**
   * The Path object of the folder being monitored
   */
  private Path m_oPath;

  /**
   * the time in milliseconds to wait before adding a new or modified file to
   * the process queue.
   */
  private int m_iDelay_ms = 4000;

  /**
   * If deleted files should be added to the delete queue.
   */
  private boolean m_bWatchForDeleteEvents = true;
  private ArrayList<String> m_lsDirectories = new ArrayList<String>();
  private WatchService m_oFileSystemWatchService = null;
  private Thread m_tShutdownHook = null;
  private boolean m_bWatchSubdirectories = true;
  private boolean m_bIgnoreFolderChanges = true;
  private boolean m_bDynamicallyRegisterNewFolders = true;
  private String sFolderWatcherName = "";

  /**
   * @param oPath The path to the directory to watch
   * @param iDelay The time in milliseconds to wait before adding a file to the
   * process queue.
   * @param bWatchDeletedEvents Should delete events be handled and added to the
   * delete queue.
   */
  public cFolderWatcher(Path oPath, int iDelay, boolean bWatchDeletedEvents)
  {
    this(oPath, iDelay, bWatchDeletedEvents, true, true, true);
  }
  
  /**
   * @param oPath The path to the directory to watch
   * @param iDelay The time in milliseconds to wait before adding a file to the
   * process queue.
   * @param bWatchDeletedEvents Should delete events be handled and added to the
   * delete queue.
   * @param bWatchSubdirectories true to watch changes to sub directories.
   * @param bIgnoreFolderChanges true to ignore event changes to directories
   * @param bDynamicallyRegisterNewFolders If a folder is created should it be registered for events
   */
  public cFolderWatcher(Path oPath, int iDelay, boolean bWatchDeletedEvents, boolean bWatchSubdirectories,
          boolean bIgnoreFolderChanges, boolean bDynamicallyRegisterNewFolders)
  {
    m_oPath = oPath;
    m_iDelay_ms = iDelay;
    m_bWatchForDeleteEvents = bWatchDeletedEvents;
    m_bWatchSubdirectories = bWatchSubdirectories;
    m_bIgnoreFolderChanges = bIgnoreFolderChanges;
    m_bDynamicallyRegisterNewFolders = bDynamicallyRegisterNewFolders;
    sFolderWatcherName = "FS-" + m_oPath;
    
    if (m_tShutdownHook == null)
    {
      m_tShutdownHook = new Thread(() ->
      {
        m_tShutdownHook = null;
        terminate();
      });
      Runtime.getRuntime().addShutdownHook(m_tShutdownHook);
    }
  }

  /**
   * Start the thread to watch the directory
   */
  public void start()
  {
    System.out.println("Starting FolderWatcher-" + sFolderWatcherName);
    try
    {
      if (m_tWatcher == null)
      {
        m_tWatcher = new Thread(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              try
              {
                createWatchService();
                WatchKey key = null;
                m_bFolderWatcherRunning = true;
                
                while (m_bKeepAlive)
                {
                  try
                  {
                    key = m_oFileSystemWatchService.take();
                  }
                  catch (InterruptedException | ClosedWatchServiceException ex)
                  {
                    System.out.println("key is null, FileSystem Watch Service is closed");
                    break;
                  }

                  Kind<?> kind = null;
                  if (key != null)
                  {
                    try
                    {
                      boolean bRecreateFolderWatcher = false;
                      for (WatchEvent<?> watchEvent : key.pollEvents())
                      {
                        Object context = null;
                        try
                        {
                          context = watchEvent.context();

                          Path dir = (Path)key.watchable();
                          Path newPath = dir.resolve(((WatchEvent<Path>) watchEvent).context());
                          final String sFile = newPath.toString();

                          File oFile = new File(sFile);

                          if (m_bIgnoreFolderChanges && oFile.isDirectory())
                          {
                            if (m_bDynamicallyRegisterNewFolders)
                            {
                              Path registerPath = oFile.toPath();
                              registerRecursive(registerPath);
                            }
                            continue;
                          }

                          if (oFile.getName().startsWith("~"))
                          {
                            // ignore this file, it is a tmp file
                            continue;
                          }

                          // Get the type of the event
                          kind = watchEvent.kind();
                          if (kind == OVERFLOW)
                          {
                            continue; // loop
                          }
                          else if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY)
                          {
                            // A new file was created or modified
                            schedule(sFile);
                          }
                          else if (kind == ENTRY_DELETE)
                          {
                            // A file was deleted
                            if (m_bWatchForDeleteEvents)
                            {
                              if (m_bIgnoreFolderChanges && isDirectory(sFile))
                              {
                                bRecreateFolderWatcher = true;
                                continue;
                              }

                              if (!addToDeleteQueue(sFile))
                              {
                                System.out.println("Error adding the file to the delete queue");
                              }
                            }
                          }
                        }
                        catch (Exception ex)
                        {
                          Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                      }
                      if (bRecreateFolderWatcher)
                      {
                        recreateWatchService();
                      }
                    }
                    catch (Exception ex)
                    {
                      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally
                    {
                      // put the key back into the ready state
                      key.reset();
                    }
                  }
                }
                m_bFolderWatcherRunning = false;
              }
              catch (Exception ex)
              {
                Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
              }
            }
            catch (Exception ex)
            {
              Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        }, "FolderWatcher-" + m_sDirectory);
        // When the JVM exists this thread should not hold it up
        m_tWatcher.setDaemon(true);
        m_tWatcher.start();
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private void createWatchService() throws IOException
  {
    File directory = new File(m_oPath.toUri().getPath());
    m_sDirectory = directory.getAbsolutePath();
    FileSystem fs = null;
    try
    {
      if (directory.exists())
      {
        Boolean isFolder = (Boolean) Files.getAttribute(m_oPath,
                "basic:isDirectory", NOFOLLOW_LINKS);

        if (!isFolder)
        {
          System.out.println("The path: '" + m_oPath + "' is not a folder");
          terminate();
        }
      }
      else
      {
        terminate();
      }
      fs = m_oPath.getFileSystem();
    }
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    m_oFileSystemWatchService = fs.newWatchService();

    try
    {
      if (m_bWatchSubdirectories)
      {
        registerRecursive(m_oPath);
      }
      else
      {
        m_oPath.register(m_oFileSystemWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        m_lsDirectories.add(m_oPath.toString());
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private boolean isDirectory(String sFile)
  {
    boolean bResult = false;
    for (String sPath: m_lsDirectories)
    {
      if (sPath.equalsIgnoreCase(sFile))
      {
        bResult = true;
        break;
      }
    }
    return bResult;
  }
  
  private void recreateWatchService()
  {
    try 
    {
      m_lsDirectories.clear();
      if (m_oFileSystemWatchService != null)
      {
        m_oFileSystemWatchService.close();
      }
      createWatchService();
    }
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private void registerRecursive(final Path root) throws IOException
  {
    try 
    {
      if (m_bKeepAlive)
      {
        // register all subfolders
        Files.walkFileTree(root, new SimpleFileVisitor<Path>()
        {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
          {
            if (!m_bKeepAlive)
            { 
              return FileVisitResult.TERMINATE;
            }
            dir.register(m_oFileSystemWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            m_lsDirectories.add(dir.toString());
            return FileVisitResult.CONTINUE;
          }
          @Override
          public FileVisitResult visitFileFailed(Path file, IOException io)
          {
            System.err.println("Ignoring directory: " + file + " (Access Denied)");
            return FileVisitResult.SKIP_SUBTREE;
          }
        });
      }
    } 
    catch (Exception ex)
    {
      Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   *
   * @return if the folder watcher thread is running
   */
  public boolean isRunning()
  {
    return m_bFolderWatcherRunning;
  }

  /**
   * Add a file to the schedule to be added to the queue to be indexed after the
   * configured delayed period.
   *
   * @param sFile the file to index.
   */
  public void schedule(String sFile)
  {
    if (m_oProcessSchedule.containsKey(sFile))
    {
      // if the file is already in the queue reset the timer, if the file is being written to, especially a 
      // large file, there may be multiple modifications to the file, we dont want the file to be added to 
      // the queue to be handled until it is finished.
      //System.out.println("Reset schedule for file: '" + sFile + "' ");
      m_oProcessSchedule.get(sFile).reset();
    }
    else
    {
      cFileTask cFileTask = new cFileTask(sFile);
      m_oProcessSchedule.put(sFile, cFileTask);
      //System.out.println("File scheduled: '" + sFile + "' ");
    }
  }

  /**
   * Add the file that was modified or added to the directory to the queue for
   * processing
   *
   * @param sFile the absolute path of the file
   * @return true if it was successfully added to the queue
   */
  public boolean addToProcessQueue(String sFile)
  {
    synchronized (m_oProcessQueue)
    {
      if (!m_oProcessQueue.contains(sFile))
      {
        //System.out.println("Queueing file: '" + sFile + "' ");
        boolean offerAccepted = m_oProcessQueue.offer(sFile);
        m_oProcessSchedule.remove(sFile);
        m_oProcessQueue.notifyAll();
        if (!offerAccepted)
        {
          // This should never happen but if it does the index will be incomplete.
          String sMessage = "Error adding document '" + sFile + "' to process queue.";
          System.out.println(sMessage);

          return false;
        }
      }
      return true;
    }
  }

  /**
   * Add the file that was modified or added to the directory to the queue for
   * processing
   *
   * @param sFile the absolute path of the file
   * @return true if it was successfully added to the queue
   */
  public boolean addToDeleteQueue(String sFile)
  {
    // before adding the file to the delete queue, check if it is in the process queue
    synchronized (m_oProcessSchedule)
    {
      if (m_oProcessSchedule.containsKey(sFile))
      {
        //System.out.println("Removing file from process queue: '" + sFile + "' ");
        cFileTask obj = m_oProcessSchedule.get(sFile);
        obj.cancel();
        m_oProcessSchedule.remove(sFile);
      }
    }

    synchronized (m_oDeleteQueue)
    {
      if (!m_oDeleteQueue.contains(sFile))
      {
        //System.out.println("Queueing file for deletion: '" + sFile + "' ");
        boolean offerAccepted = m_oDeleteQueue.offer(sFile);
        m_oDeleteQueue.notifyAll();
        if (!offerAccepted)
        {
          // This should never happen but if it does the index will contain documents that is 
          // no longer available.
          String sMessage = "Error adding document '" + sFile + "' to delete queue.";
          System.out.println(sMessage);

          return false;
        }
      }
      return true;
    }
  }

  /**
   * Get the absolute path of the directory this folder watcher is watching
   *
   * @return the path to the directory
   */
  public String getDirectoryBeingMonitored()
  {
    return m_sDirectory;
  }

  /**
   * Return the head of the process queue. This is a blocking method. If the
   * queue is empty the calling thread will wait until it is notified that an
   * element has been added to the queue
   *
   * @return The absolute path of the file from the process queue
   */
  public String getFileFromProcessQueue()
  {
    String sReturn = "";
    synchronized (m_oProcessQueue)
    {
      String sPath = m_oProcessQueue.poll();
      if (sPath == null)
      {
        try
        {
          m_oProcessQueue.wait();
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        sPath = m_oProcessQueue.poll();
      }
      if (sPath != null)
      {
        sReturn = sPath.toString();
      }

      return sReturn;
    }
  }

  /**
   * Return the head of the delete queue. This is a blocking method. If the
   * queue is empty the calling thread will wait until it is notified that an
   * element has been added to the queue
   *
   * @return The absolute path of the file from the delete queue
   */
  public String getFileFromDeleteQueue()
  {
    String sReturn = "";
    if (m_bWatchForDeleteEvents)
    {
      synchronized (m_oDeleteQueue)
      {
        String sPath = m_oDeleteQueue.poll();
        if (sPath == null)
        {
          try
          {
            m_oDeleteQueue.wait();
          }
          catch (InterruptedException ex)
          {
            Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
          }

          sPath = m_oDeleteQueue.poll();
        }
        if (sPath != null)
        {
          sReturn = sPath.toString();
        }

        return sReturn;
      }
    }
    else
    {
      return "";
    }
  }

  /**
   * Terminate the folder watcher
   */
  public void terminate()
  {
    m_bKeepAlive = false;
    if (isRunning())
    {
      System.out.println("Terminating " + sFolderWatcherName);
      if (m_tWatcher != null) 
      {
        m_tWatcher.interrupt();
      }
      try
      {
        if (m_oFileSystemWatchService != null)
        {
          m_oFileSystemWatchService.close();
        }
      }
      catch (IOException ex)
      {
        Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
      }
      if (m_tShutdownHook != null)
      {
        try
        {
          Runtime.getRuntime().removeShutdownHook(m_tShutdownHook);
        }
        catch (Throwable ex)
        {
          Logger.getLogger(cLuceneIndexReader.class.getName()).log(Level.SEVERE, null, ex);
        }
      }


      // notify all threads currently blocking on these queues to stop blocking
      synchronized (m_oDeleteQueue)
      {
        m_oDeleteQueue.notifyAll();
      }
      synchronized (m_oProcessQueue)
      {
        m_oProcessQueue.notifyAll();
      }
    }
  }

  public String getName()
  {
    return sFolderWatcherName;
  }

  public int getProcessQueueFileCount()
  {
    return m_oProcessQueue.size();
  }
  
  public int getDeleteQueueFileCount()
  {
    return m_oDeleteQueue.size();
  }

  /**
   * This is an encapsulated object to hold a file for scheduling
   */
  private class cFileTask
  {

    private String m_sPath = "";
    private Timer m_oTimer = null;

    public cFileTask(String sPath)
    {
      m_sPath = sPath;
      set();
    }

    /**
     * Cancel the scheduled event
     */
    public void cancel()
    {
      if (m_oTimer != null)
      {
        m_oTimer.cancel();
        m_oTimer = null;
      }
    }

    /**
     * Reset the scheduler
     */
    public void reset()
    {
      if (m_oTimer != null)
      {
        m_oTimer.cancel();
        set();
      }
    }

    /**
     * Set the schedule
     */
    public void set()
    {
      new Thread(() ->
      {
        m_oTimer = new Timer(); // Instantiate Timer Object
        m_oTimer.schedule(new TimerTask()
        {
          @Override
          public void run()
          {
            boolean bFlag = addToProcessQueue(m_sPath);
          }
        }, m_iDelay_ms);
      }, "cFileTask_Timer_Thread").start();
    }

  }
}
