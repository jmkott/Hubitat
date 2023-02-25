/**
 * SmartThings Device Type for Aeon Home Energy Monitor v1 (HEM v1) (Ported to Hubitat)
 * (Goal of project is to) Displays individual values for each clamp (L1, L2) for granular monitoring
 * Example: Individual circuits (breakers) of high-load devices, such as HVAC or clothes dryer
 *
 * Original Author: Copyright 2014 Barry A. Burke
 * Original Author: J.R. Farrar
 * 
 * V1.2 2022/02/20 J.M.Kott
 * fixed a few Bugs 
 * - Meter type 33 was wrong.  Changed to meter type 1
 * - PowerL2 section was comparing new value to PowerL1
 * - Total power was updating Display, but each line was not updating Display.  Fixed.
 * - Made power unit a user setting to allow for better formatting on mobile dashboards.
 * 
 **/

metadata {
	// Automatically generated. Make future change here.
	definition (
		name: 		"AEON HEM DSB09 v1-jmkott",
		namespace: 	"jmkott",
		category: 	"HEM",
		author: 	"J.M.Kott"
		importUrl: 	"https://raw.githubusercontent.com/jmkott/Hubitat/main/Drivers/Aeotec/AEON%20HEM%20DSB09%20v1-jmkott.groovy" 
	)
	{
    	capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
        capability "Refresh"
        capability "Polling"
        // capability "Battery"

        attribute "energy", "string"
        attribute "energyOne", "string"
        attribute "energyTwo", "string"
		attribute "power", "string"
        attribute "powerOne", "number"
        attribute "powerTwo", "number"
        attribute "volts", "string"
        attribute "voltage", "string"		// We'll deliver both, since the correct one is not defined anywhere

        attribute "energyDisp", "string"
		attribute "energy1Disp", "string"
        attribute "energy2Disp", "string"
        attribute "powerDisp", "string"
        attribute "power1Disp", "string"
        attribute "power2Disp", "string"


        command "reset"
        command "configure"
        command "refresh"
        command "poll"
       // command "recreateChildDevices"
       // command "deleteChildren"

		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"

//		fingerprint deviceId: "0x3101", inClusters: "0x70,0x32,0x60,0x85,0x56,0x72,0x86"
	}
    preferences {
  		input "voltageValue", "number",
        	title: "Voltage being monitored",
            defaultValue: 120,
            range: "110..240",
            required: false,
            displayDuringSetup: true
  		input "totalName", "string",
        	title: "Total Name",
            defaultValue: "Total" as String,
            required: false,
            displayDuringSetup: true
  		input "c1Name", "string",
        	title: "Clamp 1 Name",
            defaultValue: "Clamp 1" as String,
            required: false,
            displayDuringSetup: true
        input "c2Name", "string",
        	title: "Clamp 2 Name",
            defaultValue: "Clamp 2" as String,
            required: false,
            displayDuringSetup: true
        input "PowerUnit", "string",
        	title: "Power Display Unit (W or Watts) for use in the Power Display attribute",
            defaultValue: "W" as String,
            required: false,
            displayDuringSetup: true
		input "kWhCost", "string",
			title: "Enter your cost per kWh (or just use the default, or use 0 to not calculate):",
			defaultValue: 0.06,
			required: false,
			displayDuringSetup: true
        input "reportType", "number",
			title: "ReportType: Send watt/kWh data on a time interval (0), or on a change in wattage (1)? Enter a 0 or 1:",
			defaultValue: 0,
			range: "0..1",
			required: false,
			displayDuringSetup: true
		input "wattsChangedWhole", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this many watts Whole Meter (range 0 - 32,000W)",
			defaultValue: 50,
			range: "0..32000",
			required: false,
			displayDuringSetup: true
        input "wattsChangedOne", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this many watts Clamp 1: (range 0 - 32,000W)",
			defaultValue: 50,
			range: "0..32000",
			required: false,
			displayDuringSetup: true
        input "wattsChangedTwo", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this many watts Clamp 2: (range 0 - 32,000W)",
			defaultValue: 50,
			range: "0..32000",
			required: false,
			displayDuringSetup: true
		input "wattsPercent", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this percent(whole meter): (range 0 - 99%)",
			defaultValue: 10,
			range: "0..99",
			required: false,
			displayDuringSetup: true
        input "wattsPercent1", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this percent(Clamp1): (range 0 - 99%)",
			defaultValue: 10,
			range: "0..99",
			required: false,
			displayDuringSetup: true
        input "wattsPercent2", "number",
			title: "For ReportType = 1, Don't send unless watts have changed by this percent(Clamp2): (range 0 - 99%)",
			defaultValue: 10,
			range: "0..99",
			required: false,
			displayDuringSetup: true
		input "secondsWatts", "number",
			title: "For ReportType = 0, Send Watts data every how many seconds? (range 0 - 65,000 seconds)",
			defaultValue: 60,
			range: "0..65000",
			required: false,
			displayDuringSetup: true
		input "secondsKwh", "number",
			title: "For ReportType = 0, Send kWh data every how many seconds? (range 0 - 65,000 seconds)",
			defaultValue: 180,
			range: "0..65000",
			required: false,
			displayDuringSetup: true
		input "secondsBattery", "number",
			title: "If the HEM has batteries installed, send battery data every how many seconds? (range 0 - 65,000 seconds)",
			defaultValue: 43200,
			range: "0..65000",
			required: false,
			displayDuringSetup: true
        input name: 'logDebugMessages', type: 'bool',
            title: 'Enable Debug Logging',
            defaultValue: false, 
            required: true
    }

}

