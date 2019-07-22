package io.celebrium.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sun.mail.pop3.POP3Store
import io.celebrium.core.config.Configuration
import org.slf4j.LoggerFactory
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Класс Postman.
 * --------------
 *
 * @param login    логин пользователя.
 * @param password пароль.
 * @param host     хост почты.
 * @param port     порт.
 * @param domain   домен.
 * @author EMurzakaev@it.ru.
 */
class Postman(val login: String, val password: String, val host: String, val port: String, val domain: String) {

    /**
     * Папка в почте.
     */
    private lateinit var folder: Folder
    /**
     * Почта.
     */
    private lateinit var store: Store
    /**
     * Сессия.
     */
    private lateinit var session: Session

    /**
     * Подключиться к почте.
     *
     * @throws MessagingException
     */
    @Throws(MessagingException::class)
    fun connect() {
        val auth = MyAuthenticator(login, password)
        val props = System.getProperties()
        props.put("mail.smtp.port", port)
        props.put("mail.smtp.host", host)
        props.put("mail.smtp.auth", "true")
        props.put("mail.mime.charset", ENCODING)
        session = Session.getDefaultInstance(props, auth)
    }

    /**
     * Закрыть соединение.
     */
    fun closeConnection() {
        folder.close(true)
        store.close()
    }

    /**
     * Отправка сообщения.
     *
     * @param recipient   адресат.
     * @param subject     тема.
     * @param content     содержимое.
     * @param contentType тип содержимого.
     */
    fun sendMessage(recipient: String, subject: String, content: Any, contentType: String) = sendMessage(listOf(recipient), subject, content, contentType)

    /**
     * Отправка сообщения нескольким пользователям.
     *
     * @param recipients  адресаты.
     * @param subject     тема.
     * @param content     содержимое.
     * @param contentType тип содержимого.
     */
    fun sendMessage(recipients: List<String>, subject: String, content: Any, contentType: String) {
        val addresses: Array<Address> = recipients.map(::InternetAddress).toTypedArray()
        sendMessage(addresses, subject, content, contentType)
    }

    /**
     * Отправка сообщения нескольким пользователям.
     *
     * @param addresses   адресаты.
     * @param subject     тема.
     * @param content     содержимое.
     * @param contentType тип содержимого.
     */
    private fun sendMessage(addresses: Array<Address>, subject: String, content: Any, contentType: String) {
        logger.debug("Try to send message to ${Arrays.toString(addresses)} with subject : \"$subject\"")
        val msg = MimeMessage(session)
        msg.setFrom(InternetAddress("$login@$domain"))
        msg.setRecipients(Message.RecipientType.TO, addresses)
        msg.subject = subject
        msg.setContent(content, contentType)
        Transport.send(msg)
        logger.debug("Message sent")
    }

    /**
     * Получить сообщения.

     * @param messagesCount число соообщений.
     */
    fun getMessages(messagesCount: Int): Array<Message> {
        logger.debug("Try to receive $messagesCount messages from inbox...")
        openStore()
        if (store.isConnected) {
            folder = store.getFolder("INBOX")
            try {
                folder.open(Folder.READ_ONLY)
                val total = folder.messageCount
                var correctCount = messagesCount
                if (total < correctCount) {
                    correctCount = total
                }
                val result = folder.getMessages(total - (correctCount - 1), total)
                logger.debug("Result : " + result.joinToString(prefix = "\n[\n\t", separator = "\n\t", postfix = "\n]", transform = {"${it?.receivedDate} - ${it?.from} - ${it?.subject}" }))
                return result
            } catch (throwable: Throwable) {
                logger.error(throwable.message, throwable)
            }
        }
        return emptyArray()
    }

    /**
     * Открыть почту.
     */
    private fun openStore() {
        val url = URLName("pop3", host, 110, "", login, password)
        store = POP3Store(session, url)
        try {
            store.connect(host, login, password)
        } catch (throwable: Throwable) {
            logger.error(throwable.message, throwable)
        }
    }

    /**
     * Вспомогательный класс.
     *
     * @param user     логин пользователя.
     * @param password пароль пользователя.
     */
    private inner class MyAuthenticator(val user: String, val password: String) : Authenticator() {

        /**
         * Получить аутентификационные данные.
         *
         * @return аутентификационные данные.
         */
        override fun getPasswordAuthentication(): PasswordAuthentication {
            val user = this.user
            val password = this.password
            return PasswordAuthentication(user, password)
        }

    }

    /**
     * TODO: документация!
     */
    companion object {

        /**
         * Кодировка.
         */
        private val ENCODING = "UTF-8"
        /**
         * Экземпляр логгера.
         */
        private val logger = LoggerFactory.getLogger(Postman::class.java) as Logger

        /**
         * TODO: документация!
         */
        init {
            logger.level = Level.toLevel(Configuration.config.getString("log_level"))
        }

    }

}
