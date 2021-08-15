package de.luki2811.dev.coronaapp;

import javax.swing.*;
import org.json.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Window extends JFrame implements ActionListener {
    JFrame mainFrame;
    JMenuBar bar;
    JMenu menu_datei;
    JMenu menu_info;
    JMenuItem item_closing;
    JMenuItem item_version;
    JMenuItem item_quelle;
    JPanel panel;
    JPanel panel_center;
    JFileChooser fileChooser;
    JTextField tf;
    JLabel textLabel;
    JComboBox<String> comboBox;
    JButton send;

    public Window(String name){
        Dimension d = new Dimension(500, 150);
        mainFrame = new JFrame();
        mainFrame.setTitle(name);
        mainFrame.setMinimumSize(d);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        bar = new JMenuBar();
        menu_datei = new JMenu("Datei");
        menu_info = new JMenu("Info");
        item_closing = new JMenuItem("Schließen");
        item_quelle = new JMenuItem("Quelle");
        item_version = new JMenuItem("Version");
        item_closing.addActionListener(this);
        item_quelle.addActionListener(this);
        item_version.addActionListener(this);
        menu_info.add(item_version);
        menu_info.add(item_quelle);
        menu_datei.add(item_closing);
        bar.add(menu_datei);
        bar.add(menu_info);
        mainFrame.pack();
        mainFrame.setJMenuBar(bar);

        panel = new JPanel(); 
        tf = new JTextField(15); 

        String[] boxItems = {"Landkreis", "Stadtkreis", "Bundesland"};
        comboBox = new JComboBox<>(boxItems);
        
        send = new JButton("Abfragen");
        send.addActionListener(this);
        panel.add(comboBox);
        panel.add(tf);
        panel.add(send);

        textLabel = new JLabel("");
        panel_center = new JPanel();
        panel_center.add(textLabel);
        JSONObject lastInput = null;
        Path path = FileSystems.getDefault().getPath(App.fileName);
        if(path.toFile().isFile()){
            try {
                lastInput = new JSONObject(App.loadFromFile(path));
                tf.setText(lastInput.getString("text"));
                comboBox.setSelectedIndex(lastInput.getInt("indexOfComboBox"));
            } catch (JSONException e) {
                e.printStackTrace();
                System.err.println("Fehler beim Lesen der JSON-Datei!\nLöschen der 'temp.json' wird empfohlen.");;
            }
        }
        mainFrame.getContentPane().add(BorderLayout.NORTH, panel);
        mainFrame.getContentPane().add(BorderLayout.CENTER, panel_center);
        mainFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == item_closing)
        System.exit(0);

        if(e.getSource() == send){
            String location = null;
            if(App.isNullOrEmpty(tf.getText())){
                JOptionPane.showMessageDialog(null, "Eingabe ist leer", "Warnung", JOptionPane.WARNING_MESSAGE);
            } else{
                location = (comboBox.getItemAt(comboBox.getSelectedIndex())) + " " + tf.getText();
                    
                JSONObject inputAsJson = new JSONObject();
                inputAsJson.put("indexOfComboBox",comboBox.getSelectedIndex());
                inputAsJson.put("text", tf.getText());
        
                App.writeInFile(inputAsJson.toString(), App.fileName);
                String[] coronaData = App.getCoronaData(location);
                textLabel.setText(coronaData[1] + " hat eine Inzidenz (Fälle letzte 7 Tage/100.000 EW) von " + coronaData[0]);
            }
        }
        if(e.getSource() == item_quelle){
            JOptionPane.showMessageDialog(null, App.quelle, "Quelle", JOptionPane.INFORMATION_MESSAGE);
        }
        if(e.getSource() == item_version){
            JOptionPane.showMessageDialog(null, App.version, "Version", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
}