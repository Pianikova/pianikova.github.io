# Скрипты

В корне проекта лежат вспомогательные скрипты для типовых операций. Для каждого есть два варианта: `.cmd` (Windows / cmd.exe) и `.sh` (bash, в т.ч. Git Bash на Windows). После выполнения каждый скрипт печатает строку с описанием результата.

## `build` — сборка проекта

Запускает Maven-сборку всех модулей с профилями `g5-v8-dt`, `find-bugs`, `SDK` и игнорированием локальных Tycho-артефактов. Цель — `clean verify`.

На выходе в `repositories/<repo>/target/` появляются собранные zip с p2-репозиториями (`com.e1c.edt.ai.repository`, `com.e1c.edt.semantic.repository`, `com.e1c.edt.ui.eclipse.repository`).

```cmd
build.cmd
```
```bash
./build.sh
```

## `publish` — публикация на сетевой ресурс

Копирует три готовых zip из `repositories/*/target/` в `Z:\Transfer\ai_plugin\`, переименовывая их в имена без версии (`com.e1c.edt.ai.repository.zip` и т.д.) — это удобно для подключения как Eclipse update site по фиксированной ссылке.

После копирования печатает три `jar:file:`-URL, которые можно сразу вставлять в Eclipse → *Install New Software* → *Add*.

Перед запуском должен быть смонтирован сетевой диск `Z:` и должна быть выполнена сборка ([build](#build--сборка-проекта)).

```cmd
publish.cmd
```
```bash
./publish.sh
```

## `readme` — регенерация README.md

Запускает `gmavenplus-plugin` (goal `execute@generate-readme`) на корневом pom — он обновляет `README.md` из шаблона `README_TEMPLATE.md`. Запускать после изменения шаблона или метаданных, влияющих на содержимое README.

```cmd
readme.cmd
```
```bash
./readme.sh
```

## `mirror` — зеркалирование p2-репозитория из Artifactory

Скачивает p2-репозиторий codeai последней версии из Artifactory (`http://artifactory.boreas.dept07/.../codeai/1.0.4/`) в `mirroring/target/repo/` через `tycho-p2-extras-plugin:mirror`, после чего упаковывает результат в zip с именем вида `com.e1c.edt.ai.repository-1.0.4.v<timestamp>.zip` — таким же, как у локальной Tycho-сборки `com.e1c.edt.ai.repository`.

Версия извлекается автоматически из имени скачанного плагина `com.e1c.edt.ai_*.jar`, поэтому при появлении в Artifactory новой подверсии имя итогового zip обновится без правок скрипта.

Конфигурация (URL источника, имя репозитория) — в `mirroring/mirror-p2.pom`.

### Требования

Перед запуском должен существовать файл `~/.m2/settings.xml` (на Windows — `%USERPROFILE%\.m2\settings.xml`) с учётными данными для Artifactory. Скрипт проверяет его наличие и завершится с ошибкой, если файла нет.

Шаблон лежит рядом с pom — [`mirroring/settings.example.xml`](mirroring/settings.example.xml):

```xml
<settings>
    <servers>
        <server>
            <id>artifactory.boreas.dept07</id>
            <username>YOUR_LOGIN</username>
            <password>YOUR_PASSWORD</password>
        </server>
    </servers>
</settings>
```

Скопируйте его в `~/.m2/settings.xml` и подставьте свои логин/пароль от Artifactory.

```cmd
mirror.cmd
```
```bash
./mirror.sh
```
