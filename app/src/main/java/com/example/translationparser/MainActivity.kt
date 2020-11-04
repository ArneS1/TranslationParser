package com.example.translationparser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private var FILE_NAME = "translation.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val csvImportUtil = CsvImportUtil(this, FILE_NAME)
        csvImportUtil.createFiles()
    }
}