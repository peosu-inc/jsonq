@file:Suppress("unused")

package com.peosu.fn.utils

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


fun decryptFile(encryptedFile: File, password: String) {
    val fileBytes = encryptedFile.readBytes()
    if (String(fileBytes, 0, 8) != "Salted__") return

    val salt = fileBytes.sliceArray(8..15)  // Extract salt
    val encryptedData = fileBytes.sliceArray(16 until fileBytes.size)  // Extract encrypted data

    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256 + 128)
    val key = factory.generateSecret(spec).encoded
    val iv = key.sliceArray(32..47)  // Extract IV
    val keyBytes = key.sliceArray(0..31)  // Extract key

    // Decrypt
    Cipher.getInstance("AES/CBC/PKCS5Padding").run {
        init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        val data = doFinal(encryptedData)
        if(isZipFile(bytes = data))
            unzip(ByteArrayInputStream(data)){ file, isDir->
                val f = File(encryptedFile.parentFile,file)
                if(isDir) {f.mkdirs();null} else FileOutputStream(f)
            }
        else File(encryptedFile.absolutePath.replace(".enc","clr")).writeBytes(data  )
    }
}

fun zipAndEncrypt(inputFile: File,password:String){
    zipFile(inputFile)
    encryptFile(File(inputFile.absolutePath+".zip"),password)
}

fun encryptFile(inputFile: File, password: String) {
    val salt = ByteArray(8)
    SecureRandom().nextBytes(salt)

    val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val key = keyFactory.generateSecret(keySpec).encoded
    val secretKeySpec = SecretKeySpec(key, "AES")
    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)

    FileOutputStream(inputFile.absolutePath+".enc").use { fileOut ->
        fileOut.write("Salted__".toByteArray()) // Write the Salted prefix
        fileOut.write(salt) // Write the salt
        fileOut.write(iv) // Write the IV
        CipherOutputStream(fileOut, cipher).use { cipherOut ->
            FileInputStream(inputFile).use { fileIn ->
                fileIn.copyTo(cipherOut)
            }
        }
    }
}
