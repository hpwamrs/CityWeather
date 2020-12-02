package com.lihong.cityweather

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

import org.junit.Assert.*

class AppUtilsKtTest {

    @Test
    fun tempInFahrenheit() {
        val fahrenheit = tempInFahrenheit(6.0)
        assertThat(fahrenheit, equalTo(42.8))
    }
}