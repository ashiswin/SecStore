package controllers;

import api.HttpRequest;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONException;
import org.json.JSONObject;


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

    private void checkValid(String firstname,String lastname,String username,String password){
        if (!passwordTextInput.getText().equals(confirmpasswordTextInput.getText())) {
            errorType = ERROR_TYPE.DIFF_PASS;
            return;
        } else if (passwordTextInput.getText().length() <= 6){
            errorType = ERROR_TYPE.INVALID_PASS;
            return;
        }

        Task registerTask = new Task() {
            @Override
            protected Object call() throws Exception {
                System.out.println("attempting to register user");
                JSONObject isRegistered = HttpRequest.registerRequest(firstname,lastname,username,password);
                return isRegistered;
            }
        };
        new Thread(registerTask).start();
        registerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JSONObject isRegistered = (JSONObject)registerTask.getValue();
                try {
                    if ((boolean)isRegistered.get("success")){
                        errorType = ERROR_TYPE.NO_ERROR;
                        clearInput();
                        Stage stage = (Stage) usernameTextInput.getScene().getWindow();
                        stage.setScene(loginScene);
                    } else {
                        errorType = ERROR_TYPE.DUP_USER;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void handleBack(ActionEvent event){
        clearInput();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(loginScene);
    }

    public void handleConfirm(ActionEvent event){
        String firstname = firstnameTextInput.getText();
        String lastname = lastnameTextInput.getText();
        String username = usernameTextInput.getText();
        String password = passwordTextInput.getText();
        checkValid(firstname,lastname,username,password);

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
