
package driveindex.ui.fx;

import driveindex.injection.cInjector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Philip Trenwith
 */
public class DriveIndexFx extends Application
{
  private cMainLayoutController oMainLayoutController;
  
  @Override
  public void start(Stage oStage) throws Exception
  {
    FXMLLoader oLoader = new FXMLLoader(getClass().getResource("cMainLayout.fxml"));
    Parent oRoot = oLoader.load();
    Scene oScene = new Scene(oRoot);
    oStage.setTitle("Drive Index");
    oStage.setScene(oScene);
    oStage.show();
    
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
