package com.example.aifitnesstrainer.datalayer.models

class Movement (name: String, keyPointAngles: HashMap<String, Triple<Int, Int, Int>>, errorMargin: Float){
    val _name: String = name
    val _keyPointAngles: HashMap<String, Triple<Int, Int, Int>> = keyPointAngles
    val _errorMargin: Float = errorMargin

    fun getName(): String {
        return _name
    }

}