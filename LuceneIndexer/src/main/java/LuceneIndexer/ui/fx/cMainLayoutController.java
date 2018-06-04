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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Philip Trenwith
 */
public class cMainLayoutController implements Observer, Initializable
{
  private final DecimalFormat m_oNumberFormat = new DecimalFormat("###,###,###,###");

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
  private cDriveMediator m_oMediator;
  private TableColumn[] m_lsResultHeader;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    m_oSearchTab.setClosable(false);
    m_oDrives.setClosable(false);
    m_oIndex.setClosable(false);
    m_oDuplicationTab.setClosable(false);
    m_oDuplicatesTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
//    TableColumn<String, Long> oSizeColumn = new TableColumn<>(eDocument.TAG_Size);
//    PropertyValueFactory<String, Long> propertyValueFactory = new PropertyValueFactory<String, Long>(eDocument.TAG_Size);
//    new Callback<TableColumn<String, Long>, TableCell<String, Long>>()
//    {
//      @Override
//      public TableCell<String, Long> call(TableColumn<String, Long> param)
//      {
//        return new TableCell<String, Long>()
//        {
//          @Override
//          protected void updateItem(Long item, boolean empty)
//          {
//            super.updateItem(item, empty);
//
//            if (!empty)
//            {
//              setText(FileUtils.byteCountToDisplaySize(item));
//            }
//            else
//            {
//              setText(null);
//            }
//          }
//        };
//      }
//    };

