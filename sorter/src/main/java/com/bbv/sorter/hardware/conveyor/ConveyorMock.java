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

    volatile boolean status = false;
    volatile boolean mode = false;

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
        return  mode ?  random.nextBoolean(): false;
    }

    @Override
    public boolean readLightBarrier2() {
        return  mode ?  random.nextBoolean(): false;
    }

    @Override
    public boolean readLightBarrier3() {
        return  mode ?  random.nextBoolean(): false;
    }

    @Override
    public double readSpeed() {
        return  mode ?  random.nextInt(10):0.0;
    }


}
