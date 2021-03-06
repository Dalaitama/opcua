package com.bbv.sorter.hardware.conveyor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Mock Class
 */
public class ConveyorMock implements Conveyor {

    private static final Conveyor INSTANCE = new ConveyorMock();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile boolean status = true;
    private volatile int mode = 0;
    private volatile boolean valve1 = false;
    private volatile boolean valve2 = false;
    private volatile boolean valve3 = false;

    private final Random random = new Random();


    private ConveyorMock() {
    }

    static Conveyor getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean getStatus() {
        return status;
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void start() {
        logger.info("Conveyor Started");
        mode = 1;
    }

    @Override
    public void stop() {
        logger.info("Conveyor Stopped");
        mode = 0;
    }

    @Override
    public boolean readLightBarrier1() {
        return getRandomBoolean();
    }

    @Override
    public boolean readLightBarrier2() {
        return getRandomBoolean();
    }


    @Override
    public boolean readLightBarrier3() {
        return getRandomBoolean();
    }

    @Override
    public double readSpeed() {
        return mode==1 ? random.nextInt(10) : 0.0;
    }

    @Override
    public boolean readValve1() {
        return valve1;
    }

    @Override
    public boolean readValve2() {
        return valve2;
    }

    @Override
    public boolean readValve3() {
        return valve3;
    }

    @Override
    public void setValve1(boolean pressure) {
        valve1 = pressure;
    }

    @Override
    public void setValve2(boolean pressure) {
        valve2 = pressure;
    }

    @Override
    public void setValve3(boolean pressure) {
        valve3 = pressure;
    }

    @Override
    public int getLastProcessedColor() {
        return  mode==1 ? random.nextInt(7) : 0;
    }

    private boolean getRandomBoolean() {
        return  mode==1 && random.nextBoolean();
    }


}
