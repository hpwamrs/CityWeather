package com.lihong.cityweather

import java.net.HttpURLConnection
import java.net.URL


const val WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?q="
const val WEATHER_KEY =",CA&appid=aecc57c691f97787695493214c38a046"

//convert temperature in celsius  to fahrenheit
fun tempInFahrenheit( temp : Double) : Double{
    return  temp * 9/5 +32
}

//get data from internet
fun getJsonString(urlString : String ) : String{
  val TIMEOUT = 10000
  val conn = URL(urlString).openConnection() as HttpURLConnection
  conn.setRequestMethod("GET")
  conn.setConnectTimeout(TIMEOUT)
  conn.setReadTimeout(TIMEOUT)
 return conn.inputStream.bufferedReader().use { it.readText() }
}