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
package LuceneIndexer.ui.fx;

import LuceneIndexer.drives.cDrive;
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.persistance.cMetadata;
import LuceneIndexer.drives.cDriveMediator;
import static LuceneIndexer.persistance.cMetadata.m_sKEY_DURATION;
import static LuceneIndexer.persistance.cMetadata.m_sKEY_INDEXED;
import static LuceneIndexer.persistance.cMetadata.m_sKEY_LAST_SCAN;
import static LuceneIndexer.persistance.cMetadata.m_sKEY_STATUS;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.io.FileUtils;

/**
 * FXML Controller class
 *
 * @author Philip Trenwith
 */
public class cProgressPanelController implements Initializable
{
  public final static SimpleDateFormat g_DateAndTimeFormat = new SimpleDateFormat("dd MMMMMM yyyy HH:mm:ss");
  public final static SimpleDateFormat g_TimeFormat = new SimpleDateFormat("HH:mm:ss");
  
  private File m_oDriveRoot;
  private cDrive m_oDrive;
  private cMetadata m_oMetadata;
  private long lTotalSize = 0;
  private long lUsedSize = 0;
  private long lFreeSpace = 0;
  private AtomicLong oIndexedSize = new AtomicLong(0);
  private DecimalFormat oNumberFormat = new DecimalFormat( "###.##" );
  private final static double m_dGIGA_BYTE = 1073741824;// metric recommendation
  private final static double m_dTERA_BYTE = 1099511627776L;// metric recommendation
  private MenuItem oMenuItemIndex;
  
  @FXML private AnchorPane m_oAnchorPane;
  @FXML private Label m_oDrivePathLabel;
  @FXML private Label m_oLastScanLabel;
  @FXML private Label m_oFolderWatcherLabel;
  @FXML private Label m_oUsedSpaceLabel;
  @FXML private Label m_oTotalSpaceLabel;
  @FXML private Label m_oStatusLabel;
  @FXML private ProgressBar m_oProgressBar;
  @FXML private ProgressIndicator m_oProgressIndicator;
  
  /**
   * Initializes the controller class.
   */
  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    ContextMenu oContextMenu = new ContextMenu();
    oMenuItemIndex = new MenuItem("Index Drive");
    oMenuItemIndex.setId("index");
    oMenuItemIndex.setOnAction((ActionEvent event) -> 
    {
      //btnIndexNow.setText("Cancel...");
      //btnIndexNow.setActionCommand("cancel");
      
      if (oMenuItemIndex.getId().equals("index"))
      {
        oMenuItemIndex.setText("Cancel");
        oMenuItemIndex.setId("cancel");
        cDriveMediator.instance().scanDrive(m_oDriveRoot);        
      }
      else
      {
        oMenuItemIndex.setText("Index Drive");
        oMenuItemIndex.setId("index");
        cDriveMediator.instance().stopScan();
        cancel();
      }
    });
    
    MenuItem oMenuItemRefresh = new MenuItem("Refresh List");
    oMenuItemRefresh.setOnAction((ActionEvent event) ->
    {
      cMainLayoutController oController = cInjector.getInjector().getInstance(cMainLayoutController.class);
      oController.displayDrives();
    });
    
    oContextMenu.getItems().addAll(oMenuItemIndex, new SeparatorMenuItem(), oMenuItemRefresh);
    
