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

import LuceneIndexer.cConfig;
import LuceneIndexer.dialogs.cConfirmDialog;
import LuceneIndexer.lucene.eDocument;
import LuceneIndexer.drives.cDriveMediator;
import LuceneIndexer.drives.cDrive;
import LuceneIndexer.lucene.cIndex;
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
import java.util.Iterator;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
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
  @FXML private ComboBox m_oIndexDirectories;
  @FXML private Label m_oIndexSizeLabel;
  @FXML private Label m_oNumberOfDocumentsLabel;
  @FXML private Button m_oDeleteIndexButton;
  @FXML private Button m_oRefreshButton;
  @FXML private TableView m_oTotDocsTable;
  
  @FXML private Tab m_oSearchTab;
  @FXML private Tab m_oDrives;
  @FXML private Tab m_oIndex;

  private LuceneIndexer.ui.fx.cSearchTable m_oSearchTable;
  private cDriveMediator oMediator;
  
  private TableColumn[] lsResultHeader;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    m_oSearchTab.setClosable(false);
    m_oDrives.setClosable(false);
    m_oIndex.setClosable(false);
    if (cConfig.instance().getHashDocuments())
    {
      lsResultHeader = new TableColumn[]
      {
        new TableColumn(eDocument.TAG_Path),
        new TableColumn(eDocument.TAG_Filename),
        new TableColumn(eDocument.TAG_Extension),
        new TableColumn(eDocument.TAG_Category),
        new TableColumn(eDocument.TAG_Size),
        new TableColumn(eDocument.TAG_Hash)
      };
    }
    else
    {
      lsResultHeader = new TableColumn[]
      {
        new TableColumn(eDocument.TAG_Path),
        new TableColumn(eDocument.TAG_Filename),
        new TableColumn(eDocument.TAG_Extension),
        new TableColumn(eDocument.TAG_Category),
        new TableColumn(eDocument.TAG_Size)
      };
    }
    
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
      {
        m_oIndexButton.setText("Cancel...");
        m_oIndexButton.setId("cancel");
        cProgressPanelFx[] lsDrivePanels = cProgressPanelFx.getAll();
        for (cProgressPanelFx oDrivePanel: lsDrivePanels)
        {
          oDrivePanel.getController().markAsBusyIndexing(true);
        }
        new Thread(() -> 
        {
          oMediator.startScan();
        }, "handleIndexDrives_index").start();
        break;
      }
      case "cancel":
      {
        m_oIndexButton.setText("Index Drives");
        m_oIndexButton.setId("index");
        cProgressPanelFx[] lsDrivePanels = cProgressPanelFx.getAll();
        for (cProgressPanelFx oDrivePanel: lsDrivePanels)
        {
          oDrivePanel.getController().markAsBusyIndexing(false);
        }
        new Thread(() -> 
        {
          oMediator.stopScan();
          displayDrives();
        }, "handleIndexDrives_cancel").start();
        break;
      }
    }
  }
  
  @FXML
  private void handleDeleteIndex(ActionEvent event)
  {
    cConfirmDialog oDialog = new cConfirmDialog(LuceneIndexerFX.m_oStage, "Are you sure you want to delete the index?");
    oDialog.showAndWait();
    int result = oDialog.getResult();
    if (result == cConfirmDialog.YES)
    {
      m_oTotDocsTable.getItems().clear();
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
            if (!oChildFile.delete())
            {
              System.err.println("Failed to delete index file: " + oChildFile.getAbsolutePath());
            }
          }
        }

        loadIndexMetadata();
      }, "handleDeleteIndex").start();
    }
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
    cDrive oDrive = cDriveMediator.instance().getDrive(m_oIndexDirectories.getValue()+"");
    cIndex oIndex = oDrive.getIndex();
    
    File oDirectory = new File(oIndex.getIndexLocation());
    long lSize = 0;
    if (oDirectory.exists())
    {
      String[] lsFiles = oDirectory.list();
      for (String sFile: lsFiles)
      {
        File oFile = new File(oDirectory + File.separator + sFile);
        lSize += oFile.length();
      }
    }
    
    int iDocuments = oIndex.getNumberOfDocuments();
    
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
    
    ArrayList<eDocument> topdocs = oIndex.getTopNDocuments(50);
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
        m_oDriveList.getItems().add(oPanel.getParent());
        m_oIndexDirectories.getItems().add(oPanel.getRoot());
      }
      
      if (m_oIndexDirectories.getItems().size()>0)
      {
        m_oIndexDirectories.setValue(m_oIndexDirectories.getItems().get(0));
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
    m_oTotDocsTable.getItems().clear();
    for (eDocument oDoc: lsDocuments)
    {
      Map<String, String> oDataRow = new HashMap<>();
      oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
      oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
      oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
      oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
      oDataRow.put(eDocument.TAG_Size, oDoc.getFormattedFileSize());
      if (cConfig.instance().getHashDocuments())
      {
        oDataRow.put(eDocument.TAG_Hash, oDoc.sFileHash);
      }
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
      if (cConfig.instance().getHashDocuments())
      {
        oDataRow.put(eDocument.TAG_Hash, oDoc.sFileHash);
      }
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
