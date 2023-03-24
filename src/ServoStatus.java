
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Rajan
 */
public class ServoStatus {

    double az, prevAz, prevUnwrappedAz,azbias,elbias;
    double el, prevEl;
    double azCorrected, elCorrected;

    int prevAzCurrent;
    int prevAzSpeed;
    int prevElCurrent;
    int prevElSpeed;
    int cwrp;
    String wrapinfo;
    byte[] prevStatus;
    byte[] currStatus;

    private final Archive writer1, writer2, writer3, writer4;

    private int prevAzFault;
    private int prevElFault;
    private int prevAzAlarm;
    private int prevElAlarm;
    private int prevAzBrake = 1;
    private int prevElBrake = 1;
    public boolean azReady = false;
    public boolean elReady = false;
    private int azCurrent;
    private int azSpeed;
    private int elCurrent;
    private int elSpeed;
    private String statusText = "";
    private double commandAz = 0, commandEl = 0;
    DatagramSocket sock;
    private DatagramSocket sendSocket = null;
    private byte[] buffer;
    private DatagramPacket despatchPacket;
    private int azFault;
    private int elFault;
    private int azAlarm;
    private int elAlarm;
    private byte azBrake;
    private byte elBrake;
    private int byteCount;
    byte[] prevBuf1 = new byte[32];
    byte[] prevBuf2 = new byte[32];
    private String satname = "DEFAULT";
    String[] MnCFormat;
    private Timer limitDog;