    m_oAnchorPane.setOnMouseClicked((MouseEvent oEvent) -> 
    {
      if (oEvent.getButton() == MouseButton.SECONDARY)
      {
        oContextMenu.show(m_oAnchorPane, oEvent.getScreenX(), oEvent.getScreenY());
      }
    });
  }  

  public void postInitialize(File oDriveRoot, cDrive oDrive)
  {
    m_oDriveRoot = oDriveRoot;
    m_oDrive = oDrive;
    m_oDrivePathLabel.setText("Drive: " + oDriveRoot.getAbsolutePath());
    String sMountPoint = oDriveRoot.getAbsolutePath();
    sMountPoint = sMountPoint.replaceAll("[^a-zA-Z0-9]", "");
    m_oMetadata = new cMetadata(sMountPoint+"");
    if (m_oMetadata.exists())
    {
      m_oLastScanLabel.setText("Last Scan: " + ((m_oMetadata.getPropertyValue(m_sKEY_LAST_SCAN)==null) ? "" : 
              m_oMetadata.getPropertyValue(m_sKEY_LAST_SCAN) + " " + " (Scan Duration: " + g_TimeFormat.format(new Date(m_oDrive.getLastScanDuration())) + ")"));
      m_oStatusLabel.setText("Status: " + m_oMetadata.getPropertyValue(m_sKEY_STATUS));
      double dProgress = Double.parseDouble(m_oMetadata.getPropertyValue(m_sKEY_INDEXED));
      setProgress(dProgress);
    }
    else
    {
      setProgress(0.0);
    }

    //startFolderWatcher();
    scan();
  }
  
  public void deleteMetadata()
  {
    if (m_oMetadata != null)
    {
      m_oMetadata.delete();
    }
    else
    {
      System.err.println("Could not delete metadata because the element is null.");
    }
  }
  
  public void terminate()
  {
//    bRunning = false;
//    if (oFolderWatcher != null)
//    {
//      oFolderWatcher.terminate();
//    }
  }
  
  public boolean shouldShow()
  {
    return lTotalSize > 0;
  }

  public void setStatus(String sStatus)
  {
    Platform.runLater(() -> {m_oStatusLabel.setText("Status: " + sStatus);});    
    cDriveMediator.instance().setStatus(null);
  }

  public void appendIndexSize(String sFile, long lFileSize)
  {
    long lIndexedSize = oIndexedSize.addAndGet(lFileSize);
    //String sProgress = "Indexed: " + oNumberFormat.format(lIndexedSize/m_oDivider) + m_oDividerLabel;
    Platform.runLater(() -> 
    {
      double dProgress = getPercentage(lIndexedSize,lUsedSize);
      setProgress(dProgress); 
    });
  }
  
  public void markAsBusyIndexing(boolean bIndexing)
  {
    Platform.runLater(() -> 
    {
      if (bIndexing)
      {
        oMenuItemIndex.setText("Cancel");
        oMenuItemIndex.setId("cancel");
      }
      else 
      {
        oMenuItemIndex.setText("Index Drive");
        oMenuItemIndex.setId("index");
      }
    });
  }
  
  public void complete()
  {
    Platform.runLater(() -> 
    {
      oMenuItemIndex.setText("Index Drive");
      oMenuItemIndex.setId("index");

      setProgress(100);
      setStatus("Indexed");

      m_oLastScanLabel.setText("Last Scan: " + g_DateAndTimeFormat.format(m_oDrive.getLastScanTime()) + " (Scan Duration: " + g_TimeFormat.format(new Date(m_oDrive.getLastScanDuration())) + ")"); 
      m_oMetadata.setPropertyValue(m_sKEY_LAST_SCAN, g_DateAndTimeFormat.format(m_oDrive.getLastScanTime()), false);
      m_oMetadata.setPropertyValue(m_sKEY_STATUS, "Indexed", false);
      m_oMetadata.setPropertyValue(m_sKEY_INDEXED, "100", false);
      m_oMetadata.setPropertyValue(m_sKEY_DURATION, m_oDrive.getLastScanDuration()+"", true);
    });
  }
  
  public void cancel()
  {
    setStatus("Idle");
    
    //m_oMetadata.setPropertyValue(m_sKEY_STATUS", "Idle", false);
    m_oMetadata.setPropertyValue(m_sKEY_INDEXED, ((int)getPercentage(oIndexedSize.get(),lUsedSize))+"", true);
  }
  
  public void scan()
  {
    lTotalSize = m_oDriveRoot.getTotalSpace();
    lFreeSpace = m_oDriveRoot.getFreeSpace();
    lUsedSize = lTotalSize-lFreeSpace;
    
    System.out.println("Drive: " + m_oDriveRoot.getAbsolutePath() + " Total: " + FileUtils.byteCountToDisplaySize(lTotalSize) + 
            " Free: " + FileUtils.byteCountToDisplaySize(lFreeSpace) + " Used: " + FileUtils.byteCountToDisplaySize(lUsedSize));
    
    String sUsedSpace = oNumberFormat.format(lTotalSize) + " B";
    String sTotalSpace = oNumberFormat.format(lUsedSize) + " B";
    if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("KB"))
    {
      sUsedSpace = FileUtils.byteCountToDisplaySize(lUsedSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("MB"))
    {
      sUsedSpace = FileUtils.byteCountToDisplaySize(lUsedSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("GB"))
    {
      sUsedSpace = oNumberFormat.format(lUsedSize/m_dGIGA_BYTE) + " GB";
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("TB"))
    {
      sUsedSpace = oNumberFormat.format(lUsedSize/m_dTERA_BYTE) + " TB";
    }
    
    if (FileUtils.byteCountToDisplaySize(lTotalSize).endsWith("KB"))
    {
      sTotalSpace = FileUtils.byteCountToDisplaySize(lTotalSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lTotalSize).endsWith("MB"))
    {
      sTotalSpace = FileUtils.byteCountToDisplaySize(lTotalSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lTotalSize).endsWith("GB"))
    {
      sTotalSpace = oNumberFormat.format(lTotalSize/m_dGIGA_BYTE) + " GB";
    }
    else if (FileUtils.byteCountToDisplaySize(lTotalSize).endsWith("TB"))
    {
      sTotalSpace = oNumberFormat.format(lTotalSize/m_dTERA_BYTE) + " TB";
    }
    
    m_oDrivePathLabel.setText("Drive: " + m_oDriveRoot.getAbsolutePath());
    m_oUsedSpaceLabel.setText("Used Space: " + sUsedSpace);
    m_oTotalSpaceLabel.setText("Total: " + sTotalSpace);
    if (getPercentage(lUsedSize, lTotalSize) > 85)
    {
      m_oUsedSpaceLabel.setStyle("-fx-text-fill: red ;");
    }
  }

  public void resetProgress() 
  {
    Platform.runLater(() -> 
    {
      setProgress(0.0);
      oIndexedSize.set(0);
    });
  }
  
  public float getPercentage(long n, long total) 
  {
    float proportion = ((float) n) / ((float) total);
    return proportion * 100;
  }
  
  public File getRoot()
  {
    return m_oDriveRoot;
  }

  private void setProgress(double dProgress)
  {
    double d = dProgress/100f;
    m_oProgressBar.setProgress(d);
    m_oProgressIndicator.setProgress(d);
  }
}
