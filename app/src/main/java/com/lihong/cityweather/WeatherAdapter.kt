package com.lihong.cityweather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.city_item.view.*

class WeatherAdapter(
        private val cities: MutableList<CityData>,
        private val clickListener: (CityData) -> Unit) : RecyclerView.Adapter<WeatherAdapter.WeatherHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.city_item, parent, false)
        return WeatherHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherHolder, position: Int) {
        holder.bind(cities[position], clickListener)
    }

    override fun getItemCount(): Int {
        return cities.size
    }

    class WeatherHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(city: CityData, clickListener: (CityData) -> Unit) {
            itemView.city_name.text = city.cityName
            itemView.city_temperature.text = city.temperature
            itemView.city_weather.text = city.weather
            itemView.setOnClickListener { clickListener(city) }
        }
    }
}