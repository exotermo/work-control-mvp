package com.workcontrol.app.data.auth

/** Queries the device lock state at the moment a mobile session would be created. */
fun interface DeviceSecurity {
    fun isDeviceSecure(): Boolean
}
