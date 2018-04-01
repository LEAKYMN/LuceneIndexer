package driveindex.ui.fx;

import driveindex.cConfig;
import driveindex.lucene.cLuceneIndexReader;
import driveindex.lucene.eDocument;
import driveindex.scanner.cDriveMediator;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Philip Trenwith
 */
public class cMainLayoutController implements Observer, Initializable
{
  private final DecimalFormat oNumberFormat = new DecimalFormat( "###,###,###,###" );
  
  @FXML private AnchorPane m_oMainAnchorPane;
  @FXML private Label m_oStatusLabel;
  @FXML private CheckBox m_oWholeWords;
  @FXML private VBox m_oSearchBox;
  @FXML private TableView m_oResultTable;
  @FXML private Button m_oIndexButton;
  
  @FXML private ListView m_oDriveList;
  
  @FXML private Label m_oIndexDirectoryLabel;
  @FXML private Label m_oIndexSizeLabel;
  @FXML private Label m_oNumberOfDocumentsLabel;
  @FXML private Button m_oDeleteIndexButton;
  @FXML private Button m_oRefreshButton;
  @FXML private TableView m_oTotDocsTable;

  private final TableColumn[] lsResultHeader = new TableColumn[]
  {
    new TableColumn(eDocument.TAG_Path),
    new TableColumn(eDocument.TAG_Filename),
    new TableColumn(eDocument.TAG_Extension),
    new TableColumn(eDocument.TAG_Category),
    new TableColumn(eDocument.TAG_Size)
  };

  private driveindex.ui.fx.cSearchTable m_oSearchTable;
  private cDriveMediator oMediator;
  
//  private Method columnToFitMethod;
//  public void autoFitTable(TableView tableView) 
//  {
//    m_oSearchTable.getItems().addListener(new ListChangeListener<Object>() {
//      @Override
//      public void onChanged(Change<?> c) {
//        for (Object column : tableView.getColumns())
//        {
//          try
//          {
//            columnToFitMethod.invoke(m_oSearchTable.getSkin(), column, -1);
//          }
//          catch (IllegalAccessException | InvocationTargetException e)
//          {
//            e.printStackTrace();
//          }
//        }
//      }
//    });
//  }
  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
//    try 
//    {
//        columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
//        columnToFitMethod.setAccessible(true);
//    } 
//    catch (NoSuchMethodException e) 
//    {
//        e.printStackTrace();
//    }

    m_oIndexButton.setId("index");
    double dTotalWidth = m_oMainAnchorPane.getPrefWidth() - 10;
    double dColumnWidth = dTotalWidth / lsResultHeader.length;
    for (int i = 0; i < lsResultHeader.length; i++)
    {
      TableColumn<Map, String> oResultColumn = (TableColumn) lsResultHeader[i];
      oResultColumn.setCellValueFactory(new MapValueFactory(oResultColumn.getText()));
      oResultColumn.setMinWidth(dColumnWidth);
      m_oResultTable.getColumns().add(oResultColumn);
      
      TableColumn<Map, String> oIndexColumn = new TableColumn<>(oResultColumn.getText());
      oIndexColumn.setCellValueFactory(new MapValueFactory(oResultColumn.getText()));
      oIndexColumn.setMinWidth(dColumnWidth);
      m_oTotDocsTable.getColumns().add(oIndexColumn);
    }

//    m_oSearchTable = new driveindex.ui.swing.cSearchTable(); //m_oSearchTable = new cSearchTable(dColumnWidth);
//    JScrollPane oJTable = m_oSearchTable.getTable();
//    SwingNode oSwingNode = new SwingNode();
//    oSwingNode.setContent(oJTable);
//    m_oSearchBox.getChildren().addAll(oSwingNode);
    
    m_oSearchTable = new cSearchTable(dColumnWidth);
    TableView oTable = m_oSearchTable.getTable();
    m_oSearchBox.getChildren().addAll(oTable);
    
    oMediator = cDriveMediator.instance();
    
