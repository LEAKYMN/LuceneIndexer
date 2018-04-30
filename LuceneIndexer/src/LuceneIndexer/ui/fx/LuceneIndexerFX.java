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

import LuceneIndexer.dialogs.cConfirmDialog;
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.linux.cLinux;
import LuceneIndexer.persistance.cSerializationFactory;
import LuceneIndexer.persistance.cWindowBounds;
import LuceneIndexer.drives.cDriveMediator;
import LuceneIndexer.lucene.cIndex;
import java.io.File;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Philip Trenwith
 */
public class LuceneIndexerFX extends Application
{
  private cSerializationFactory m_oSerializationFactory = new cSerializationFactory();
  private cMainLayoutController oMainLayoutController;
  private cWindowBounds oWindowBounds;
  private File fBounds;
  public static Stage m_oStage;

  @Override
  public void start(Stage oStage) throws Exception
  {
    FXMLLoader oLoader = new FXMLLoader(getClass().getResource("cMainLayout.fxml"));
    Parent oRoot = oLoader.load();
    Scene oScene = new Scene(oRoot);
    m_oStage = oStage;
    oStage.setTitle("Lucene Indexer");
    
    oWindowBounds = new cWindowBounds();
    oWindowBounds.setX(50);
    oWindowBounds.setY(50);
    oWindowBounds.setW(1200);
    oWindowBounds.setH(800);
    fBounds = new File("bounds.ser");
    if (fBounds.exists())
    {
      oWindowBounds = (cWindowBounds) m_oSerializationFactory.deserialize(fBounds, false);
    }
    
    oStage.setX(oWindowBounds.getX());
    oStage.setY(oWindowBounds.getY());
    oStage.setWidth(oWindowBounds.getW());
    oStage.setHeight(oWindowBounds.getH());
    
    oStage.setScene(oScene);
    oStage.show();
    
    oStage.setOnCloseRequest(new EventHandler<WindowEvent>()
    {
      @Override
      public void handle(WindowEvent t)
      {
        cConfirmDialog oDialog = new cConfirmDialog(m_oStage, "Are you sure you want to exit?");
        oDialog.showAndWait();
        int result = oDialog.getResult();
        if (result == cConfirmDialog.YES)
        {
          terminate();
        }
        else
        {
          t.consume();
        }
      }
    });

    oMainLayoutController = oLoader.<cMainLayoutController>getController();
    cInjector oInjector = new cInjector(this, oMainLayoutController);
    
    cDriveMediator.instance().loadDrives();
  }

  private void terminate()
  {
    oWindowBounds.setX((int)m_oStage.getX());
    oWindowBounds.setY((int)m_oStage.getY());
    oWindowBounds.setH((int)m_oStage.getHeight());
    oWindowBounds.setW((int)m_oStage.getWidth());
    m_oSerializationFactory.serialize(oWindowBounds, fBounds, false);
    
    cDriveMediator.instance().stopScan();
    
    String sOperatingSystem = System.getProperty("os.name");
    if (sOperatingSystem.equalsIgnoreCase("Linux"))
    {
      cLinux.unmountMountedDrives();
    }
    cIndex.closeIndexWriters();
    System.exit(0);
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    launch(args);
  }
}
