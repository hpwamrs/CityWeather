package com.lihong.cityweather

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {
    @get: Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_isActivityInView() {
        onView(withId(R.id.main)).check(matches(isDisplayed()))
    }

    @Test
    fun test_menu_temp() {
        onView(withId(R.id.menu_temp)).check(matches(withText(R.string.temp_fahrenheit)))
        onView(withId(R.id.menu_temp))
            .perform(click())
            .check(matches(withText(R.string.temp_celsius)))
    }

    @Test
    fun test_textView() {
        onView(allOf(withId(R.id.city_name), withText(R.string.cityName)))
        onView(allOf(withId(R.id.city_temp), withText(R.string.temp)))
        onView(allOf(withId(R.id.city_weather), withText(R.string.weather)))
    }
}