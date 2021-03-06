package application.controllers;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * Created by Oon Tong on 4/12/2018.
 */
public class ConfirmExitController {
    boolean isClose = false;

    public void handleYes(ActionEvent event){
        isClose = true;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
    public void handleNo(ActionEvent event){
        isClose = false;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public boolean getResult(){
        return isClose;
    }
}
