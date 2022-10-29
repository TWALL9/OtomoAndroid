package com.tom.otomoproto
import otomo.OtomoProto.TopMsg
import otomo.OtomoProto.Joystick

class TopMsg {
    fun new_joystick(speed: Float, heading: Float): ByteArray {
        val js = Joystick.newBuilder().setHeading(heading).setSpeed(speed)
        val top = TopMsg.newBuilder().setJoystick(js)
        return top.build().toByteArray()
    }
}