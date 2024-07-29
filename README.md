# FMC Plugin
## Comment 
This is able to use in both Velocity and Spigot.<br>But this is created for myself for my server.<br>So this plugin is maybe good for plugin developers.<br>Freely to edit!<br>
## Velocity Command list
### `/hub`
### `/fmcp hub`
Moving to hub server<br>
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
Requesting to let server start-up to Admin through discord and python<br>
In FMC Server, using python and php script for requesting to Discord<br>
>Here is [python and php scripts](https://github.com/bella2391/Discord_Button)
### `/fmcp start <server>`
Let server start by bat file of windows
### `/fmcp cancel`
Only sending "canceled event"
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
### `/fmc fv <cmd>`
Forwarding Velocity's command in Spigot
### `/fmc fly`
Flying in survival mode
### `/fmc reload`
Reloading config
### `/fmc potion <potion-effect-type>`
Adding potion effect within a radius of 5 squares
### `/fmc test <arg-1>`
Only returning arg-1 player writes

## Dependancy
* [Luckperms](https://github.com/LuckPerms/LuckPerms)

## Lisence
This project is licensed under the MIT License, see the LICENSE.txt file for details

