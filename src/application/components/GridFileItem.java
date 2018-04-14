package application.components;

import java.io.File;

import application.client.DownloadClient;
import application.singleton.Global;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class GridFileItem extends VBox {
    public int id;
    public String filename;
    public int size;
    public String checksum;
    public int owner;
    public ImageView thumbnail;

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
                else if(event.getButton() == MouseButton.PRIMARY) {
                	FileChooser fileChooser = new FileChooser();
			        fileChooser.setTitle("Choose where to save downloaded file");
			        GridFileItem g = (GridFileItem) event.getSource();
			        File file = fileChooser.showSaveDialog(g.getScene().getWindow());
			        if (file != null) {
			            DownloadClient.runDownload(g.id, file.getAbsolutePath());
			        }
                }
            }
        });
    }

    private void initialize(){
        thumbnail = new ImageView();
        thumbnail.setStyle("-fx-border-width:1; -fx-border-color: #ca1010");
        thumbnail.setFitHeight(177);
        thumbnail.setFitWidth(177);
        if(this.filename.toLowerCase().contains(".png")
        		|| this.filename.toLowerCase().contains(".jpg")
        		|| this.filename.toLowerCase().contains(".jpeg")
        		|| this.filename.toLowerCase().contains(".gif")) {
            thumbnail.setImage(new Image("./application/assets/images/image.png"));
        }
        else {
            thumbnail.setImage(new Image("./application/assets/images/document.png"));
        }
        Label filename = new Label();
        filename.setText(this.filename.substring(0, Math.min(20, this.filename.length())));
        filename.setAlignment(Pos.CENTER);
        filename.setStyle("-fx-border-width:1; -fx-border-color: #ca1010");
        filename.setTextAlignment(TextAlignment.CENTER);
        filename.setTextFill(Color.web("#ca1010"));
        filename.setFont(Font.font("Segoe UI",16));

        this.getChildren().addAll(thumbnail,filename);
    }
}
