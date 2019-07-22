# Celebrium Web

#### Создание Page Object'а
1. необходимо унаследоваться от класса WebPage, в данном случае конструктору WebPage передается путь до конфигурации страницы

```kotlin
import io.celebrium.web.page.WebPage
    
/**
* Страница авторизации в системе
* 
* @param fileName путь до конфигурационного файла страницы
*/
class LoginPage(fileName: String) : WebPage(fileName) {
    ...
}
```

2. создать файл конфигурации страницы

```hocon
xpath {
  field = "//div[text()='%s']/following::input[1]"
  button = "//button[descendant::span[text()='%s']]"
  ...
}
```

3. создать экземпляр страницы, указав путь до конфигурации страницы

```kotlin
val loginPage = LoginPage(pathToConfig)
```


#### Использование экшенов
WebPage предоставляет следующие экщены:
1. **appearance** - ожидание появления элемента на странице
1. **click** - нажатие на элемент
1. **disappearance** - ожидание исчезновения элемента на странице
1. **findElement** - поиск элемента (-ов) на странице
1. **attribute** - получение атрибута элемента
1. **text** - получение текста элемента (-ов)
1. **input** - ввод значения в поле ввода
1. **mouseOver** - наведение указателя мыши на элемент
1. **select** - выбор значения из выпадающего списка
1. **sendKeys** - нажатие клавиш (-и) 

```kotlin
import io.celebrium.web.page.WebPage
    
/**
* Страница авторизации в системе
* 
* @param fileName путь до конфигурационного файла страницы
*/
class LoginPage(fileName: String) : WebPage(fileName) {
    
    ...
    
    /**
    * Авторизация в системе
    * 
    * @param userName учетная запись пользователя
    * @param password пароль пользователя
    */
    fun login(userName: Srring, password: String) {
        // ввод логина пользователя
        // используется шаблон field из конфигурации страницы 
        // в данном случае получится xpath выражение 
        // "//div[text()='Логин:']/following::input[1]"
        input()
            .template("field")
            .parameters("Логин:")
            .value(userName)
            .perform()
    
        // ввод пароля
        input()
            .template("field")
            .parameters("Пароль:")
            .value(password)
            .perform()
    
        // нажатие на кнопку Войти
        click()
            .template("button")
            .parameters("Войти")
            .perform()
    }
    
    ...
    
}
```
Помимо испоьзования шаблонов xpath выражений можно использовать сами xpath выражения:
```kotlin
click()
    .xpath("//button[descendant::span[text()='Войти']]")
    .perform()
```
Каждый экшен позволяет задавать следующие параметры:
1. **.assertType(AssertType.SOFT)** - задание типа ошибки в случае неудачного выполнения действия (блокирующие/неблокирующие ошибки)
1. **.errorMessage("Неудалось авторизоваться!"")** - сообщение ошибки
1. **.errorDescription("Неудалось авторизоваться пользователю $userName с паролем $password")** - описание ошибки
1. **.disableAttachments()** - выключение вложений до и после действия
1. **.retry({ println("Ошибка выполнения действия") })** - блок кода, вызываемый в промежутках попыток выполнить действие
1. **.template("Кнопка")** - название шаблона в файле конфигурации страницы
1. **.parameters("Войти")** - параметры, подставляемые в шаблон xpath выражения
1. **.timeout(10, TimeUnit.SECONDS)** - ограничение времени выполнения действия
1. **.title("Нажатие на кнопку Войти")** - заголовок дествия
1. **.titlef("Нажатие на кнопку %s", button)** - аналогично title с использованием формата
1. **.xpath("//button[text()='Войти']")** - xpath выражение
1. **.visible(false)** - по умолчанию для действия элемент должен быть виден на странице, для случаев когда элемент не отображается на странице нужно установить параметр в false
