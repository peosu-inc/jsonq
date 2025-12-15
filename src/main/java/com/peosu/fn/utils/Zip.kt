@file:Suppress("unused")
package com.peosu.fn.utils

import com.peosu.fn.ds.FnList
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


fun isZipFile(file: File?=null, filePath:String?=null, bytes:ByteArray?=null): Boolean {
    val isZip= {data:ByteArray->data.size >= 4 && data[0] == 0x50.toByte() && data[1] == 0x4B.toByte()
            && data[2] == 0x03.toByte() && data[3] == 0x04.toByte()}

    return file?.let { isZip(it.readBytes())}?:false||
            filePath?.let { isZip(File(it).readBytes())}?:false||
            bytes?.let(isZip)?:false
}

fun unzip(zippedFilePath:String) {
    val folder= File(zippedFilePath).parentFile
    Files.newInputStream(Paths.get(zippedFilePath)).use {
        unzip(it) { file, isDir ->
            val f= File(folder,file)
            if(isDir) {f.mkdirs();null} else FileOutputStream(f)
        }
    }
}

fun copy(input: InputStream, out: OutputStream) {
    try {
        val bIn= BufferedInputStream(input)
        val bOut=BufferedOutputStream(out)
        val b = ByteArray(4096)
        var i = bIn.read(b)
        while (i != -1) {
            bOut.write(b, 0, i)
            i = bIn.read(b)
        }
        bOut.flush()
    } catch (e: IOException) { e.printStackTrace() }
}


fun unzip(input: InputStream, getOutput:(fileName:String, isDirectory:Boolean)-> OutputStream?) {
    try {
        ZipInputStream(input).use{ zis->
            FnList.generate { zis.nextEntry }
                .map{getOutput(it.name,it.isDirectory)}
                .forEachItem{out-> out?.let { zis.copyTo(out) } }
        }
    } catch (e: IOException) { e.printStackTrace() }
}

fun zipFile(inputFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream("${inputFile.absolutePath.replace(".zip","")}.zip"))).use { zos ->
        if (inputFile.isDirectory) {
            inputFile.walk().forEach { file ->
                if (file.isFile) {
                    val zipEntry = ZipEntry(file.relativeTo(inputFile.parentFile!!).path)
                    zos.putNextEntry(zipEntry)
                    file.inputStream().copyTo(zos)
                    zos.closeEntry()
                }
            }
        } else {
            zos.putNextEntry(ZipEntry(inputFile.name))
            inputFile.inputStream().copyTo(zos)
            zos.closeEntry()
        }
    }
}
