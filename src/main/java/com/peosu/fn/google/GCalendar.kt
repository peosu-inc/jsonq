package com.peosu.fn.google
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.collect.Lists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Calendar as JCalendar


abstract  class GCalendar(private val key: File, private val calendarId:String){

    abstract fun getCountryHolidays():Map<String, CountryHoliday>
    private val countryHolidaysFilter by lazy {getCountryHolidays()}

    private fun connectToGCalendar(): Calendar {
        val cred= GoogleCredentials.fromStream(FileInputStream(key))
            .createScoped(Lists.newArrayList(CalendarScopes.CALENDAR_EVENTS_READONLY))
        return Calendar.Builder(
            GoogleNetHttpTransport.newTrustedTransport()
            , GsonFactory.getDefaultInstance()
            , HttpCredentialsAdapter(cred)
        ).build()
    }

    suspend fun getEvents(startTime:Long,endTime:Long,gCalendarId: String=calendarId): Events = withContext(Dispatchers.IO){
        connectToGCalendar().events().list(gCalendarId)
            .setMaxResults(50)
            .setTimeMin(DateTime(startTime))
            .setTimeMax(DateTime(endTime))
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
    }

    private fun getYearEnd()=DateTime("${JCalendar.getInstance().get(JCalendar.YEAR)}-12-31T23:59:59Z").value
    private fun getYearStart()=DateTime("${JCalendar.getInstance().get(JCalendar.YEAR)}-01-01T00:00:00Z").value


    suspend fun getPublicHolidays(country:String, start: Long?=null, end: Long?=null) : List<Event> = withContext(Dispatchers.IO){
        countryHolidaysFilter[country]!!.run{
            getEvents(start?:getYearStart(),end?:getYearEnd(),calendarId).items.filter{evt->
                holidays.find{evt.summary.contains(it)}!=null
            }
        }
    }
}

data class CountryHoliday(val calendarId: String, val country: String, val holidays:List<String>)
