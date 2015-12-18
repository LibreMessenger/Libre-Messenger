# Pix-Art Messenger

Pix-Art Messenger is a fork of the official [Conversations](https://github.com/siacs/Conversations) app with some modifications described under features.

Pix-Art Messenger can be downloaded from github [releases](https://github.com/kriztan/Conversations/releases)

![screenshots](https://raw.githubusercontent.com/siacs/Conversations/master/screenshots.png)

## Design principles

* Be as beautiful and easy to use as possible without sacrificing security or
  privacy
* Rely on existing, well established protocols (XMPP)
* Do not require a Google Account or specifically Google Cloud Messaging (GCM)
* Require as few permissions as possible

## Features

* End-to-end encryption with either [OTR](https://otr.cypherpunks.ca/) or [OpenPGP](http://www.openpgp.org/about_openpgp/)
* Send and receive images as well as other kind of files
* Share your location via an external [plug-in](https://play.google.com/store/apps/details?id=eu.siacs.conversations.sharelocation&referrer=utm_source%3Dgithub)
* Indication when your contact has read your message
* Intuitive UI that follows Android Design guidelines
* Pictures / Avatars for your Contacts
* Syncs with desktop client
* Conferences (with support for bookmarks)
* Address book integration
* Multiple accounts / unified inbox
* Very low impact on battery life

our individual features:
* accounts are hard linked to pix-art.de
* increased avatar-sizes in account- and contact-details with prefering XMPP-avatars over addressbook avatars
* in-app self updater with dayly check for updates
* moved writing info from chatwindow into actionbar-subtitle
* show lastseen info in actionbar-subtitle
* display group chat members in actionbar-subtitle
* images are transfered as JPEG
* support more file-types for file-transfer (pdf, doc, docx, txt, m4a, m4b, mp3, mp2, wav, aac, aif, aiff, aifc, mid, midi, 3gpp, avi, mp4, mpeg, mpg, mpe, mov, 3gp, apk, vcf, ics, zip, rar)
* show contacts name in locations shared in conferences

### XMPP Features

Pix-Art Messenger works only with `pix-art.de`. However XMPP is an
extensible protocol. These extensions are standardized as well in so called
XEP's. Pix-Art Messenger supports a couple of these to make the overall user
experience better. Our XMPP-Server is prosody and supports all of the listed XEP's:

* [XEP-0065: SOCKS5 Bytestreams](http://xmpp.org/extensions/xep-0065.html) (or mod_proxy65). Will be used to transfer
  files if both parties are behind a firewall (NAT).
* [XEP-0163: Personal Eventing Protocol](http://xmpp.org/extensions/xep-0163.html) for avatars
* [XEP-0191: Blocking command](http://xmpp.org/extensions/xep-0191.html) lets you blacklist spammers or block contacts
  without removing them from your roster.
* [XEP-0198: Stream Management](http://xmpp.org/extensions/xep-0198.html) allows XMPP to survive small network outages and
  changes of the underlying TCP connection.
* [XEP-0280: Message Carbons](http://xmpp.org/extensions/xep-0280.html) which automatically syncs the messages you send to
  your desktop client and thus allows you to switch seamlessly from your mobile
  client to your desktop client and back within one conversation.
* [XEP-0237: Roster Versioning](http://xmpp.org/extensions/xep-0237.html) mainly to save bandwidth on poor mobile connections
* [XEP-0313: Message Archive Management](http://xmpp.org/extensions/xep-0313.html) synchronize message history with the
  server. Catch up with messages that were sent while Pix-Art Messenger was
  offline.
* [XEP-0352: Client State Indication](http://xmpp.org/extensions/xep-0352.html) lets the server know whether or not
  Pix-Art Messenger is in the background. Allows the server to save bandwidth by
  withholding unimportant packages.
* [XEP-0363: HTTP File Upload](http://xmpp.org/extensions/xep-0363.html) allows you to share files in conferences and with offline
  contacts. Requires an [additional component](https://github.com/siacs/HttpUploadComponent)
  on your server.

## Team

#### Head of Development (original Conversations)

* [Daniel Gultsch](https://github.com/inputmice) 

#### Head of Development (Pix-Art Messenger)

* [Christian Schneppe](https://github.com/kriztan)

#### Code Contributions (original Conversations)

(In order of appearance)

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

#### Translations
Translations are managed on [Transifex](https://www.transifex.com/projects/p/conversations/) for original features

Translations for our own features are managed on github. If you would like to help translation the app create a pull-request.

## FAQ

### General

#### How do I install Pix-Art Messenger?

Pix-Art Messenger is entirely open source and licensed under GPLv3. So if you are a
software developer you can check out the sources from GitHub and use ant to
build your apk file.

Pix-Art Messenger can be downloaded from github [releases](https://github.com/kriztan/Conversations/releases)

#### How do I create an account?

With Pix-Art Messenger you can only create and use accounts on `pix-art.de`. You can create your own account for free. You only need a nickname and a password. You don't need to add `@pix-art.de` to your nickname. After creating your own `pix-art.de` account you'll find me in your contact list. If you have any questions about our app or our service feel free to ask me or join our support-conference `support@room.pix-art.de`.

#### How does the address book integration work?

The address book integration was designed to protect your privacy. Pix-Art Messenger
neither uploads contacts from your address book to your server nor fills your
address book with unnecessary contacts from your online roster. If you manually
add a Jabber ID to your phones address book Pix-Art Messenger will use the name and
the profile picture of this contact. To make the process of adding Jabber IDs to
your address book easier you can click on the profile picture in the contact
details within Pix-Art Messenger. This will start an "add to address book" intent
with the JID as the payload. This doesn't require Pix-Art Messenger to have write
permissions on your address book but also doesn't require you to copy/paste a
JID from one app to another.

#### I get 'delivery failed' on my messages

If you get delivery failed on images it's probably because the recipient lost
network connectivity during reception. In that case you can try it again at a
later time.

For text messages the answer to your question is a little bit more complex.
When you see 'delivery failed' on text messages, it is always something that is
being reported by the server. The most common reason for this is that the
recipient failed to resume a connection. When a client loses connectivity for a
short time the client usually has a five minute window to pick up that
connection again. When the client fails to do so because the network
connectivity is out for longer than that all messages sent to that client will
be returned to the sender resulting in a delivery failed.

Other less common reasons are that the message you sent didn't meet some
criteria enforced by the server (too large, too many). Another reason could be
that the recipient is offline and the server doesn't provide offline storage.

Usually you are able to distinguish between these two groups in the fact that
the first one happens always after some time and the second one happens almost
instantly.

#### Where can I see the status of my contacts? How can I set a status or priority?

Statuses are a horrible metric. Setting them manually to a proper value rarely
works because users are either lazy or just forget about them. Setting them
automatically does not provide quality results either. Keyboard or mouse
activity as indicator for example fails when the user is just looking at
something (reading an article, watching a movie). Furthermore automatic setting
of status always implies an impact on your privacy (are you sure you want
everybody in your contact list to know that you have been using your computer at
4am‽).

In the past status has been used to judge the likelihood of whether or not your
messages are being read. This is no longer necessary. With Chat Markers
(XEP-0333, supported by Pix-Art Messenger since 0.4) we have the ability to **know**
whether or not your messages are being read. Similar things can be said for
priorities. In the past priorities have been used (by servers, not by clients!)
to route your messages to one specific client. With carbon messages (XEP-0280,
supported by Pix-Art Messenger since 0.1) this is no longer necessary. Using
priorities to route OTR messages isn't practical either because they are not
changeable on the fly. Metrics like last active client (the client which sent
the last message) are much better.

Unfortunately these modern replacements for legacy XMPP features are not widely
adopted. However Pix-Art Messenger should be an instant messenger for the future and
instead of making Pix-Art Messenger compatible with the past we should work on
implementing new, improved technologies and getting them into other XMPP clients
as well.

Making these status and priority optional isn't a solution either because
Pix-Art Messenger is trying to get rid of old behaviours and set an example for
other clients.

### Security

#### Encryption methodes

You can choose between different end-to-end encryption methodes, which are explained in the following lines. Our Server only accepts TLS encrypted connections to clients and only allows unencrypted connections to other server, if there is no way to encrypt the connections. If you are chatting with users to other servers, you should use an end-to-end encryption methode or ask me to look if the server-connections are encrypted or not.

#### Why are there three end-to-end encryption methods and which one should I choose?

In most cases OTR should be the encryption method of choice. It works out of the box with most contacts as long as they are online. However, openPGP can, in some cases, (message carbons to multiple clients) be more flexible. Unlike OTR, OMEMO works even when a contact is offline, and works with multiple devices. It also allows asynchronous file-transfer when the server has [HTTP File Upload](http://xmpp.org/extensions/xep-0363.html). However, OMEMO is not as widely supported as OTR and is currently implemented only by Pix-Art Messenger. OMEMO should be preffered over OTR for contacts who use Pix-Art Messenger.

#### How do I use OpenPGP

Before you continue reading you should note that the OpenPGP support in
Pix-Art Messenger is experimental. This is not because it will make the app unstable
but because the fundamental concepts of PGP aren't ready for widespread use.
The way PGP works is that you trust Key IDs instead of JID's or email addresses.
So in theory your contact list should consist of Public-Key-IDs instead of
JID's. But of course no email or XMPP client out there implements these
concepts. Plus PGP in the context of instant messaging has a couple of
downsides: It is vulnerable to replay attacks, it is rather verbose, and
decrypting and encrypting takes longer than OTR. It is however asynchronous and
works well with message carbons.

To use OpenPGP you have to install the open source app
[OpenKeychain](http://www.openkeychain.org) and then long press on the account in
manage accounts and choose renew PGP announcement from the contextual menu.

#### How does the encryption for conferences work?

For conferences the only supported encryption method is OpenPGP (OTR does not
work with multiple participants). Every participant has to announce their
OpenPGP key (see answer above). If you would like to send encrypted messages to
a conference you have to make sure that you have every participant's public key
in your OpenKeychain. Right now there is no check in Pix-Art Messenger to ensure
that. You have to take care of that yourself. Go to the conference details and
touch every key id (The hexadecimal number below a contact). This will send you
to OpenKeychain which will assist you on adding the key.  This works best in
very small conferences with contacts you are already using OpenPGP with. This
feature is regarded experimental. Pix-Art Messenger is the only client that uses
XEP-0027 with conferences. (The XEP neither specifically allows nor disallows
this.)

#### I found a bug

If you have troubles or problems please report them first into our support chat `support@room.pix-art.de`, so we can decide if it's an issue caused by our features or a global issue.

Only global issues should be reported into the Conversations [issue tracker][issues]. If your app crashes please
provide a stack trace. If you are experiencing misbehaviour please provide
detailed steps to reproduce. Always mention whether you are running the latest
Play Store version or the current HEAD. If you are having problems connecting to
your XMPP server your file transfer doesn’t work as expected please always
include a logcat debug output with your issue (see above).

[issues]: https://github.com/siacs/Conversations/issues
