package com.efttt.lockall;

public class TimerHelper {
    private long sumOn = 0;
    private long lastOn = System.currentTimeMillis();
    private long lastOff = 0;
    private long restStart = 0;
    private boolean isShowing = false;

    public long getSumOn() {
        return sumOn;
    }

    public void setSumOn(long sumOn) {
        this.sumOn = sumOn;
    }

    public long getLastOn() {
        return lastOn;
    }

    public void setLastOn(long lastOn) {
        this.lastOn = lastOn;
    }

    public long getRestStart() {
        return restStart;
    }

    public void setRestStart(long restStart) {
        this.restStart = restStart;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
    }

    public long getLastOff() {
        return lastOff;
    }

    public void setLastOff(long lastOff) {
        this.lastOff = lastOff;
    }
}
