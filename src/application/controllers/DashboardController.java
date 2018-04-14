package application.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import application.api.HttpRequest;
import application.client.SplitChunksClient;
import application.components.GridFileItem;
import application.components.GridFilePane;
import application.singleton.Global;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
    GridFilePane gridFilePane;
    
    private Scene loginScene;
    private boolean highestToLowest = false;
    @FXML
    public void initialize(){
        gridFilePane = new GridFilePane();
        filePane.setContent(gridFilePane);
        filePane.setPadding(new Insets(10, 10, 10, 10));
        gridFilePane.setHgap(10);
        gridFilePane.setVgap(10);
        gridFilePane.setPrefColumns(4);
        
    	searchTextInput.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				ObservableList<Node> children = gridFilePane.getChildren();
				for(Node n : children) {
					GridFileItem i = (GridFileItem) n;
					if(i.filename.contains(newValue) || newValue.equals("")) {
						i.setManaged(true);
						i.setVisible(true);
					}
					else {
						i.setManaged(false);
						i.setVisible(false);
					}
				}
			}
    	});


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
                	for(File f : db.getFiles()) {
                		SplitChunksClient.runSplitChunks(f.getAbsolutePath().replace("\\","/"), Global.getInstance().getAuth());
                	}
                    handleRefresh(null);
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
        if (file != null) {
            SplitChunksClient.runSplitChunks(file.getAbsolutePath().replace("\\","/"), Global.getInstance().getAuth());
            handleRefresh(null);
        }
    }

    public void handleExit(){
        Stage stage = (Stage)logoutButton.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
    }


    public void handleDirectionToggle(ActionEvent event){
    	highestToLowest = directionToggle.isSelected();
    	
        directionImage.setImage(new Image("./application/assets/images/arrow-" + (highestToLowest ? "up" : "down") + ".png"));

    	gridFilePane.getChildren().clear();
        gridFilePane.initialize(sortedFileList());
    }

    public void handleViewToggle(ActionEvent event){
        viewImage.setImage(new Image("./application/assets/images/" + ((viewToggle.isSelected() ? "th" : "list")) + ".png"));
    }

    public void handleSearch(ActionEvent event){
        searchTextInput.setText("");
    }
    
    public void handleRefresh(ActionEvent event){
        Task<JSONArray> getFilesTask = new Task<>(){

            @Override
            protected JSONArray call() throws Exception {
                return HttpRequest.getFilesRequest(Global.getInstance().getId());
            }
        };
        new Thread(getFilesTask).start();
        getFilesTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JSONArray fileList = (JSONArray) getFilesTask.getValue();
                Global.getInstance().setFileList(fileList);
                try {
                    if (fileList != null) {
                    	gridFilePane.getChildren().clear();
                        gridFilePane.initialize(sortedFileList());
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    
    public JSONArray sortedFileList() {
    	ArrayList<JSONObject> children = new ArrayList<>();
    	for(int i = 0; i < Global.getInstance().getFileList().length(); i++) {
    		try {
				children.add(Global.getInstance().getFileList().getJSONObject(i));
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	Collections.sort(children, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					return o1.getString("filename").toLowerCase().compareTo(o2.getString("filename").toLowerCase()) * ((highestToLowest) ? -1 : 1);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return 0;
			}
    	});
    	JSONArray sorted = new JSONArray();
    	for (JSONObject o : children) {
    		sorted.put(o);
        }
    	
    	return sorted;
    }
}