    displayDrives();
  }

  @FXML
  private void handleSearch(ActionEvent event)
  {
    new Thread(() -> 
    {
      if (m_oSearchTable != null)
      {
        m_oSearchTable.search();
      }
    }, "handleSearch").start();
  }
  
  @FXML 
  private void handleIndexDrives(ActionEvent event)
  {
    switch (m_oIndexButton.getId()) 
    {
      case "index":
        m_oIndexButton.setText("Cancel...");
        m_oIndexButton.setId("cancel");
        new Thread(() -> 
        {
          oMediator.startScan();
        }, "handleIndexDrives_index").start();
        break;
      case "cancel":
        m_oIndexButton.setText("Index Drives");
        m_oIndexButton.setId("index");
        new Thread(() -> 
        {
          oMediator.stopScan();
          displayDrives();
        }, "handleIndexDrives_cancel").start();
        break;
    }
  }
  
  @FXML
  private void handleDeleteIndex(ActionEvent event)
  {
    new Thread(() -> 
    {
      oMediator.stopScan();
      cProgressPanelFx[] oPanels = cProgressPanelFx.getAll();
      if (oPanels != null)
      {
        for (cProgressPanelFx oPanel: oPanels)
        {
          oPanel.deleteMetadata();
        }
      }

      File oDirectory = new File(cConfig.instance().getIndexLocation());
      String[] lsFiles = oDirectory.list();
      if (lsFiles != null)
      {
        for (String sFile: lsFiles)
        {
          File oChildFile = new File(cConfig.instance().getIndexLocation() + File.separator + sFile);
          System.out.println("Deleting Index File: " + oChildFile.getAbsolutePath());
          oChildFile.delete();
        }
      }

      loadIndexMetadata();
    }, "handleDeleteIndex").start();
  }
  
  @FXML
  private void handleRefresh(ActionEvent event)
  {
    new Thread(() -> {loadIndexMetadata();}, "handleRefresh").start();
  }

  @Override
  public void update(Observable o, Object arg)
  {
    if (arg != null)
    {
      Platform.runLater(() -> 
      {
        m_oStatusLabel.setText("Status: " + arg);
      });
    }
    displayDrives();
  }

  public void loadIndexMetadata()
  {
    File oDirectory = new File(cConfig.instance().getIndexLocation());
    long lSize = 0;
    if (oDirectory.exists())
    {
      String[] lsFiles = oDirectory.list();
      for (String sFile: lsFiles)
      {
        File oFile = new File(cConfig.instance().getIndexLocation() + File.separator + sFile);
        lSize += oFile.length();
      }
    }
    cLuceneIndexReader.instance().open();
    int iDocuments = cLuceneIndexReader.instance().getNumberOfDocuments();
    
    long flSize = lSize;
    Platform.runLater(() -> 
    {
      m_oIndexDirectoryLabel.setText("Index Directory: " + oDirectory.getAbsolutePath());
      m_oIndexSizeLabel.setText("Index Size: " + FileUtils.byteCountToDisplaySize(flSize));

      if (iDocuments == -1)
      {
        m_oNumberOfDocumentsLabel.setText("Number of Documents: N/A" );
      }
      else
      {
        m_oNumberOfDocumentsLabel.setText("Number of Documents: " + oNumberFormat.format(iDocuments));
      }
    });
    
    ArrayList<eDocument> topdocs = cLuceneIndexReader.instance().getTopNDocuments(50);
    setIndexTableDocuments(topdocs);
  }
  
  public void displayDrives()
  {
    Platform.runLater(() -> 
    {
      m_oDriveList.getItems().clear();
      cProgressPanelFx[] lsPanels = oMediator.listDrives();
      for (cProgressPanelFx oPanel: lsPanels)
      {
        m_oDriveList.getItems().add(oPanel.getRoot());
      }
    });
  }

  public void scanComplete()
  {
    Platform.runLater(() -> 
    {
      m_oIndexButton.setText("Index Drives");
      m_oIndexButton.setId("index");
      update(null, "");
    });
  }
  
  public void setIndexTableDocuments(ArrayList<eDocument> lsDocuments)
  {
    for (eDocument oDoc: lsDocuments)
    {
      Map<String, String> oDataRow = new HashMap<>();
      oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
      oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
      oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
      oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
      oDataRow.put(eDocument.TAG_Size, oDoc.getFormattedFileSize());
      m_oTotDocsTable.getItems().add(oDataRow);    
    }
  }

  public void setResults(ArrayList<eDocument> lsResults)
  {
    m_oResultTable.getItems().clear();
    for (eDocument oDoc: lsResults)
    {
      Map<String, String> oDataRow = new HashMap<>();
      oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
      oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
      oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
      oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
      oDataRow.put(eDocument.TAG_Size, oDoc.getFormattedFileSize());
      m_oResultTable.getItems().add(oDataRow);    
    }
  }

  public boolean getWholeWords()
  {
    return m_oWholeWords.isSelected();
  }

  public boolean getCaseSensitive()
  {
    return false;
  }

  public void setStatus(String sStatus)
  {
    update(null, sStatus);
  }
}
