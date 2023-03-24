
import java.util.Calendar;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Istrac
 */
public class TimeStruct {

    int year, month, day, hour, min, sec;
    private Calendar c;

    public TimeStruct() {
        c = Calendar.getInstance();
    }

    TimeStruct(TimeStruct aTime) {
        year = aTime.year;
        month = aTime.month;
        day = aTime.day;
        hour = aTime.hour;
        min = aTime.min;
        sec = aTime.sec;
        c = Calendar.getInstance();
    }

    TimeStruct(int year, int month, int day, int hour, int min, int sec) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        c = Calendar.getInstance();
    }

    void parseDate(String string) {
        String[] ss = string.split("-");
        year = Integer.parseInt(ss[0]);
        month = Integer.parseInt(ss[1]);
        day = Integer.parseInt(ss[2]);
    }

    void parseTime(String string) {
        String[] ss = string.split(":");
        hour = Integer.parseInt(ss[0]);
        min = Integer.parseInt(ss[1]);
        sec = (int) Double.parseDouble(ss[2]);
    }

    void setCurrentTime() {
        Calendar ct = Calendar.getInstance();
        year = ct.get(Calendar.YEAR);
        month = ct.get(Calendar.MONTH) + 1;
        day = ct.get(Calendar.DAY_OF_MONTH);
        hour = ct.get(Calendar.HOUR_OF_DAY);
        min = ct.get(Calendar.MINUTE);
        sec = ct.get(Calendar.SECOND);
    }

    boolean isAheadof(TimeStruct aTime) {

        boolean result = false;
        if (year > aTime.year) {
            result = true;
        } else if (year == aTime.year) {
            if (month > aTime.month) {
                result = true;
            } else if (month == aTime.month) {
                if (day > aTime.day) {
                    result = true;
                } else if (day == aTime.day) {
                    if (hour > aTime.hour) {
                        result = true;
                    } else if (hour == aTime.hour) {
                        if (min > aTime.min) {
                            result = true;
                        } else if (min == aTime.min) {
                            if (sec >= aTime.sec) {
                                result = true;
                            }
                        }

                    }
                }
            }
        }
        return result;
    }

    boolean isEqual(TimeStruct aTime) {
        boolean result = false;
        if (sec == aTime.sec) {
            if (min == aTime.min) {
                if (hour == aTime.hour) {
                    if (day == aTime.day) {
                        if (month == aTime.month) {
                            if (year == aTime.year) {
                                result = true;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public int getDifference(TimeStruct aTime){
        c.set(year, month - 1, day, hour, min, sec);
        long millisecs1 = c.getTimeInMillis();
        c.set(aTime.year, aTime.month - 1, aTime.day, aTime.hour, aTime.min, aTime.sec);
        long millisecs2 = c.getTimeInMillis();
        int diff=(int) ((millisecs1-millisecs2)/1000);
        if(diff>200){
            int hgh=0;
        }
        return diff;
    }

    public void advance(int seconds) {

        c.set(year, month - 1, day, hour, min, sec);
        long millisecs = c.getTimeInMillis() + seconds * 1000;
        c.setTimeInMillis(millisecs);
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH) + 1;
        day = c.get(Calendar.DAY_OF_MONTH);
        hour = c.get(Calendar.HOUR_OF_DAY);
        min = c.get(Calendar.MINUTE);
        sec = c.get(Calendar.SECOND);
    }

    boolean isBehind(TimeStruct aTime) {
        boolean result = false;
        if (year < aTime.year) {
            result = true;
        } else if (year == aTime.year) {
            if (month < aTime.month) {
                result = true;
            } else if (month == aTime.month) {
                if (day < aTime.day) {
                    result = true;
                } else if (day == aTime.day) {
                    if (hour < aTime.hour) {
                        result = true;
                    } else if (hour == aTime.hour) {
                        if (min < aTime.min) {
                            result = true;
                        } else if (min == aTime.min) {
                            if (sec < aTime.sec) {
                                result = true;
                            }
                        }

                    }
                }
            }
        }
        return result;
    }

    public String getString(){
        return year+"-"+month+"-"+day+" "+hour+":"+min+":"+sec;
    }
    public String getTime(){
        return String.format("%02d",hour)+":"+String.format("%02d",min)+":"+String.format("%02d",sec);
    }

}
