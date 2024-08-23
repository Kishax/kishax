# FMC Plugin
## Comment 
This is able to use in both Velocity and Spigot.<br>
But this is created for myself for my server.<br>
So this plugin is maybe good for plugin developers.<br>
[FMC-Fabric-Mod](https://github.com/bella2391/FMC-Plugin/tree/fabric) and [FMC-Forge-Mod](https://github.com/bella2391/FMC-Plugin/tree/forge) with similar functionality to spigot are currently under development. This is all needed by connecting to Velocity even if host server is fabric or forge server.<br>
Freely to edit!<br>

## Linkage to Discord
For Velocity Server, Velocity Server notifys Embed's message or plain-text-message under each events.<br>
When server switching, joining, disconnecting like this.<br>
![alt text](images/event_message.png)<br>
When chatting like this.<br>
![alt text](images/chat_message2.png)
## Convert Romaji to Kanji
This brings an automatic chat conversion Romaji to Kanji like this.<br>
![alt text](images/chat_conv.png)
## Velocity Command list
### `/hub`
### `/fmcp hub`
Moving to hub server<br>
### `/fmcp cend`
After executing, Velocity will be shutdown!<br>
Before being shutdown, discord's embed editing like this.
![alt text](images/proxy_shutdown.png)
### `/fmcp maintenance <status | switch> discord <true | false>`
This enable server to be maintenance mode, which is that for example, it is openable for only Admin who has permission:group.super-admin, others disconnecting.<br>
If arg5 sets "true", server can notify to Discord whether maintenance mode is true or not.<br>
### `/fmcp perm <add | remove | list> [Short:permission] [target:player]`
Adding or removing permission written in config.yml by adding or removing permission in mysql database for luckperm MySQL mode.
### `/fmcp ss <server>`
Getting server status and checking whether you have FMC account from MySQL<br>
In FMC Server, using python script for getting minecrafts' status<br>
>Here is [python scripts](https://github.com/bella2391/Mine_Status)<br>
### `/fmcp stp <server>`
Moving to specific server as server command
### `/fmcp req <server>`
Requesting to let server start-up to Admin through discord like this.<br>
![alt text](images/req_button.png)<br>
If someone presses `YES` button, here will be like this.<br>
![alt text](images/reqsul_notification.png)<br>
Here is minecraft's player chat area.<br>
![alt text](images/req_minecraft_chat.png)<br>
### `/fmcp start <server>`
Let server start by bat file of windows
### `/fmcp cancel`
Only sending "canceled event"
### `/fmcp conv <add | remove | reload | switch> [<add | remove>:key] [<add>:value] [<add>:<true | false>]`
Switching converting type of Romaji to Kanji, reloading romaji.csv from `plugins/fmc/romaji.csv`, or adding/removing a theirself word into the csv file that has a lot of maps of conversion romaji to kana. 
### `/fmcp chat <switch | status>`
Switching the way of sending chating message to Discord. <br>
There are Embed editing type or Plane text message type.<br>
* Embed editing type (Using Bot)<br>
![alt text](images/embed_editing_type.png)<br>
* Plane text message type (Using Webhook)<br>
![alt text](images/plain_text_message_type.png)<br>
### `/fmcp debug`
Switching debug mode. In details, this is only replacing config value each other. For example, Discord.ChannelId and Debug.ChannelId.
### `/fmcp reload`
Reloading configuration.
## Socket Server
Sockets are enable us to communicate between Velocity and Spigot Servers.<br>
### Reason
* Communication Available even when players are offline<br>
* Not Java, for example, PHP can be access to it.<br>
#### Here is PHP example code
```
<?php
  // server address & port
  $serverAddress = '127.0.0.1';
  $serverPort = 8766;

  // create socket and connect
  $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
  socket_connect($socket, $serverAddress, $serverPort);

  // send message
  socket_write($socket, $message, strlen($message));

  // close
  socket_close($socket);
```
## Spigot Command list
### `/fmc fv <player> <proxy_cmd>`
Forwarding Velocity's command in Spigot
### `/fmc reload`
Reloading config
### `/fmc test <arg-1>`
Only returning arg-1 player writes

## Dependancy
* [Luckperms](https://github.com/LuckPerms/LuckPerms)

## Lisence
This project is licensed under the MIT License, see the LICENSE.txt file for details

