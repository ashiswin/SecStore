package controllers;

import api.HttpRequest;
import client.SplitChunksClient;
import common.protocols.SplitChunks;
import components.GridFilePane;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONArray;
import org.json.JSONObject;
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
    @FXML ScrollPane filePane;
    @FXML TextField searchTextInput;
    GridPane gridView;

    private Scene loginScene;

    @FXML
    public void initialize(){



        filePane.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != filePane && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });
        filePane.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    System.out.println(db.getFiles());
                }
                event.setDropCompleted(db.hasFiles());
                event.consume();
            }
        });

    }


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
        if (file != null)
            SplitChunksClient.runSplitChunks(file.getAbsolutePath().replace("\\","/"));
        
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

    public void handleSearch(ActionEvent event){
        searchTextInput.setText("");
    }

    public void handleRefresh(ActionEvent event){
        Task getFilesTask = new Task(){

            @Override
            protected Object call() throws Exception {
                return HttpRequest.getFilesRequest(Global.getInstance().getId());
            }
        };
        new Thread(getFilesTask).start();
        getFilesTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JSONArray fileList = (JSONArray) getFilesTask.getValue();
                try {
                    if (fileList != null) {
                        //TODO
                       //RESET THE GRID PANE;
                        System.out.println("Refresh");
                        filePane.setContent(new GridFilePane(Global.getInstance().getFileList()));
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
