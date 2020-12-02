package com.lihong.cityweather

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.lihong.cityweather.databinding.ActivityWeatherDataBinding
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.URL

class WeatherDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWeatherDataBinding
    private var cityName: String = ""
    private var isCelsius = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_weather_data)
        setSupportActionBar(binding.toolbar)
        cityName = intent.getStringExtra(CITY_NAME) ?: ""
        getWeatherData(cityName)
    }

    private fun getWeatherData(cityName: String) {
        val urlString = "$WEATHER_URL$cityName$WEATHER_KEY"
        var currentTemp: Double
        var currentTempString: String
        var lowTemp: Double
        var lowTempString: String
        var highTemp: Double
        var highTempString: String

        doAsync {
            val result = getJsonString(urlString)
            val resultJson = JSONObject(result)
            val name = resultJson.getString("name")
            val weather = resultJson.getJSONArray("weather")
            val item = weather.getJSONObject(0)
            val description = item.getString("description")
            val icon = item.getString("icon")
            val main = resultJson.getJSONObject("main")

            if (isCelsius) {
                //change temperature default Kelvin unit to Celsius unit
                currentTemp = main.getDouble("temp") - 273.15
                currentTempString = "${String.format("%.0f", currentTemp)} °C"
                lowTemp = main.getDouble("temp_min") - 273.15
                lowTempString = "Low: ${String.format("%.0f", lowTemp)} °C"
                highTemp = main.getDouble("temp_max") - 273.15
                highTempString = "High: ${String.format("%.0f", highTemp)} °C"
            } else {
                currentTemp = tempInFahrenheit(main.getDouble("temp") - 273.15)
                currentTempString = "${String.format("%.0f", currentTemp)} °F"
                lowTemp = tempInFahrenheit(main.getDouble("temp_min") - 273.15)
                lowTempString = "Low: ${String.format("%.0f", lowTemp)} °F"
                highTemp = tempInFahrenheit(main.getDouble("temp_max") - 273.15)
                highTempString = "High: ${String.format("%.0f", highTemp)} °F"
            }

            val humidity = main.getInt("humidity")
            val humidityString = "Humidity: $humidity %"
            val wind = resultJson.getJSONObject("wind")
            val windSpeed = wind.getDouble("speed")
            val windSpeedString = "Wind: $windSpeed meter/sec"

            uiThread {
                binding.name.text = name
                binding.description.text = description
                binding.temperature.text = currentTempString
                binding.lowTemp.text = lowTempString
                binding.highTemp.text = highTempString
                binding.humidity.text = humidityString
                binding.wind.text = windSpeedString
                getIcon(icon)
            }
        }
    }

    private fun getIcon(icon: String) {
        val urlString = "$BASE_ICON_URL$icon@2x.png"
        val chartURL = URL(urlString)
        var image: Bitmap? = null
        doAsync {
            var exception: Exception? = null
            try {
                image = BitmapFactory.decodeStream(chartURL.openConnection().getInputStream())
            } catch (e: Exception) {
                exception = e
            }
            uiThread {
                if (exception != null) {
                    Toast.makeText(it,  getString(R.string.no_data), Toast.LENGTH_SHORT)
                            .show()
                } else {
                    binding.iconImage.setImageBitmap(image)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_weather_data, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_temp -> {
                if (item.title == getString(R.string.temp_celsius)) {
                    isCelsius = true
                    item.title = getString(R.string.temp_fahrenheit)
                } else
                    if (item.title == getString(R.string.temp_fahrenheit)) {
                        isCelsius = false
                        item.title = getString(R.string.temp_celsius)
                    }
                getWeatherData(cityName)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}