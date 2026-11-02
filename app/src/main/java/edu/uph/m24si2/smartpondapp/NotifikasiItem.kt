package edu.uph.m24si2.smartpondapp

data class NotifikasiItem(
    var id: String = "",
    var title: String = "",
    var message: String = "",
    var time: String = "",
    var type: String = "info" // "suhu", "kekeruhan", "level", "pompa", "feeder"
)
