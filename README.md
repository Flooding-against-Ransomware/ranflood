# Ranflood

<img src="icon.png?raw=true" width="33%">

Ranflood consists of two programs: a client (`ranflood`) and a daemon (`ranfloodd`).

The user interacts with the client to issue commands to the daemon.

At the moment, Ranflood supports five types of flooding strategies:

- `random`: based on the flooding of a given location with randomly-generated files.
- `on-the-fly`:  a is copy-based strategy, where the generation of files uses copies of actual files found at a flooding location. File replication adds a layer of defence as it helps to increase the likelihood of preserving the users’ files by generating additional, valid copies that might escape the ransomware.
This strategy introduces a new `snapshoot` action that precedes the flooding one, which saves a list of the valid files with their digest signature (e.g., MD5), so that the flooding operations can use the signature as an integrity verification to skip the encrypted files. 
- `shadow`: another copy-based strategy which increases the efficiency of the `on-the-fly` by preserving archival copies of the files of the user.
- `sss-exfiltration`, `sss-ransomware`: using Shamir's Secret Sharing, they allow to split each file into multiple shard-files, such that a minimum amount of them is needed to reconstruct the original one. While being surely slower than copy-based strategies, having to apply additional cryptographic operations, they can prevent an efiltration ransomware from obtaining a user's file, if the attacker hasn't collected enough shards. The two strategies onoly differ for default parameters combinations, which are configured to best address, respectively, exfiltration and crypto ransomwares.

The daemon should run as a service (always on, restarted if it crashes, etc.). The daemon accepts as a parameter a path to a `setting.ini` file which reports where to store the on-the-fly and shadow-copy DBs and archives and sets its request address (the default is `localhost:7890`).

For instance, an example `setting.ini` file for a Windows installation is

```
[RandomFlooder]
MaxFileSize = 768 KB
[OnTheFlyFlooder]
Signature_DB = C:\Users\thesave\Desktop\ranflood_testsite\signatures.db
ExcludeFolderNames = Application Data, OtherFolder
[ShadowCopyFlooder]
ArchiveDatabase = C:\Users\thesave\Desktop\ranflood_testsite\archives.db
ArchiveRoot = C:\Users\thesave\Desktop\ranflood_testsite\archives
ExcludeFolderNames = Application Data, OtherFolder
[SSSExfiltration]
ExcludeFolderNames = Application Data, OtherFolder
ShardsCreated = 150
ShardsNeeded = 6
RemoveOriginals = true
[SSSRansomware]
ExcludeFolderNames = Application Data, OtherFolder
ShardsCreated = 200
ShardsNeeded = 2
RemoveOriginals = false
[ZMQ_JSON_Server]
address = tcp://localhost:7890
[HTTP_Server]
port = 8081
[WEBSOCKET_Server]
port = 8080
```

The playground folder in this repo includes an [illustrative setting.ini](https://github.com/thesave/ranflood/blob/master/src/tests/java/playground/settings.ini) file for Linux/macOS.

## Using the Ranflood Client

Issuing the command `ranflood --help` shows the list of actions users can invoke on the running daemon, through the client.

Roughly, the main commands users can issue to the Ranflood daemon have the following three parameters:

- `action`: whether we want to `flood` or perform a data `snapshot` (the latter is for issuing copy-based strategies, and can be used with `sss` strategies to improve their performance);
- `target folder`: the folder where to perform the action;
- `method`: the strategy we consider for the action (Random, On-the-Fly, Shadow, SSSExfiltration, SSSRansomware).

Focussing on the `flood` command, there are three sub-actions we can ask the daemon to perform:

- `ranflood flood list`: list all ongoing flooding operations, identified by a unique id;
- `ranflood flood start` <method> <targetFolder>: start a flooding operation (`random`, `on-the-fly`, `shadow`, `sss-exfiltration`, `sss-ransomware`) on a specific folder;
- `ranflood flood stop` <method> <ids>: stop a list of flooding operations, identified via their ids.

## Restoration, after an attack

Users can try to recover the environment from before the attack using the `filechecker` tool included in this repository --- this is a different software than Ranflood and, in the future, will have a dedicated project repository.

Users can compile the `filechecker` by issuing the command `gradle filecheckerJar` in the root folder of the repository (it requires at least `java 18` and `gradle 7`), which produces the executable `filechecker.jar` program under the path `build/libs`. 

### Copy-based strategies

The tool removes the decoy files generated by Ranflood and tries to restore the files of the user lost to encryption, mainly when used after employing a copy-based flooding strategy.

The tool has two commands. The first, run in ordinary conditions, is `save`, and it saves a list of snapshot signatures of the files of the user. 
The signatures are used to compare the files present after an attack, issuing the command `check`. 
This operation removes the files whose signatures do not appear in the list previously saved (and passed as a parameter of the check command). 
These files are mainly those generated by the ransomware (i.e., the encrypted files of the user) and the random decoy files generated by Ranflood. In case Ranflood used a copy-based strategy, 
if the `filechecker` cannot find the original copy of the file of the user, but it can find a flooding-generated replica, it uses the latter to restore the original file.

### SSS strategies

The tool can also be used to restore files after an attack when using the `SSS` strategies, through its subcommand `restore`, which tries to reconstruct the original files from the shards found in the given directory.  
Currently, it also requires, as a parameter, the checksums file returned by the `save` command (run before an attack); however, files can also be restored, in their original path, without a checksum entry. Different conflict cases are also handled: a file isn't reconstructed if it already exists with correct path and checksum, while it is saved with a different name if it's conflicting with another one.  

## To Do

- [ ] Export the following components in separate repositories
  - [ ] [FileChecker](https://github.com/Flooding-against-Ransomware/ranflood/tree/master/src/filechecker/java/org/ranflood/filechecker) and [analyser](https://github.com/Flooding-against-Ransomware/ranflood/tree/master/result_analyser)
  - [ ] Rig [management](https://github.com/Flooding-against-Ransomware/ranflood/tree/master/management-scripts) and [testing](https://github.com/Flooding-against-Ransomware/ranflood/tree/master/scripts) scripts
  - [ ] [Ransomware Samples](resources/ransomwares)


## Cite this repository

If you use this software in your work, please cite it using the following metadata.

```
@article{BGMMOP23,
  title = {Data Flooding against Ransomware: Concepts and Implementations},
  author = {Davide Berardi and Saverio Giallorenzo and Andrea Melis and Simone Melloni and Loris Onori and Marco Prandini},
  journal = {Computers & Security},
  pages = {103295},
  year = {2023},
  issn = {0167-4048},
  doi = {https://doi.org/10.1016/j.cose.2023.103295},
}
```
