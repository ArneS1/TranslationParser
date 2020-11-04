package com.example.translationparser

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.io.InputStream
import javax.xml.parsers.ParserConfigurationException

import java.io.IOException

import javax.xml.transform.TransformerException

import java.io.FileOutputStream

import javax.xml.transform.stream.StreamResult

import javax.xml.transform.dom.DOMSource

import javax.xml.transform.OutputKeys

import javax.xml.transform.TransformerFactory

import javax.xml.transform.Transformer

import javax.xml.parsers.DocumentBuilder

import javax.xml.parsers.DocumentBuilderFactory




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
        createXML()
    }

    private fun createXML(){
        val rows = getRows(context.resources.assets.open(fileName))
        val dom : javax.swing.text.Document
        val documentBuilderFactory = javax.xml.parsers.DocumentBuilderFactory().getInstance()

        for( row in rows){
            if(row.get(0).toString().contains("==")){
                //TODO: create Method for row comment import
            } else {
                saveStringToXML(row)
            }
        }
    }

    fun saveStringToXML(newString: String?) {
        val dom: Document
        var e: Element? = null

        // instance of a DocumentBuilderFactory
        val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        try {
            // use factory to get an instance of document builder
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            // create instance of DOM
            dom = db.newDocument()

            // create the root element
            val rootEle: Element = dom.createElement("string")
            rootEle.appendChild(dom.createTextNode(newString))

            dom.appendChild(rootEle)
            try {
                val tr: Transformer = TransformerFactory.newInstance().newTransformer()
                tr.setOutputProperty(OutputKeys.INDENT, "yes")
                tr.setOutputProperty(OutputKeys.METHOD, "xml")
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd")
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")

                // send DOM to file
                tr.transform(DOMSource(dom),
                        StreamResult(FileOutputStream(xml)))
            } catch (te: TransformerException) {
                println(te.message)
            } catch (ioe: IOException) {
                println(ioe.message)
            }
        } catch (pce: ParserConfigurationException) {
            println("UsersXML: Error trying to instantiate DocumentBuilder $pce")
        }
    }
}