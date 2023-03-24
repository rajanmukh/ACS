/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Istrac
 */
public class PsudoClock {

    public static class DateTime {
        int yr=2022,mon=10,day=18,hh=14,min=50,ss=0;
    }
    private static DateTime t0=new DateTime();
    private static DateTime t=new DateTime();
    private static int i=0;

    public static DateTime getTime() {
        
        int sec=t0.ss+i*10;
        t.ss=sec%60;
        int min = t0.min+(sec/60);
        t.min = min%60;
        int hr= t0.hh+(min/60);
        t.hh = hr;
        i++;
        return t;
    }

}
