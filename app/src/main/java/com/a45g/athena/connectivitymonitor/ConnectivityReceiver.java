package com.a45g.athena.connectivitymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static com.a45g.athena.connectivitymonitor.HelperFunctions.getTime;

public class ConnectivityReceiver
        extends BroadcastReceiver {
    
    private static final String LOG_TAG = ConnectivityReceiver.class.getName();

    private StringBuilder sb = null;

    public ConnectivityReceiver() {;}

    @Override
    public void onReceive(Context context, Intent intent) {

        sb = new StringBuilder();
        sb.append(System.getProperty("line.separator"));

        if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
            NetworkInfo ni = getNetworkInfo(intent);
            if (ni.getState().toString().equals("CONNECTED")) {
                displayAction(intent);
                displayKey(intent, "wifiInfo");
            }
            else{
                return;
            }
        }
        else
        if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")){
            NetworkInfo ni = getNetworkInfo(intent);
            if (ni.getState().toString().equals("CONNECTED")) {
                if (ni.getTypeName().equals("WIFI")){
                    displayAction(intent);
                    displayAllKeys(intent);
                    getAllNetworks(context);
                    ConfigService.startActionWifiEnable(context);

                    DatabaseOperations databaseOperations = new DatabaseOperations(context);
                    databaseOperations.openWrite();
                    databaseOperations.insertConnectivityEvent(getTime(), "WIFI", "CONNECTED",sb.toString());
                    databaseOperations.close();
                }
                else if (ni.getTypeName().equals("MOBILE")){
                    return;
                }
                else{
                    return;
                }
            }
            else if (ni.getState().toString().equals("DISCONNECTED")) {
                sb.append("Disconnected "+ni.getTypeName()).append(System.getProperty("line.separator"));

                if (ni.getTypeName().equals("WIFI")){
                    displayAction(intent);
                    displayAllKeys(intent);
                    ConfigService.startActionWiFiDisable(context);

                    DatabaseOperations databaseOperations = new DatabaseOperations(context);
                    databaseOperations.openWrite();
                    databaseOperations.insertConnectivityEvent(getTime(), "WIFI", "DISCONNECTED",sb.toString());
                    databaseOperations.close();
                }
                else if (ni.getTypeName().equals("MOBILE")){
                    return;
                }
                else{
                    return;
                }
            }
        }
        else{
            if (intent.getAction().equals("android.intent.action.ANY_DATA_STATE")){
                Bundle extras = intent.getExtras();
                if ((extras.get("reason") != null && extras.get("reason").equals("connected"))
                        && (extras.get("state") != null && extras.get("state").equals("CONNECTED"))
                        && (extras.get("apn") != null && extras.get("apn").equals("land"))
                        ){
                    displayAction(intent);
                    displayAllKeys(intent);
                    ConfigService.startActionLTEEnable(context);

                    DatabaseOperations databaseOperations = new DatabaseOperations(context);
                    databaseOperations.openWrite();
                    databaseOperations.insertConnectivityEvent(getTime(), "LTE", "CONNECTED",sb.toString());
                    databaseOperations.close();
                }
                else if ((extras.get("state") != null && extras.get("state").equals("DISCONNECTED"))
                        && (extras.get("apn") != null && extras.get("apn").equals("land"))
                        && (extras.get("reason") != null && extras.get("reason").equals("specificDisabled")
                                || (extras.get("reason") == null))){
                    displayAction(intent);
                    displayAllKeys(intent);
                    ConfigService.startActionLTEDisable(context);

                    DatabaseOperations databaseOperations = new DatabaseOperations(context);
                    databaseOperations.openWrite();
                    databaseOperations.insertConnectivityEvent(getTime(), "LTE", "DISCONNECTED",sb.toString());
                    databaseOperations.close();
                }
                else
                {
                    return;
                }
            }
        }

        //if (mainActivity != null)
        //    mainActivity.getOutputFragment().addOutput(sb.toString(), getTime());
        Intent i=new Intent("com.a45g.athena.connectivitymonitor.ACTION_DISPLAY");
        i.putExtra("timestamp", getTime());
        i.putExtra("value", sb.toString());
        context.sendBroadcast(i);
    }

    private void getAllNetworks(Context context){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] nets = connectivity.getAllNetworkInfo();

            for(int i=0; i<nets.length; i++){
                if (nets[i].getState() == NetworkInfo.State.CONNECTED
                    && (nets[i].getTypeName().equals("MOBILE") || nets[i].getTypeName().equals("WIFI")))
                {
                    Log.v(LOG_TAG, "Connected "+nets[i].getTypeName());
                    sb.append("Connected "+nets[i].getTypeName()).append(System.getProperty("line.separator"));


                    if (nets[i].getType() == ConnectivityManager.TYPE_WIFI) {
                        displayWifiInfo(context);

                        LinkProperties prop = connectivity.getLinkProperties(connectivity.getActiveNetwork());
                        sb.append("IP info: "+prop.toString()).append(System.getProperty("line.separator"));

                    } else if (nets[i].getType() == ConnectivityManager.TYPE_MOBILE) {
                        displayLTEInfo(context);

                        LinkProperties prop = connectivity.getLinkProperties(connectivity.getActiveNetwork());
                        sb.append("IP info: "+prop.toString()).append(System.getProperty("line.separator"));

                    }
                }
            }
        }
    }

    private void displayAction(Intent intent){
        Log.v(LOG_TAG, "Action: " + intent.getAction());
        sb.append("Action: " + intent.getAction()).append(System.getProperty("line.separator"));
    }

    private void displayAllKeys(Intent intent){
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key: extras.keySet()) {
                Log.v(LOG_TAG, key + ": " +
                        extras.get(key));
                sb.append(key + ": " +
                        extras.get(key)).append(System.getProperty("line.separator"));
            }
        }
        else {
            Log.v(LOG_TAG, "no extras");
        }
    }

    private void displayKey(Intent intent, String key){
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.v(LOG_TAG, key + ": " +
                    extras.get(key));
            sb.append(key + ": " +
                    extras.get(key)).append(System.getProperty("line.separator"));

        }
    }

    private NetworkInfo getNetworkInfo(Intent intent) {
        Bundle extras = intent.getExtras();
        return (NetworkInfo)(extras.get("networkInfo"));
    }

    public void connectivityInfo(Context context){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null)
                    if (info.getState() == NetworkInfo.State.CONNECTED)
                    {
                        Log.v(LOG_TAG, "Connected "+info.getTypeName());


                        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                            displayWifiInfo(context);
                        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            displayLTEInfo(context);
                        }
                    }


        }
    }

    private void displayWifiInfo(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String wifiName = wifiManager.getConnectionInfo().getSSID();
        if (wifiName != null) {
            Log.v(LOG_TAG, "WiFi SSID: " + wifiName);
            sb.append("WiFi SSID: " + wifiName).append(System.getProperty("line.separator"));

        }
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        Log.v(LOG_TAG, "DHCP Info: "+dhcpInfo.toString());
        sb.append("DHCP Info: "+dhcpInfo.toString()).append(System.getProperty("line.separator"));
    }

    private void displayLTEInfo(Context context){
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkName = tm.getNetworkOperatorName();
        if (networkName != null){
            Log.v(LOG_TAG, "Mobile network name: "+networkName+"\n IP: "+getMobileIP());
            sb.append("Mobile network name: "+networkName).append(System.getProperty("line.separator"));
            //sb.append("IP: "+getMobileIP()).append(System.getProperty("line.separator"));
        }
       // Log.v(LOG_TAG, tm.getCarrierConfig());

    }

    public String intToIp(int i) {
        return ((i >> 24 ) & 0xFF ) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ( i & 0xFF) ;
    }

    public String getMobileIP() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipaddress = inetAddress .getHostAddress().toString();
                        return ipaddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(LOG_TAG, "Exception in Get IP Address: " + ex.toString());
        }
        return null;
    }
}