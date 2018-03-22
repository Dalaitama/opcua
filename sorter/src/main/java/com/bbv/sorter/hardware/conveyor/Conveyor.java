package com.bbv.sorter.hardware.conveyor;


public interface Conveyor {

    /**
     * Return the Status of the Conveyor
     * true = Online
     * false = Offline
     *
     * @return the value
     */
    boolean getStatus();


    /**
     * Return the Mode of the Conveyor
     * 1 = Started
     * 0 = Stopped
     *
     * @return the value
     */
    int getMode();


    void start();

    void stop();

    boolean readLightBarrier1();

    boolean readLightBarrier2();

    boolean readLightBarrier3();

    double readSpeed();

    boolean readValve1();

    boolean readValve2();

    boolean readValve3();

    void setValve1(boolean pressure);

    void setValve2(boolean pressure);

    void setValve3(boolean pressure);

    int getLastProcessedColor();


}
