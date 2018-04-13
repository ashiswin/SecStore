package components;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import singleton.Global;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class GridFileItem extends VBox {
    int id;
    String filename;
    int size;
    String checksum;
    int owner;
    ImageView thumbnail;

    public GridFileItem(int id, String filename, int size, String checksum, int owner) {
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-border-color: #ca1010; -fx-border-width:2");
        this.id = id;
        this.filename = filename;
        this.size = size;
        this.checksum = checksum;
        this.owner = owner;
        initialize();
        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY){
                    System.out.println(filename);
                    RightClickMenu menu = new RightClickMenu(id,filename);
                    menu.show(Global.getInstance().getCurrentScene().getWindow(),event.getScreenX(),event.getScreenY());

                }
            }
        });
    }

    private void initialize(){
        ImageView thumbnail = new ImageView();
        thumbnail.setFitHeight(100);
        thumbnail.setFitWidth(100);
        Label filename = new Label();
        filename.setText(this.filename);
        filename.setAlignment(Pos.CENTER);
        filename.setTextAlignment(TextAlignment.CENTER);
        filename.setTextFill(Color.web("#ca1010"));
        filename.setFont(Font.font("Segoe UI",16));

        this.getChildren().addAll(thumbnail,filename);
    }
}
