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

import java.net.URL;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Philip M. Trenwith
 */
public class cConfirmDialog extends Stage
{
  private int iRESULT = -1;
  public static final int YES = 1;
  public static final int NO = 2;
  
  private final int WIDTH_DEFAULT = 300;

  public static final int ICON_INFO = 0;
  public static final int ICON_ERROR = 1;
  public static final int ICON_CONFIRM = 2;

  public cConfirmDialog(Stage owner, String msg)
  {
    setResizable(false);
    setAlwaysOnTop(true);
    initModality(Modality.WINDOW_MODAL);
    initStyle(StageStyle.TRANSPARENT);

    Label label = new Label(msg);
    label.setWrapText(true);
    label.setGraphicTextGap(20);
    label.setFont(new Font(14));
    //label.setGraphic(new ImageView(getImage(type)));

    Button YES_Button = new Button("Yes");
    YES_Button.setPadding(new Insets(0, 20, 10, 20));
    YES_Button.setTextAlignment(TextAlignment.CENTER);
    YES_Button.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        iRESULT = YES;
        cConfirmDialog.this.close();
      }
    });
    Button NO_Button = new Button("No");
    NO_Button.setPadding(new Insets(0, 20, 10, 20));
    NO_Button.setTextAlignment(TextAlignment.CENTER);
    NO_Button.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event)
      {
        iRESULT = NO;
        cConfirmDialog.this.close();
      }
    });

    BorderPane borderPane = new BorderPane();
    URL stylesheet = getClass().getResource("confirm.css");
    if (stylesheet != null)
    {
      borderPane.getStylesheets().add(stylesheet.toExternalForm());
    }
    borderPane.setTop(label);

    HBox hbox2 = new HBox();
    hbox2.setAlignment(Pos.CENTER);
    hbox2.setSpacing(5);
    hbox2.getChildren().add(YES_Button);
    hbox2.getChildren().add(NO_Button);
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
    oScene.getStylesheets().add("/styles/Dialog.css");
    setScene(oScene);

    // make sure this stage is centered on top of its owner
    if (owner != null)
    {
      setX(owner.getX() + (owner.getWidth() / 2 - width / 2));
      setY(owner.getY() + (owner.getHeight() / 2 - height / 2));
    }
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
  
  public int getResult()
  {
    return iRESULT;
  }
}
