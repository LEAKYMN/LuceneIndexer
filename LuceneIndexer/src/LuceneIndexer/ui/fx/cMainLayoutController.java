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
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Philip Trenwith
 */


public class cMainLayoutController implements Observer, Initializable
{
  private final DecimalFormat oNumberFormat = new DecimalFormat("###,###,###,###");
  private ContextMenu m_oSearchResultsContextMenu = new ContextMenu();

  @FXML
  private AnchorPane m_oMainAnchorPane;
  @FXML
  private Label m_oStatusLabel;
  @FXML
  private CheckBox m_chkWholeWords;
  @FXML
  private VBox m_oSearchBox;
  @FXML
  private TableView m_oResultTable;
  @FXML
  private Button m_oIndexButton;

  @FXML
  private ListView m_oDriveList;

  @FXML
  private Label m_oIndexDirectoryLabel;
  @FXML
  private ComboBox m_cmbSearchIndex;
  @FXML
  private ComboBox m_cmbDuplicateIndex;
  @FXML
  private ComboBox m_cmbIndexDirectories;
  @FXML
  private Label m_oIndexSizeLabel;
  @FXML
  private Label m_oNumberOfDocumentsLabel;
  @FXML
  private Button m_oDuplicatesButton;
  @FXML
  private Button m_oDeleteIndexButton;
  @FXML
  private Button m_oRefreshButton;
  @FXML
  private TableView m_oTotDocsTable;

  @FXML
  private Tab m_oSearchTab;
  @FXML
  private Tab m_oDrives;
  @FXML
  private Tab m_oIndex;
  @FXML
  private Tab m_oDuplicationTab;
  @FXML
  private TableView m_oDuplicatesTable;

  private cSearchTable m_oSearchTable;
  private cDriveMediator oMediator;
  private TableColumn[] lsResultHeader;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    m_oSearchTab.setClosable(false);
    m_oDrives.setClosable(false);
    m_oIndex.setClosable(false);
    m_oDuplicationTab.setClosable(false);
    TableColumn<String, Long> oSizeColumn = new TableColumn<>(eDocument.TAG_Size);
    PropertyValueFactory<String, Long> propertyValueFactory = new PropertyValueFactory<String, Long>(eDocument.TAG_Size);
    new Callback<TableColumn<String, Long>, TableCell<String, Long>>()
    {
      @Override
      public TableCell<String, Long> call(TableColumn<String, Long> param)
      {
        return new TableCell<String, Long>()
        {
          @Override
          protected void updateItem(Long item, boolean empty)
          {
            super.updateItem(item, empty);

            if (!empty)
            {
              setText(FileUtils.byteCountToDisplaySize(item));
            }
            else
            {
              setText(null);
            }
          }
        };
      }
    };
    
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
        new TableColumn(eDocument.TAG_Size),
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

