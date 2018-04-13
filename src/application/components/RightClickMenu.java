package components;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import singleton.Global;

import java.io.File;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class RightClickMenu extends ContextMenu{
    int id;
    String filename;

    public RightClickMenu(int id, String filename) {
        this.id = id;
        this.filename = filename;
        MenuItem save = new MenuItem("Save");
        save.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("saving " + filename);
                FileChooser fc = new FileChooser();
                String[] fileSplit = filename.split("\\.",2);
                System.out.println(fileSplit[0]);
                System.out.println("*."+fileSplit[1]);
                fc.setInitialFileName(fileSplit[0]);
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileSplit[1],"*."+fileSplit[1]));
                File saveFile = fc.showSaveDialog(Global.getInstance().getCurrentScene().getWindow());
                if (saveFile != null){
                    System.out.println(saveFile.getAbsolutePath());
                    System.out.println(saveFile.getName());
                }

            }
        });
        this.getItems().addAll(save);
    }
}