    public ServoStatus(String path) {

        prevStatus = new byte[18];
        currStatus = new byte[18];
        writer1 = new Archive(path + "Angle/" + Time.getDate() + ".txt");
        writer2 = new Archive(path + "Discrete/" + Time.getDate() + ".txt");
        writer3 = new Archive(path + "Analog/" + Time.getDate() + ".txt");
        writer4 = new Archive(path + Time.getDate() + ".txt");
        try {
            sendSocket = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(ServoStatus.class.getName()).log(Level.SEVERE, null, ex);
        }
        buffer = new byte[128];

        despatchPacket = new DatagramPacket(buffer, 128);
        try {
            InetAddress destinationIP = InetAddress.getByName("127.0.0.1");
            despatchPacket.setAddress(destinationIP);
            despatchPacket.setPort(8000);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServoStatus.class.getName()).log(Level.SEVERE, null, ex);
        }
        MnCFormat = new String[60];
        for(int i=0;i<60;i++){
            MnCFormat[i] = new String("0");
        }
        setModeVal(1);
        limitDog = new Timer(1000,new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetLimit();
            }
        });
        limitDog.setRepeats(true);
        limitDog.start();
    }

    public void setSatname(String satname) {
        this.satname = satname;
        MnCFormat[57] = satname;
    }

    public void setAngleStatus(byte[] buf) {
        az = getValue(buf, 1)-azbias;
        el = getValue(buf, 6)-elbias;
        cwrp = (buf[10] == 1) ? 1 : 0;
        wrapinfo = (cwrp == 0x01) ? "CW" : "CCW";
        azCorrected = getValue(buf, 11);
        elCorrected = getValue(buf, 15);
        boolean azGlitch = az != azCorrected;
        boolean ezGlitch = el != elCorrected;
        boolean angleChanged = Math.abs(az - prevAz) > 0.1 || Math.abs(el - prevEl) > 0.1;
        boolean condition = angleChanged || azGlitch || ezGlitch;
        
            //pack MnC frame
            double unwrappedAz = az + cwrp * 360;
            if (unwrappedAz > prevUnwrappedAz) {
                MnCFormat[9] = "2";
            } else {
                MnCFormat[9] = "1";
            }
            prevUnwrappedAz = unwrappedAz;
            if (el > prevEl) {
                MnCFormat[10] = "1";
            } else {
                MnCFormat[10] = "2";
            }
            MnCFormat[53] = String.valueOf(cwrp);
            //    
            prevAz = az;
            prevEl = el;

        
        logAngleStatus(condition);
        MnCFormat[2] = String.format("%.3f", az);
        MnCFormat[3] = String.format("%.3f", el);
        MnCFormat[6] = String.format("%.3f", az - commandAz);
        MnCFormat[7] = String.format("%.3f", el - commandEl);

    }

    public void setOtherStatus(byte[] buf) {

        azCurrent = getInt(buf, 0);//;256 * buf[0] + buf[1];
        azSpeed = getInt(buf, 2);//256 * buf[2] + buf[3];
        elCurrent = getInt(buf, 4);//256 * buf[4] + buf[5];
        elSpeed = getInt(buf, 6);//256 * buf[6] + buf[7];
        azFault = getInt(buf, 8);//256 * buf[8] + buf[9];
        elFault = getInt(buf, 10);//256 * buf[10] + buf[11];
        azAlarm = getInt(buf, 12);//256 * buf[12] + buf[13];
        elAlarm = getInt(buf, 14);//256 * buf[14] + buf[15];
        azBrake = buf[16];
        elBrake = buf[17];
        //pack MnC frame
        MnCFormat[11] = String.format("%2d", azSpeed);
        MnCFormat[12] = String.format("%2d", elSpeed);
        MnCFormat[15] = (azFault == 0) ? "0" : "1";
        MnCFormat[16] = (elFault == 0) ? "0" : "1";
        MnCFormat[19] = String.valueOf(azBrake);
        MnCFormat[21] = String.valueOf(elBrake);
        MnCFormat[25] = String.valueOf(azCurrent);
        MnCFormat[26] = String.valueOf(elCurrent);
        MnCFormat[27] = String.valueOf(azSpeed);
        MnCFormat[28] = String.valueOf(elSpeed);
        //detect change
        boolean analogParamChanged = Math.abs(azCurrent - prevAzCurrent) > 10 || Math.abs(elCurrent - prevElCurrent) > 10
                || Math.abs(azSpeed - prevAzSpeed) > 10 || Math.abs(elSpeed - prevElSpeed) > 2;
        if (analogParamChanged) {

            prevAzCurrent = azCurrent;
            prevElCurrent = elCurrent;
            prevAzSpeed = azSpeed;
            prevElSpeed = elSpeed;
        }
        logAnalogStatus(analogParamChanged);
        statusText = "";
        boolean condition = azFault != prevAzFault || elFault != prevElFault || azAlarm != prevAzAlarm || elAlarm != prevElAlarm
                || azBrake != prevAzBrake || elBrake != prevElBrake;

        logDiscreteStatus(condition);

        azReady = azBrake == 0;
        elReady = elBrake == 0;
    }

    private synchronized void logAngleStatus(boolean flag) {
        String timeStamp = Time.getTimeStamp();
        packInit(0);
        pack(timeStamp);
        pack(String.valueOf(commandAz));
        pack(String.valueOf(az));
        pack(String.valueOf(azCorrected));
        pack(String.valueOf(commandEl));
        pack(String.valueOf(el));
        pack(String.valueOf(elCorrected));
        pack(wrapinfo);
        pack(satname);
        send();
        if (flag) {
            writer1.writecm(timeStamp);
            writer1.writecm(commandAz);
            writer1.writecm(az);
            writer1.writecm(azCorrected);
            writer1.writecm(commandEl);
            writer1.writecm(el);
            writer1.writecm(elCorrected);
            writer1.write(wrapinfo);
            writer1.writeln();
        }
    }

    private void logAnalogStatus(boolean flag) {
        String timeStamp = Time.getTimeStamp();
        packInit(1);
        pack(timeStamp);
        pack(String.valueOf(azCurrent));
        pack(String.valueOf(elCurrent));
        pack(String.valueOf(azSpeed));
        pack(String.valueOf(elSpeed));
        send();
        if (flag) {
            writer3.writecm(timeStamp);
            writer3.writecm(azCurrent);
            writer3.writecm(elCurrent);
            writer3.writecm(azSpeed);
            writer3.write(elSpeed);
            writer3.writeln();
        }
    }

    private void logDiscreteStatus(boolean flag) {
        String str1, str2, str3, str4, str5, str6;
        String timeStamp = Time.getTimeStamp();
        packInit(2);
        pack(timeStamp);
        str1 = String.format("%04d", azFault);
        pack(str1);
        str2 = String.format("%04d", elFault);
        pack(str2);
        str3 = String.format("%04d", azAlarm);
        pack(str3);
        str4 = String.format("%04d", elAlarm);
        pack(str4);
        str5 = (azBrake == 0) ? "ON" : "OFF";
        pack(str5);
        str6 = (elBrake == 0) ? "ON" : "OFF";
        pack(str6);
        send();

        if (flag) {
            statusText = "\n" + Time.getDateTime() + "\n";
            if (azFault != prevAzFault) {
                statusText = statusText + "Az Fault status: " + str1 + "\n";
                prevAzFault = azFault;
            }

            writer2.writecm(timeStamp);
            writer2.writecm(str1);
            writer2.writecm(str2);

            if (elFault != prevElFault) {
                statusText = statusText + "El Fault status: " + str2 + "\n";
                prevElFault = elFault;
            }

            writer2.writecm(str3);
            if (azAlarm != prevAzAlarm) {
                statusText = statusText + "Az Alarm status: " + str3 + "\n";
                prevAzAlarm = azAlarm;
            }

            writer2.writecm(str4);
            if (elAlarm != prevElAlarm) {
                statusText = statusText + "El Alarm status: " + str4 + "\n";
                prevElAlarm = elAlarm;
            }

            writer2.writecm(str5);

            if (azBrake != prevAzBrake) {
                statusText = statusText + ((azBrake == 0) ? "Az Break released" : "Az Break applied") + "\n";
                prevAzBrake = azBrake;
            }

            writer2.write(str6);
            if (elBrake != prevElBrake) {
                statusText = statusText + ((elBrake == 0) ? "El Break released" : "El Break applied") + "\n";
                prevElBrake = elBrake;
            }
            writer2.writeln();
        }
    }

    private void packInit(int type) {
        byteCount = 3;
        pack(String.valueOf(type));
    }

    private void pack(String val) {
        byte[] bytes = val.getBytes();
        int len = bytes.length;
        System.arraycopy(bytes, 0, buffer, byteCount, len);
        byteCount = byteCount + len;
        buffer[byteCount] = ',';
        byteCount++;

    }

    private void send() {
        byteCount--;
        despatchPacket.setLength(byteCount);
        try {
            sendSocket.send(despatchPacket);
        } catch (IOException ex) {
            Logger.getLogger(ServoStatus.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String getStatus() {
        return statusText;
    }

    private double getValue(byte[] buf, int off) {
        int i0 = (int) buf[off + 0];
        if (i0 < 0) {
            i0 += 256;
        }
        int i1 = (int) buf[off + 1];
        if (i1 < 0) {
            i1 += 256;
        }
        int i2 = (int) buf[off + 2];
        if (i2 < 0) {
            i2 += 256;
        }
        int i3 = (int) buf[off + 3];
        if (i3 < 0) {
            i3 += 256;
        }
        int val = ((256 * i0 + i1) * 1000) + (256 * i2 + i3);
        return val / 1000.0;
    }

    private int getInt(byte[] buf, int off) {
        int i0 = (int) buf[off + 0];
        if (i0 < 0) {
            i0 += 256;
        }
        int i1 = (int) buf[off + 1];
        if (i1 < 0) {
            i1 += 256;
        }
        return 256 * i0 + i1;
    }

    public synchronized void setCommandAngle(double commandAz, double commandEl) {
        this.commandAz = commandAz;
        this.commandEl = commandEl;
        MnCFormat[4] = String.format("%.3f", commandAz);
        MnCFormat[5] = String.format("%.3f", commandEl);
    }

    public void setID(int ID) {
        String sID = String.format("%X", ID);
        byte[] bytes = sID.getBytes();
        buffer[0] = bytes[0];
        buffer[1] = bytes[1];
        buffer[2] = ',';
        MnCFormat[0] = sID;
        MnCFormat[1] = "000";//dummy
    }

    void logInfo(byte[] buf, int alertID, int len) {

        if (alertID == 0x06) {
            String timeStamp = Time.getTimeStamp();
            writer4.writecm(timeStamp);
            writer4.writecm("GLITCH");
            writer4.writecm(buf[0]);
            writer4.write(buf[1]);
            writer4.writeln();
        } else {
            statusText = "";
            switch (alertID) {
                case 0x05:
                    if (compare(buf, prevBuf1, len) != 0) {
                        String timeStamp = Time.getTimeStamp();
                        writer4.writecm(timeStamp);
                        statusText = "\n" + Time.getDateTime() + "\n";
                        writer4.writecm("LIMIT");
                        writer4.write(buf, len);
                        writer4.writeln();
                        statusText = statusText + "LIMIT REACHED";
                        System.arraycopy(buf, 0, prevBuf1, 0, len);
                    }
                    limitDog.restart();
                    break;
                case 0x07:
                    if (compare(buf, prevBuf2, len) != 0) {
                        writer4.writecm("BRAKE");
                        writer4.write(buf, len);
                        writer4.writeln();
                        //statusText = statusText;
                        System.arraycopy(buf, 0, prevBuf2, 0, len);
                    }
                    break;
                case 0x08:
                    writer4.writecm("INIT");
                    writer4.write(buf, len);
                    writer4.writeln();
                    break;
            }

        }
    }

    private int compare(byte[] buf1, byte[] buf2, int len) {
        int retVal = 0;
        for (int i = 0; i < len; i++) {
            retVal = Byte.compare(buf1[i], buf2[i]);
            if (retVal != 0) {
                break;
            }
        }
        return retVal;
    }

    public boolean isFault() {
        return azFault != 0 || elFault != 0;
    }

    public boolean isSevereFault() {
        return (azFault > 1000) || (elFault > 1000);
    }

    // for simulation purpose
    public void setSimAngle(double az, double el) {
        double prevAz = this.az;
        double currAz = 360 - this.az + (1 - cwrp) * 360;
        double azCW = 360 - az;
        double azCCW = 720 - az;
        double destAz;
        if (azCCW > 660) {
            destAz = azCW;
        } else if (azCW < 60) {
            destAz = azCCW;
        } else {
            double diffCW = Math.abs(azCW - currAz);
            double diffCCW = Math.abs(azCCW - currAz);
            if (diffCW < diffCCW) {
                destAz = azCW;
            } else {
                destAz = azCCW;
            }
        }
        if (destAz > 360) {
            this.az = 720 - destAz;
        } else {
            this.az = 360 - destAz;
        }
        this.el = el;
        if (currAz < 360 && destAz > 360) {
            this.cwrp = 0;
            this.wrapinfo = "CCW";
        }
        if (currAz > 360 && destAz < 360) {
            this.cwrp = 1;
            this.wrapinfo = "CW";
        }
    }

    public void resetBuf() {
        for (int i = 0; i < prevBuf1.length; i++) {
            prevBuf1[i] = 0;
        }
        for (int i = 0; i < prevBuf2.length; i++) {
            prevBuf2[i] = 0;
        }
    }

    public void setModeVal(int mode) {
        MnCFormat[8] = String.valueOf(mode);
    }

    public void setVisibility(String currAOS, String currLOS) {
        MnCFormat[58] = currAOS;
        MnCFormat[59] = currLOS;
    }
    private void resetLimit(){
        for(int i=31;i<47;i++)
        MnCFormat[i] = "0";
    }

    void setBiases(double azbias, double elbias) {
        this.azbias=azbias;
        this.elbias=elbias;
    }

}
