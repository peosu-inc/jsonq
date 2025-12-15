package com.peosu.fn.google
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.collect.Lists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

open class GSheet(private val key: File, private val sheetId:String){

    suspend fun getRangeValues(range:String): List<List<Any>> = withContext(Dispatchers.IO){
        val response = connectToSheets()
            .spreadsheets().values()[sheetId, range]
            .execute()
        response.getValues().orEmpty()
    }

    fun openAnother(sheetId:String): GSheet = GSheet(key, sheetId)

    suspend fun getRangeValuesAsStrings(range:String): List<List<String>> = withContext(Dispatchers.IO){
        getRangeValues(range).map{l->l.map{it.toString()}}
    }

    suspend fun getRangeValues(ranges:Set<String>): List<ValueRange>
    = withContext(Dispatchers.IO){ getRangeValues(ranges.toList()) }

    suspend fun getRangeValues(ranges:List<String>): List<ValueRange> = withContext(Dispatchers.IO){
        val response = connectToSheets()
            .spreadsheets().values().batchGet(sheetId)
            .setRanges(ranges).execute();
        response.valueRanges.orEmpty()
    }

    private fun connectToSheets(): Sheets{
        val cred= GoogleCredentials.fromStream(FileInputStream(key))
            .createScoped(Lists.newArrayList(SheetsScopes.SPREADSHEETS_READONLY))
        return Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport()
            ,GsonFactory.getDefaultInstance()
            ,HttpCredentialsAdapter(cred)
        ).build()
    }

    suspend fun<T> getData(dataSlice: DataSlice<T>, vararg injections:Pair<String,String>): List<T> {
        val data=  getRangeValues(dataSlice.sheetRange);
        val header = dataSlice.headers.ifEmpty { data[0].map { v -> v.toString() } }
        val values=if(dataSlice.headers.isEmpty()) data else data.drop(1)
        return values.map { row ->
            val res=row.take(header.size).mapIndexed{ i, v -> header[i] to v.toString() }
                .toMap().toMutableMap();
            res.putAll(injections.associate { (k,v)->k to v })
            res.let(dataSlice.transformer)
        }
    }

    suspend fun getSheetNames(): List<String> = withContext(Dispatchers.IO){
        val sheets =connectToSheets()
            .spreadsheets().get(sheetId).setFields("sheets.properties.title")
            .execute().sheets
            .map { it.properties.title }
         sheets
    }

    suspend fun <T> getData(dataSlices:List<DataSlice<T>>): Map<String,List<T>> {
        val ranges= dataSlices.associateBy { it.sheetRange }.toMap()

        return getRangeValues(ranges.keys).associate { range ->
            val chunk=ranges[range.range]!!
            val data = range.getValues()
            val header = chunk.headers.ifEmpty { data[0].map { v -> v.toString() } }
            val values=if(chunk.headers.isEmpty()) data else data.drop(1)

            chunk.name to values.map { row ->
                row.mapIndexed{ i, v -> header[i] to v.toString() }
                    .toMap().let(chunk.transformer)
            }
        }
    }
}

data class DataSlice<T>(
    val sheetRange:String,
    val name:String=sheetRange,
    var transformer: (Map<String,String>)->T,
    var headers:List<String> =listOf(),
)