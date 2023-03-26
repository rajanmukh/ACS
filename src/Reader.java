
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author RDA digital
 */
public class Reader {

    Angle angle;
    int angleIdx = 0;
    String line;
    private BufferedReader br = null;
    int count = 0;
    double azEpoch, elEpoch;
    int direction;
    private int COUNTLIMIT = 50;
    String satName = "", prevSatName = "";
    String currAOS = "", currLOS = "";
    private boolean changeFlag = true;
    File folder;
    LinkedList newfiles;
    File[] prevFiles;

    private String[] satnum;
    private TimeStruct[] AOS;
    private TimeStruct[] LOS;
    private int satindex = 0;
    private int noOfPass;
    private boolean transition;
    private TimeStruct readTime;// readYr,readMon,readDay,readHr,readMin,readSec;
    private String ID;
    private Angle[] transtionAngles;
    private final Angle invalidAngle;
    private ServoStatus servoStat;
    private static final int SCHEDULE_OVER = -2, TRANSITION = -1;
    private static String SCHEDULARPATH;
    private String readPath;
    private AntennaActionListener listener;
    private int restTime;
    private Timer startTimer;
    private TimeStruct transStartTime;

    public void setListener(AntennaActionListener listener) {
        this.listener = listener;
    }

    public Reader(int id) {
        ID = String.format("%X", id);
        readTime = new TimeStruct();
        angle = new Angle();
        invalidAngle = new Angle();
        invalidAngle.setUnavailable();
        try {
            File file = new File("pathConfig.txt");
            BufferedReader t = new BufferedReader(new FileReader(file));
            SCHEDULARPATH = t.readLine().trim();

        } catch (IOException ex) {
        }
        readPath = "BUFFER/" + ID;
        startTimer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transition = false;
                listener.makeReady();
            }
        });
        startTimer.setRepeats(false);
        //readNewSchedule();

    }

    public Angle getAngle(TimeStruct cmdTime) {

        int currIndex = determineSatIndex(cmdTime);
        if (currIndex == SCHEDULE_OVER) {
            readNewSchedule();
            currIndex = determineSatIndex(cmdTime);
            if (currIndex == SCHEDULE_OVER) {
                return invalidAngle;
            }
        }
        if (currIndex != satindex) {
            loadSattrack(currIndex);

            if (cmdTime.isBehind(AOS[satindex])) {
                generateIntermediateAngles(cmdTime);
                transition = true;
                setSatName("DEFAULT");
                System.out.println("Transition begins");
            }
        }

        if (transition) {
            if (cmdTime.isAheadof(AOS[satindex])) {
                transition = false;
                setSatName(satnum[satindex]);
                System.out.println("Transition ends");
            }
        }

        if (transition) {
            int diff = cmdTime.getDifference(transStartTime);
            int len = transtionAngles.length;
            if (diff < len) {
                return transtionAngles[diff];
            } else {
                listener.makeStandby();
                startTimer.setInitialDelay(restTime * 1000);
                startTimer.start();
                return transtionAngles[transtionAngles.length - 1];
            }
        } else {
            return readAngle(cmdTime);

        }
    }

    public void setTransition(boolean transition) {
        this.transition = transition;
    }

    public static void main(String[] args) {
        Reader reader = new Reader(161);
        TimeStruct currtime = new TimeStruct(20222, 10, 26, 2, 0, 30);
//        currtime.setCurrentTime();

        for (int i = 0; i < 10000; i++) {
            Angle angle1 = reader.getAngle(currtime);

            currtime.advance(30);
        }
        int i = 0;
    }

    public boolean isSatChanged() {
        boolean result = false;
        if (changeFlag == true) {
            changeFlag = false;
            result = true;
        }
        return result;
    }

    public boolean isInTransition() {
        return transition;
    }

    void link(ServoStatus servoStatus) {
        servoStat = servoStatus;
    }

    private void loadSattrack(int index) {
        satindex = index;
        try {
            if (br != null) {
                br.close();
                br = null;
            }

            String fileName = satnum[satindex] + "_SatOrbit.pgm";
            File file = new File(readPath + "/pgm/" + fileName);
            setSatName(satnum[satindex]);
            br = new BufferedReader(new FileReader(file));
            br.readLine();
            br.readLine();
            readTime = new TimeStruct();

        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, satnum[satindex] + " angle file not available");
        } catch (IOException ex) {
            Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setSatName(String str) {
        prevSatName = satName;
        satName = str;
        currAOS = AOS[satindex].getTime();
        if (!str.equals("DEFAULT")) {
            currLOS = LOS[satindex].getTime();
        } else {
            currLOS = "";
        }

        if (!satName.equals(prevSatName)) {
            changeFlag = true;
        }
    }

    public void readNewSchedule() {

        try {
            if (br != null) {
                br.close();
                br = null;
            }
            copySchedularTo(readPath);
            
            File schFile = new File(readPath + "/meogen.sch");
            BufferedReader schread = new BufferedReader(new FileReader(schFile));
            for (int i = 0; i < 3; i++) {//discard first 3 lines
                schread.readLine();
            }

            satnum = new String[300];
            AOS = new TimeStruct[300];
            LOS = new TimeStruct[300];
            satindex = 0;
            int i = 0;
            String ln;
            while ((ln = schread.readLine()) != null) {
                String[] p = parse(ln);
                if (p[0] == null) {
                    break;
                }
                if (p.length > 2) {
                    String satID = p[2];
                    if (satID.equals(ID)) {
                        satnum[i] = p[1];
                        AOS[i] = new TimeStruct();
                        LOS[i] = new TimeStruct();
                        String dateStr = p[0];
                        AOS[i].parseDate(dateStr);
                        LOS[i].parseDate(dateStr);
                        AOS[i].parseTime(p[3]);
                        LOS[i].parseTime(p[4]);
                        if (LOS[i].isBehind(AOS[i])) {
                            LOS[i].advance(86400);
                        }
                        i++;
                    }
                }
            }
            schread.close();
            noOfPass = i;
            loadSattrack(0);

        } catch (IOException ex) {
            Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

        }
    }

    private void generateIntermediateAngles(TimeStruct cmdTime) {
        int currentwrap, nextwrap;

        currentwrap = 1 - servoStat.cwrp;

        TimeStruct simTime = new TimeStruct(AOS[satindex]);
        Angle simCmdAngle = readAngle(simTime);
        Angle nextAOSangle = simCmdAngle.clone();
        if (simCmdAngle.az > 300) {
            nextwrap = 1;//CCW
        } else if (simCmdAngle.az < 60) {
            nextwrap = 0;//CW
        } else {
            nextwrap = currentwrap;
            while (simTime.isBehind(LOS[satindex])) {
                double nextAz = (360 - simCmdAngle.az) + currentwrap * 360;
                if (nextAz < 60) {//limit reach condition
                    if (!simTime.isEqual(AOS[satindex])) {
                        int kk = 0;
                    }
                    nextwrap = 1;//CCW
                    break;
                } else if (nextAz > 660) {
                    if (!simTime.isEqual(AOS[satindex])) {
                        int kk = 0;
                    }
                    nextwrap = 0;//CW
                    break;
                }
                simTime.advance(30);
                simCmdAngle = readAngle(simTime);
            }
        }
        double currAz = (360 - servoStat.az) + currentwrap * 360;
        double currEl = servoStat.el;
        double finalAz = (360 - nextAOSangle.az) + nextwrap * 360;
        double finalEl = nextAOSangle.el;
        int trTimeSpan = AOS[satindex].getDifference(cmdTime);
        transStartTime = new TimeStruct(cmdTime);
        restTime = 0;
        if (trTimeSpan > 200) {
            restTime = trTimeSpan - 180;
            trTimeSpan = 180;
        }
        trTimeSpan -= 10;
        transtionAngles = new Angle[trTimeSpan + 20];
        double azStep = (finalAz - currAz) / trTimeSpan;
        double elStep = (finalEl - currEl) / trTimeSpan;
        for (int i = 1; i <= trTimeSpan; i++) {
            double az = currAz + i * azStep;
            double el = currEl + i * elStep;
            if (az > 360) {
                az = 720 - az;
            } else {
                az = 360 - az;
            }
            transtionAngles[i - 1] = new Angle(az, el);
        }
        for (int i = 0; i < 20; i++) {
            transtionAngles[trTimeSpan + i] = transtionAngles[trTimeSpan - 1];
        }
    }

    private int determineSatIndex(TimeStruct aTime) {
        int index = satindex;
        while (aTime.isAheadof(LOS[index])) {
            index++;
            if (index == noOfPass) {
                return SCHEDULE_OVER;
            }
        }
        return index;
    }
//        if (scheduleOver) {
//            JOptionPane.showMessageDialog(null, "Load new schedule and satellite track");
//        } else {
//            
//            
//            if (aTime.isBehind(AOS[satindex])) {
//                if (transition == false) {
//                    generateIntermediateAngles(aTime);
//                }
//                transition = true;
//            } else {
//                if(transition==true){
//                    satindex++;
//                    loadSattrack(satindex);
//                }
//                transition = false;
//            }
//        }
//    }

    private Angle readAngle(TimeStruct time) {
        boolean reinitialized = false;
        while (true) {
            try {
                String[] ss2;
                if (time.isAheadof(readTime)) {
                    while (true) {
                        if (br == null) {
                            return invalidAngle;
                        }
                        line = br.readLine();
                        if (line == null) {
                            return invalidAngle;
                        }
                        ss2 = line.split(" ");
                        if (ss2.length > 8) {
                            break;
                        }
                    }
                    readTime.parseDate(ss2[0]);
                    readTime.parseTime(ss2[1]);

                    int k = 2;
                    if (readTime.isEqual(time)) {
                        String str;
                        while ((str = ss2[k++]).isEmpty()) {
                        }
                        angle.az = Double.parseDouble(str);
                        while ((str = ss2[k++]).isEmpty()) {
                        }
                        angle.el = Double.parseDouble(str);
                        return angle;
                    }
                } else {
                    if (!reinitialized) {
                        loadSattrack(satindex);
                        reinitialized = true;
                    } else {
                        return invalidAngle;
                    }
                }

            } catch (IOException ex) {
                return invalidAngle;

            }
        }
    }

    private synchronized static void copySchedularTo(String path) {
        try {
            
            Files.copy(Paths.get(Reader.SCHEDULARPATH, "/meogen.sch"), Paths.get(path, "/meogen.sch"), StandardCopyOption.REPLACE_EXISTING);
            File dir = new File(Reader.SCHEDULARPATH + "/pgm");
            String[] listFiles = dir.list();;
            for (String fileName : listFiles) {
                if (fileName.endsWith(".pgm")) {
                    Files.copy(Paths.get(Reader.SCHEDULARPATH, "/pgm/", fileName), Paths.get(path, "/pgm/", fileName), StandardCopyOption.REPLACE_EXISTING);
                    
                    Files.copy(Paths.get(Reader.SCHEDULARPATH, "/pgm/", fileName), Paths.get(path, "/pgm/", fileName), StandardCopyOption.REPLACE_EXISTING);
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(Reader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stopTimer() {
        startTimer.stop();
    }

    private String[] parse(String ln) {
        int ind1 = 0, ind2 = 0;
        boolean wordfound = false;
        int j = 0;
        String[] str = new String[6];
        int len = ln.length();
        for (int i = 0; i < len; i++) {
            char c = ln.charAt(i);
            if (!wordfound) {
                if (c != ' ' && c != '\t') {
                    ind1 = i;
                    wordfound = true;
                }
            } else {
                if (c == ' ' || c == '\t' || c == '\n') {
                    //form the substring
                    ind2 = i;
                    wordfound = false;
                    str[j] = ln.substring(ind1, ind2);
                    j++;

                } else if (i == len - 1) {
                    str[j] = ln.substring(ind1, len);
                }
            }
        }
        return str;
    }
}
