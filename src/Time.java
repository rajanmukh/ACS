/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.Calendar;
import java.util.Locale;

/**
 *
 * @author Administrator
 */
public class Time {

    private static Calendar cald;
    private static boolean datechanged;
    int hr, min, sec;
    private static final int offset = 0;//19800000;

    public static Time getInstance() {
        cald = Calendar.getInstance();
        int hr = cald.get(Calendar.HOUR);
        int min = cald.get(Calendar.MINUTE);
        int sec = cald.get(Calendar.SECOND);
        if(cald.get(Calendar.AM_PM)==Calendar.PM){
            hr=hr+12;
        }
        return new Time(hr, min, sec);
    }

    public Time(int hr, int min, int sec) {
        this.hr = hr;
        this.min = min;
        this.sec = sec;


    }

    public static boolean isDatechanged() {
        return datechanged;
    }

    public static String getTime() {
        cald = Calendar.getInstance(Locale.ENGLISH);
        long timeInMillis = cald.getTimeInMillis();
        cald.setTimeInMillis(timeInMillis + offset);
        int hr = cald.get(Calendar.HOUR_OF_DAY);
        int min = cald.get(Calendar.MINUTE);
        int sec = cald.get(Calendar.SECOND);
        
        datechanged = ((hr == 0) && (min == 0) && (sec == 0));
        String hour = (hr < 10) ? "0" + hr : "" + hr;
        String minute = (min < 10) ? "0" + min : "" + min;
        String seconds = (sec < 10) ? "0" + sec : "" + sec;
        String timeStr = hour + ":" + minute + ":" + seconds;
        return timeStr;
    }

    public static String getPreciseTime() {
        cald = Calendar.getInstance(Locale.ENGLISH);
        long timeInMillis = cald.getTimeInMillis();
        cald.setTimeInMillis(timeInMillis + offset);
        int hr = cald.get(Calendar.HOUR);
        int min = cald.get(Calendar.MINUTE);
        int sec = cald.get(Calendar.SECOND);
        String s = (cald.get(Calendar.AM_PM) == 0) ? "AM" : "PM";
        int ms = cald.get(Calendar.MILLISECOND);

        String hour = (hr < 10) ? "0" + hr : "" + hr;
        String minute = (min < 10) ? "0" + min : "" + min;
        String seconds = (sec < 10) ? "0" + sec : "" + sec;
        String milli = String.valueOf(ms);
        if (ms < 100) {
            if (ms < 10) {
                milli = "00" + milli;
            } else {
                milli = "0" + milli;
            }
        }
        String timeStr = hour + ": " + minute + ": " + seconds + "." + milli + " " + s;
        return timeStr;
    }

    public static String getDate() {

        cald = Calendar.getInstance(Locale.ENGLISH);
        long timeInMillis = cald.getTimeInMillis();
        cald.setTimeInMillis(timeInMillis + offset);
        int d = cald.get(Calendar.DATE);
        int m = cald.get(Calendar.MONTH) + 1;
        int y = cald.get(Calendar.YEAR);
        String day = (d < 10) ? "0" + d : "" + d;
        String mon = (m < 10) ? "0" + m : "" + m;
        String dateStr = day + "-" + mon + "-" + y;
        return dateStr;
    }
    public static String getTimeStamp(){
        cald = Calendar.getInstance(Locale.ENGLISH);
        long timeInMillis = cald.getTimeInMillis();
        cald.setTimeInMillis(timeInMillis + offset);
        int d = cald.get(Calendar.DATE);
        int m = cald.get(Calendar.MONTH) + 1;
        int y = cald.get(Calendar.YEAR);
        int hr = cald.get(Calendar.HOUR_OF_DAY);
        int min = cald.get(Calendar.MINUTE);
        int sec = cald.get(Calendar.SECOND);
        String day = (d < 10) ? "0" + d : "" + d;
        String mon = (m < 10) ? "0" + m : "" + m;                
        String hour = (hr < 10) ? "0" + hr : "" + hr;
        String minute = (min < 10) ? "0" + min : "" + min;
        String seconds = (sec < 10) ? "0" + sec : "" + sec;
        String timeStamp = day + "," + mon + "," + y+","+hour + "," + minute + "," + seconds;
        return timeStamp;
    }
    
    public static String getDateTime(){
        cald = Calendar.getInstance(Locale.ENGLISH);
        long timeInMillis = cald.getTimeInMillis();
        cald.setTimeInMillis(timeInMillis + offset);
        int d = cald.get(Calendar.DATE);
        int m = cald.get(Calendar.MONTH) + 1;
        int y = cald.get(Calendar.YEAR);
        int hr = cald.get(Calendar.HOUR_OF_DAY);
        int min = cald.get(Calendar.MINUTE);
        int sec = cald.get(Calendar.SECOND);
        String day = (d < 10) ? "0" + d : "" + d;
        String mon = (m < 10) ? "0" + m : "" + m;                
        String hour = (hr < 10) ? "0" + hr : "" + hr;
        String minute = (min < 10) ? "0" + min : "" + min;
        String seconds = (sec < 10) ? "0" + sec : "" + sec;
        String timeStamp = y + "-" + mon + "-" + day+" "+hour + ":" + minute + ":" + seconds+" ";
        return timeStamp;
    }
    
}
