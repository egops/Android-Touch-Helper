package com.efttt.lockall;

public class TimerHelper {
    private long sumOn = 0;
    private long lastOn = System.currentTimeMillis();
    private long beginTime = 0;
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

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
    }
}
