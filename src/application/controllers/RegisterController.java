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
    ERROR_TYPE errorType = ERROR_TYPE.NO_ERROR;

    public RegisterController setLoginScene(Scene loginScene) {
        this.loginScene = loginScene;
        return this;
    }

    private void checkValid(){
        boolean userValid = true;
        if (!userValid) {
            errorType = ERROR_TYPE.DUP_USER;
        }else if (!passwordTextInput.getText().equals(confirmpasswordTextInput.getText())) {
            errorType = ERROR_TYPE.DIFF_PASS;
        } else if (passwordTextInput.getText().length() <= 6){
            errorType = ERROR_TYPE.INVALID_PASS;
        } else {
            errorType = ERROR_TYPE.NO_ERROR;
        }
    }

    public void handleBack(ActionEvent event){
        clearInput();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(loginScene);
    }

    public void handleConfirm(ActionEvent event){
        checkValid();
        switch (errorType){
            case DUP_USER:
                errorTextInput.setDisable(false);
                errorTextInput.setText("That username is already taken!");
                break;
            case DIFF_PASS:
                errorTextInput.setDisable(false);
                errorTextInput.setText("Your password does not match!");
                break;
            case INVALID_PASS:
                errorTextInput.setDisable(false);
                errorTextInput.setText("Your password is too short!");
                break;
            case NO_ERROR:
                clearInput();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(loginScene);
                break;
        }
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

enum ERROR_TYPE{
    DUP_USER,
    DIFF_PASS,
    INVALID_PASS,
    NO_ERROR
}
