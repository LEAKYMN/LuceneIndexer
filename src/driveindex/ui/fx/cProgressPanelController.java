package driveindex.ui.fx;

import driveindex.injection.cInjector;
import driveindex.persistance.cMetadata;
import driveindex.scanner.cDriveMediator;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javax.swing.JProgressBar;
import org.apache.commons.io.FileUtils;

/**
 * FXML Controller class
 *
 * @author Philip Trenwith
 */
public class cProgressPanelController implements Initializable
{
  private File m_oDriveRoot;
  private cMetadata m_oMetadata;
  private long lTotalSize = 0;
  private long lUsedSize = 0;
  private long lFreeSpace = 0;
  private AtomicLong oIndexedSize = new AtomicLong(0);
  private final static SimpleDateFormat g_DF = new SimpleDateFormat("dd MMMMMM yyyy HH:mm:ss");
  private DecimalFormat oNumberFormat = new DecimalFormat( "###.##" );
  private final static double m_dGIGA_BYTE = 1073741824;// metric recommendation
  private final static double m_dTERA_BYTE = 1099511627776L;// metric recommendation 
  private double m_oDivider = 1;
  private String m_oDividerLabel = "";
  private MenuItem oMenuItemIndex;
  private javax.swing.JProgressBar jProgressBar;
  
  @FXML private AnchorPane m_oAnchorPane;
  @FXML private Label m_oDrivePathLabel;
  @FXML private Label m_oLastScanLabel;
  @FXML private Label m_oFolderWatcherLabel;
  @FXML private Label m_oUsedSpaceLabel;
  @FXML private Label m_oTotalSpaceLabel;
  @FXML private Label m_oStatusLabel;
  @FXML private SwingNode m_oProgressNode;
  
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
        m_oStatusLabel.setText("Idle");
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
    jProgressBar = new JProgressBar();
    jProgressBar.setStringPainted(true);
     
    m_oProgressNode.setContent(jProgressBar);
  }  

  public void postInitialize(File oDriveRoot)
  {
    m_oDriveRoot = oDriveRoot;
    
    m_oDrivePathLabel.setText("Drive: " + oDriveRoot.getAbsolutePath());
    String sMountPoint = oDriveRoot.getAbsolutePath();
    sMountPoint = sMountPoint.replaceAll("[^a-zA-Z0-9]", "");
    m_oMetadata = new cMetadata(sMountPoint+"");
    if (m_oMetadata.exists())
    {
      m_oLastScanLabel.setText("Last Scan: " + ((m_oMetadata.getPropertyValue("lastscan")==null) ? "" : m_oMetadata.getPropertyValue("lastscan")));
      m_oStatusLabel.setText("Status: " + m_oMetadata.getPropertyValue("status"));
      jProgressBar.setValue(Integer.parseInt(m_oMetadata.getPropertyValue("indexed")));
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
    String sProgress = "Indexed: " + oNumberFormat.format(lIndexedSize/m_oDivider) + m_oDividerLabel;
//    if (lFileSize > 0)
//    {
//      System.out.println(sProgress + " -> " + sFile + " (" + lFileSize + ")");
//    }
    //jProgressBar.setString("Indexed: " + FileUtils.byteCountToDisplaySize(lIndexedSize) + " / " + FileUtils.byteCountToDisplaySize(lTotalSize));
    //m_oProgressBar.setString("Indexed: " + oNumberFormat.format(lIndexedSize/m_oDivider) + m_oDividerLabel);
    Platform.runLater(() -> 
    {
      jProgressBar.setString(sProgress);
      jProgressBar.setValue((int)getPercentage(lIndexedSize,lUsedSize));
      jProgressBar.repaint();
    });
  }
  
  public void complete()
  {
    Platform.runLater(() -> 
    {
      oMenuItemIndex.setText("Index Drive");
      oMenuItemIndex.setId("index");

      jProgressBar.setString("100%");
      jProgressBar.setValue(100);
      setStatus("Indexed");

      Date time = new GregorianCalendar().getTime();
      m_oLastScanLabel.setText("Last Scan: " + g_DF.format(time)); 
      m_oMetadata.setPropertyValue("lastscan", g_DF.format(time), false);
      m_oMetadata.setPropertyValue("status", "Indexed", false);
      m_oMetadata.setPropertyValue("indexed", "100", true);
    });
  }
  
  public void cancel()
  {
    setStatus("Idle");
    
    m_oMetadata.setPropertyValue("status", "Idle", false);
    m_oMetadata.setPropertyValue("indexed", ((int)getPercentage(oIndexedSize.get(),lUsedSize))+"", true);
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
      m_oDividerLabel = " KB";
      m_oDivider = 1024;
      sUsedSpace = FileUtils.byteCountToDisplaySize(lUsedSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("MB"))
    {
      m_oDividerLabel = " MB";
      m_oDivider = 1024*1024;
      sUsedSpace = FileUtils.byteCountToDisplaySize(lUsedSize);
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("GB"))
    {
      m_oDividerLabel = " GB";
      m_oDivider = m_dGIGA_BYTE;
      sUsedSpace = oNumberFormat.format(lUsedSize/m_dGIGA_BYTE) + " GB";
    }
    else if (FileUtils.byteCountToDisplaySize(lUsedSize).endsWith("TB"))
    {
      m_oDividerLabel = " GB";
      m_oDivider = m_dGIGA_BYTE;
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
    m_oStatusLabel.setText("Status: Idle");
    
    if (getPercentage(lUsedSize, lTotalSize) > 85)
    {
      m_oUsedSpaceLabel.setStyle("-fx-text-fill: red ;");
    }
    
    //m_oProgressBar.setStringPainted(true);
  }

  public void resetProgress() 
  {
    oIndexedSize.set(0);
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
}
