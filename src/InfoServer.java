
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author RDA
 */
public class InfoServer extends Thread {

    private byte[] info;
    private int infoLen;
    public ServoStatus[] servoStat;

    public InfoServer() {
        info = new byte[1330];
        infoLen = 0;
        servoStat=new ServoStatus[7];
        
        start();
    }

    public synchronized void updateInfo() {
        info[0]='$';
        info[5]=',';
        info[7]=',';
        info[19]=',';
        info[21]=',';
        info[23]=',';
        info[25]=',';
        info[27]=',';
        info[21]=',';
        info[47]=',';
        int index=48;
        for(int i=0;i<6;i++){
            for(int j=0;j<60;j++){
                byte[] bytes = servoStat[i].MnCFormat[j].getBytes();
                int len=bytes.length;
                System.arraycopy(bytes, 0, info, index, len);                
                index+=len;
                info[index]=',';
                index++;
            }
            info[--index]=';';
            index++;
        }
        info[index]='*';
        infoLen = index+1;
        
        byte[] bytes = String.format("%04d", infoLen).getBytes();
        System.arraycopy(bytes, 0, info, 1, 4);
        
    }

    @Override
    public void run() {
        try {
            ServerSocket listenSock = new ServerSocket(1337);
            while (true) {
                Socket sock = listenSock.accept();
                System.out.println("connected");
                DataOutputStream dataOutputStream = new DataOutputStream(sock.getOutputStream());
                serveInfo(dataOutputStream);
            }
        } catch (IOException ex) {
            Logger.getLogger(InfoServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void serveInfo(DataOutputStream stream) {

        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        write(stream);
                        Thread.sleep(2000);
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(InfoServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }.start();
    }

    private synchronized void write(DataOutputStream stream) throws IOException {
        updateInfo();
        stream.write(info, 0, infoLen);
    }

    public static void main(String[] args) {
        InfoServer infoServer = new InfoServer();
        //infoServer.updateInfo("202", 161);
    }
    

}
