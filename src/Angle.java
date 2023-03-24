/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Rajan
 */
public class Angle {
    
    double az;
    double el;
    
    
    public Angle() {
        
    }
    public static Angle getInvalidAngle(){
        Angle angle = new Angle();
        angle.setUnavailable();
        return angle;
    }

    Angle(double az, double el) {
        this.az=az;
        this.el=el;
    }

    void setUnavailable() {
        az=-1;
        el=-1;
    }

    boolean isAvailable() {
        return az >=0;
    }

    void setFileOver() {
        az=-2;
        el=-2;
    }
    boolean isFileOver() {
        return az == -2;
    }

    
    
    public Angle clone()  { 
        Angle a=new Angle();
        a.az=az;
        a.el=el;
        return a;
    }   
    
}
