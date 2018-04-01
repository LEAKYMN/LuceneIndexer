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
package driveindex.ui.fx;

import driveindex.persistance.cMetadata;
import driveindex.scanner.folderwatcher.cFolderWatcher;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Philip Trenwith
 */
public class cProgressPanelFx extends Application
{
  private static HashMap<String, cProgressPanelFx> oDrives = new HashMap();
  private cProgressPanelController oProgressPanelController;
  private cFolderWatcher oFolderWatcher;
  private boolean bRunning = true;
  private Parent oRoot;
  private Scene oScene;
  private File oDriveRoot;
  private cMetadata oMetadata;
  
  public static cProgressPanelFx get(String sRoot)
  {
    cProgressPanelFx oReturn = oDrives.get(sRoot);
    if (oReturn == null)
    {
      File oFile = new File(sRoot);
      oReturn = new cProgressPanelFx(oFile);
      oDrives.put(oFile.getAbsolutePath(), oReturn);
    }
    return oReturn;
  }
  
  public static cProgressPanelFx[] getAll()
  {
    Collection<cProgressPanelFx> lsDrives = oDrives.values();
    cProgressPanelFx[] oReturn = new cProgressPanelFx[0];
    if (lsDrives != null)
    {
      oReturn = lsDrives.toArray(oReturn);
    }
    return oReturn;
  }
  
  public static void terminateAll()
  {
    Collection<cProgressPanelFx> lsPanels = oDrives.values();
    Iterator<cProgressPanelFx> oIterator = lsPanels.iterator();
    while (oIterator.hasNext())
    {
      cProgressPanelFx oPanel = oIterator.next();
      oPanel.terminate();
    }
  }
  
  public void terminate()
  {
    bRunning = false;
    if (oFolderWatcher != null)
    {
      oFolderWatcher.terminate();
    }
  }
  
  private cProgressPanelFx(File oRoot)
  {
    try
    {
      oDriveRoot = oRoot;
      oDrives.put(oDriveRoot.getAbsolutePath(), this);
      start(null);
    }
    catch (Exception ex)
    {
      Logger.getLogger(cProgressPanelFx.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  @Override
  public void start(Stage stage) throws Exception
  {
    FXMLLoader oLoader = new FXMLLoader(getClass().getResource("cProgressPanel.fxml"));
    oRoot = oLoader.load();
    oScene = new Scene(oRoot);
       
    oProgressPanelController = oLoader.<cProgressPanelController>getController();
    
    oProgressPanelController.postInitialize(oDriveRoot);
  }
  
  public Parent getRoot()
  {
    return oRoot;
  }
  
  public cProgressPanelController getController()
  {
    return oProgressPanelController;
  }

  public void deleteMetadata()
  {
    oProgressPanelController.deleteMetadata();
  }

  public void resetProgress()
  {
    oProgressPanelController.resetProgress();
  }

  public void setStatus(String sStatus)
  {
    oProgressPanelController.setStatus(sStatus);
  }

  public void cancel()
  {
    oProgressPanelController.cancel();
  }

  public void complete()
  {
    oProgressPanelController.complete();
  }

  public void appendIndexSize(String sFile, long length)
  {
    oProgressPanelController.appendIndexSize(sFile, length);
  }
}
