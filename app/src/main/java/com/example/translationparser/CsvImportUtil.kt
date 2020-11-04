package com.example.translationparser

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.io.InputStream

class CsvImportUtil(private var context: Context, private var fileName: String) {

    companion object {
        private const val columnTitle = 0
        private const val columnContent = 1

        private fun getRows(inputStream: InputStream): List<List<String>> {
            return csvReader {
                delimiter = ';'
            }.readAll(inputStream)
        }
    }

    fun createFiles() {
        createXML2()
    }

    private fun createXML() {
        val rows = getRows(context.resources.assets.open(fileName))
        for (row in rows) {
            System.out.println(row)
        }
    }

    private fun createXML2() {
        val csvReader = CSVReaderBuilder(FileReader(fileName))
                .withCSVParser(CSVParserBuilder().withSeparator(';').build())
                .build()

        // Maybe do something with the header if there is one
        val header = csvReader.readNext()

        // Read the rest
        var line: Array<String>? = csvReader.readNext()
        while (line != null) {
            // Do something with the data
            println(line[0])

            line = csvReader.readNext()
        }
    }
}