package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private AlarmStatus alarmStatus;
    private ArmingStatus armingStatus;
    private Set<Sensor> sensors;
    private Boolean catDetectedState;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;

        sensors = securityRepository.getSensors();

        alarmStatus = securityRepository.getAlarmStatus();
        if(alarmStatus == null){
            alarmStatus = AlarmStatus.NO_ALARM;
        }

        armingStatus = securityRepository.getArmingStatus();
        if(armingStatus == null){
            armingStatus = ArmingStatus.DISARMED;
        }

        catDetectedState = false;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        else
        {
            for (Sensor currentSensor : sensors){
                currentSensor.setActive(false);
            }

            if ((armingStatus == ArmingStatus.ARMED_HOME) && (catDetectedState)){
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }
        this.armingStatus = armingStatus;
        securityRepository.setArmingStatus(armingStatus);

        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        int armedCount = 0;

        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        else {
            for (Sensor currentSensor : sensors) {
                if (currentSensor.getActive() == true){
                    armedCount++;
                }
            }

            // if no cat is detected then disable the alarm as long as no sensors are triggered.
            if (armedCount == 0) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        // remember the cat detected state
        catDetectedState = cat;

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        this.alarmStatus = status;

        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(armingStatus == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(alarmStatus) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(alarmStatus) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            // if in ALARM state do nothing to meet requirement 4:
            // If alarm is active, change in sensor state should not affect the alarm state.
            case ALARM -> {}
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // if the sensor is activated always handle a sensor activation even if
        // the sensor is already activated.
        // If a sensor is activated while already active and the system is in pending state, change it to alarm state.
        if(active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            handleSensorDeactivated();
        }

        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return alarmStatus;
    }

    public Set<Sensor> getSensors() {
        return sensors;
    }

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        sensors.remove(sensor);
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return armingStatus;
    }
}
