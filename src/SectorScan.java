/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Istrac
 */
public class SectorScan {

    private double az, el;
    private final double START_AZ;
    private final double END_AZ;
    private final double START_EL;
    private final double END_EL;
    private final double AZ_STEP = 1, EL_STEP = 1;
    private final double AZ_SECTOR = 20, EL_SECTOR = 20;
    private final double AZ_MID = 30, EL_MID = 20;
    private Angle angle;
    private int az_direction = 1;
    private int el_direction = 1;

    public SectorScan() {
        START_AZ = wrap(AZ_MID - AZ_SECTOR / 2);
        END_AZ = wrap(AZ_MID + AZ_SECTOR / 2);
        START_EL = EL_MID - EL_SECTOR / 2;
        END_EL = EL_MID + EL_SECTOR / 2;
        az = START_AZ;
        el = START_EL;
        angle = new Angle();
    }

    public Angle getHSECAngle() {
        boolean finished = false;
        if (isAzLimitReached()) {
            if (isElLimitReached()) {
                finished = true;
            } else {
                az_direction = -az_direction;
                el += EL_STEP;
            }
        } else {
            az = wrap(az + az_direction * AZ_STEP);
        }
        if (finished) {
            angle.setUnavailable();
        } else {
            angle.az = az;
            angle.el = el;
        }
        return angle;
    }

    public Angle getVSECAngle() {
        boolean finished = false;
        if (isElLimitReached()) {
            if (isAzLimitReached()) {
                finished = true;
            } else {
                el_direction = -el_direction;
                az += AZ_STEP;
            }
        } else {
            el += el_direction * EL_STEP;
        }
        if (finished) {
            angle.setUnavailable();
        } else {
            angle.az = az;
            angle.el = el;
        }
        return angle;
    }

    private double wrap(double d) {
        double val = d;
        if (d < 0) {
            val += 360;
        } else if (d >= 360) {
            val -= 360;
        }
        return val;
    }

    private boolean isAzLimitReached() {
        boolean result = false;
        double next_az = wrap(az + az_direction * AZ_STEP);
        double limit;
        if (az_direction > 0) {
            limit = END_AZ;
            if (next_az > az) {
                result = (next_az > limit) && (az <= limit);
            } else {
                result = next_az > limit || az <= limit;
            }
        } else {
            limit = START_AZ;
            if (next_az < az) {
                result = next_az < limit && az >= limit;
            } else {
                result = next_az < limit || az >= limit;
            }
        }
        return result;
    }

    private boolean isElLimitReached() {
        double next_el = el + el_direction * EL_STEP;
        if (el_direction > 0) {
            return next_el > END_EL;
        } else {
            return next_el < START_EL;
        }
    }
}
