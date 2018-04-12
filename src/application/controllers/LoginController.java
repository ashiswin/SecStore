package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;



public class LoginController {
    @FXML private TextField usernameTextInput;
    @FXML private PasswordField passwordTextInput;
    @FXML private javafx.scene.control.Button loginButton;
    @FXML private Hyperlink registerLink;

    private Scene registerScene;
    private Scene dashboardScene;

    public LoginController setRegisterScene(Scene registerScene) {
        this.registerScene = registerScene;
        return this;
    }

    public LoginController setDashboardScene(Scene dashboardScene) {
        this.dashboardScene = dashboardScene;
        return this;
    }

    public void handleLogin(ActionEvent event) {
        String username = usernameTextInput.getText();
        String password = passwordTextInput.getText();
        System.out.println(username + "\n" + password);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(dashboardScene);

        //Auth user
        clearInput();
    }

    public void handleRegister(){
        clearInput();
        Stage stage = (Stage)registerLink.getScene().getWindow();
        stage.setScene(registerScene);

    }


    public void handleExit(){
        Stage stage = (Stage)usernameTextInput.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void clearInput(){
        usernameTextInput.setText("");
        passwordTextInput.setText("");
    }
}
