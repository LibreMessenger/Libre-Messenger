![logo](/art/icon.png)
# Pix-Art Messenger

Pix-Art Messenger ist eine Kopie der offiziellen Android-App [Conversations](https://github.com/siacs/Conversations) mit einigen Änderungen.

[Download](https://xmpp.pix-art.de/Pix-Art_Messenger/)

Alternativ kannst du das IzzySoft F-Droid Repo installieren: https://android.izzysoft.de/repo, darin ist unser Messenger ebenfalls enthalten.

#### Wie kann ich bei der Übersetzung helfen?
Übersetzungen werden bei [https://poeditor.com](https://poeditor.com/join/project/I4cKFcY0Iv) geführt. Dort kann erstellt, geändert und ergänzt werden.

[Hier geht's zur Projektseite bei poeditor.com](https://poeditor.com/join/project/I4cKFcY0Iv)

#### Wie erstelle ich Debug- bzw. ADB-Logs?
1. Lade dir die SDK-Plattform-Tools für dein Betriebssystem von Google herunter: https://developer.android.com/studio/releases/platform-tools.html
2. Falls noch nicht getan, lade dir die ADB Treiber für dein Betriebssystem von Google herunter, für Windows hier: https://developer.android.com/studio/run/win-usb.html
3. Entpacke die zip (für Windows nach z.B. C:\ADB\)
4. Öffne die Kommandozeile (CMD) mit Start > Ausführen: cmd
5. Wechsele in der Kommandozeile in das Verzeichnis C:\ADB wie folgt 

  ```
  c:
  cd ADB
  ```
  
6. Auf deinem Telefon gehst du in die Einstellungen und suchst nach dem Punkt `Entwickleroptionen`. Sollte dieser bei dir nicht vorhanden sein, musst du diese Optionen erst noch freischalten. Dazu wechselst du in den Einstellungen in den Punkt `über das Telefon` und suchst dort nach `Buildnummer` oder Ähnlichem. Diese Zeile musst Du mindestens 7 mal hintereinander antippen, es sollte dann ein Hinweis eingeblendet werden, der dir bestätigt, dass du nun Entwickler bist.
7. In den `Entwickleroptionen` suchst du nach dem Eintrag `USB-Debugging` und aktivierst ihn.
8. Schließe dein Handy mit dem USB-Kabel an deinen PC an. Die erforderlichen Treiber sollten zumindest in Windows automatisch installiert werden.
9. Wenn alles ohne Fehler geklappt hat, kannst du wieder in die Kommandozeile gehen und testen, ob alles funktioniert. Gib dazu in CMD `adb devices -l` ein, es sollte in etwa sowas bei dir stehen:
  ```
  C:\ADB>adb devices -l
  List of devices attached
  * daemon not running. starting it now on port 5037 *
  * daemon started successfully *
  8551198ade34b951       unauthorized
  ```
10. Nun kannst du mit der Ausgabe der Debug-Logs beginnen. Dazu gibst du im CMD folgendes ein und die Ausgabe beginnt in die Datei `logcat.txt` im Verzeichnis `C:\ADB`:
  ```
  C:\ADB>adb -d logcat -v time | FINDSTR Pix-Art > logcat.txt
  ``` 
11. Führe nun die Schritte aus, die zum Fehler führen.

12. Zum Schluss schaue dir die `logcat.txt` an, lösche ggf. persönliche Angaben und sende diese Datei zur Problemlösung mit einer Beschreibung des Fehlers und was man tun muss, um diesen Fehler zu erhalten, an mich. Nutz dafür den Menüpunkt [Issues](https://github.com/kriztan/Pix-Art-Messenger/issues)
