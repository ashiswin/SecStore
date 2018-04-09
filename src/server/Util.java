package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class Util {
	public static String getPublicIP() {
		try {
	        URL whatismyip = new URL("http://checkip.amazonaws.com");
	        BufferedReader in = null;
	        try {
	            in = new BufferedReader(new InputStreamReader(
	                    whatismyip.openStream()));
	            String ip = in.readLine();
	            return ip;
	        } finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
    }
	
	public static String getLocalIP() {
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		    while (e.hasMoreElements())
		    {
		        NetworkInterface n = e.nextElement();
		        if(n.getName().equals("lo")) continue;
		        Enumeration<InetAddress> ee = n.getInetAddresses();
		        while (ee.hasMoreElements())
		        {
		            InetAddress i = ee.nextElement();
		            if(i.getHostAddress().length() <= 15) {
		            	return i.getHostAddress();
		            }
		        }
		    }
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		return null;
	}
}