      TableColumn<Map, String> oDuplicateTableColumn = new TableColumn<>(oResultColumn.getText());
      oDuplicateTableColumn.setCellValueFactory(new MapValueFactory(oDuplicateTableColumn.getText()));
      oDuplicateTableColumn.setMinWidth(dColumnWidth);
      m_oDuplicatesTable.getColumns().add(oDuplicateTableColumn);
    }

    m_oSearchTable = new cSearchTable(dColumnWidth);
    TableView oTable = m_oSearchTable.getTable();
    m_oSearchBox.getChildren().addAll(oTable);

    MenuItem oOpenLocation = new MenuItem("Open File Location");
    oOpenLocation.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        try
        {
          HashMap oItems = (HashMap) m_oResultTable.getSelectionModel().getSelectedItem();
          String sPath = (String) oItems.get("Path");
          Desktop.getDesktop().open(new File(sPath));
        }
        catch (IOException ex)
        {
          Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    });
    MenuItem oPlayFile = new MenuItem("Open/Play File");
    oPlayFile.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        try
        {
          HashMap oItems = (HashMap) m_oResultTable.getSelectionModel().getSelectedItem();

          String sPath = (String) oItems.get("Path");
          String sFilename = (String) oItems.get("Filename");
          String sAbsolutePath = sPath + File.separator + sFilename;
          Object sExtension = oItems.get("Extension");
          if (sExtension != null && !(sExtension + "").isEmpty())
          {
            sAbsolutePath += "." + sExtension;
          }
          Desktop.getDesktop().open(new File(sAbsolutePath));
          System.out.println();
        }
        catch (IOException ex)
        {
//          if (ex.getMessage().contains("No application is associated"))
//          {
//            try
//            {
//              final String cmd = String.format("cmd.exe /C start %s", "\"" + new File(sAbsolutePath).getAbsolutePath() + "\"");
//              Runtime.getRuntime().exec(cmd);
//            }
//            catch (final Throwable t)
//            {
//              t.printStackTrace();
//            }
//          }
          Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    });
    
    MenuItem oDeleteFile = new MenuItem("Delete File");
    oDeleteFile.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        try
        {
          HashMap oItems = (HashMap) m_oResultTable.getSelectionModel().getSelectedItem();
          String sPath = (String) oItems.get("Path");
          String sFilename = (String) oItems.get("Filename");
          String sAbsolutePath = sPath + File.separator + sFilename;
          Object sExtension = oItems.get("Extension");
          if (sExtension != null && !(sExtension + "").isEmpty())
          {
            sAbsolutePath += "." + sExtension;
          }
          File oFile = new File(sAbsolutePath);
          cConfirmDialog oConfirmDialog = new cConfirmDialog(LuceneIndexerFX.m_oStage, "Are you sure you want to delete '" + sAbsolutePath + "'?");
          if (oConfirmDialog.getResult() == cConfirmDialog.YES)
          {
            oFile.delete();
            File oDirectory = new File(sPath);
            if (oDirectory.list().length == 0)
            {
              oDirectory.delete();
            }
          }
        }
        catch (Exception ex)
        {
          Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    });

    // Add MenuItem to ContextMenu
    m_oSearchResultsContextMenu.getItems().addAll(oOpenLocation, oPlayFile);
    m_oResultTable.setContextMenu(m_oSearchResultsContextMenu);
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
        for (cProgressPanelFx oDrivePanel : lsDrivePanels)
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
        for (cProgressPanelFx oDrivePanel : lsDrivePanels)
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
          for (cProgressPanelFx oPanel : oPanels)
          {
            oPanel.deleteMetadata();
          }
        }

        File oDirectory = new File(cConfig.instance().getIndexLocation());
        String[] lsFiles = oDirectory.list();
        if (lsFiles != null)
        {
          for (String sFile : lsFiles)
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
    new Thread(() ->
    {
      loadIndexMetadata();
    }, "handleRefresh").start();
  }

  @FXML
  private void handleFindDuplicates(ActionEvent event)
  {
    m_oDuplicatesButton.setText("Please wait...");
    m_oDuplicatesButton.setDisable(true);
    new Thread(() ->
    {
      findDuplicates();
      Platform.runLater(()-> 
      {
        m_oDuplicatesButton.setText("Find Duplicates");
        m_oDuplicatesButton.setDisable(false);
      });
    }, "handleFindDuplicates").start();
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

  public void findDuplicates()
  {
    m_oDuplicatesTable.getItems().clear();
    char cDriveLetter = (m_cmbDuplicateIndex.getValue()+"").toCharArray()[0];
    HashMap<String, ArrayList<eDocument>> oDuplicates = cIndex.findDuplicates(cDriveLetter);
    Set<String> keySet = oDuplicates.keySet();
    for (String sHash: keySet)
    {
      ArrayList<eDocument> lsDocs = oDuplicates.get(sHash);
      for (eDocument oDoc : lsDocs)
      {
        Map<String, Object> oDataRow = new HashMap<>();
        oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
        oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
        oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
        oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
        oDataRow.put(eDocument.TAG_Size, oDoc.lFileSize);
        if (cConfig.instance().getHashDocuments())
        {
          oDataRow.put(eDocument.TAG_Hash, oDoc.sFileHash);
        }
        m_oDuplicatesTable.getItems().add(oDataRow);
      }
    }
  }

  public void loadIndexMetadata()
  {
    cDrive oDrive = cDriveMediator.instance().getDrive(m_cmbIndexDirectories.getValue() + "");
    cIndex oIndex = oDrive.getIndex();

    File oDirectory = new File(oIndex.getIndexLocation());
    long lSize = 0;
    if (oDirectory.exists())
    {
      String[] lsFiles = oDirectory.list();
      for (String sFile : lsFiles)
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
        m_oNumberOfDocumentsLabel.setText("Number of Documents: N/A");
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
      int iSelectedDuplicateIndex = Math.max(0, m_cmbDuplicateIndex.getSelectionModel().getSelectedIndex());
      int iSelectedSearchIndex = Math.max(0, m_cmbSearchIndex.getSelectionModel().getSelectedIndex());
      int iSelectedMetadataIndex = Math.max(0, m_cmbIndexDirectories.getSelectionModel().getSelectedIndex());
      m_oDriveList.getItems().clear();
      m_cmbDuplicateIndex.getItems().clear();
      m_cmbSearchIndex.getItems().clear();
      m_cmbIndexDirectories.getItems().clear();
      
      //m_cmbDuplicateIndex.getItems().add("All");
      m_cmbSearchIndex.getItems().add("All");

      cProgressPanelFx[] lsPanels = oMediator.listDrives();
      for (cProgressPanelFx oPanel : lsPanels)
      {
        m_oDriveList.getItems().add(oPanel.getParent());
        m_cmbDuplicateIndex.getItems().add(oPanel.getRoot());
        m_cmbSearchIndex.getItems().add(oPanel.getRoot());
        m_cmbIndexDirectories.getItems().add(oPanel.getRoot());
      }

      if (m_cmbDuplicateIndex.getItems().size() > 0)
      {
        m_cmbDuplicateIndex.setValue(m_cmbDuplicateIndex.getItems().get(iSelectedSearchIndex));
      }
      if (m_cmbSearchIndex.getItems().size() > 0)
      {
        m_cmbSearchIndex.setValue(m_cmbSearchIndex.getItems().get(iSelectedSearchIndex));
      }
      if (m_cmbIndexDirectories.getItems().size() > 0)
      {
        m_cmbIndexDirectories.setValue(m_cmbIndexDirectories.getItems().get(iSelectedMetadataIndex));
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
    for (eDocument oDoc : lsDocuments)
    {
      Map<String, Object> oDataRow = new HashMap<>();
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
    for (eDocument oDoc : lsResults)
    {
      Map<String, Object> oDataRow = new HashMap<>();
      oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
      oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
      oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
      oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
      oDataRow.put(eDocument.TAG_Size, oDoc.lFileSize);
      if (cConfig.instance().getHashDocuments())
      {
        oDataRow.put(eDocument.TAG_Hash, oDoc.sFileHash);
      }
      m_oResultTable.getItems().add(oDataRow);
    }
  }

  public boolean getWholeWords()
  {
    return m_chkWholeWords.isSelected();
  }

  public boolean getCaseSensitive()
  {
    return false;
  }

  public void setStatus(String sStatus)
  {
    update(null, sStatus);
  }

  public String getIndex()
  {
    return m_cmbSearchIndex.getValue() + "";
  }
}
