# RanFlood

<img src="icon.png?raw=true" width="33%">

RanFlood consists in two programs: a client and a daemon.

The user interacts with the client to issue commands to the daemon.

The daemon should run as a service (always on, restarted if it crashes, etc.). The daemon accepts as parameter a path to a `setting.ini` file which reports where to store the on-the-fly signatures DB and sets its request address (the default is `localhost:7890`). The playground folder in this repo includes an [exemplary setting.ini](https://github.com/thesave/ranflood/blob/master/src/test/java/playground/settings.ini) file.