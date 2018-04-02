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
package driveindex.ui.swing;

import driveindex.injection.cInjector;
import driveindex.lucene.cLuceneIndexReader;
import driveindex.lucene.eDocument;
import driveindex.lucene.eSearchField;
import driveindex.ui.fx.cMainLayoutController;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Observable;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 *
 * @author Philip M. Trenwith
 */
public class cSearchTable extends Observable
{
  private final String[] lsHeader = new String[] 
      { eDocument.TAG_Path, eDocument.TAG_Filename, eDocument.TAG_Extension, eDocument.TAG_Category, eDocument.TAG_Size };
  private final JScrollPane oScrollPane;
  private final JTable oTable;
  private JPopupMenu oPopupMenu;
  
  public cSearchTable()
  {
    String data[][]={ {"", "", "", "", ""} };    
    String column[]=lsHeader;
    oTable = new JTable(data, column);
    oTable.setRowHeight(22);
    oScrollPane = new JScrollPane(oTable);
    oScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    oScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    //oTable.addKeyListener(new cKeyListener());
    
    cPopupMenuActionListener oPopupMenuActionListener = new cPopupMenuActionListener();
    oPopupMenu = new JPopupMenu();
    JMenuItem oMenuItemSearch = new JMenuItem("Search");
    oMenuItemSearch.addActionListener(oPopupMenuActionListener);
    oMenuItemSearch.setActionCommand("search");
    oPopupMenu.add(oMenuItemSearch);
    JMenuItem oMenuItemClear = new JMenuItem("Clear");
    oMenuItemClear.addActionListener(oPopupMenuActionListener);
    oMenuItemClear.setActionCommand("clear");
    oPopupMenu.add(oMenuItemClear);
    
    oTable.addMouseListener(new MouseAdapter() 
    {
      @Override
      public void mouseReleased(MouseEvent e) 
      {
        if (e.getButton() != 0 && e.getButton() != 1 && oPopupMenu != null) 
        {
          oPopupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
        }
      }
    });
  }
  
  public void search()
  {
    setStatus("Searching...");
    String sPath = oTable.getValueAt(0, 0)+"";
    String sFilename = oTable.getValueAt(0, 1)+"";
    String sExtension = oTable.getValueAt(0, 2)+"";
    String sCategory = oTable.getValueAt(0, 3)+"";
    String sSize = oTable.getValueAt(0, 4)+"";
    
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
    
    cMainLayoutController oController = cInjector.getInjector().getInstance(cMainLayoutController.class);
    cLuceneIndexReader oReader = cLuceneIndexReader.instance();
    ArrayList<eDocument> lsResults = oReader.search(lsSearchFields, oController.getWholeWords(), oController.getCaseSensitive());
    oController.setResults(lsResults);
    setStatus("");
  }
  
  public JScrollPane getTable()
  {
    return oScrollPane;
  }
  
  public void setStatus(String sStatus)
  {
    System.out.println(sStatus);
    setChanged();
    notifyObservers(sStatus);
  }
  
  private class cKeyListener implements KeyListener
  {
    @Override
    public void keyTyped(KeyEvent e)
    {
      // Do nothing
      System.out.println(e.getKeyChar());
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
      // Do nothing
      System.out.println(e.getKeyChar());
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
      if (e.getKeyCode() == 10) // Enter
      {
        new Thread(() -> {search();}).start();
      }
    }
  }
  
  private class cPopupMenuActionListener implements ActionListener
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      switch (e.getActionCommand()) 
      {
        case "search":
          new Thread(() -> {search();}).start();
          break;
        case "clear":
          TableModel model = oTable.getModel();
          model.setValueAt("", 0, 0);
          model.setValueAt("", 0, 1);
          model.setValueAt("", 0, 2);
          model.setValueAt("", 0, 3);
          model.setValueAt("", 0, 4);
          break;
      }
    }
  }
}
