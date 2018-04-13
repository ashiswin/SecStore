package singleton;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class Global {
    private int id;
    private String key;
    private String firstname;

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
        return this;
    }

    private static Global global = new Global();

    public static Global getInstance(){
        return global;
    }
}
