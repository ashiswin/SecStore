package application.controllers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import application.api.HttpRequest;
import application.singleton.Global;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class LoginController {
    @FXML private TextField usernameTextInput;
    @FXML private PasswordField passwordTextInput;
    @FXML private javafx.scene.control.Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private ProgressIndicator loginProgress;
    @FXML private Label errorLabel;

    private Scene registerScene;
    private Scene dashboardScene;
    private DashboardController dashboardController;
    
    public LoginController setRegisterScene(Scene registerScene) {
        this.registerScene = registerScene;
        return this;
    }

    public LoginController setDashboardScene(Scene dashboardScene) {
        this.dashboardScene = dashboardScene;
        return this;
    }
    
    public void setDashboardController(DashboardController dashboardController) {
    	this.dashboardController = dashboardController;
    }

    public void handleLogin(ActionEvent event) {
        String username = usernameTextInput.getText();
        String password = passwordTextInput.getText();

        attemptLogin(username,password);
    }

    private void attemptLogin(String username, String password){
        loginProgress.setVisible(true);
        errorLabel.setVisible(false);

        Task loginTask = new Task() {
            @Override
            protected Object call() throws Exception {
                System.out.println("attempting login");
                Object result = HttpRequest.authRequest(username,password);
                //Do authentication

                return result;
            }
        };

        Task getFilesTask = new Task(){

            @Override
            protected Object call() throws Exception {
                return HttpRequest.getFilesRequest(Global.getInstance().getId());
            }
        };

        loginTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JSONObject result = (JSONObject) loginTask.getValue();

                try {
                    if ((boolean)result.get("success")){
                        Global global = Global.getInstance();
                        global.setId(Integer.parseInt((String)result.get("id")));
                        global.setKey((String)result.get("key"));
                        global.setFirstname((String)result.get("firstname"));
                        global.setAuth(result);
                        new Thread(getFilesTask).start();
                    } else {
                        errorLabel.setVisible(true);
                        loginProgress.setVisible(false);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        getFilesTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JSONArray fileList = (JSONArray) getFilesTask.getValue();
                try {
                    if (fileList != null) {
                        Global global = Global.getInstance();
                        global.setFileList(fileList);
                        Global.getInstance().setCurrentScene(dashboardScene);
                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setScene(dashboardScene);
                        dashboardController.handleRefresh(null);
                        resetScene();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        loginProgress.progressProperty().bind(loginTask.progressProperty());
        new Thread(loginTask).start();
    }

    public void handleRegister(){
        resetScene();
        Stage stage = (Stage)registerLink.getScene().getWindow();
        stage.setScene(registerScene);

    }




    public void handleExit(){
        Stage stage = (Stage)usernameTextInput.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void resetScene(){
        usernameTextInput.setText("");
        passwordTextInput.setText("");
        errorLabel.setVisible(false);
        loginProgress.setVisible(false);
    }
}
