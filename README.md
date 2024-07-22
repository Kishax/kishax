# FMC Plugin
>## This is able to use in both bungee and spigot.<br>But this is created for myself for my server.<br>So this plugin is maybe good for plugin developers.<br>Freely to edit!<br>
## Bungee Command list
### /hub
### /fmcb hub
Moving to hub server<br>
### /fmcb ss \<server\>
Getting server status and checking whether you have FMC account from MySQL<br>
In FMC Server, using python script for getting minecrafts' status<br>
>Here is [python scripts](https://github.com/bella2391/Mine_Status)<br>
### /fmcb stp \<server\>
Moving to specific server as server command
### /fmcb req \<server\>
Requesting to let server start-up to Admin through discord and python<br>
In FMC Server, using python and php script for requesting to Discord<br>
>Here is [python and php scripts](https://github.com/bella2391/Discord_Button)
### /fmcb start \<server\>
Let server start by bat file of windows
### /fmcb cancel
Only sending "canceled event"
## Socket Server
Sockets are enable us to communicate between BungeeCord and Spigot Servers.<br>
There are more extendance than BungeeCord Message Channel.
### Reason
・Communication Available even when players are offline<br>
・Not Java, for example, PHP can be access to it.<br>
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
### /fmc fly
Flying in survival mode
### /fmc reload
Reloading config
### /fmc potion \<potion-effect-type\>
Adding potion effect within a radius of 5 squares
### /fmc test \<arg-1\>
Only returning arg-1 player writes
