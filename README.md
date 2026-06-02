<div align="center">

<img height="192" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Logo">

<h1>WeChat Lan</h1>

A compact local area network (LAN) communication application that strives to align with WeChat in
terms of interaction and functionality, while deeply leveraging the underlying capabilities of the
Android system to achieve direct and efficient interconnection between devices using various
near-field communication technologies.

English · [中文](./README.zh-CN.md)

[![GitHub](https://img.shields.io/badge/GitHub-WeChat-blue?style=flat-square&logo=github)](https://github.com/chengdongqing/WeChat)
[![Gitee](https://img.shields.io/badge/Gitee-WeChat-red?style=flat-square&logo=gitee)](https://gitee.com/chengdongqing/WeChat)
[![Demo](https://img.shields.io/badge/Releases-Download-green?style=flat-square&logo=android)](https://gitee.com/chengdongqing/WeChat/releases)
[![Juejin](https://img.shields.io/badge/掘金社区-white?style=flat-square&logo=juejin)](https://juejin.cn/post/7621443853845594154)

</div>

---

### I. Core Philosophy

1. **Decentralization:** Get rid of server reliance and achieve end-to-end direct connection between
   devices.
2. **Multi-modal Connectivity:** Supports various near-field communication methods such as Wi-Fi,
   Wi-Fi Direct, and Bluetooth.
3. **Full-stack Technology:** Deeply integrated hardware scheduling, end-to-end encryption,
   high-speed file transfer, and real-time audio/video communication.

### II. Technology Stack

|                          | Technology Selection          | Description                                                                    |
|--------------------------|-------------------------------|--------------------------------------------------------------------------------|
| **UI Framework**         | Jetpack Compose               | Build responsive, modern UI                                                    |
| **Dependency Injection** | Hilt                          | Modular code decoupling                                                        |
| **Page Navigation**      | Navigation3                   | Type-safe state navigation                                                     |
| **Data Persistence**     | Room3, DataStore              | Efficient local data caching and configuration management                      |
| **Multimedia**           | WebRTC, CameraX, MLKit, Coil3 | Provide real-time audio/video and media processing capabilities                |
| **Map Service**          | AMap                          | Pluggable architecture, supports free switching between Standard and Lite SDKs |
| **Friend Addition**      | BLE                           | Bluetooth Low Energy                                                           |
| **Service Discovery**    | NSD                           | Service registration, discovery, and resolution within the LAN                 |
| **Communication**        | TCP Socket / RfcommSocket     | Underlying connection schemes between devices                                  |

**All use mainstream technology stacks and the latest versions**

### III. Feature Overview

#### 1. Overview

* Chat
* Contacts
* Me
* Settings
* Login

#### 2. Chat

* Chat List
* Mark as unread/read
* Pin chat
* Hide chat
* Delete chat
* Unread count display


* Message Types
* Text, voice, emoji, image, video, location, file, music, contact card


* Input Box
* Text input, emoji input, full-screen input, message draft


* Calls
* Video call, voice call


* Message Operations
* Copy, forward, delete, recall, download
* Multi-select (batch forward, delete, download)
* Switch earpiece/speaker (voice message)


* Status Display
* Online status, encryption status, play voice via earpiece


* Chat Information
* Mute notifications
* Pin chat
* Set chat background
* Clear chat history

#### 3. Contact Functions

* Contact List
* New friends, me, friend list, friend count, index bar
* Long press to set friend profile


* Contact Details
* Avatar (click to view full image), remarks, nickname, WeChat ID, gender, signature


* Friend Profile
* Remarks, notes, friend source, time added


* Friend Settings
* Set friend profile
* Recommend him/her to a friend
* Add to blacklist
* Delete friend

#### 4. Personal Profile

* Avatar: Preview, modify, save
* Basic Info: Name, gender, WeChat ID, signature
* My QR Code
* QR code generation, scan, change style, save image

#### 5. Notification Settings

* Notification Types
* Message notifications, voice and video call notifications


* Notification Content
* Only show "You received 1 message"
* Show friend and group names
* Show friend/group names and message content


* System Settings
* System notification settings entry
* New message notification in chat interface (sound control, vibration control)


* Ringtones & Alerts
* Message alert: Follow system, built-in list (support preview)
* Incoming call ringtone: Follow system, WeChat, other built-in lists (support preview)
* Friends can also hear your incoming call ringtone when calling you

#### 6. Other Settings

* Interface & Display
* Dark mode (follow system/light/dark)
* Font size (slider adjustment, real-time preview)
* Multi-language (follow system/Simplified Chinese/English)


* Friend Permissions
* Verification required when adding me as a friend
* Contact blacklist


* More
* System permission status display


* Connection Mode
* Wi-Fi, Wi-Fi Direct, Bluetooth


* Chat Settings
* Global chat background setting
* Use earpiece to play voice messages
* Use independent send button
* End-to-end encryption


* Chat History Management
* Clear all chat history


* Account Management
* Logout

---

### IV. Functional Details

See details at: [Juejin Community](https://juejin.cn/post/7621443853845594154)