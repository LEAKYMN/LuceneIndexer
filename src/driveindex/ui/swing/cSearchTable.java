
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
    
    oTable.addKeyListener(new cKeyListener());
    
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
        if (e.isPopupTrigger() && oPopupMenu != null) 
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
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
      // Do nothing
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
