
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Istrac
 */
public class Antenna implements AntennaActionListener {

    private byte[] buf;
    private int ID;
    private JCheckBox conn, stdby, strt, stp, off;
    private JLabel az, el, wrapinfo;
    JTextArea info;
    private String ip;
    private int port;
    private byte[] stdbyPkt, startPkt, stopPkt, limitPkt;
    private DataOutputStream dataOutputStream;
    private volatile boolean keepSending = false;
    private Socket socket;

    private volatile boolean connected = false;
    private ButtonGroup grp;
    public ServoStatus servoStatus;
    double commandAz, commandEl;
    Reader reader;
    Designate designateForm;
    private JLabel satname;
    private Timer blink;
    private boolean blinkRunning = false;
    private boolean show = true;
    private JLabel AOS;
    private JLabel LOS;
    private JLabel cmdAz;
    private JLabel cmdEl;
    private JLabel warning;
    private Thread running;
    private final double azbias;
    private final double elbias;

    public Antenna(int ID) {
        buf = new byte[128];
        stdbyPkt = new byte[5];
        startPkt = new byte[4 + 16];
        stopPkt = new byte[3];
        limitPkt = new byte[4 + 26];
        this.ID = ID;
        servoStatus = new ServoStatus("Status" + "/" + String.format("%X", ID) + "/");
        servoStatus.setID(ID);
        servoStatus.az = 100;
        servoStatus.el = 20;
        {
            servoStatus.cwrp = 1;
            servoStatus.wrapinfo = "CW";
        }
        XMLReadWrite bread = new XMLReadWrite("bias.xml");
        azbias=Double.parseDouble(bread.getTextByTag(String.format("%X",ID), "az").trim());
        elbias=Double.parseDouble(bread.getTextByTag(String.format("%X",ID), "el").trim());
        servoStatus.setBiases(azbias,elbias);
        reader = new Reader(ID);
        reader.link(servoStatus);
        reader.setListener(this);
        blink = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (show) {
                    warning.setForeground(Color.red);
                    warning.setText("ATTENTION");
                } else {
                    warning.setText("");
                }
                show = !show;
            }
        });

    }

    public void linkCommands(JCheckBox ch1, JCheckBox ch2, JCheckBox ch3, JCheckBox ch4, JCheckBox ch5) {
        conn = ch1;
        stdby = ch2;
        off = ch3;
        strt = ch4;
        stp = ch5;
        conn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        stdby.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                makeReady();
            }
        });
        strt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAntenna();
            }
        });
        stp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAntenna();
            }
        });
        off.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                makeStandby();
            }
        });

    }

    public void linkStatus(JLabel lb1, JLabel tx1, JLabel tx2, JLabel tx3, JTextArea a) {
        warning = lb1;
        warning.setForeground(Color.red);
        az = tx1;
        el = tx2;
        wrapinfo = tx3;
        info = a;
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        DefaultCaret caret = (DefaultCaret) info.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void linkInfo(JLabel lb1, JLabel lb2, JLabel lb3, JLabel lb4, JLabel lb5) {
        satname = lb1;
        AOS = lb2;
        LOS = lb3;
        cmdAz = lb4;
        cmdEl = lb5;
    }

    private void connect() {

        if (!connected) {
            //connection attempt thread
            new Thread() {
                @Override
                public void run() {
                    try {
                        socket = new Socket(ip, port);
                        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        connectionStatus(true);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        stdby.setSelected(connected);
                        makeReady();
                        while (connected) {
                            try {
                                while (true) {
                                    byte b0 = dataInputStream.readByte();
                                    if (b0 != (byte) 0xEE) {
                                        continue;
                                    }
                                    byte b1 = dataInputStream.readByte();
                                    if (b1 == (byte) ID) {
                                        break;
                                    }
                                }
                                byte cID = dataInputStream.readByte();
                                switch (cID) {
                                    case 0x07://angle
                                        dataInputStream.readByte();//not used
                                        dataInputStream.readFully(buf, 0, 19);
                                        servoStatus.setCommandAngle(commandAz, commandEl);
                                        servoStatus.setAngleStatus(buf);
                                        az.setText(String.format("%3.2f", servoStatus.az));
                                        el.setText(String.format("%3.2f", servoStatus.el));
                                        wrapinfo.setText(servoStatus.wrapinfo);
                                        break;
                                    case 0x08://info
                                        byte alertID = dataInputStream.readByte();
                                        if (alertID == 0x01) {
                                            dataInputStream.readFully(buf, 0, 18);
                                            servoStatus.setOtherStatus(buf);
                                            String str = servoStatus.getStatus();
                                            if (!str.isEmpty()) {
                                                info.append(str);
                                            }
                                            if (servoStatus.isFault()) {
                                                if (!blinkRunning) {
                                                    blink.start();
                                                    blinkRunning = true;
                                                    if (servoStatus.isSevereFault()) {
//                                                        stdby.setSelected(true);
//                                                        makeStandby();
                                                    }
                                                }
                                            } else {
                                                if (blinkRunning) {
                                                    blink.stop();
                                                    blinkRunning = false;
                                                    warning.setText("");
                                                }
                                            }
                                            boolean ready = servoStatus.azReady && servoStatus.elReady;
                                            strt.setEnabled(ready);
                                            if (!servoStatus.azReady || !servoStatus.elReady) {
                                                warning.setText("STANDBY");
                                                servoStatus.setModeVal(1);
                                            }
                                            
                                        } else {
                                            int len;
                                            switch (alertID) {
                                                case 0x05:
                                                    len = 16;
                                                case 0x07:
                                                    len = 18;
                                                    break;
                                                default:
                                                    len = 10;
                                                    break;
                                            }
                                            dataInputStream.readFully(buf, 0, len);
                                            servoStatus.logInfo(buf, alertID, len);
                                            String str = servoStatus.getStatus();
                                            if (!str.isEmpty()) {
                                                info.append(str);
                                            }
                                        }
                                }
                            } catch (IOException ex) {
                                connectionStatus(false);
                            }

                        }
                    } catch (IOException ex) {
                        connectionStatus(false);
                    }
                }
            }.start();
        } else {
            try {
                socket.close();
                connectionStatus(false);
            } catch (IOException ex) {
                Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void makeReady() {
        new Thread() {
            @Override
            public void run() {

                if (keepSending) {
                    strt.setSelected(true);
                    return;
                }
                reader.stopTimer();
                stdby.setSelected(true);

                stdbyPkt[0] = (byte) 0xEE;
                stdbyPkt[1] = (byte) ID;
                stdbyPkt[2] = (byte) 0x01;
                stdbyPkt[3] = (byte) 0x0A;
                stdbyPkt[4] = (byte) 0x00;
                send(0);
                int count = 0;
                while (!servoStatus.azReady && connected) {
                    try {
                        Thread.sleep(100);
                        count++;
                        if (count % 30 == 0) {
                            send(0);
                            System.out.println(String.format("%X: ", ID) + "Az ON command sent for the " + count / 30 + " th time");
                            System.out.flush();
                        }
                        if (count == 300) {
                            System.out.println(String.format("%X: ", ID) + "30 s has elapsed since 1st Az ON command was sent");
                            break;
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (servoStatus.azReady && connected) {
                    stdbyPkt[3] = (byte) 0x0E;
                    send(0);
                    count = 0;
                    while (!servoStatus.elReady) {
                        try {
                            Thread.sleep(100);
                            count++;
                            if (count % 30 == 0) {
                                send(0);
                                System.out.println(String.format("%X: ", ID) + "El ON command sent for the " + count / 30 + " th time");
                                System.out.flush();
                            }
                            if (count == 300) {
                                System.out.println(String.format("%X: ", ID) + "30 s has elapsed since 1st EL ON command was sent");
                                break;
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                if (servoStatus.azReady && servoStatus.elReady && connected) {
                    if (!keepSending) {
                        strt.setSelected(true);
                        servoStatus.resetBuf();
                        startAntenna();
                    }
                }
            }
        }.start();
    }

    /**
     *
     */
    @Override
    public void makeStandby() {
        new Thread() {
            @Override
            public void run() {
                keepSending = false;
                off.setSelected(true);
                if (keepSending) {
                    stopAntenna();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                stdbyPkt[0] = (byte) 0xEE;
                stdbyPkt[1] = (byte) ID;
                stdbyPkt[2] = (byte) 0x01;
                stdbyPkt[3] = (byte) 0x0A;
                stdbyPkt[4] = (byte) 0x01;
                send(0);
                int count = 0;
                servoStatus.setSatname("DEFAULT");
                while (servoStatus.azReady && connected) {
                    try {
                        Thread.sleep(100);
                        count++;
                        if (count % 30 == 0) {
                            send(0);
                        }
                        if (count == 100) {
                            break;
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (servoStatus.azReady == false) {
                    stdbyPkt[3] = (byte) 0x0E;
                    send(0);
                    count = 0;
                    while (servoStatus.elReady && connected) {
                        try {
                            Thread.sleep(100);
                            count++;
                            if (count % 30 == 0) {
                                send(0);
                            }
                            if (count == 100) {
                                break;
                            }
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }                   
                    
                }
            }
        }.start();
    }

    public void startAntenna() {

        startPkt[0] = (byte) 0xEE;
        startPkt[1] = (byte) ID;
        startPkt[2] = (byte) 0x04;
        startPkt[3] = (byte) 0x22;
        //azimuth
        startPkt[4] = (byte) 0x0A;
        //elevation
        startPkt[9] = (byte) 0x0E;
        TimeStruct time = new TimeStruct();
//        TimeStruct time = new TimeStruct(2023, 03, 23, 5, 48, 00);
        reader.setTransition(false);
//        infoServer.updateInfo(reader.satName, ID);
//        satname.setForeground(Color.BLACK);
//        satname.setText("DEFAULT");
//        SectorScan scan = new SectorScan();
        
        if (!keepSending) {
            keepSending = true;            
            running = new Thread() {
                @Override
                public void run() {
                    double prevAz = 10;//default angle value
                    double prevEl = 30;//default angle value

                    while (keepSending) {
                        try {
                            time.setCurrentTime();
//                            time.advance(10);
                            Angle angle;
                            if (designateForm.isDesignated(ID)) {
                                if (Math.abs(prevAz - servoStatus.az) < 0.1 && Math.abs(prevEl - servoStatus.el) < 2) {
                                    designateForm.setDesignateComplete(ID, true);
                                }
                                angle = designateForm.getAngle(ID);
                                commandAz = angle.az;
                                commandEl = angle.el;
                                servoStatus.setModeVal(5);
                            } else {

                                angle = reader.getAngle(time);
//                                angle=scan.getHSECAngle();

                                if (!angle.isAvailable()) {
                                    System.out.println(String.format("%X", ID) + ": Angle unavailable so using previous angle");
                                    commandAz = prevAz;
                                    commandEl = prevEl;
                                    servoStatus.setSatname("DEFAULT");
                                } else {
                                    commandAz = angle.az;
                                    commandEl = angle.el;
                                    if ((angle.az < 0 || angle.az > 360)) {//||(angle.el < 5 || angle.el > 84)
                                        System.out.println(String.format("%X", ID) + ": Angle outside limit so using the limit angle angle");
                                        if (angle.az < 0 || angle.az >= 360) {
                                            commandAz = 0;
                                        }
                                        if (angle.el < 5) {
                                            commandEl = 5;
                                        } else if (angle.el > 85) {
                                            commandEl = 85;
                                        }

                                    }
                                }

                                if (reader.isSatChanged()) {
                                    servoStatus.setSatname(reader.satName);
                                    satname.setText(reader.satName);
                                    AOS.setText(reader.currAOS);
                                    LOS.setText(reader.currLOS);
                                    servoStatus.setVisibility(reader.currAOS, reader.currLOS);
                                }
                                servoStatus.setModeVal(4);
                            }
                            prevAz = commandAz;
                            prevEl = commandEl;
                            satname.setText(reader.satName);
                            servoStatus.setSatname(reader.satName);

                            String caz = String.format("%3.2f", commandAz);
                            String cel = String.format("%3.2f", commandEl);
                            System.out.println(String.format("%X", ID) + " " + time.getString() + "\t az:" + caz + "\tel : " + cel + "\tpresent wrap: " + servoStatus.wrapinfo + "\t" + reader.satName);
                            cmdAz.setText(caz);
                            cmdEl.setText(cel);
                            setValue(commandAz+azbias, startPkt, 5);
                            //el val 4 bytes 10-13
                            setValue(commandEl+elbias, startPkt, 10);
                            //timestamp 6 bytes 14-19

                            startPkt[14] = (byte) time.day;
                            startPkt[15] = (byte) time.month;
                            startPkt[16] = (byte) time.year;
                            startPkt[17] = (byte) time.hour;
                            startPkt[18] = (byte) time.min;
                            startPkt[19] = (byte) time.sec;
                            send(1);
                            if (reader.isInTransition()) {
                                warning.setForeground(Color.black);
                                warning.setText("TRANSITION");
                            } else if (!blink.isRunning()) {
                                warning.setForeground(Color.black);
                                warning.setText("TRACKING");
                            }
                            if (keepSending) {
                                if (designateForm.isDesignated(ID)) {
                                    designateForm.setDesignateComplete(ID, false);
                                    Thread.sleep(1000);
                                } else {
                                    Thread.sleep(10000);
                                }

                            }

                        } catch (InterruptedException ex) {
                            Logger.getLogger(Antenna.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    warning.setText("");
                }

            };
            running.start();

        }
    }

    public void stopAntenna() {
        keepSending = false;        
        stopPkt[0] = (byte) 0xEE;
        stopPkt[1] = (byte) ID;
        stopPkt[2] = (byte) 0x10;
        send(2);
    }

    public void sendLimits(double azCwPl, double azCwEl, double azCcwPl, double azCcwEl, double elUpPl, double elUpEl, double elDnPl, double elDnEl) {
        limitPkt[0] = (byte) 0xEE;
        limitPkt[1] = (byte) ID;
        limitPkt[2] = 0x12;//cID
        limitPkt[3] = 0x1A;//length
        //azimuth
        int offset = 4;
        limitPkt[offset + 0] = 0x0A;
        limitPkt[offset + 1] = 0x01;
        limitPkt[offset + 2] = (byte) (azCwEl / 256);
        limitPkt[offset + 3] = (byte) (azCwEl % 256);
        limitPkt[offset + 4] = 0x02;
        limitPkt[offset + 5] = (byte) (azCwPl / 256);
        limitPkt[offset + 6] = (byte) (azCwPl % 256);
        limitPkt[offset + 7] = 0x03;
        limitPkt[offset + 8] = (byte) (azCcwEl / 256);
        limitPkt[offset + 9] = (byte) (azCcwEl % 256);
        limitPkt[offset + 10] = 0x04;
        limitPkt[offset + 11] = (byte) (azCcwPl / 256);
        limitPkt[offset + 12] = (byte) (azCcwPl % 256);
        //elevation
        offset = 17;
        limitPkt[offset + 2] = (byte) (elUpEl / 256);
        limitPkt[offset + 3] = (byte) (elUpEl % 256);
        limitPkt[offset + 4] = 0x02;
        limitPkt[offset + 5] = (byte) (elUpPl / 256);
        limitPkt[offset + 6] = (byte) (elUpPl % 256);
        limitPkt[offset + 7] = 0x03;
        limitPkt[offset + 8] = (byte) (elDnEl / 256);
        limitPkt[offset + 9] = (byte) (elDnEl % 256);
        limitPkt[offset + 10] = 0x04;
        limitPkt[offset + 11] = (byte) (elDnPl / 256);
        limitPkt[offset + 12] = (byte) (elDnPl % 256);
        send(3);
    }

    public void setAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean isConnected() {
        return connected;
    }

    private synchronized void send(int code) {

        try {
            switch (code) {
                case 0:
                    dataOutputStream.write(stdbyPkt);
                    break;
                case 1:
                    if (keepSending) {
                        dataOutputStream.write(startPkt);
                    }
                    break;
                case 2:
                    dataOutputStream.write(stopPkt);
                    break;
                case 3:
                    dataOutputStream.write(limitPkt);
            }
            dataOutputStream.flush();

        } catch (IOException ex) {
            keepSending = false;
            connectionStatus(false);
        }
    }

    private void setValue(double val, byte[] buf, int off) {
        int iPart = (int) val;
        int dPart = (int) ((val - iPart) * 1000);
        buf[off + 0] = (byte) (iPart / 256);
        buf[off + 1] = (byte) (iPart % 256);
        buf[off + 2] = (byte) (dPart / 256);
        buf[off + 3] = (byte) (dPart % 256);
    }

    public static void main(String[] args) {

        Antenna antenna = new Antenna(0xA1);
        antenna.startAntenna();
        //antenna.getValue(new byte[]{(byte) 130, (byte) 130, 30, 30}, 0);
    }

    private void connectionStatus(boolean flag) {
        connected = flag;
        stdby.setEnabled(flag);
        strt.setEnabled(flag);
        stp.setEnabled(flag);
        off.setEnabled(flag);
        conn.setSelected(flag);
        if (flag) {
            conn.setText("Connected");
        } else {
            conn.setText("Connect");
            servoStatus.azReady = false;
            servoStatus.elReady = false;
            warning.setText("");
            servoStatus.setSatname("DEFAULT");
        }
        if (flag == false) {
            grp.clearSelection();
        }
    }

    public void setButtonGroup(ButtonGroup grp) {
        this.grp = grp;
        connectionStatus(false);
    }

    public void linkDesignateForm(Designate d) {
        this.designateForm = d;
    }

    private boolean getError(double currAz, double currEl, int wrap) {
        double uwrapAz = 360 - currAz + (1 - wrap) * 360;
        double uwcmdAz = 360 - commandAz + (1 - wrap) * 360;
        if (uwcmdAz < 60 || uwcmdAz > 600) {
            uwcmdAz = 360 - commandAz + (wrap) * 360;
        }
        double azError = uwcmdAz - uwrapAz;
        double elError = commandEl - currEl;
        return Math.abs(azError) > 1 || elError > 1;
    }
    
    public boolean isRunning(){
        return keepSending;
    }
    
}
