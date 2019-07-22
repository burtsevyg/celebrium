package io.celebrium.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.celebrium.core.config.Configuration
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Объект FilesManager.
 * --------------------
 *
 * TODO: документация!
 *
 * @author EMurzakaev@it.ru.
 */
object FilesManager {

    /**
     * TODO: документация!
     */
    private val logger = LoggerFactory.getLogger(FilesManager::class.java) as Logger

    /**
     * TODO: документация!
     */
    init {
        logger.level = Level.toLevel(Configuration.config.getString("log_level"))
    }

    /**
     * Метод записи строки в файл.
     *
     * TODO: документация!
     *
     * @param source         строка
     * @param outputFileName имя выходного файла
     */
    @JvmStatic
    fun writeToFile(source: String, outputFileName: String) {
        try {
            BufferedWriter(
                    OutputStreamWriter(
                            FileOutputStream(outputFileName), "utf-8")).use { it.write(source) }
        } catch (exception: IOException) {
            logger.error(exception.message, exception)
        }
    }

    /**
     * Метод создания директории
     *
     * TODO: документация!
     *
     * @param directoryName название директории
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createDirectory(directoryName: String) {
        val directory = File(directoryName)
        if (!directory.exists()) {
            Files.createDirectory(directory.toPath())
        }
    }

    /**
     * Метод копирования файла
     *
     * TODO: документация!
     *
     * @param sourceFile копируемый файл
     * @param outputFile выходной файл
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(sourceFile: File, outputFile: File) = Files.copy(sourceFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun delete(path: String) {
        val bufferFile = File(path)
        if (!bufferFile.exists()) return
        if (bufferFile.isDirectory) {
            bufferFile.listFiles().forEach { delete(it.path) }
            bufferFile.delete()
        } else {
            bufferFile.delete()
        }
    }

    /**
     * TODO: документация!
     */
    @JvmStatic
    fun getTextFromFile(fileName: String): String {
        return try {
            Files.readAllLines(File(fileName).toPath(), Charset.forName("UTF-8")).joinToString("\n")
        } catch (exception: IOException) {
            logger.error(exception.message, exception)
            ""
        }
    }

}
