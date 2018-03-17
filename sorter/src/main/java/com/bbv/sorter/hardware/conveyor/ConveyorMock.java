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

    private volatile boolean status = false;
    private volatile boolean mode = false;
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
    public boolean getMode() {
        return mode;
    }

    @Override
    public void start() {
        logger.info("Conveyor Started");
        mode = true;
    }

    @Override
    public void stop() {
        logger.info("Conveyor Stopped");
        mode = false;
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
        return mode ? random.nextInt(10) : 0.0;
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

    private boolean getRandomBoolean() {
        return mode ? random.nextBoolean() : false;
    }


}
