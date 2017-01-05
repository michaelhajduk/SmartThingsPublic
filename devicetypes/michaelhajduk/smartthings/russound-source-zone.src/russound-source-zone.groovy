import groovy.json.JsonSlurper

preferences {
    input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
    input("port", "string", title:"Port", description: "8000", defaultValue: 8000 , required: true, displayDuringSetup: true)
    input("zone", "string", title:"Zone", description: "1", defaultValue: "1" , required: true, displayDuringSetup: true)
	input("source", "string", title:"source", description: "1", defaultValue: "1" , required: true, displayDuringSetup: true)
    input("username", "string", title:"Username", description: "webiopi", required: true, displayDuringSetup: true)
    input("password", "password", title:"Password", description: "Password", required: true, displayDuringSetup: true)
}

metadata {
    definition (name: "Russound Source+Zone", namespace: "michaelhajduk/smartthings", author: "Michael Hajduk") {
		capability "Switch"
        
		command "on"
        command "off"
        
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
           
        main "switch"
        details(["switch"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes

// handle commands

def on() {
 	log.debug "russound zone on"
//    sendEvent(name: "switch", value: "off")
	def uri2 = "/rus/EVENT_C[1].Z["+zone+"]!SelectSource_"+source
    postAction(uri2)
//    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOn"
//    postAction(uri)
}

def off() {
	log.debug "russound zone off"
//    sendEvent(name: "switch", value: "off")
    def uri = "/rus/EVENT_C[1].Z["+zone+"]!ZoneOff"
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
    device.deviceNetworkId = "$iphex:$porthex:RUS:$zone:$source"
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