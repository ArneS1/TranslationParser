package com.example.translationparser

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.io.InputStream

class CsvImportUtil(private var context: Context, private var fileName: String) {

    companion object{
        private const val columnTitle = 0
        private const val columnContent = 1

        private fun getRows(inputStream: InputStream): List<List<String>>{
            return csvReader{
                delimiter = ';'
            }.readAll(inputStream)
        }
    }

    fun createFiles(){
        createXML2()
    }

    private fun createXML(){
        val rows = getRows(context.resources.assets.open(fileName))
        for( row in rows){
            System.out.println(row)
        }
    }

    private fun createXML2(){
        csvReader().open(fileName) {
            readAllAsSequence().forEach { row ->
                //Do something with the data
                println(row)
            }
        }
    }
}