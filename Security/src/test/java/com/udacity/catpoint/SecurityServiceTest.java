package com.udacity.catpoint;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;
    @Mock
    BufferedImage bufferedImage;
    private SecurityService securityService;

    // sensors used in the SecurityService
    static Sensor sensor1;
    static Sensor sensor2;
    static Sensor sensor3;

    @BeforeAll
    static void init() {
    }

    @BeforeEach
    void testSetup()
    {
        securityService = new SecurityService(securityRepository, imageService);

        sensor1 = new Sensor ("sensor1",SensorType.MOTION);
        sensor2 = new Sensor ("sensor2",SensorType.DOOR);
        sensor3 = new Sensor ("sensor3",SensorType.WINDOW);
        securityService.addSensor(sensor1);
        securityService.addSensor(sensor2);
        securityService.addSensor(sensor3);

        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor1,false);
        securityService.changeSensorActivationStatus(sensor2,false);
        securityService.changeSensorActivationStatus(sensor3,false);
    }

    @DisplayName("Test 1: Activate single sensor to monitor Alarm Status")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
                names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_1SingleSensorActivation_AlarmStatus(ArmingStatus armingStatus) {

        securityService.setArmingStatus(armingStatus);
        securityService.changeSensorActivationStatus(sensor1,true);

        // expect PENDING status if the system is armed.
        switch(armingStatus) {
            case DISARMED:
                assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
                break;
            case ARMED_HOME:
            case ARMED_AWAY:
                assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
        }
    }

    @DisplayName("Test 2: Activate dual sensors to monitor Alarm Status")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_2MultipleSensorActivations_AlarmStatus(ArmingStatus armingStatus) {

        securityService.setArmingStatus(armingStatus);
        securityService.changeSensorActivationStatus(sensor1,true);
        securityService.changeSensorActivationStatus(sensor2,true);

        // expect ALARM status if the system is armed.
        switch(armingStatus) {
            case DISARMED:
                assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
                break;
            case ARMED_HOME:
            case ARMED_AWAY:
                assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }
    }

    @DisplayName("Test 3: Pending alarm status to none")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_3PendingStatusToNone_AlarmStatus(ArmingStatus armingStatus) {

        securityService.setArmingStatus(armingStatus);
        securityService.changeSensorActivationStatus(sensor1,true);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor1,false);

        // expect no alarm in all arming conditions.
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @DisplayName("Test 4: Active alarm stays active")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_4ActiveAlarmStaysActive_AlarmStatus(ArmingStatus armingStatus) {

        securityService.setArmingStatus(armingStatus);
        securityService.changeSensorActivationStatus(sensor1,true);
        securityService.changeSensorActivationStatus(sensor2,true);
        securityService.changeSensorActivationStatus(sensor3,true);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor1,false);
        securityService.changeSensorActivationStatus(sensor2,false);
        securityService.changeSensorActivationStatus(sensor3,false);

        // expect ALARM status in all cases.
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @DisplayName("Test 5: Activating active sensor sets alarm state to ALARM")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_5ActivateActiveSensor_AlarmStatus(ArmingStatus armingStatus) {

        securityService.setArmingStatus(armingStatus);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor1,true);
        securityService.changeSensorActivationStatus(sensor1,true);

        // expect ALARM status if the system is armed.
        switch(armingStatus) {
            case DISARMED:
                assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
                break;
            case ARMED_HOME:
            case ARMED_AWAY:
                assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }
    }

    @DisplayName("Test 6: Deactivating inactive sensor does not change alarm state")
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class,
            names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    public void changeSensorActivationStatus_6DeactivateInactiveSensor_AlarmStatus(AlarmStatus alarmStatus) {

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.setAlarmStatus(alarmStatus);
        securityService.changeSensorActivationStatus(sensor1,false);
        securityService.changeSensorActivationStatus(sensor1,false);

        assertEquals(alarmStatus, securityService.getAlarmStatus());

    }

    @DisplayName("Test 7: If a cat is detected while Armed-Home sound the alarm")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_7CatDetectedWhileArmedHome_AlarmStatus(ArmingStatus armingStatus) {
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.setArmingStatus(armingStatus);

        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        // expect ALARM status if the system is armed.
        switch(armingStatus) {
            case DISARMED:
            case ARMED_AWAY:
                assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
                break;
            case ARMED_HOME:
                assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }

        // cover the case where the system is armed while a cat is detected.
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.setArmingStatus(armingStatus);

        // expect ALARM status if the system is armed.
        switch(armingStatus) {
            case DISARMED:
            case ARMED_AWAY:
                assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
                break;
            case ARMED_HOME:
                assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }
    }

    @DisplayName("Test 8: If a cat is not detected while Armed-Home do not sound the alarm")
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 , 3})
    public void changeSensorActivationStatus_8CatNotDetectedWhileArmedHome_AlarmStatus(int countArmedSensors) {
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        switch(countArmedSensors){
        default:
            sensor3.setActive(true);
        case 2:
            sensor2.setActive(true);
        case 1:
            sensor1.setActive(true);
        case 0:
            // do nothing
}
        // cover the removal of a sensor
        securityService.removeSensor(sensor3);

        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);

        if (countArmedSensors > 0){
            assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }
        else{
            assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
        }
    }

    @DisplayName("Test 9: If system is disarmed then turn off the alarm.")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"ARMED_HOME", "ARMED_AWAY"})
    public void changeSensorActivationStatus_9SystemDisarmed_AlarmStatus(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
        securityService.setAlarmStatus(AlarmStatus.ALARM);
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @DisplayName("Test 10: If system is armed then reset all sensors.")
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 , 3})
    public void changeSensorActivationStatus_10SystemArmed_AlarmStatus(int countArmedSensors) {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        switch(countArmedSensors){
            default:
                sensor3.setActive(true);
            case 2:
                sensor2.setActive(true);
            case 1:
                sensor1.setActive(true);
            case 0:
                // do nothing
        }

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        int activeCount = 0;

        // count the number of activated sensors.
        for (Sensor currentSensor : securityService.getSensors()) {
            if (currentSensor.getActive() == true){
                activeCount++;
            }
        }

        assertTrue(activeCount == 0);
    }

    @DisplayName("Test 11: If system armed-home while showing a cat, then alarm.")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"DISARMED", "ARMED_AWAY"})
    public void changeSensorActivationStatus_11ArmWhileShowingCat_AlarmStatus(ArmingStatus armingStatus) {
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        securityService.setArmingStatus(armingStatus);

        if (armingStatus == ArmingStatus.ARMED_HOME){
            assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
        }
        else {
            assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
        }
    }

}