# Pix-Art Messenger

Pix-Art Messenger ist eine Kopie der offiziellen Android-App [Conversations](https://github.com/siacs/Conversations) mit einigen Änderungen, die unter Features beschrieben sind.

Pix-Art Messenger kann von github [releases](https://github.com/kriztan/Conversations/releases) heruntergeladen werden.

![screenshots](https://raw.githubusercontent.com/siacs/Conversations/master/screenshots.png)

## Design Grundsätze

* so schön und einfach wie möglich zu sein, ohne die Sicherheit oder Privatsphäre zu beeinträchtigen
* auf existierenden und gut etablierten protokollen (XMPP) basierend
* es benötigt kein Google Account oder Google Cloud Messaging (GCM)
* verlangt so wenig Rechte wie möglich

## Features
### Standard-Features

* Ende-zu-Ende Verschlüsselung entweder mit [OMEMO](http://conversations.im/omemo/), [OTR](https://otr.cypherpunks.ca/) oder [OpenPGP](http://www.openpgp.org/about_openpgp/)
* Austausch von Bildern sowie anderen Dateien
* Teile Standorte mit einem externen Plugin [plug-in](https://github.com/kriztan/ShareLocationPlugin)
* Zeige Leseberichte an
* Intuitive Benutzeroberfläche
* Avatare Deiner Kontakte
* Synchronisiere Nachrichtenverlauf mit anderen Clients
* Konferenzen bzw. Gruppenchats
* Adressbuchintegration
* Unterstützung für mehrere Benutzerkonten
* sehr geringe Akku-Belastung

### Pix-Art-Messenger Features
* Benutzerkonten sind an pix-art.de gebunden
* Avatare in Konten- und Kontakt-Details werden vergrößert dargestellt 
* XMPP-Avatare werden vor Adressbuch-Avataren bevorzugt dargestellt
* integrierte tägliche Suche nach Aktualisierungen (Updates)
* Benutzer-schreibt-Info wird als Actionbar-Untertitel dargestellt
* zeige zuletzt-gesehen-Info als Actionbar-Untertitel
* zeige Gruppenchat-Mitglieder als Actionbar-Untertitel
* Bildtransfer als JPG
* Dateitransfer folgender Dateitypen (pdf, doc, docx, txt, m4a, m4b, mp3, mp2, wav, aac, aif, aiff, aifc, mid, midi, 3gpp, avi, mp4, mpeg, mpg, mpe, mov, 3gp, apk, vcf, ics, zip, rar)
* zeige Kontaktnamen in geteilten Standorten

### XMPP Features

Pix-Art Messenger funktioniert nur mit Konten auf `pix-art.de`. Das XMPP Protokoll ist standardisiert. Erweiterungen und Funktionen werden XEP genannt. Unser XMPP-Server [Prosody](https://prosody.im/) arbeitet mit unserem Pix-Art Messenger optimal zusammen; folgende Funktionen werden unterstützt:

* [XEP-0065: SOCKS5 Bytestreams](http://xmpp.org/extensions/xep-0065.html) (oder mod_proxy65). Wird für die Dateiübertragung verwendet, wenn sich beide Seiten hinter einer Firewall (NAT) befinden.
* [XEP-0163: Personal Eventing Protocol](http://xmpp.org/extensions/xep-0163.html) für Avatare und OMEMO
* [XEP-0191: Blocking command](http://xmpp.org/extensions/xep-0191.html) um Kontakte/Spammer zu blockieren
* [XEP-0198: Stream Management](http://xmpp.org/extensions/xep-0198.html) erlaubt Unterbrechungen der Verbindungen ohne Zerstörung der Verbindung (für mobile Geräte)
* [XEP-0280: Message Carbons](http://xmpp.org/extensions/xep-0280.html) Synchronisiert Nachrichten zwischen verschiedenen Client-Programmen
* [XEP-0237: Roster Versioning](http://xmpp.org/extensions/xep-0237.html) um Datentransfer niedrig zu halten
* [XEP-0313: Message Archive Management](http://xmpp.org/extensions/xep-0313.html) synchronisiert den Nachrichtenverlauf mit dem Server. So können Nachrichten abgerufen werden, während die App offline war.
* [XEP-0352: Client State Indication](http://xmpp.org/extensions/xep-0352.html) lässt den Server wissen, wann die App im Vorder- oder Hintergrund ist, um Bandbreite und Datentransfer niedrig zu halten.
* [XEP-0363: HTTP File Upload](http://xmpp.org/extensions/xep-0363.html) erlaubt den Transfer von Dateien/Bildern in Gruppenchats und an offine Kontakte. 

## Entwickler

#### Entwickler von original Conversations

* [Daniel Gultsch](https://github.com/inputmice) 

#### Entwickler von Pix-Art Messenger

* [Christian Schneppe](https://github.com/kriztan)

#### Quellcode-Ergänzungen von original Conversations

* [Rene Treffer](https://github.com/rtreffer) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3Artreffer+is%3Amerged))
* [Andreas Straub](https://github.com/strb) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3Astrb+is%3Amerged))
* [Alethea Butler](https://github.com/alethea) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3Aalethea+is%3Amerged))
* [M. Dietrich](https://github.com/emdete) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3Aemdete+is%3Amerged))
* [betheg](https://github.com/betheg) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3Abetheg+is%3Amerged))
* [Sam Whited](https://github.com/SamWhited) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3ASamWhited+is%3Amerged))
* [BrianBlade](https://github.com/BrianBlade) ([PRs](https://github.com/siacs/Conversations/pulls?utf8=%E2%9C%93&q=is%3Apr+author%3ABrianBlade+is%3Amerged))

#### Logo
* [Ilia Rostovtsev](https://github.com/qooob) (Progress)
* [Diego Turtulici](http://efesto.eigenlab.org/~diesys) (Original)
* [fiaxh](https://github.com/fiaxh) (OMEMO)

#### Übersetzungen
Pix-Art Messenger unterstützt momentan nur Englisch und Deutsch.
Übersetzungen für Conversations werden auf [Transifex](https://www.transifex.com/projects/p/conversations/) geführt.

## FAQ

### Allgemeines

#### Wie installiere ich Pix-Art Messenger?

Pix-Art Messenger ist OpenSource und lizensiert unter GPLv3. Du kannst den Quellcode von GitHub laden und Deine eigene APK erstellen.

Pix-Art Messenger kann als APK von GitHub [releases](https://github.com/kriztan/Conversations/releases) heruntergeladen werden

#### Wie erstelle ich ein Benutzerkonto?

Pix-Art Messenger kannst Du nur mit Konten auf `pix-art.de` verwenden. Du kannst Dein Benutzerkonto kostenlos erstellen. Dazu benötigst Du einen Nicknamen und ein Passwort. Die Endung `@pix-art.de` hinter Deinem Nicknamen wird nicht mehr benötigt (sie ist jedoch gemeinsam mit Deinem Nicknamen Deine tatsächliche XMPP-Adresse) . Nachdem Du Dein Konto auf `pix-art.de` erstellt hast, findest Du mich in Deiner Kontakt-Liste. Bei Fragen und/oder Problemen kannst Du mich anschreiben oder unserer Support-Gruppe `support@room.pix-art.de` beitreten.

#### Wie funktioniert die Adressbuch-Integration?

Die Adressbuch-Integration respektiert Deine Privatsphäre. Pix-Art Messenger
lädt zu keiner Zeit Kontaktdaten an unseren Server hoch, sondern liest lediglich die XMPP-Adresse von Deinen Kontakten aus, um Avatre und/oder Namen in Pix-Art Messenger darzustellen. Wenn Du einen XMPP-Kontakt zu Deinem Adressbuch hinzufügen möchtest, klicke auf das Kontaktbild/Avatar und wähle den entsprechenden Kontakt aus, Pix-Art Messenger wird dem Kontakt die XMPP-Adresse zuweisen. 

#### Ich erhalte bei Nachrichten den Hinweis 'Zustellung fehlgeschlagen'

Falls die Zustellung bei Bilder fehlschlägt ist Deine Verbindung oder die Deines Kontaktes während der Übertragung getrennt worden. Versuche es einfach erneut.

Bei Text-Nachrichten ist die Antwort komplexer. 
Fehlgeschlagene Textnachrichten werden vom Server gemeldet. Der häufigste Grund ist, dass Dein Kontakt die bestehende Verbindung nicht mehr fortgesetzt hat oder offline gegangen ist. Wenn Du und Dein Kontakt Pix-Art Messenger nutzen, dann wird die Nachricht möglicherweise dennoch zugestellt, da der Server den Nachrichtenverlauf speichert und beim Herstellen der Verbindung zustellt.

Weitere Gründe können sein, dass Deine Nachricht zu groß ist.

Sollten diese Probleme häufig auftreten, wende Dich an mich.

### Sicherheit

#### Verschlüsselungsmethoden

Du kannst zwischen verschiedenen Ende-zu-Ende Verschlüsselungen wählen. 
Unser Server akzeptiert nur TLS-verschlüsselte Verbindungen zu Client-Anwendungen und unverschlüsselte Verbindungen zu Fremdservern nur dann, wenn der Fremdserver keine Verschlüsselung unterstützt. Wenn Du also mit Kontakten anderen Server schreibst, ist es ratsam eine Ende-zu-Ende Verschlüsselung zu verwenden oder Du fragst mich, ob die Verbindung zum Fremdserver verschlüsselt ist oder nicht.

#### Warum gibt es drei Ende-zu-Ende Verschlüsselungstypen und welche soll ich nehmen?

In den meisten Fällen sollte OTR die Methode Deiner Wahl sein. Sie funktioniert ohne Probleme solange beide online sind. OpenPGP kann, in einigen Fällen, (bei mehreren Endgeräten) flexibler sein. OMEMO dagegen funktioniert auch dann, wenn Dein Kontakt offline ist, und auch mit mehreren Endgeräten. OMEMO unterstützt außerdem den Austausch von Dateien wenn der Server [HTTP File Upload](http://xmpp.org/extensions/xep-0363.html) nutzt. OMEMO ist jedoch nicht so weit verbreitet wie OTR. 

#### Wie verwende ich OpenPGP

Für die Verwendung von OpenPGP musst Du die OpenSource App [OpenKeychain](http://www.openkeychain.org) installieren und in der Konto-Verwaltung mit einem langen Klick auf das entsprechende Konto den Punkt <öffentlichen OpenPGP-Schlüssel veröffentlichen> wählen.

#### Wie funktioniert die Verschlüsselung in Konferenzen/Gruppenchats?

Für Gruppenchats kann nur OpenPGP verwendet werden. Jedes Gruppenmitglied muss seinen eigenen öffentlichen OpenPGP Schlüssel veröffentlichen (siehe Punkt zuvor). Wenn Du eine verschlüsselte Nachricht versenden möchtest, musst Du sicher gehen, dass alle Mitglieder Deinen öffentlichen Schlüssel kennen und Du deren öffentlichen Schlüssel kennst. Aktuell ist diese Prüfung im Pix-Art Messenger nicht automatisiert möglich, sondern muss mit jedem Kontakt manuell durchgeführt werden.

#### Ich habe einen Fehler/Bug gefunden

Falls Du Fehler/Bugs in Pix-Art Messenger findest, teile sie zuerst in unserer Support-Gruppe `support@room.pix-art.de`, damit ich prüfen kann, ob es ein Problem unserer Features ist oder ein Globales.

Nur globale sollten im Conversations [issue tracker][https://github.com/siacs/Conversations/issues] gemeldet werden. 
