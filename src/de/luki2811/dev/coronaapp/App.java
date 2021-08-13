package de.luki2811.dev.coronaapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.*;
import java.awt.Container;
import java.time.LocalDate;
import javax.swing.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.awt.Dimension;

public class App{
    public static void main(String[] args) throws Exception { 

        Path path = FileSystems.getDefault().getPath("temp.txt");

        String[] boxItems = {"Landkreis", "Stadtkreis", "Bundesland"};
        JComboBox comboBox = new JComboBox(boxItems);
		JTextField text = new JTextField();
        text.setText(loadFromFile(path));
        
        Object[] message = {comboBox, text};

        JOptionPane pane = new JOptionPane( message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        pane.createDialog(null, "Eingabe").setVisible(true);

        String location = null;
        if(!isNullOrEmpty(text.getText())){
            location = (comboBox.getItemAt(comboBox.getSelectedIndex())) + " " + text.getText();
            writeInTxt(text.getText(), "temp.txt");
        }

        if (!avaibleConnection()) {
            JOptionPane.showMessageDialog(null, "Es konnte keine Internetverbindung hergestellt werden", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }

        String[] coronaData = new String[2];
        try {
            if(location.startsWith("Stadtkreis") || location.startsWith("Landkreis"))
            coronaData = getInzidenzLand(location);
            else
            coronaData = getInzidenzBund(location);
        } catch (NullPointerException e) {
            coronaData = null;
            System.exit(-1);
        }
        
        
        System.out.printf("%s hat eine Coronainzidenz von: %s", coronaData[0], coronaData[1]);
        System.out.println(" ");
        openWindow(coronaData[1], coronaData[0]);
    }
        
    private static void openWindow(String county, String covidInzidenz){
        String output = (county + " hat eine Inzidenz (Fälle letzte 7 Tage/100.000 EW) von " + covidInzidenz);
        JFrame window = new JFrame();
        window.setTitle("Covid-19 Inzidenz " + county);
        window.setSize(600 , 100);
        window.setLocationRelativeTo(null);
        Container content = new JPanel();
        JLabel data = new JLabel(output);
        JLabel quelle = new JLabel("Quelle: Robert Koch-Institut (RKI), dl-de/by-2-0");
        LocalDate date = LocalDate.now();
        JLabel dateLabel = new JLabel(String.format("Stand: "+ date));
        content.add(data);
        content.add(quelle);
        content.add(dateLabel);
        window.setContentPane(content);
        window.setMinimumSize(new Dimension(600, 100));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
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
            System.exit(-1);
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
        
        String json = jsonObject.get("features").toString().replaceAll("\\[\\{\"attributes\":","").replaceAll("}]","");
        
        if (json.equals("[]")){
            JOptionPane.showMessageDialog(null, "Fehler bei der Auswertung: '" + location.replaceAll("%20", " ") + "' konnte nicht gefunden werden", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.err.println(json);
            System.exit(-1);
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

    public static void writeInTxt(String text, String nameOfFile){
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
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            in = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }
}