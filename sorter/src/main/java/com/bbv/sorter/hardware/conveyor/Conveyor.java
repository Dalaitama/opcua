package com.bbv.sorter.hardware.conveyor;


public interface Conveyor {

    /**
     * Return the Status of the Conveyor
     * true = Online
     * false = Offline
     * @return the value
     */
    boolean getStatus();


    /**
     * Return the Mode of the Conveyor
     * true = Started
     * false = Stopped
     * @return the value
     */
    boolean getMode();


    void start();

    void stop();

    boolean readLightBarrier1();
    boolean readLightBarrier2();
    boolean readLightBarrier3();
    double readSpeed();



}
