package singleton;

import javafx.scene.Scene;
import org.json.JSONArray;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class Global {
    private int id;
    private String key;
    private String firstname;

    public Scene getCurrentScene() {
        return currentScene;
    }

    public Global setCurrentScene(Scene currentScene) {
        this.currentScene = currentScene;
        return this;
    }

    Scene currentScene;

    public JSONArray getFileList() {
        return fileList;
    }

    public Global setFileList(JSONArray fileList) {
        this.fileList = fileList;
        return this;
    }

    JSONArray fileList;

    public String getFirstname() {
        return firstname;
    }

    public Global setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }



    public int getId() {
        return id;
    }

    public Global setId(int id) {
        this.id = id;
        return this;
    }

    public String getKey() {
        return key;
    }

    public Global setKey(String key) {
        this.key = key;
        return this;
    }

    public Global reset(){
        this.key = null;
        this.id = -1;
        this.firstname = null;
        this.fileList = null;
        return this;
    }

    private static Global global = new Global();

    public static Global getInstance(){
        return global;
    }
}
