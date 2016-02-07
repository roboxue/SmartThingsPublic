/** Android IP Camera
 *
 *  Author: Rob Landry
 * 
 *  URL: http://github.com/roblandry/android-ip-camera.device
 * 
 *  Date: 3/6/15
 *  
 *  Version: 1.0.1
 * 
 *  Description: This is a custom device type. This works with the Android IP Camera app. It allows you to take photos, 
 *  record video, turn on/off the led, focus, overlay, and night vision. It displays various sensors including battery 
 *  level, humidity, temperature, and light (lux). The sensor data is all dependent on what your phone supports.
 * 
 *  Copyright: 2015 Rob Landry
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

preferences
{
	input("username",	"text",		title: "Camera username",	description: "Username for web login")
	input("password",	"password",	title: "Camera password",	description: "Password for web login")
	input("url",		"text",		title: "IP or URL of camera",	description: "Do not include http://")
	input("port",		"text",		title: "Port",			description: "Port")
}

metadata {
	definition (name: "Android IP Camera", author: "Robert Xue", namespace: "roboxue") {
		capability "Presence Sensor"
		capability "Sensor"
		capability "Image Capture"
		capability "Switch"
		capability "Actuator"
		capability "Battery"
		capability "Temperature Measurement"
		capability "Illuminance Measurement"

		command "recordOn"
        command "recordOff"
        command "ledOn"
		command "ledOff"
		command "focusOn"
		command "focusOff"
		command "nightVisionOn"
		command "nightVisionOff"
		command "refresh"
	}
    
    simulator {
		status "present": "presence: 1"
		status "not present": "presence: 0"
	}

	tiles(scale: 2) {
		carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }

		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
			state("default", label: '', action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF")
		}

		standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
			state("take", label: 'Take Photo', action: "Image Capture.take", icon: "st.camera.take-photo", nextState:"taking")
			state("taking", label: 'Taking...', action: "Image Capture.take", icon: "st.camera.take-photo", backgroundColor: "#79b821")
		}

		standardTile("record", "device.record", width: 2, height: 2) {
			state("recordOff", label: 'Record Off', action:"recordOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("recordOn", label: 'Record On', action:"recordOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("led", "device.led", width: 2, height: 2) {
			state("ledOff", label: 'Led Off', action:"ledOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("ledOn", label: 'Led On', action:"ledOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("focus", "device.focus", width: 1, height: 1, decoration:"flat") {
			state("focusOff", label: 'Focus Off', action:"focusOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("focusOn", label: 'Focus On', action:"focusOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("nightVision", "device.nightVision", width: 1, height: 1, decoration:"flat") {
			state("nightVisionOff", label: 'Night Vision Off', action:"nightVisionOn", icon:"st.switches.light.off", backgroundColor: "#ffffff")
			state("nightVisionOn", label: 'Night Vision On', action:"nightVisionOff", icon:"st.switches.light.on", backgroundColor: "#79b821")
		}

		standardTile("refresh", "device.switch", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
			state("default", label:"", action:"refresh", icon:"st.secondary.refresh")
		}
        
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
			state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ebeef2")
		}
        
        valueTile("battery", "device.battery", width: 1, height: 1) {
			state("battery", label:'${currentValue}% battery', unit:"${unit}")
        }
        
        valueTile("temperature", "device.temperature", width: 1, height: 1) {
			state("temperature", label:'${currentValue} °C', unit:"${unit}")
        }
        
        valueTile("illuminance", "device.illuminance", width: 1, height: 1) {
			state("illuminance", label:'${currentValue} lux', unit:"${unit}")
		}
        
        valueTile("pressure", "device.pressure", width: 1, height: 1) {
			state("pressure", label:'${currentValue} mbar', unit:"${unit}")
		}
        
		main "camera"
		details(["cameraDetails","take","record","led","presence","focus","nightVision","refresh","battery","temperature","illuminance","pressure"])
	}
}


def parseCameraResponse(def response) {
	if(response.headers.'Content-Type'.contains("image/jpeg")) {
		def imageBytes = response.data

		if(imageBytes) {
			storeImage(getPictureName(), imageBytes)
		}
	} else {
		log.error("${device.label} could not capture an image.")
	}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	"image" + "_$pictureUuid" + ".jpg"
}

private take() {
	log.info("${device.label} taking photo")

	httpGet("http://${username}:${password}@${url}:${port}/photo_save_only.jpg"){
		httpGet("http://${username}:${password}@${url}:${port}/photo.jpg"){
			response -> log.info("${device.label} image captured")
			parseCameraResponse(response)
		}
	}
}

def on(theSwitch="led") {
	def sUrl
	switch ( theSwitch ) {
		case "record":
			sUrl = "/startvideo?force=1"
			break

		case "focus":
			sUrl = "focus"
			break

		case "overlay":
			sUrl = "settings/overlay?set=on"
			break

		case "nightVision":
			sUrl = "settings/night_vision?set=on"
			break

		default:
			sUrl = "enabletorch"
	}

	httpGet("http://${username}:${password}@${url}:${port}/${sUrl}"){
		response -> log.info("${device.label} ${theSwitch} On")
		sendEvent(name: "${theSwitch}", value: "${theSwitch}On")
	}

}

def off(theSwitch="led") {
	def sUrl
	switch ( theSwitch ) {
		case "record":
			sUrl = "stopvideo?force=1"
			break

		case "focus":
			sUrl = "nofocus"
			break

		case "overlay":
			sUrl = "settings/overlay?set=off"
			break

		case "nightVision":
			sUrl = "settings/night_vision?set=off"
			break

		default:
			sUrl = "disabletorch"
	}

	httpGet("http://${username}:${password}@${url}:${port}/${sUrl}"){
		response -> log.info("${device.label} ${theSwitch} Off")
		sendEvent(name: "${theSwitch}", value: "${theSwitch}Off")
	}

}

def recordOn() { on("record") }

def recordOff() { off("record") }

def ledOn() { on("led") }

def ledOff() { off("led") }

def focusOn() { on("focus") }

def focusOff() { off("focus") }

def nightVisionOn() { on("nightVision") }

def nightVisionOff() { off("nightVision") }

def installed() { runPeriodically(20*60, poll) }

def configure() { poll() }

def poll() { refresh() }

def refresh() { getSensors() }

def getSensors() {

	def params = [
		uri: "http://${username}:${password}@${url}:${port}",
		path: "/sensors.json",
		contentType: 'application/json'
	]

	log.debug "Params = ${params}"

    def battery
    def temperature
    def illuminance
    def pressure

	try {
		httpGet(params) { 
			response -> log.debug "Start httpGet"
            battery = response.data.battery_level.data[0][1][0]
            sendEvent(name: "battery", unit: "${response.data.battery_level.unit}", value: "${battery}")
            
            temperature = response.data.temp.data[0][1][0]
            sendEvent(name: "temperature", unit: "${response.data.temp.unit}", value: "${temperature}")
            
            illuminance = response.data.light.data[0][1][0]
            sendEvent(name: "illuminance", unit: "${response.data.light.unit}", value: "${illuminance}")
            
            pressure = response.data.pressure.data[0][1][0]
            sendEvent(name: "pressure", unit: "${response.data.pressure.unit}", value: "${pressure}")
		}
	}
	catch(e) { log.warn "$e" }
}

def cToF(temp) {
	return temp * 1.8 + 32
}

def parse(String description) {
	def name = parseName(description)
	def value = parseValue(description)
	def linkText = getLinkText(device)
	def descriptionText = parseDescriptionText(linkText, value, description)
	def handlerName = getState(value)
	def isStateChange = isStateChange(device, name, value)

	def results = [
		name: name,
		value: value,
		unit: null,
		linkText: linkText,
		descriptionText: descriptionText,
		handlerName: handlerName,
		isStateChange: isStateChange,
		displayed: displayed(description, isStateChange)
	]
	log.debug "Parse returned $results.descriptionText"
	return results

}

private String parseName(String description) {
	if (description?.startsWith("presence: ")) {
		return "presence"
	}
	null
}

private String parseValue(String description) {
	switch(description) {
		case "presence: 1": return "present"
		case "presence: 0": return "not present"
		default: return description
	}
}

private parseDescriptionText(String linkText, String value, String description) {
	switch(value) {
		case "present": return "$linkText has arrived"
		case "not present": return "$linkText has left"
		default: return value
	}
}

private getState(String value) {
	switch(value) {
		case "present": return "arrived"
		case "not present": return "left"
		default: return value
	}
}
