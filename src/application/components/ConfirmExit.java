package components;

import controllers.ConfirmExitController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.*;

import java.io.IOException;

/**
 * Created by Oon Tong on 4/12/2018.
 */
public class ConfirmExit {

    public static void display(FXMLLoader loader){
        ConfirmExitController controller = loader.getController();
        try {
            Pane confirmExitPane = loader.load();
            Stage window = new Stage();
            window.initStyle(StageStyle.UNDECORATED);
            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle("Are you sure?");
            window.setMinWidth(250);
            window.setScene(new Scene(confirmExitPane,250,200));
            window.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