def installed() {
	reset()						// The order here is important
	configure()					// Since reports can start coming in even before we finish configure()
	refresh()
}

def updated() {
    logDebug "updated"

 //   if (!childDevices) {
 //      createChildDevices()
 //   }
 //   else if (device.label != state.oldLabel) {
 //       childDevices.each {
 //           def newLabel = "$device.displayName (CH${channelNumber(it.deviceNetworkId)})"
 //           it.setLabel(newLabel)
 //       }

 //       state.oldLabel = device.label
 //   }

	configure()
	resetDisplay()
	refresh()
}

def parse(String description) {
    logDebug "parse"
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result) {
		logDebug "Parse returned ${result?.descriptionText}"
		return result
	} else {
	}
}


def zwaveEvent(hubitat.zwave.commands.meterv1.MeterReport cmd) {
    logDebug "zwaveEvent.meterv1"
    def dispValue
    def newValue
    def formattedValue

	//def timeString = new Date().format("h:mm a", location.timeZone)
//    logDebug "in zwaveEvent for Total"
//    logDebug "Total cmd.meterType ${cmd.meterType}"
//    logDebug "Total cmd.Scale ${cmd.scale}"

//    if (cmd.meterType == 33) {
    if (cmd.meterType == 1) {
		if (cmd.scale == 0) {
//        	logDebug "in setting Total Energy"
            newValue = Math.round(cmd.scaledMeterValue * 100) / 100
        	if (newValue != state.energyValue) {
        		formattedValue = String.format("%5.2f", newValue)
                dispValue = "${totalName}\n${formattedValue}\nkWh"		// total kWh label
                sendEvent(name: "energyDisp", value: dispValue as String, unit: "", descriptionText: "Display Energy: ${newValue} kWh", displayed: false)
                state.energyValue = newValue
                [name: "energy", value: newValue, unit: "kWh", descriptionText: "Total Energy: ${formattedValue} kWh"]

            }
		}
		else if (cmd.scale==2) {
//        	logDebug "in setting Total Power"
        	newValue = Math.round(cmd.scaledMeterValue*10)/10
            formattedValue = String.format("%5.1f", newValue)
        	//newValue = Math.round(cmd.scaledMeterValue)		// really not worth the hassle to show decimals for Watts
        	if (newValue != state.powerValue) {
    			dispValue = "${totalName}\n"+newValue+"\n${PowerUnit}"	// Total watts label
                sendEvent(name: "powerDisp", value: dispValue as String, unit: "", descriptionText: "Display Power: ${newValue} ${PowerUnit}", displayed: false)
                state.powerValue = newValue
                [name: "power", value: newValue, unit: "W", descriptionText: "Total Power: ${formattedValue} ${PowerUnit}"]
           }
		}
 	}
}

