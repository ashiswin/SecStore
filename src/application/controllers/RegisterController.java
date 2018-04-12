package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


/**
 * Created by Oon Tong on 4/12/2018.
 */
public class RegisterController {
    @FXML TextField firstnameTextInput;
    @FXML TextField lastnameTextInput;
    @FXML TextField usernameTextInput;
    @FXML PasswordField passwordTextInput;
    @FXML PasswordField confirmpasswordTextInput;
    @FXML TextField errorTextInput;
    private Scene loginScene;

    public RegisterController setLoginScene(Scene loginScene) {
        this.loginScene = loginScene;
        return this;
    }

    public void handleBack(ActionEvent event){
        clearInput();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(loginScene);
    }

    public void handleConfirm(ActionEvent event){
        if (passwordTextInput.getText() != confirmpasswordTextInput.getText()) {
            errorTextInput.setDisable(false);
            errorTextInput.setText("Your passwords do not match!");
            return;
        }

        clearInput();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(loginScene);
    }

    public void handleExit(){
        Stage stage = (Stage)usernameTextInput.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void clearInput(){
        usernameTextInput.setText("");
        lastnameTextInput.setText("");
        firstnameTextInput.setText("");
        passwordTextInput.setText("");
        confirmpasswordTextInput.setText("");
        errorTextInput.setText("");
        errorTextInput.setDisable(true);
    }
}
