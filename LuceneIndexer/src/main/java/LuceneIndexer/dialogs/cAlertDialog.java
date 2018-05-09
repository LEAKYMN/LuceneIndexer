/*
 * Copyright (C) 2018 0400490
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
package LuceneIndexer.dialogs;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Philip M. Trenwith
 */
public class cAlertDialog extends Stage
{

  private final int WIDTH_DEFAULT = 300;

  public static final int ICON_INFO = 0;
  public static final int ICON_ERROR = 1;

  public cAlertDialog(Stage owner, String msg, int type)
  {
    setResizable(false);
    setAlwaysOnTop(true);
    initModality(Modality.APPLICATION_MODAL);
    initStyle(StageStyle.TRANSPARENT);

    Label label = new Label(msg);
    label.setWrapText(true);
    label.setGraphicTextGap(20);
    //label.setGraphic(new ImageView(getImage(type)));

    Button OK_Button = new Button("OK");
    OK_Button.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        cAlertDialog.this.close();
      }
    });

    BorderPane borderPane = new BorderPane();
    borderPane.getStylesheets().add(getClass().getResource("/styles/Dialog.css").toExternalForm());
    borderPane.setTop(label);

    HBox hbox2 = new HBox();
    hbox2.setAlignment(Pos.CENTER);
    hbox2.getChildren().add(OK_Button);
    borderPane.setBottom(hbox2);

    // calculate width of string
    final Text text = new Text(msg);
    text.snapshot(null, null);
    // + 20 because there is padding 10 left and right
    int width = (int) text.getLayoutBounds().getWidth() + 40;

    if (width < WIDTH_DEFAULT)
    {
      width = WIDTH_DEFAULT;
    }

    int height = 100;

    final Scene oScene = new Scene(borderPane, width, height);
    oScene.setFill(Color.TRANSPARENT);
    oScene.getStylesheets().add("/styles/Styles.css");
    setScene(oScene);

    // make sure this stage is centered on top of its owner
    setX(owner.getX() + (owner.getWidth() / 2 - width / 2));
    setY(owner.getY() + (owner.getHeight() / 2 - height / 2));
  }

  private Image getImage(int type)
  {
    if (type == ICON_ERROR)
    {
      return new Image(getClass().getResourceAsStream("/images/error.png"));
    }
    else
    {
      return new Image(getClass().getResourceAsStream("/images/info.png"));
    }
  }
}