def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    logDebug "zwaveEvent.multichannelv3"
	def dispValue
	def newValue
	def formattedValue

//    logDebug "in zwaveEvent for MultiChannel"
//    logDebug "MultiChannel cmd.sourceEndPoint ${cmd.sourceEndPoint}"
//    logDebug "MultiChannel cmd.commandClass ${cmd.commandClass}"

   	if (cmd.commandClass == 50) {
   		def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1]) // can specify command class versions here like in zwave.parse
		if (encapsulatedCommand) {
			if (cmd.sourceEndPoint == 1) {
				if (encapsulatedCommand.scale == 2 ) {
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 10) / 10
                    formattedValue = String.format("%5.1f", newValue)
                    //logDebug "newValue ${newValue} ; state.powerL1 ${state.powerL1} ; formattedValue ${formattedValue}"
                    if (newValue != state.powerL1) {
                        //dispValue = "${formattedValue}"	// L1 Watts Label
					    dispValue = "${c1Name}\n${formattedValue}\n${PowerUnit}"	// L1 Watts Label
                        sendEvent(name: "power1Disp", value: dispValue as String, unit: "", descriptionText: "Display L1 Power: ${newValue} Watts", displayed: false)
						state.powerL1 = newValue
                        //logDebug "newValue ${newValue} ; dispValue ${dispValue} ; state.powerL1 ${state.powerL1} ; formattedValue ${formattedValue}"
						[name: "powerOne", value: newValue, unit: "W", descriptionText: "L1 Power: ${formattedValue} ${PowerUnit}"]
					}
				}
				else if (encapsulatedCommand.scale == 0 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
					formattedValue = String.format("%5.2f", newValue)
					if (newValue != state.energyL1) {
    					dispValue = "${c1Name}\n${formattedValue}\nkWh"		// L1 kWh label
                        //dispValue = "${formattedValue}"
                        sendEvent(name: "energy1Disp", value: dispValue as String, unit: "", descriptionText: "Display L1 Energy: ${newValue} kWh", displayed: false)
						state.energyL1 = newValue
						[name: "energyOne", value: newValue, unit: "", descriptionText: "L1 Energy: ${formattedValue} kWh"]
					}
				}
			}
			else if (cmd.sourceEndPoint == 2) {
				if (encapsulatedCommand.scale == 2 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 10) / 10
                    formattedValue = String.format("%5.1f", newValue)
                    //logDebug "newValue ${newValue} ; state.powerL2 ${state.powerL2} ; formattedValue ${formattedValue}"
					if (newValue != state.powerL2) {
                        //dispValue = "${formattedValue}"	// L2 Watts Label
                        dispValue = "${c2Name}\n${formattedValue}\n${PowerUnit}"	// L2 Watts Label
                        sendEvent(name: "power2Disp", value: dispValue as String, unit: "", descriptionText: "Display L2 Power: ${newValue} Watts", displayed: false)
						state.powerL2 = newValue
						[name: "powerTwo", value: newValue, unit: "", descriptionText: "L2 Power: ${formattedValue} ${PowerUnit}"]
					}
				}
				else if (encapsulatedCommand.scale == 0 ){
					newValue = Math.round(encapsulatedCommand.scaledMeterValue * 100) / 100
					formattedValue = String.format("%5.2f", newValue)
					if (newValue != state.energyL2) {
                        //dispValue = "${formattedValue}"
					    dispValue = "${c2Name}\n${formattedValue}\nkWh"		// L2 kWh label
                        sendEvent(name: "energy2Disp", value: dispValue as String, unit: "", descriptionText: "Display L2 Energy: ${newValue} kWh", displayed: false)
						state.energyL2 = newValue
						[name: "energyTwo", value: newValue, unit: "", descriptionText: "L2 Energy: ${formattedValue} kWh"]
					}
				}
			}
		}
	}
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    logDebug "zwaveEvent.batteryv1"
	def map = [:]
	map.name = "battery"
	map.unit = "%"

	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} battery is low"
		map.isStateChange = true
	}
	else {
		map.value = cmd.batteryLevel
	}
	//logDebug map
	return map
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
    logDebug "Unhandled event ${cmd}"
	[:]
}

