package application.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Oon Tong on 4/13/2018.
 */
public class HttpRequest {
    static String url = "http://www.secstore.stream";

    public static JSONObject authRequest(String username,String password){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("http://www.secstore.stream/Authenticate.php").openConnection();

            String urlParameters = "username=" + username + "&password=" + password;

            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", urlParameters.getBytes().length + "");
            con.setRequestMethod("POST");

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(urlParameters.getBytes());

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject isAuth = new JSONObject(response.toString());
            return isAuth;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject registerRequest(String firstname,String lastname,String username,String password){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url + "/Register.php").openConnection();

            String urlParameters = String.format("firstname=%s&lastname=%s&username=%s&password=%s",firstname,lastname,username,password);

            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", urlParameters.getBytes().length + "");
            con.setRequestMethod("POST");


            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(urlParameters.getBytes());

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);

            }
            in.close();

            JSONObject isRegistered = new JSONObject(response.toString());
            return isRegistered;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray getFilesRequest(int uid){
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url + "/Files.php?uid=" + uid).openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);

            }
            in.close();

            JSONArray fileList = new JSONArray(response.toString());
            return fileList;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
