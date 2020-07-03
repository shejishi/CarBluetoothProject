package com.egai.wukong.EventBusMessage;

public class DataStream {

    private int car_speed,motor_speed,oil_tmp,water_tmp;

    public DataStream(int car_speed, int motor_speed, int oil_tmp, int water_tmp) {
        this.car_speed = car_speed;
        this.motor_speed = motor_speed;
        this.oil_tmp = oil_tmp;
        this.water_tmp = water_tmp;
    }

    public int getCar_speed() {
        return car_speed;
    }

    public int getMotor_speed() {
        return motor_speed;
    }

    public int getOil_tmp() {
        return oil_tmp;
    }

    public int getWater_tmp() {
        return water_tmp;
    }

    @Override
    public String toString() {
        return "DataEvent{" +
                "car_speed=" + car_speed +
                ", motor_speed=" + motor_speed +
                ", oil_tmp=" + oil_tmp +
                ", water_tmp=" + water_tmp +
                '}';
    }
}
