package edu.uph.m24si2.smartpondapp

data class JadwalPakan(
    var id: String = "",
    var time: String = "08:00",
    var label: String = "Pagi",
    var portion: String = "Porsi 1x",
    var enabled: Boolean = true
)
