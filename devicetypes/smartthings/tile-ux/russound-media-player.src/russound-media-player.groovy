/**
 *  Copyright 2015 SmartThings
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
 *  Sonos Player
 *
 *  Author: SmartThings
 *
 */
import groovy.json.JsonSlurper

preferences {
    input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
    input("port", "string", title:"Port", description: "8000", defaultValue: 8000 , required: true, displayDuringSetup: true)
    input("zone", "string", title:"Zone", description: "1", defaultValue: "1" , required: true, displayDuringSetup: true)
    input("username", "string", title:"Username", description: "webiopi", required: true, displayDuringSetup: true)
    input("password", "password", title:"Password", description: "Password", required: true, displayDuringSetup: true)
}
 
metadata {
	definition (
		name: "Russound Media Player",
		namespace: "smartthings/tile-ux",
		author: "SmartThings") {
		capability "thermostat"
		capability "Refresh"
		capability "Sensor"
		capability "Music Player"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "mediaMulti", type:"mediaPlayer", width:6, height:4) {
			tileAttribute("device.status", key: "PRIMARY_CONTROL") {
				attributeState("paused", label:"Paused",)
				attributeState("playing", label:"Playing")
				attributeState("stopped", label:"Stopped")
			}
			tileAttribute("device.status", key: "MEDIA_STATUS") {
				attributeState("paused", label:"Paused", action:"music Player.play", nextState: "playing")
				attributeState("playing", label:"Playing", action:"music Player.pause", nextState: "paused")
				attributeState("stopped", label:"Stopped", action:"music Player.play", nextState: "playing")
			}
			tileAttribute("device.status", key: "PREVIOUS_TRACK") {
				attributeState("status", action:"music Player.previousTrack", defaultState: true)
			}
			tileAttribute("device.status", key: "NEXT_TRACK") {
				attributeState("status", action:"music Player.nextTrack", defaultState: true)
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState("level", action:"music Player.setLevel")
			}
			tileAttribute ("device.mute", key: "MEDIA_MUTED") {
				attributeState("unmuted", action:"music Player.mute", nextState: "muted")
				attributeState("muted", action:"music Player.unmute", nextState: "unmuted")
			}
			tileAttribute("device.trackDescription", key: "MARQUEE") {
				attributeState("trackDescription", label:"${currentValue}", defaultState: true)
			}
		}

		main "mediaMulti"
		details(["mediaMulti"])
	}
}

def installed() {
	state.currentTrack = 0

	sendEvent(name: "level", value: 50)
	sendEvent(name: "mute", value: "unmuted")
	sendEvent(name: "status", value: "stopped")
}

def parse(description) {
	// No parsing will happen with this simulated device.
}

def play() {
	sendEvent(name: "status", value: "playing")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOn"
    postAction(uri)
}

def pause() {
	sendEvent(name: "status", value: "paused")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOff"
    postAction(uri)
}

def stop() {
	sendEvent(name: "status", value: "stopped")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOff"
    postAction(uri)
}

def previousTrack() {
	sendEvent(name: "trackDescription", "previoustrack")
}

def nextTrack() {
	sendEvent(name: "trackDescription", "nexttrack")
}

def mute() {
	sendEvent(name: "mute", value: "muted")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOff"
    postAction(uri)
}

def unmute() {
	sendEvent(name: "mute", value: "unmuted")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOn"
    postAction(uri)
}

def setLevel(level) {
	sendEvent(name: "level", value: level)
    russ(level)    
}

def russ(val){
    def v = math.round( val / 2 )
	def uri = "/rus/EVENT_C[1].Z["+zone+"]!KeyPress_Volume_"+v
    postAction(uri)
}

// ------------------------------------------------------------------

private postAction(uri){
    setDeviceNetworkId(ip,port)

    def userpass = encodeCredentials(username, password)

    def headers = getHeader(userpass)

    def hubAction = new physicalgraph.device.HubAction(
            method: "POST",
            path: uri,
            headers: headers
    )//,delayAction(1000), refresh()]
    log.debug("Executing hubAction on " + getHostAddress())
    //log.debug hubAction
    hubAction
}

// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
    description.split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
}

private encodeCredentials(username, password){
    log.debug "Encoding credentials"
    def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

private getHeader(userpass){
    log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
    new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$iphex:$porthex:RUSPLAYER:$zone"
    log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
    return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    return hexport
}
