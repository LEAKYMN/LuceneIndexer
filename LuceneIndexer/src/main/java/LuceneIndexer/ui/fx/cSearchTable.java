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
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.lucene.eDocument;
import LuceneIndexer.lucene.eSearchField;
import LuceneIndexer.lucene.cIndex;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;

/**
 *
 * @author Philip M. Trenwith
 */
public class cSearchTable
{
  private TableView<SearchField> m_oSearchTable = new TableView<>();

  private final ObservableList<SearchField> m_oSearchData = FXCollections.observableArrayList(
          new SearchField("", "", "", "", "", "")
  );
  private ContextMenu m_oContextMenu;
  private MenuItem m_oMenuItemSearch;
  private MenuItem m_oMenuItemClear;

  public cSearchTable(double dColumnWidth)
  {
    m_oSearchTable.setEditable(true);
    m_oSearchTable.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
    {
      @Override
      public void handle(KeyEvent e)
      {
        if (e.getCode() == KeyCode.ENTER)
        {
          executeSearchOnNewThread();
        }
      }
    });

    Callback<TableColumn<SearchField, String>, TableCell<SearchField, String>> cellFactory
            = (TableColumn<SearchField, String> param) -> new EditingCell();

    TableColumn<SearchField, String> oPathCol = new TableColumn(eDocument.TAG_Path);
    oPathCol.setMinWidth(dColumnWidth);
    oPathCol.setCellValueFactory(cellData -> cellData.getValue().pathProperty());
    oPathCol.setCellFactory(cellFactory);
    oPathCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setPath(t.getNewValue());
    });

    TableColumn<SearchField, String> oFilenameCol = new TableColumn(eDocument.TAG_Filename);
    oFilenameCol.setMinWidth(dColumnWidth);
    oFilenameCol.setCellValueFactory(cellData -> cellData.getValue().filenameProperty());
    oFilenameCol.setCellFactory(cellFactory);
    oFilenameCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setFilename(t.getNewValue());
    });

    TableColumn<SearchField, String> oExtensionCol = new TableColumn(eDocument.TAG_Extension);
    oExtensionCol.setMinWidth(dColumnWidth);
    oExtensionCol.setCellValueFactory(cellData -> cellData.getValue().extensionProperty());
    oExtensionCol.setCellFactory(cellFactory);
    oExtensionCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setExtension(t.getNewValue());
    });

    TableColumn<SearchField, String> oCategoryCol = new TableColumn(eDocument.TAG_Category);
    oCategoryCol.setMinWidth(dColumnWidth);
    oCategoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
    oCategoryCol.setCellFactory(cellFactory);
    oCategoryCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setCategory(t.getNewValue());
    });

    TableColumn<SearchField, String> oSizeCol = new TableColumn(eDocument.TAG_Size);
    oSizeCol.setMinWidth(dColumnWidth);
    oSizeCol.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
    oSizeCol.setCellFactory(cellFactory);
    oSizeCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setSize(t.getNewValue());
    });
    
    TableColumn<SearchField, String> oHashCol = new TableColumn(eDocument.TAG_Hash);
    oHashCol.setMinWidth(dColumnWidth);
    oHashCol.setCellValueFactory(cellData -> cellData.getValue().hashProperty());
    oHashCol.setCellFactory(cellFactory);
    oHashCol.setOnEditCommit(
            (TableColumn.CellEditEvent<SearchField, String> t)
            -> 
            {
              ((SearchField) t.getTableView().getItems()
                      .get(t.getTablePosition().getRow()))
                      .setHash(t.getNewValue());
    });

    m_oSearchTable.setItems(m_oSearchData);
    m_oSearchTable.getColumns().addAll(oPathCol, oFilenameCol, oExtensionCol, 
            oCategoryCol, oSizeCol);
    if (cConfig.instance().getHashDocuments())
    {
      m_oSearchTable.getColumns().add(oHashCol);
    }
    m_oSearchTable.setFixedCellSize(28);
    m_oSearchTable.prefHeightProperty().bind(Bindings.size(m_oSearchTable.getItems()).
            multiply(m_oSearchTable.getFixedCellSize()).add(30));

    // create the context menu
    cContextMenuEventHandler oContextMenuEventHandler = new cContextMenuEventHandler();
    m_oContextMenu = new ContextMenu();

    m_oMenuItemSearch = new MenuItem("Search");
    m_oMenuItemSearch.setOnAction(oContextMenuEventHandler);
    m_oMenuItemSearch.setId("search");

    m_oMenuItemClear = new MenuItem("Clear");
    m_oMenuItemClear.setOnAction(oContextMenuEventHandler);
    m_oMenuItemClear.setId("clear");

    m_oContextMenu.getItems().addAll(m_oMenuItemSearch, m_oMenuItemClear);
    m_oSearchTable.setContextMenu(m_oContextMenu);
  }

  public TableView getTable()
  {
    return m_oSearchTable;
  }

  public void executeSearchOnNewThread()
  {
    new Thread(()
            -> 
            {
              try
              {
                Thread.sleep(100);
              }
              catch (InterruptedException ex)
              {
                Logger.getLogger(cSearchTable.class.getName()).log(Level.SEVERE, null, ex);
              }
              search();
    }, "handleSearch").start();
  }

  public void search()
  {
    cMainLayoutController oUIController = cInjector.getInjector().getInstance(cMainLayoutController.class);
    // clear the results table
    oUIController.setResults(new ArrayList());
            
    SearchField oSearchField = m_oSearchData.get(0);
    String sPath = oSearchField.sPath.get();
    String sFilename = oSearchField.sFilename.get();
    String sExtension = oSearchField.sExtension.get();
    String sCategory = oSearchField.sCategory.get();
    String sSize = oSearchField.sSize.get();
    String sHash = oSearchField.sHash.get();

    System.out.println("Searching... (" + sPath + ":" + sFilename + ":" + sExtension + ":" + sCategory + ":" + sSize + ":" + sHash + ")");
    oUIController.setStatus("Searching... (" + sPath + ":" + sFilename + ":" + sExtension + ":" + sCategory + ":" + sSize + ":" + sHash + ")");

    ArrayList<eSearchField> lsSearchFields = new ArrayList();
    if (!sPath.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Path, sPath));
    }
    if (!sFilename.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Filename, sFilename));
    }
    if (!sExtension.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Extension, sExtension));
    }
    if (!sCategory.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Category, sCategory));
    }
    if (!sSize.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Size, sSize));
    }
    if (!sHash.isEmpty())
    {
      lsSearchFields.add(new eSearchField(eDocument.TAG_Hash, sHash));
    }

    String sIndex = oUIController.getIndex();
    ArrayList<eDocument> lsResults;
    if (sIndex.equals("All"))
    {
      lsResults = cIndex.searchAll(lsSearchFields, oUIController.getWholeWords(), oUIController.getCaseSensitive());
    }
    else
    {
      char cDriveLetter = sIndex.toCharArray()[0];
      lsResults = cIndex.search(cDriveLetter, lsSearchFields, oUIController.getWholeWords(), oUIController.getCaseSensitive());
    }
    
    oUIController.setResults(lsResults);
    oUIController.setStatus("");
  }

  private class SearchField
  {
    private final SimpleStringProperty sPath;
    private final SimpleStringProperty sFilename;
    private final SimpleStringProperty sExtension;
    private final SimpleStringProperty sCategory;
    private final SimpleStringProperty sSize;
    private final SimpleStringProperty sHash;

    public SearchField(String sPath, String sFilename, String sExtension, 
            String sCategory, String sSize, String sHash)
    {
      this.sPath = new SimpleStringProperty(sPath);
      this.sFilename = new SimpleStringProperty(sFilename);
      this.sExtension = new SimpleStringProperty(sExtension);
      this.sCategory = new SimpleStringProperty(sCategory);
      this.sSize = new SimpleStringProperty(sSize);
      this.sHash = new SimpleStringProperty(sHash);
    }

    public String getPath()
    {
      return sPath.get();
    }

    public StringProperty pathProperty()
    {
      return this.sPath;
    }

    public void setPath(String sPath)
    {
      this.sPath.set(sPath);
    }

    public String getFilename()
    {
      return sFilename.get();
    }

    public StringProperty filenameProperty()
    {
      return this.sFilename;
    }

    public void setFilename(String sFilename)
    {
      this.sFilename.set(sFilename);
    }

    public String getExtension()
    {
      return sExtension.get();
    }

    public StringProperty extensionProperty()
    {
      return this.sExtension;
    }

    public void setExtension(String sExtension)
    {
      this.sExtension.set(sExtension);
    }

    public String getCategory()
    {
      return sCategory.get();
    }

    public StringProperty categoryProperty()
    {
      return this.sCategory;
    }

    public void setCategory(String sCategory)
    {
      this.sCategory.set(sCategory);
    }

    public String getSize()
    {
      return sSize.get();
    }

    public StringProperty sizeProperty()
    {
      return this.sSize;
    }

    public void setSize(String sSize)
    {
      this.sSize.set(sSize);
    }
    
    public String getHash()
    {
      return sHash.get();
    }

    public StringProperty hashProperty()
    {
      return this.sHash;
    }

    public void setHash(String sHash)
    {
      this.sHash.set(sHash);
    }
  }

  private class EditingCell extends TableCell<SearchField, String>
  {
    private TextField m_oTextField;

    private EditingCell()
    {
      createTextField();
    }

    @Override
    public void startEdit()
    {
      if (!isEmpty())
      {
        super.startEdit();
        setText(null);
        setGraphic(m_oTextField);
        m_oTextField.selectAll();
      }
    }

    @Override
    public void cancelEdit()
    {
      super.cancelEdit();

      setText((String) getItem());
      setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty)
    {
      super.updateItem(item, empty);

      if (empty)
      {
        setText(item);
        setGraphic(null);
      }
      else if (isEditing())
      {
        if (m_oTextField != null)
        {
          m_oTextField.setText(getString());
//                        setGraphic(null);
        }
        setText(null);
        setGraphic(m_oTextField);
      }
      else
      {
        setText(getString());
        setGraphic(null);
      }
    }

    private void createTextField()
    {
      m_oTextField = new TextField(getString());

      m_oTextField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
      m_oTextField.setOnAction((e) -> commitEdit(m_oTextField.getText()));
      m_oTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
              -> 
              {
                if (!newValue)
                {
                  commitEdit(m_oTextField.getText());
                }
      });
    }

    private String getString()
    {
      return getItem() == null ? "" : getItem();
    }
  }

  private class cContextMenuEventHandler implements EventHandler<ActionEvent>
  {
    @Override
    public void handle(ActionEvent event)
    {
      MenuItem oSource = (MenuItem) event.getSource();

      switch (oSource.getId())
      {
        case "search":
          search();
          break;
        case "clear":
          SearchField oSearchField = m_oSearchData.get(0);
          oSearchField.setPath("");
          oSearchField.setFilename("");
          oSearchField.setExtension("");
          oSearchField.setCategory("");
          oSearchField.setSize("");
          oSearchField.setHash("");
          break;

        default:
          break;
      }
    }
  }
}
