// inspired by https://www.belimo.com/mam/americas/pictures_and_graphics/marketing/teaser_images/iot/MAN18_Belimo_IoT_Glossary_EN-US.pdf

/**
 * Controller: Anything that has the capability to affect a physical entity, like changing its state or moving it.
 */
interface Controller {
    suspend fun startControlling(): Boolean
    val devices: Set<Device>
}

interface Device: Identifiable

/**
 * Sensor: To determine certain physical or chemical characteristics and transform them into an electrical signal to make them digitally process able.
 * Sensors form the backbone of the IoT, helping to bridge the gap between digital and physical.
 */
interface Sensor : Device {
    suspend fun startSensing()
}

interface SensorWithState<Value> : Sensor {
    val lastValue: InstantValue<Value>?
}

/**
 * Actuator: Actuators transform electrical signals (energy, usually transported by air, electric current, or liquid) into different forms of energy
 * such as motion or pressure. This is the opposite of what sensors do, which is to capture physical characteristics and transform them into electrical signals.
 */
interface Actuator : Device

/*
 * Device: Technical physical component (hardware) with communication capabilities to other IT systems.
 * A device can be either attached, embedded inside a physical entity, or monitor a physical entity in its vicinity.
 *
 * Observer: Anything that has the capability to monitor a physical entity, like its state or location.
 */