    if (cConfig.instance().getHashDocuments())
    {
      m_lsResultHeader = new TableColumn[]
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
      m_lsResultHeader = new TableColumn[]
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
    double dColumnWidth = dTotalWidth / m_lsResultHeader.length;
    for (int i = 0; i < m_lsResultHeader.length; i++)
    {
      if (((TableColumn) m_lsResultHeader[i]).getText().equals(eDocument.TAG_Size))
      {
        TableColumn<eDocument, Number> oResultColumn = (TableColumn) m_lsResultHeader[i];
        oResultColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        oResultColumn.setCellFactory(tc -> new TableCell<eDocument, Number>()
        {
          @Override
          protected void updateItem(Number value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(FileUtils.byteCountToDisplaySize(value.longValue()));
            }
          }
        });
        oResultColumn.setMinWidth(dColumnWidth);
        m_oResultTable.getColumns().add(oResultColumn);

        TableColumn<eDocument, Number> oIndexColumn = new TableColumn<>(oResultColumn.getText());
        oIndexColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        oIndexColumn.setCellFactory(tc -> new TableCell<eDocument, Number>()
        {
          @Override
          protected void updateItem(Number value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(FileUtils.byteCountToDisplaySize(value.longValue()));
            }
          }
        });
        oIndexColumn.setMinWidth(dColumnWidth);
        m_oTotDocsTable.getColumns().add(oIndexColumn);

        TableColumn<eDocument, Number> oDuplicateTableColumn = new TableColumn<>(oResultColumn.getText());
        oDuplicateTableColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        oDuplicateTableColumn.setCellFactory(tc -> new TableCell<eDocument, Number>()
        {
          @Override
          protected void updateItem(Number value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(FileUtils.byteCountToDisplaySize(value.longValue()));
            }
          }
        });
        oDuplicateTableColumn.setMinWidth(dColumnWidth);
        m_oDuplicatesTable.getColumns().add(oDuplicateTableColumn);
      }
      else
      {
        TableColumn<eDocument, String> oResultColumn = (TableColumn) m_lsResultHeader[i];
        oResultColumn.setCellValueFactory(cellData -> cellData.getValue().getProperty(oResultColumn.getText()));
        oResultColumn.setCellFactory(tc -> new TableCell<eDocument, String>()
        {
          @Override
          protected void updateItem(String value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(value);
            }
          }
        });
        oResultColumn.setMinWidth(dColumnWidth);
        m_oResultTable.getColumns().add(oResultColumn);

        TableColumn<eDocument, String> oIndexColumn = new TableColumn<>(oResultColumn.getText());
        oIndexColumn.setCellValueFactory(cellData -> cellData.getValue().getProperty(oResultColumn.getText()));
        oIndexColumn.setCellFactory(tc -> new TableCell<eDocument, String>()
        {
          @Override
          protected void updateItem(String value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(value);
            }
          }
        });
        oIndexColumn.setMinWidth(dColumnWidth);
        m_oTotDocsTable.getColumns().add(oIndexColumn);

        TableColumn<eDocument, String> oDuplicateTableColumn = new TableColumn<>(oResultColumn.getText());
        oDuplicateTableColumn.setCellValueFactory(cellData -> cellData.getValue().getProperty(oResultColumn.getText()));
        oDuplicateTableColumn.setCellFactory(tc -> new TableCell<eDocument, String>()
        {
          @Override
          protected void updateItem(String value, boolean empty)
          {
            super.updateItem(value, empty);
            if (value == null || empty)
            {
              setText("");
            }
            else
            {
              setText(value);
            }
          }
        });
        oDuplicateTableColumn.setMinWidth(dColumnWidth);
        m_oDuplicatesTable.getColumns().add(oDuplicateTableColumn);
      }
    }

    m_oSearchTable = new cSearchTable(dColumnWidth);
    TableView oTable = m_oSearchTable.getTable();
    m_oSearchBox.getChildren().addAll(oTable);

    MenuItem oOpenLocationResults = new MenuItem("Open File Location");
    oOpenLocationResults.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oItems = (eDocument) m_oResultTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenLocation(oItems);
      }
    });
    MenuItem oPlayFileResults = new MenuItem("Open/Play File");
    oPlayFileResults.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oItems = (eDocument) m_oResultTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenFile(oItems);
      }
    });

    MenuItem oDeleteFileResults = new MenuItem("Delete File");
    oDeleteFileResults.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oItems = (eDocument) m_oResultTable.getSelectionModel().getSelectedItem();
        String sPath = (String) oItems.pathProperty().get();
        String sFilename = (String) oItems.filenameProperty().get();
        String sAbsolutePath = sPath + File.separator + sFilename;
        Object sExtension = oItems.extensionProperty().get();
        if (sExtension != null && !(sExtension + "").isEmpty())
        {
          sAbsolutePath += "." + sExtension;
        }
        String sMessage = "Are you sure you want to delete '" + sAbsolutePath + "'?";

        cConfirmDialog oConfirmDialog = new cConfirmDialog(LuceneIndexerFX.m_oStage, sMessage);
        oConfirmDialog.showAndWait();
        if (oConfirmDialog.getResult() == cConfirmDialog.YES)
        {
          handleContextMenuDeleteFile(oItems);

          char cDriveLetter = (m_cmbSearchIndex.getValue() + "").toCharArray()[0];
          if (cIndex.deleteFile(cDriveLetter, new File(sAbsolutePath)))
          {
            m_oResultTable.getItems().remove(oItems);
          }
        }
      }
    });

    // Add MenuItem to ContextMenu
    ContextMenu oSearchResultsContextMenu = new ContextMenu();
    oSearchResultsContextMenu.getItems().addAll(oOpenLocationResults, oPlayFileResults, oDeleteFileResults);
    m_oResultTable.setContextMenu(oSearchResultsContextMenu);

    MenuItem oOpenLocationIndex = new MenuItem("Open File Location");
    oOpenLocationIndex.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oTotDocsTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenLocation(oDoc);
      }
    });
    MenuItem oPlayFileIndex = new MenuItem("Open/Play File");
    oPlayFileIndex.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oTotDocsTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenFile(oDoc);
      }
    });

    MenuItem oDeleteFileIndex = new MenuItem("Delete File");
    oDeleteFileIndex.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oTotDocsTable.getSelectionModel().getSelectedItem();

        String sPath = (String) oDoc.pathProperty().get();
        String sFilename = (String) oDoc.filenameProperty().get();
        String sAbsolutePath = sPath + File.separator + sFilename;
        Object sExtension = oDoc.extensionProperty().get();
        if (sExtension != null && !(sExtension + "").isEmpty())
        {
          sAbsolutePath += "." + sExtension;
        }
        String sMessage = "Are you sure you want to delete '" + sAbsolutePath + "'?";

        cConfirmDialog oConfirmDialog = new cConfirmDialog(LuceneIndexerFX.m_oStage, sMessage);
        oConfirmDialog.showAndWait();
        if (oConfirmDialog.getResult() == cConfirmDialog.YES)
        {
          handleContextMenuDeleteFile(oDoc);

          char cDriveLetter = (m_cmbIndexDirectories.getValue() + "").toCharArray()[0];
          if (cIndex.deleteFile(cDriveLetter, new File(sAbsolutePath)))
          {
            m_oTotDocsTable.getItems().remove(oDoc);
          }
        }
      }
    });

    // Add MenuItem to ContextMenu
    ContextMenu oTopDocsContextMenu = new ContextMenu();
    oTopDocsContextMenu.getItems().addAll(oOpenLocationIndex, oPlayFileIndex, oDeleteFileIndex);
    m_oTotDocsTable.setContextMenu(oTopDocsContextMenu);

    MenuItem oOpenLocationDuplicates = new MenuItem("Open File Location");
    oOpenLocationDuplicates.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oDuplicatesTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenLocation(oDoc);
      }
    });
    MenuItem oPlayFileDuplicates = new MenuItem("Open/Play File");
    oPlayFileDuplicates.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oDuplicatesTable.getSelectionModel().getSelectedItem();
        handleContextMenuOpenFile(oDoc);
      }
    });

    MenuItem oDeleteFileDuplicates = new MenuItem("Delete File");
    oDeleteFileDuplicates.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        ObservableList oObservableList = m_oDuplicatesTable.getSelectionModel().getSelectedItems();
        if (oObservableList != null && !oObservableList.isEmpty())
        {
          String sMessage = "Are you sure you want to delete " + oObservableList.size() + " items?";
          if (oObservableList.size() == 1)
          {
            eDocument oDoc = (eDocument) oObservableList.get(0);
            String sPath = (String) oDoc.pathProperty().get();
            String sFilename = (String) oDoc.filenameProperty().get();
            String sAbsolutePath = sPath + File.separator + sFilename;
            Object sExtension = oDoc.extensionProperty().get();
            if (sExtension != null && !(sExtension + "").isEmpty())
            {
              sAbsolutePath += "." + sExtension;
            }
            sMessage = "Are you sure you want to delete '" + sAbsolutePath + "'?";
          }
          cConfirmDialog oConfirmDialog = new cConfirmDialog(LuceneIndexerFX.m_oStage, sMessage);
          oConfirmDialog.showAndWait();
          if (oConfirmDialog.getResult() == cConfirmDialog.YES)
          {
            oObservableList.forEach(oList ->
            {
              eDocument oDoc = (eDocument) oList;
              handleContextMenuDeleteFile(oDoc);
              m_oDuplicatesTable.getItems().remove(oDoc);
            });
          }
        }
      }
    });

    MenuItem oMarkFileDuplicates = new MenuItem("Mark");
    oMarkFileDuplicates.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        eDocument oDoc = (eDocument) m_oDuplicatesTable.getSelectionModel().getSelectedItem();
        handleContextMenuMarkFile(oDoc);
      }
    });

    // Add MenuItem to ContextMenu
    ContextMenu oDuplicatesContextMenu = new ContextMenu();
    oDuplicatesContextMenu.getItems().addAll(oOpenLocationDuplicates, oPlayFileDuplicates, oDeleteFileDuplicates, oMarkFileDuplicates);
    m_oDuplicatesTable.setContextMenu(oDuplicatesContextMenu);

    m_oMediator = cDriveMediator.instance();

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
          m_oMediator.startScan();
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
          m_oMediator.stopScan();
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
        m_oMediator.stopScan();
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
    if (m_oDuplicatesButton.getText().equals("Find Duplicates"))
    {
      m_oDuplicatesButton.setText("Cancel?");
      new Thread(() ->
      {
        findDuplicates();
        Platform.runLater(() ->
        {
          m_oDuplicatesButton.setText("Find Duplicates");
        });
      }, "handleFindDuplicates").start();
    }
    else if (m_oDuplicatesButton.getText().equals("Cancel?"))
    {
      m_oDuplicatesButton.setText("Find Duplicates");
      cancelDuplicationSearch();
    }
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

  public void cancelDuplicationSearch()
  {
    char cDriveLetter = (m_cmbDuplicateIndex.getValue() + "").toCharArray()[0];
    cIndex.cancelDuplicationSearch(cDriveLetter);
  }

  public void findDuplicates()
  {
    m_oDuplicatesTable.getItems().clear();
    char cDriveLetter = (m_cmbDuplicateIndex.getValue() + "").toCharArray()[0];
    HashMap<String, ArrayList<eDocument>> oDuplicates = cIndex.findDuplicates(cDriveLetter);
    Set<String> keySet = oDuplicates.keySet();
    keySet.stream().map((sHash) -> oDuplicates.get(sHash)).forEachOrdered((lsDocs) ->
    {
      lsDocs.forEach((oDoc) ->
      {
        m_oDuplicatesTable.getItems().add(oDoc);
      });
    });
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
        m_oNumberOfDocumentsLabel.setText("Number of Documents: " + m_oNumberFormat.format(iDocuments));
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

      cProgressPanelFx[] lsPanels = m_oMediator.listDrives();
      for (cProgressPanelFx oPanel : lsPanels)
      {
        m_oDriveList.getItems().add(oPanel.getParent());
        m_cmbDuplicateIndex.getItems().add(oPanel.getRoot());
        m_cmbSearchIndex.getItems().add(oPanel.getRoot());
        m_cmbIndexDirectories.getItems().add(oPanel.getRoot());
      }

      if (m_cmbDuplicateIndex.getItems().size() > 0)
      {
        m_cmbDuplicateIndex.setValue(m_cmbDuplicateIndex.getItems().get(iSelectedDuplicateIndex));
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
    lsDocuments.forEach((oDoc) ->
    {
      m_oTotDocsTable.getItems().add(oDoc);
    });
  }

  public void setResults(ArrayList<eDocument> lsResults)
  {
    m_oResultTable.getItems().clear();
    lsResults.forEach((oDoc) ->
    {
      //      Map<String, Object> oDataRow = new HashMap<>();
//      oDataRow.put(eDocument.TAG_Path, oDoc.sFilePath);
//      oDataRow.put(eDocument.TAG_Filename, oDoc.sFileName);
//      oDataRow.put(eDocument.TAG_Extension, oDoc.sFileExtension);
//      oDataRow.put(eDocument.TAG_Category, oDoc.sFileCategory);
//      oDataRow.put(eDocument.TAG_Size, new SizeProperty(oDoc.lFileSize));
//      if (cConfig.instance().getHashDocuments())
//      {
//        oDataRow.put(eDocument.TAG_Hash, oDoc.sFileHash);
//      }
      m_oResultTable.getItems().add(oDoc);
    });
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

  private void handleContextMenuOpenLocation(eDocument oDoc)
  {
    if (oDoc != null)
    {
      try
      {
        String sPath = (String) oDoc.pathProperty().get();
        Desktop.getDesktop().open(new File(sPath));
      }
      catch (IOException ex)
      {
        Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private void handleContextMenuOpenFile(eDocument oDoc)
  {
    if (oDoc != null)
    {
      try
      {
        String sPath = (String) oDoc.pathProperty().get();
        String sFilename = (String) oDoc.filenameProperty().get();
        String sAbsolutePath = sPath + File.separator + sFilename;
        Object sExtension = oDoc.extensionProperty().get();
        if (sExtension != null && !(sExtension + "").isEmpty())
        {
          sAbsolutePath += "." + sExtension;
        }
        Desktop.getDesktop().open(new File(sAbsolutePath));
      }
      catch (IOException ex)
      {
        Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private void handleContextMenuDeleteFile(eDocument oDoc)
  {
    if (oDoc != null)
    {
      String sPath = (String) oDoc.pathProperty().get();
      String sFilename = (String) oDoc.filenameProperty().get();
      String sAbsolutePath = sPath + File.separator + sFilename;
      Object sExtension = oDoc.extensionProperty().get();
      if (sExtension != null && !(sExtension + "").isEmpty())
      {
        sAbsolutePath += "." + sExtension;
      }
      File oFile = new File(sAbsolutePath);
      oFile.delete();
      File oDirectory = new File(sPath);
      if (oDirectory.list().length == 0)
      {
        oDirectory.delete();
      }
    }
  }

  private void handleContextMenuMarkFile(eDocument oDoc)
  {
    if (oDoc != null)
    {
      try
      {
        String sPath = (String) oDoc.pathProperty().get();
        String sFilename = (String) oDoc.filenameProperty().get();
        String sAbsolutePath = sPath + File.separator + sFilename;
        Object sExtension = oDoc.extensionProperty().get();
        if (sExtension != null && !(sExtension + "").isEmpty())
        {
          sAbsolutePath += "." + sExtension;
        }
        Desktop.getDesktop().open(new File(sAbsolutePath));
      }
      catch (IOException ex)
      {
        Logger.getLogger(cMainLayoutController.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
