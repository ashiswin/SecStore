package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

public class LoginPageController {
	@FXML private TextField txtUsername;
    @FXML private TextField txtPassword;
    @FXML private Separator sepUsername;
    @FXML private Separator sepPassword;
    
    public void initialize() {
        txtUsername.focusedProperty().addListener((ov, oldV, newV) -> {
		   if (!newV) { // focus lost
			   sepUsername.setStyle("-fx-base: #bebebe;");
		   }
		   else {
			      sepUsername.setStyle("-fx-base: #f3f3f3");
		   }
        });
        txtPassword.focusedProperty().addListener((ov, oldV, newV) -> {
 		   if (!newV) { // focus lost
 			   sepPassword.setStyle("-fx-base: #bebebe;");
 		   }
 		   else {
  		      sepPassword.setStyle("-fx-base: #f3f3f3");
 		   }
         });
    }
    @FXML protected void btnLogin_Pressed(ActionEvent event) {
        // TODO: Perform login
    }
}
