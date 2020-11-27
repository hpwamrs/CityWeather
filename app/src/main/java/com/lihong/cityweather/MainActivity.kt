package com.lihong.cityweather


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_city_dialog.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.*


const val FIRST_ADD = "First added"
const val SORT_NAME = "Sort by name"

class MainActivity : AppCompatActivity() {
    private var cities: MutableList<CityData> = arrayListOf()
    var firstRun: Boolean = true
    private lateinit var mLoadingIndicator: ProgressBar
    lateinit var sharedPref: SharedPreferences
    private var addedType: String = SORT_NAME
    var isFahrenheit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)
        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        setupRecyclerView()

        firstRun = sharedPref.getBoolean("firstRun", true)
        if (firstRun) {
            addDefaultData()
            val editor = sharedPref.edit()
            editor.putBoolean("firstRun", false)
            editor.apply()
        } else {
            cities.clear()
            rv_cities.adapter?.notifyDataSetChanged()
            refreshData()
        }

        swipe_refresh.setOnRefreshListener {
            if (addedType == FIRST_ADD) {
                addedType = SORT_NAME
            }
            cities.clear()
            rv_cities.adapter?.notifyDataSetChanged()
            refreshData()
            if (swipe_refresh.isRefreshing()) {
                swipe_refresh.setRefreshing(false)
            }
        }

        mLoadingIndicator = progressBar

        add_city_fab.setOnClickListener {
            addSymbolDialog()
        }

        addedType = sharedPref.getString("AddType", SORT_NAME)!!
        if (addedType == FIRST_ADD) {
            addedType = SORT_NAME
        }
    }

    private fun refreshData() {
        if (haveNetworkConnection()) {
            val tags = sharedPref.all.keys.toMutableList().filter { it != "firstRun" && it != "AddType" }

            for (tag in tags) {
                getCityWeatherData(tag)
            }
        } else {
            Toast.makeText(this, "No Internet connection!", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private fun addSymbolDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_city_dialog, null)
        val dialogTitle = getString(R.string.add_city)
        val builder = AlertDialog.Builder(this@MainActivity, R.style.AppTheme)
                .setView(dialogView)
                .setTitle(dialogTitle)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
        val dialog = builder.show()
        val editCity = dialogView.editText_city
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            saveCity(editCity)
            dialog.dismiss()
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun saveCity(editCity: EditText) {
        val pattern = Regex("[a-zA-Z]*")
        val city = editCity.text.toString()
        if (city.trim().isNotEmpty() && pattern.matches(city)) {
            val key = city.toUpperCase(Locale.ENGLISH)
            val tags = sharedPref.all.keys.toMutableList()
                    .filter { it != "firstRun" && it != "AddType" }
            if (!tags.contains(key)) {
                addedType = FIRST_ADD
                if (haveNetworkConnection()) {
                    getCityWeatherData(key)
                } else {
                    Toast.makeText(this, "No Internet connection!", Toast.LENGTH_SHORT)
                            .show()
                }
            } else {
                Toast.makeText(this, "The city has been added!", Toast.LENGTH_SHORT)
                        .show()
            }

            editCity.text.clear()
            val imm =
                    this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editCity.getWindowToken(), 0)
        } else {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(R.string.missingTitle)
            builder.setPositiveButton(R.string.OK, null)
            builder.setMessage(R.string.missingMessage)
            val errorDialog = builder.create()
            errorDialog.show()
        }
    }

    private fun setupRecyclerView() {
        rv_cities.layoutManager = LinearLayoutManager(this)
        rv_cities.addItemDecoration(
                DividerItemDecoration(
                        this,
                        LinearLayoutManager.VERTICAL
                )
        )
        rv_cities.adapter =
                cities.let { WeatherAdapter(it, { city: CityData -> cityClicked(city) }) }

        val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                ) {
                    override fun onMove(
                            rV: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                    ): Boolean {
                        return false
                    }

                    override fun onSwiped(
                            viewHolder: RecyclerView.ViewHolder,
                            direction: Int
                    ) {
                        val position = viewHolder.absoluteAdapterPosition
                        val tmp = cities[position]
                        val cityName = tmp.cityName

                        val editor = sharedPref.edit()
                        editor.remove(cityName.toUpperCase(Locale.ENGLISH))
                        editor.apply()
                        cities.remove(tmp)
                        rv_cities.adapter?.notifyItemRemoved(position)
                        val layout = rv_cities

                        Snackbar.make(layout, "stock was Deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo") { _ ->
                                    tmp.let { cities.add(position, it) }
                                    rv_cities.adapter?.notifyItemInserted(position)
                                    editor.putString(cityName.toUpperCase(Locale.ENGLISH), cityName)
                                    editor.apply()
                                }
                                .show()
                    }
                })

        helper.attachToRecyclerView(rv_cities)
    }

    private fun cityClicked(cityData: CityData) {
        val intent = Intent(this, WeatherDataActivity::class.java)
        intent.putExtra("CityName", cityData.cityName)
        startActivity(intent)
    }

    private fun addDefaultData() {
        val defaultCities = listOf("Waterloo", "Toronto", "Ottawa")
        if (haveNetworkConnection()) {
            for (city in defaultCities) {
                getCityWeatherData(city)
                val key = city.toUpperCase(Locale.ENGLISH)
                val editor = sharedPref.edit()
                editor.putString(key, city)
                editor.apply()
            }
        } else {
            Toast.makeText(this, "No Internet connection!", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private fun getCityWeatherData(city: String) {
        progressBar.setVisibility(View.VISIBLE)
        var exception: Exception? = null
        var cityWeather = CityData(city)
        val urlString = "$WEATHER_URL$city$WEATHER_KEY"

        if (haveNetworkConnection()) {
            doAsync {
                try {
                    val result = getJsonString(urlString)
                    val weatherData = JSONObject(result)
                    val cityName = weatherData.getString("name")
                    val weather = weatherData.getJSONArray("weather")
                    val item = weather.getJSONObject(0)
                    val weatherDescription = item.getString("description")
                    val main = weatherData.getJSONObject("main")
                    val temperature: Double
                    val tempString: String
                    if (!isFahrenheit) {
                        temperature = main.getDouble("temp") - 273.15
                        tempString = "${String.format("%.0f", temperature)} °C"
                    } else {
                        temperature = tempInFahrenheit(main.getDouble("temp") - 273.15)
                        tempString = "${String.format("%.0f", temperature)} °F"
                    }
                    cityWeather = CityData(cityName, tempString, weatherDescription)
                } catch (e: Exception) {
                    exception = e
                }
                uiThread {
                    if (exception != null) {
                        if (addedType == FIRST_ADD) {
                            Toast.makeText(it, "No data available!", Toast.LENGTH_SHORT)
                                    .show()
                            progressBar.setVisibility(View.INVISIBLE)
                        }
                    } else {
                        when (addedType) {
                            FIRST_ADD -> {
                                cities.add(0, cityWeather)
                                val key = city.toUpperCase(Locale.ENGLISH)
                                val editor = sharedPref.edit()
                                editor.putString(key, city)
                                editor.apply()
                            }
                            SORT_NAME -> {
                                cities.add(cityWeather)
                                cities =
                                        cities.sortedWith(compareBy({ it.cityName }))
                                                .toMutableList()
                            }
                        }
                        rv_cities.adapter =
                                cities.let { it1 ->
                                    WeatherAdapter(
                                            it1,
                                            { cityData: CityData -> cityClicked(cityData) })
                                }
                        rv_cities.adapter?.notifyItemRangeInserted(0, cities.size)
                        val editor = sharedPref.edit()
                        editor.putString("AddType", addedType)
                        editor.apply()
                    }
                    progressBar.setVisibility(View.INVISIBLE)
                }
            }
        } else {
            Toast.makeText(this, "No Internet connection!", Toast.LENGTH_SHORT)
                    .show()
            progressBar.setVisibility(View.INVISIBLE)
        }
    }

    private fun haveNetworkConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (android.os.Build.VERSION.SDK_INT < 23) {
            val ni = cm.activeNetworkInfo
            if (ni != null) {
                return ni.isConnected && (ni.type == ConnectivityManager.TYPE_WIFI || ni.type == ConnectivityManager.TYPE_MOBILE)
            }
        } else {
            val n = cm.activeNetwork
            if (n != null) {
                val nc = cm.getNetworkCapabilities(n)
                if (nc != null) {
                    return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(
                            NetworkCapabilities.TRANSPORT_WIFI
                    )
                }
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add -> {
                addSymbolDialog()
                true
            }
            R.id.menu_refresh -> {
                cities.clear()
                addedType = SORT_NAME
                refreshData()
                true
            }
            R.id.menu_temp -> {
                if (item.title == getString(R.string.temp_ceisius)) {
                    cities.clear()
                    item.setTitle(getString(R.string.temp_fahrenheit))
                    isFahrenheit = false
                    refreshData()
                } else
                    if (item.title == getString(R.string.temp_fahrenheit)) {
                        cities.clear()
                        item.setTitle(getString(R.string.temp_ceisius))
                        isFahrenheit = true
                        refreshData()
                    }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}