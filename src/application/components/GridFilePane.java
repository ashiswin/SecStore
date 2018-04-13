package components;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class GridFilePane extends TilePane {
    public GridFilePane(JSONArray fileList) {
        this.setOrientation(Orientation.HORIZONTAL);
        this.setTileAlignment(Pos.CENTER_LEFT);
        this.setPrefColumns(4);
        this.setStyle("-fx-background-color: #cacaca");

        ObservableList children = this.getChildren();
        for (int i = 0; i < fileList.length();i++){
            try {
                JSONObject file = fileList.getJSONObject(i);
                int id = Integer.parseInt((String)file.get("id"));
                String filename = (String)file.get("filename");
                int size = Integer.parseInt((String)file.get("size"));
                String checksum = (String)file.get("checksum");
                int owner = Integer.parseInt((String)file.get("owner"));
                GridFileItem fileItem = new GridFileItem(id,filename,size,checksum,owner);
                children.add(fileItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}
