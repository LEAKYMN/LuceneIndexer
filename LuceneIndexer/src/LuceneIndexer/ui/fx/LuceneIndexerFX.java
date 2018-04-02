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

import LuceneIndexer.injection.cInjector;
import LuceneIndexer.linux.cLinux;
import LuceneIndexer.scanner.cDriveMediator;
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
  private cMainLayoutController oMainLayoutController;

  @Override
  public void start(Stage oStage) throws Exception
  {
    FXMLLoader oLoader = new FXMLLoader(getClass().getResource("cMainLayout.fxml"));
    Parent oRoot = oLoader.load();
    Scene oScene = new Scene(oRoot);
    oStage.setTitle("Drive Index");
    oStage.setX(50);
    oStage.setY(20);
    oStage.setScene(oScene);
    oStage.show();

    oStage.setOnCloseRequest(new EventHandler<WindowEvent>()
    {
      @Override
      public void handle(WindowEvent t)
      {
        String sOperatingSystem = System.getProperty("os.name");
        if (sOperatingSystem.equalsIgnoreCase("Linux"))
        {
          cLinux.unmountMountedDrives();
        }
        cDriveMediator.instance().closeIndexWriter();
        System.exit(0);
      }
    });

    oMainLayoutController = oLoader.<cMainLayoutController>getController();
    cInjector oInjector = new cInjector(this, oMainLayoutController);
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    launch(args);
  }
}
