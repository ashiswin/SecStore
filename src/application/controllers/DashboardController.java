package controllers;

import client.SplitChunksClient;
import common.protocols.SplitChunks;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import singleton.Global;

import java.io.File;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class DashboardController {
    @FXML Button logoutButton;
    @FXML ToggleButton directionToggle;
    @FXML ToggleButton viewToggle;
    @FXML ImageView directionImage;
    @FXML ImageView viewImage;
    private Scene loginScene;

    public DashboardController setLoginScene(Scene loginScene) {
        this.loginScene = loginScene;
        return this;
    }

    public void handleLogout(){
        Global.getInstance().reset();
        System.out.println(Global.getInstance().getKey());
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.setScene(loginScene);
    }

    public void handleUpload(ActionEvent event){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file to upload");
        File file = fileChooser.showOpenDialog(logoutButton.getScene().getWindow());
        SplitChunksClient.runSplitChunks(file.getAbsolutePath());
    }

    public void handleExit(){
        Stage stage = (Stage)logoutButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
    }


    public void handleDirectionToggle(ActionEvent event){
        directionImage.setImage(new Image("./assets/images/arrow-" + ((directionToggle.isSelected() ? "up" : "down")) + ".png"));
    }

    public void handleViewToggle(ActionEvent event){
        viewImage.setImage(new Image("./assets/images/" + ((viewToggle.isSelected() ? "th" : "list")) + ".png"));
    }
}
