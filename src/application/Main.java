import controllers.ConfirmExitController;
import controllers.DashboardController;
import controllers.LoginController;
import controllers.RegisterController;
import javafx.application.Application;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Main extends Application {
    Stage window;
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage primaryStage) throws Exception{
        window = primaryStage;
        window.initStyle(StageStyle.UNDECORATED);

        FXMLLoader loginPaneLoader = new FXMLLoader(getClass().getResource("./fxml/LoginScene.fxml"));
        Parent loginPane = loginPaneLoader.load();
        Scene loginScene = new Scene(loginPane, 800, 600);
        loginPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        loginPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                window.setX(event.getScreenX() - xOffset);
                window.setY(event.getScreenY() - yOffset);
            }
        });

        FXMLLoader dashboardPaneLoader = new FXMLLoader(getClass().getResource("./fxml/DashboardScene.fxml"));
        System.out.println(getClass().getResource("./fxml/DashboardScene.fxml"));
        Parent dashboardPane = dashboardPaneLoader.load();
        Scene dashboardScene = new Scene(dashboardPane, 800, 600);
        dashboardPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        dashboardPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                window.setX(event.getScreenX() - xOffset);
                window.setY(event.getScreenY() - yOffset);
            }
        });

        FXMLLoader registerPaneLoader = new FXMLLoader(getClass().getResource("./fxml/RegisterScene.fxml"));
        Parent registerPane = registerPaneLoader.load();
        Scene registerScene = new Scene(registerPane, 800, 600);
        registerPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        registerPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                window.setX(event.getScreenX() - xOffset);
                window.setY(event.getScreenY() - yOffset);
            }
        });

        LoginController loginController = loginPaneLoader.getController();
        loginController.setDashboardScene(dashboardScene);
        loginController.setRegisterScene(registerScene);

        RegisterController RegisterController = registerPaneLoader.getController();
        RegisterController.setLoginScene(loginScene);

        DashboardController dashboardController = dashboardPaneLoader.getController();
        dashboardController.setLoginScene(loginScene);

        window.setOnCloseRequest(e -> {
            e.consume();
            closeProgram();
        });


        primaryStage.setScene(loginScene);
        primaryStage.setTitle("SecStore");
        primaryStage.show();
    }
    public void closeProgram() {
            FXMLLoader confirmExitLoader = new FXMLLoader(getClass().getResource("./fxml/ConfirmExit.fxml"));
            ConfirmExit.display(confirmExitLoader);
        ConfirmExitController controller = confirmExitLoader.getController();
            if (controller.getResult())
                window.close();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
