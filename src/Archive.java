
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author RDA digital
 */
public class Archive {

    private PrintWriter pw;

    public Archive(String filename) {
        try {
            File file = new File(filename);
            if (file.exists() == false) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
        } catch (IOException ex) {
            Logger.getLogger(Archive.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void writecm(double val) {
        pw.print(String.format("%3.3f", val) + ",");
    }

    public void writecm(int val) {
        pw.print(val + ",");
    }

    public void writecm(String val) {
        pw.print(val + ",");
    }

    public void write(double val) {
        pw.print(String.format("%3.3f", val));
    }

    public void write(int val) {
        pw.print(val);
    }

    public void write(String val) {
        pw.print(val);
    }

    public void writeln() {
        pw.println("");
        pw.flush();
    }

    public void close() {
        pw.close();
    }

    void write(byte[] buf,int len) {
        for(int i=0;i<len-1;i++){
            pw.write(String.format("0x%02X,", buf[i]));
        }
        pw.write(String.format("0x%02X", buf[len-1]));
    }

}