def refresh() {			// Request HEMv1 to send us the latest values for the 4 we are tracking
	logDebug "refresh()"

	delayBetween([
		zwave.meterV2.meterGet(scale: 1).format(),		// Change 0 to 1 if international version
		zwave.meterV2.meterGet(scale: 2).format(),
	])
    resetDisplay()
}

def poll() {
	logDebug "poll()"
	refresh()
}

def resetDisplay() {
	logDebug "resetDisplay()"

    sendEvent(name: "powerDisp", value: "${totalName}\n" + state.powerValue + "\n${PowerUnit}", unit: "W")
    sendEvent(name: "energyDisp", value: "${totalName}\n" + state.energyValue + "\nkWh", unit: "kWh")
    sendEvent(name: "power1Disp", value: c1Name + "\n" + state.powerL1 + "\n${PowerUnit}", unit: "W")
    sendEvent(name: "energy1Disp", value: c1Name + "\n" + state.energyL1 + "\nkWh", unit: "kWh")
    sendEvent(name: "power2Disp", value: c2Name + "\n" + state.powerL2 + "\n${PowerUnit}", unit: "W")
    sendEvent(name: "energy2Disp", value: c2Name + "\n" + state.energyL2 + "\nkWh", unit: "kWh")
}

def reset() {
	logDebug "reset()"

    state.energyValue = ""
    state.powerValue = ""
    state.energyL1 = ""
    state.energyL2 = ""
    state.powerL1 = ""
    state.powerL2 = ""

    resetDisplay()

	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format()
	]

    configure()
}

def configure() {
	logDebug "configure()"

	Long kwhDelay = settings.kWhDelay as Long
    Long wDelay = settings.wDelay as Long

    if (kwhDelay == null) {		// Shouldn't have to do this, but there seem to be initialization errors
		kwhDelay = 15
	}

	if (wDelay == null) {
		wDelay = 15
	}

	def cmd = delayBetween([

	// Perform a complete factory reset. Use this all by itself and comment out all others below.
	// Once reset, comment this line out and uncomment the others to go back to normal
//	zwave.configurationV1.configurationSet(parameterNumber: 255, size: 4, scaledConfigurationValue: 1).format()

        zwave.configurationV1.configurationSet(parameterNumber: 1, size: 2, scaledConfigurationValue: voltageValue).format(),		// assumed voltage
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: reportType).format(),			// Disable (=0) selective reporting
		zwave.configurationV1.configurationSet(parameterNumber: 4, size: 2, scaledConfigurationValue: wattsChangedWhole).format(),			// Don't send whole HEM unless watts have changed by 1
		zwave.configurationV1.configurationSet(parameterNumber: 5, size: 2, scaledConfigurationValue: wattsChangedOne).format(),			// Don't send L1 Data unless watts have changed by 1
		zwave.configurationV1.configurationSet(parameterNumber: 6, size: 2, scaledConfigurationValue: wattsChangedTwo).format(),			// Don't send L2 Data unless watts have changed by 1
		zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, scaledConfigurationValue: wattsPercent).format(),			// Or by 5% (whole HEM)
		zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: wattsPercent1).format(),			// Or by 5% (L1)
	    zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: wattsPercent2).format(),			// Or by 5% (L2)

		zwave.configurationV1.configurationSet(parameterNumber: 100, size: 4, scaledConfigurationValue: 1).format(),		// reset to defaults
		zwave.configurationV1.configurationSet(parameterNumber: 110, size: 4, scaledConfigurationValue: 1).format(),		// reset to defaults
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 772).format(),		// watt (don't change)
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: secondsWatts).format(), 	// every %Delay% seconds
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 6152).format(),   	// kwh (don't change)
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: secondsKwh).format(), // Every %Delay% seconds
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 1).format(),		// battery (don't change)
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: secondsBattery).format() 		// every hour


	], 2000)


	logDebug cmd
	cmd
}

//
// Custom Methods
//

def logDebug(message) {
    if (logDebugMessages) log.debug message
}
