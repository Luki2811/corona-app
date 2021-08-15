package de.luki2811.dev.coronaapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.*;
import javax.swing.*;
import java.net.InetAddress;
import java.net.MalformedURLException;

public class App{

    final static String fileName = "temp.json";
    final static String version = "v.0.3.0-alpha";
    final static String quelle = "Quelle: Robert Koch-Institut (RKI), dl-de/by-2-0";

    public static void main(String[] args) throws Exception { 
        Window fenster = new Window("Corona App");
    }

    public static String[] getCoronaData(String location){
        
        if (!App.avaibleConnection()) {
            JOptionPane.showMessageDialog(null, "Es konnte keine Internetverbindung hergestellt werden", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        String[] coronaData = new String[2];
        try {
            if(location.startsWith("Stadtkreis") || location.startsWith("Landkreis"))
            coronaData = App.getInzidenzLand(location);
            else
            coronaData = App.getInzidenzBund(location);
        } catch (NullPointerException ed) {
            coronaData = null;
            ed.printStackTrace();
            System.exit(-1);
        }
        return coronaData;
    }
        
    public static boolean isNullOrEmpty(String str){
        return str == null || str.isEmpty();
    }
    
    public static boolean avaibleConnection(){
        try {
            final InetAddress host = InetAddress.getByName("services7.arcgis.com");
            return host.isReachable(1000);  
        } catch (IOException e) {
            e.getStackTrace();
            return false;
        }       
    }

    public static String[] getInzidenzLand(String location){
        String[] covidInzidenz = new String[2];
        URL url;
        try {
            url = new URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/RKI_Landkreisdaten/FeatureServer/0/query?where=county%20%3D%20'" + location.toLowerCase().replaceAll(" ", "%20").replaceAll("landkreis", "lk").replaceAll("stadtkreis", "sk").replaceAll("\u00e4", "%C3%A4").replaceAll("\u00f6", "%C3%B6").replaceAll("\u00fc", "%C3%BC") + "'&outFields=cases7_per_100k,county&returnGeometry=false&returnDistinctValues=true&outSR=4326&f=json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            url = null;
        }
        JSONObject jsonObject = getJSONfromURL(url);
        
        String json = jsonObject.get("features").toString().replaceAll("\\[\\{\"attributes\":","").replaceAll("}]","");
        
        if (json.equals("[]")){
            JOptionPane.showMessageDialog(null, "Fehler bei der Auswertung: '" + location.replaceAll("%20", " ") + "' konnte nicht gefunden werden", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.err.println(json);
        }
        JSONObject jsonObj = new JSONObject(json);

        covidInzidenz[0] = Double.toString(round(Double.parseDouble(jsonObj.get("cases7_per_100k").toString()),2));
        covidInzidenz[1] = jsonObj.getString("county");
        return covidInzidenz;
        
    }

    public static JSONObject getJSONfromURL(URL url) {
        JSONObject jsonObject;
        try(InputStream is = url.openStream();
            Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                jsonObject = new JSONObject(scanner.useDelimiter("//Z").next().toString());
            
            }
        catch(IOException e){
            e.printStackTrace();
            jsonObject = null;
        }
        catch(NullPointerException e){
            e.printStackTrace();
            jsonObject = null;
            System.err.println("URL konnte nicht gefunden werden");
            System.exit(-1);
        }
        return jsonObject;
    }

    public static String[] getInzidenzBund(String location){
        String[] covidInzidenz;
        URL url;
        try {
            url = new URL("https://services7.arcgis.com/mOBPykOjAyBO2ZKk/arcgis/rest/services/Coronaf%C3%A4lle_in_den_Bundesl%C3%A4ndern/FeatureServer/0/query?where=LAN_ew_GEN%20%3D%20'" + location.toLowerCase().replaceAll("bundesland ", "").replaceAll(" ", "%20").replaceAll("\u00e4", "%C3%A4").replaceAll("\u00f6", "%C3%B6").replaceAll("\u00fc", "%C3%BC") + "'&outFields=LAN_ew_GEN,cases7_bl_per_100k&returnGeometry=false&outSR=4326&f=json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            url = null;
        }
        JSONObject jsonObject = getJSONfromURL(url);
        String json = null;

        try {
            json = jsonObject.get("features").toString().replaceAll("\\[\\{\"attributes\":","").replaceAll("}]","");
        } catch (JSONException e) {
            ;
        }
        
        
        if (json.equals("[]")){
            JOptionPane.showMessageDialog(null, "Fehler bei der Auswertung: '" + location.replaceAll("%20", " ") + "' konnte nicht gefunden werden", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.err.println(json);
        }
        JSONObject jsonObj = new JSONObject(json);
        covidInzidenz = new String[2];
        
        covidInzidenz[0] = Double.toString(round(Double.parseDouble(jsonObj.get("cases7_bl_per_100k").toString()),2));
        covidInzidenz[1] = jsonObj.getString("LAN_ew_GEN");
        return covidInzidenz;  
    }

    /**
    * Rundet den übergebenen Wert auf die Anzahl der übergebenen Nachkommastellen
    *
    * @param value ist der zu rundende Wert.
    * @param decimalPoints ist die Anzahl der Nachkommastellen, auf die gerundet werden soll.
    */
    private static double round(double value, int decimalPoints) {
        double d = Math.pow(10, decimalPoints);
        return Math.round(value * d) / d;
    }

    public static void writeInFile(String text, String nameOfFile){
        FileWriter writer;
        File datei = new File(nameOfFile);
        
        try {
            writer = new FileWriter(datei);
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadFromFile(Path path){
        String in = null;
        try {
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1);
            in = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }
}