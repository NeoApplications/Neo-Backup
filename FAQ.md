# Frequently Asked Questions

**also see other *.md files, e.g. NOTES.md for some more technical info that do not fit well in this FAQ, or TROUBLESHOOTING.md**

* [What is Neo Backup?](#what-is-neo-backup)
* [What is OAndBackupX?](#what-is-oandbackupx)
* [Which Android Versions are supported?](#which-android-versions-are-supported)
* [How do I use NB?](#how-do-i-use-nb)
* [What are all these backup-parts (icons)? / which parts are included in a backup?](#what-are-all-these-backup-parts-icons--which-parts-are-included-in-a-backup)
* [What are Special Backups?](#what-are-special-backups)
  * [How can I backup SMS \& Call log?](#how-can-i-backup-sms--call-log)
  * [How to restore special backups for bluetooth and wifi?](#how-to-restore-special-backups-for-bluetooth-and-wifi)
* [Do I need a rooted phone?](#do-i-need-a-rooted-phone)
  * [What is root access used for?](#what-is-root-access-used-for)
* [Why is NB so slow?](#why-is-nb-so-slow)
  * [So why use SAF then?](#so-why-use-saf-then)
  * [Below some "performance" or time measuring infos from an older phone](#below-some-performance-or-time-measuring-infos-from-an-older-phone)
* [I do not see any apps in the list. What can be the reason?](#i-do-not-see-any-apps-in-the-list-what-can-be-the-reason)
* [What should I do, when I get the "No SAF manager" message?](#what-should-i-do-when-i-get-the-no-saf-manager-message)
* [I do not see the app which is currently backed up in the notification during batch or scheduled backups](#i-do-not-see-the-app-which-is-currently-backed-up-in-the-notification-during-batch-or-scheduled-backups)
* [At restore the data directory of the app does not exist](#at-restore-the-data-directory-of-the-app-does-not-exist)
* [How does NB stop / pause / (un)suspend apps during backup?](#how-does-nb-stop--pause--unsuspend-apps-during-backup)
  * [Do I need to pause apps?](#do-i-need-to-pause-apps)
* [Are you going to support older Android versions?](#are-you-going-to-support-older-android-versions)
* [Can I use NB to switch to a new device / new OS / new Custom ROM / new major release of my ROM?](#can-i-use-nb-to-switch-to-a-new-device--new-os--new-custom-rom--new-major-release-of-my-rom)
* [Why do I have to login/register to app x y z again after restore?](#why-do-i-have-to-loginregister-to-app-x-y-z-again-after-restore)
* [Why is it not recommended to backup system apps?](#why-is-it-not-recommended-to-backup-system-apps)
* [Can I backup and restore the settings of NB?](#can-i-backup-and-restore-the-settings-of-nb)
  * [preferences](#preferences)
  * [schedules](#schedules)
  * [blocklist](#blocklist)
  * [alternative way - save some files](#alternative-way---save-some-files)
  * [alternative way 2 - use another NB variant to backup](#alternative-way-2---use-another-nb-variant-to-backup)
* [Is there a roadmap / an overview of features planned to be implemented?](#is-there-a-roadmap--an-overview-of-features-planned-to-be-implemented)
* [Where do I find the so called "DevTools"?](#where-do-i-find-the-so-called-devtools)
* [I read about selection and context menu in the Telegram group, where do I find this?](#i-read-about-selection-and-context-menu-in-the-telegram-group-where-do-i-find-this)
* [Can I control NB from scripts or Tasker or similar?](#can-i-control-nb-from-scripts-or-tasker-or-similar)
  * [now I want to detect the end of the operation](#now-i-want-to-detect-the-end-of-the-operation)
* [What is the difference to implementations like Seedvault?](#what-is-the-difference-to-implementations-like-seedvault)
* [What is the difference to the famous Titanium Backup?](#what-is-the-difference-to-the-famous-titanium-backup)
* [Do I need encryption?](#do-i-need-encryption)
* [How can I open encrypted backups on my computer?](#how-can-i-open-encrypted-backups-on-my-computer)
* [What does the notification of schedules and batch jobs tell me?](#what-does-the-notification-of-schedules-and-batch-jobs-tell-me)
* [Does NB support multi-user setups / work-profile?](#does-nb-support-multi-user-setups--work-profile)
* [Does NB support remote backup locations?](#does-nb-support-remote-backup-locations)
* [I need label/tags/similar on some backups to identify them](#i-need-labeltagssimilar-on-some-backups-to-identify-them)
* [How do I filter on changed data?](#how-do-i-filter-on-changed-data)
* [How do I create a support log?](#how-do-i-create-a-support-log)
* [Troubleshooting](#troubleshooting)

## What is Neo Backup?

Neo Backup (NB) is a fork of [OAndBackup](https://gitlab.com/jensstein/oandbackup) (which is inactive) with the aim to keep such a great and useful FOSS backup and recovery tool alive beyond 202X.

Most of the functionality and UI of the app has been re-written and it has already gotten much more stable. It is also a goal to add features which could ease the backup/restore workflow with any device.

NB **requires root** and allows you to backup individual apps with their data. Backup and restore of individual programs one at a time, batch backup and restore of multiple programs, as well as scheduled backups are supported.

Neo-Backup is part of the [NeoApplications Suite](https://github.com/NeoApplications)

## What is OAndBackupX?

OAndBackupX (OABX) was the former name of the project. You may find some reference in here or in the in-app usage notes, we have missed and are yet to change. The initial OABX release was in March 2020. The rename to NB took place more or less exactly at the second birthday of the project (between version 7 and 8), so stable version 7.0.0 still has the name OAndBackupX.

## Which Android Versions are supported?

Oldest supported version: Android  8 - "Oreo" </br>
Newest supported version: The newest stable Android version, unless stated otherwise

See also - [Are you going to support older Android versions?](#are-you-going-to-support-older-android-versions)

## How do I use NB?

The first start will guide you through the most important preferences. It is still quite useful to quickly check the preferences, to see what else can be set. E.g. you can define the amount of kept backup revisions or decide which [app parts](#what-are-all-these-backup-parts-icons--which-parts-are-included-in-a-backup) should be included.

There are 4 screens/tabs:

(navigate between them using the icons of the menu-bar on the bottom of the screen)

1. Main (AppList based on the current filter + access to AppSheet of each app)
2. Batch - Backup
3. Batch - Restore
4. Scheduled backups

The preferences menu can be opened via button in the upper right corner (fifth and very right icon of the menu-bar until v8.2.0).

A safe way to start is to do some backup and restore tests of an uncritical app via AppSheet.
Go forward and backup multiple apps via Batch and finally define schedules e.g. with Custom lists.

In case of any problems report it in [the Telegram or Matrix group](https://github.com/NeoApplications/Neo-Backup#community) first (sometimes there is an easy solution) and/or [raise an issue here in github](https://github.com/NeoApplications/Neo-Backup/issues).

## What are all these backup-parts (icons)? / which parts are included in a backup?

Each backup basically consists of the two different parts:

1. the software itself is stored in an **APK** file (some bigger apps have multiple parts - called split APK files)

2. its **data** (created while using an app, settings, etc.)

    The data can be split again into several data types:

    * **"normal"** data

      <details><summary>Show details ...</summary>

        * Stored usually in `/data/data`
        * Included in the backup by default

      </details>

    * **external** data

      <details><summary>Show details ...</summary>

        * Stored usually in `/Android/data/` in the external storage (internal storage in android current terminology)
        * Not included in the backup by default (can be enabled in preferences)

      </details>

    * **obb** files

      <details><summary>Show details ...</summary>

        * Stored usually in `/Android/obb/` in the external storage (internal storage in android current terminology)
        * Not included in the backup by default (can be enabled in preferences)

      </details>

    * **device protected** data

      <details><summary>Show details ...</summary>

        * Stored usually in `/data/user_de/`
        * introduced in Android version 7
        * It's usual data, but can be accessed before even unlocking your phone (authentication).
        * It's usually data that is needed for the pre-start phase of your device.
          E.g. a preference which decides if your app should start on device boot or not.
        * It is also called Device Encrypted (user_*DE*) Data because it is encrypted by a key which is tied to the physical device
        * there are a lot of articles out there which explains it in more detail and  how it differs from credential encrypted storage (CES)
          e.g. this one: https://lampham.medium.com/device-encrypted-storage-and-direct-boot-mode-in-android-6e5e25d173f3
        * if apps store important data in this area it depends on how the developer in charge has implemented it
        * Included in the backup by default

      </details>

    * **media** files

      <details><summary>Show details ...</summary>

        * this type is related to a controversial change that originally was slated for Android 10
          it is mandatory for all new apps since August 2020 and/or every app targeting Android 11 (SDK 30)
        * the main thing here is called "Scoped Storage"
        * Storage is then divided into Shared Storage and Private Storage
        * as part of SAF this is Android's approach to secure the access to media files
          and limit it to the ones of each app individually
        * e.g. chat apps have/had to move their media data into data directories, instead of generic folders
          (e.g. WhatsApp's well known WhatsApp folder)
        * Android might wants to move all the data directories to `/storage/emulated/0/Android`
          Below this folder you could find folders for `data`, `obb`, `media`
          ... an in them, a folder of each app's package name
        * Not included in the backup by default (can be enabled in preferences)

      </details>

    * **cache** data

      <details><summary>Show details ...</summary>

        * Included in the backup by default

      </details>

You can individually choose which parts you want to include in the backup &rarr; as global setting in preferences, (beginning with version NB [OABX] 6.x) per schedule or even per App.

## What are Special Backups?

Special backups describes system data that's bound to the user and not to certain apps. </br>***Enable special backups in advanced preferences to see them.***</br>

Beginning with version 8 some of them are supported.

* SMS/MMS
* Call-logs
* Wifi Access Points (user reported differently, but should work &rarr; after restore)
* Bluetooth (also reported working by some user)

Enable them in the preferences.

For all the others NB does not provide full support right now, try at your own discretion.

### How can I backup SMS & Call log?

**SMS/MMS and Call-logs** <br/>
NB starts supporting backup SMS/MMS and Call logs beginning with version 8. The current implementation (of [DL](https://github.com/dl200010)) writes all the details into a JSON format.

The Call-Logs are saved in data providers like some other special data (`com.android.providers.telephony`).

*You may need to restart your device after restoring special data, to see the restored data*.

NB will ask for the needed privileges for SMS and Call-Logs after a restart (of NB).

**contacts, calendar** and todo-lists <br/>
Users ask quite often about this.
We advise to use [DecSync](https://github.com/39aldo39/DecSync) with its diverse apps.
Alternatively use a CalDAV/CardDAV management app (like DavX5) and sync them with a trustworthy mail provider account (or your private Mailserver, a Nextcloud, etc. etc.).

For contacts it should also work to back up the data of "Contacts Storage" (package `com.android.providers.contacts`) system app.
Restoring it later on should restore contacts fine, but it's not guaranteed. Up to now it's not implemented as a [special backup](#what-are-special-backups) by NB.

### How to restore special backups for bluetooth and wifi?

A user writes: I always had trouble to restore Bluetooth. Now I found a reliable workflow.
1. Disable Bluetooth
2. Restore the Bluetooth backup
3. Enable Bluetooth again and wait a few seconds
4. Don't reboot before as the BT restore gets wiped (from some sort of cache) otherwise
5. Now reboot (as I use to do after bulk restore when switching ROMs)

WiFi restore does need the reboot after the restoring process

## Do I need a rooted phone?

Yes, Oui, Si, Si, Ja, Ja, Da, Ay...

### What is root access used for?

In short:
Accessing the APK+data of all apps (including system apps and special backups), so to access [all the necessary paths in the filesystem](#what-are-all-these-backup-parts-icons--which-parts-are-included-in-a-backup).

More Details?:

You need access to the data directories of the apps. Android creates user accounts for each installed app and assigns the permissions to the data directory just to the user and some system user(s). If the ROM doesn't bring a feature or interface to do backups like the deprecated adb backup or Google Cloud Backup does, it's impossible due to the security model.

Even more detailed?:

It's different...depending on the app and probably also for different ROMs.

E.g. for one of the devs ROMs:
`/data/app/*` all belong to `system:system`, while
`/data/data/*` usually belong to `appuser:appuser`, but some system data belong to system users e.g. system or radio,
(`appuser` is used as a wildcard name, usually something like `u0_595`)
`/system/app/*` belong to root.

Most probably "system" (which is not necessarily the user "system") starts an app as the appuser. It uses root access to become the appuser to start the app, which then runs as appuser.

Naturally system apps and services run as several system users.

On a A10 ROM a user (in the terminal) cannot access `/data/app` and `/data/data` itself. Reading an app's apk directory is possible (normal read access for others via r attribute) if you know the name, e.g. You can do

```bash
ls -l /data/app/<app_package_name>-VP8zj7n2sqzHID5Oqfh88w==
```

but there is no chance to find out the name because of the random code that changes on each installation (which also invalidates links to inside).

Some directories cannot be read, but only be opened (only x attribute)

You cannot access `/data/data/*` at all, so app data is protected between apps.

## Why is NB so slow?

NB is not slow anymore...
a lot of work was put into optimizations and parallel execution.

The startup including scanning for backups on internal or external storage (which is the most time consuming) should be done in 4-5 seconds.
Only on remote locations (over the network) it will be much longer...

<details><summary>Click to show some more details on why NB was slow ...</summary>

Since rebasing the app on SAF (Storage Access Framework) the performance is bound to what Android's (or Google's) framework can provide.
Needless to say: This is how much love this framework receives from the developers... [Fuck-Storage-Access-Framework](https://github.com/K1rakishou/Fuck-Storage-Access-Framework/)

The "slow" up to v7 came from the way the data was read from the system.
With using root via su commands you need to call multiple shell commands for every file to get their attributes for adding them to the tar file.
v8 uses the tar command, so this is solved.

Streaming the tar file to or from the SAF file system, never was a speed problem.

The other kind of "slow", that is still existent, comes from scanning the backup directory for the properties files and reading all these to build the package list.

This is suboptimal with SAF, but isn't much faster with RootFile, because a command is invoked for each directory instead of one command for the whole tree. At some point this will probably be changed to scanning in one go (also with SAF if this is possible).

Users tend to ask for using `RootFile` access instead. It is about toybox capabilities and the internal data structures.
Today SAF access is as fast as root access. Note: root file access could be faster, if it wouldn't use shell commands.
For now there are no plans to put the machinery into a separate root service, that could use direct file access.

If you want to try `RootFile`, you need to enable `shadowRootFile`.
This works by searching for parts of the shitty uri scheme (you see it in the setting for the backup directory) in certain places. If it is found, then it's used, otherwise it falls back to SAF.
Note, this can completely lock up the app, because certain file location can block completely (not sure why).
To change back the backup directory, you can clear data, or edit it in the shared_prefs folder or you could try to start the Preferences activity of NB.

</details>

### So why use SAF then?

[Access documents and other files from shared storage](https://developer.android.com/training/data-storage/shared/documents-files)
In the next Android versions Google will (most probably) force apps more and more to access the storage via SAF.

<ins>**Pro:**</ins>

* standardized way of accessing files on all storage providers
* more secure -> apps can only access their own data
* the ability of NB to backup to external SD card (or cloud providers) comes through SAF
* virtual file systems can provide some advantages, which seems to be the main purpose, more control
* ...

<ins>**Con:**</ins>

* Performance, more of Performance and tons of Performance (with multithreading the pricture is different, SAF is slightly faster than using root commands)
* obfuscation of the classical path structure
* unreliable file names (providers can rename the files as they like, meaning a file named xyz can be named 1234)
* insane strategy if the file already exists, instead of overwriting it, it creates a new file with a number, you need to delete the file, then wait until it's really deleted, then write the new one and hope the caches are updated
* you would think that accessing GDrive or other remote storage should be a natural advantage of SAF, but interestingly we don't know any app besides DocumentsUI that can access a GDrive directory via SAF

### Below some "performance" or time measuring infos from an older phone

Todays measurements are like 1-2 sec to scan ~500 backup instances.

<details><summary>Click here to show the test-details ...</summary>

#### General facts

* Device: Fairphone 2 (SoC: Qualcomm MSM8974AB-AB)
* SoC's CPU: Snapdragon 801 (quad-core) 2.26 GHz
* OS: A10 / LOS17.1
* 238 Apps - system + user apps
* no gApps
* root via Magisk
* NB (OABX) - version used for the last test listed here: v7.0.0 stable
* Backup folder on internal storage

As this is a quite old SOC, it can be called low end benchmark. 😉

Prefs ...
(I list only the differences from default)

Service-Prefs:

* Encryption configured
* Back up external data - enabled
* Back up obb files - enabled
* Back up media files - enabled
* Number of backup revisions = 1

Advanced-Prefs:

* Restore with all permissions - enabled
* Allow downgrading - enabled
* STOP/CONT - enabled <- default now as well

#### Test description

**Test 1**

Initial refresh (load the app list after the first Start - no backup exists at that point in time)

**Test 2**

Backing up all apps (two Apps excluded as too big + have their own in app bkp possibility) apk + all enabled data types via scheduled backup or batch.

Especially this test is a bit heat dependent with such an old device/SOC.
Phone heats up during such longer running stuff.

Also a second in this examples run takes longer, due to ensure the revision count (time is needed to delete the old backups).
If the default revision count (2) is used, of course, the first run, which takes longer, is the 3rd one. ;-)

**Test 3**

Refresh after starting NB with all those apps and their bkps created under Test 2

#### Measured times

(they generally vary a lot - depending on what the device is doing in parallel)

**Test 1:**

    11 Sek

    Former versions tested:
    5.0.0-beta1: 11 Sek
    4.0.1-alpha4: 11 Sek

**Test 2:**

    First run: 16m 45s; Second run: 21m 40s

    Former versions tested:
    5.0.0-beta1 (around 200 Apps that time): first run: 14m 40s; Second run: ---
    4.0.1-alpha4: first run: 17m 49s; Second run: ---

**Test 3:**

    20/21 Sek

    Former versions tested:
    5.0.0-beta1: 14 Sek
    4.0.1-alpha4: 19,5 Sek

Side-Comment:

Full scheduled/batch backup with last SAF free release (v3.2.0.) took about 10 minutes (for 195 apps at that time)

--> so much faster

The time difference (for most of the test-tasks shown here) is due to:

* no [SAF](#why-is-nb-so-slow)
* no external data support
* no obb files support
* other folder structure
* different encryption
* (no external sd-card support )

</details>

## I do not see any apps in the list. What can be the reason?

In most cases you choose a special Android folder for your backups. (e.g. common mistake is root '/storage/emulated/0' or the "Downloads" folder)
This is not supported by [SAF](#why-is-nb-so-slow). You find a full list which folders are not allowed [here](https://developer.android.com/training/data-storage/shared/documents-files/#document-tree-access-restrictions).

<ins>Solution:</ins>

Create a separate/dedicated sub-folder and choose it in NB preferences (User preferences) as your "Backup folder".

Another mistake, which might happen is, that you set a filters, which lead to an empty result.

## What should I do, when I get the "No SAF manager" message?

> "There's no file manager to manage SAF. Maybe you've disabled the built-in file manager. Please restore it or re-enable it."

You may wanna read about [SAF](#why-is-nb-so-slow) first.

This message and the details already tell you what's wrong. You have to have the default file manager app (normally called "Files", package name: `com.android.documentsui`) installed, which is an integral part of the system and is not supposed to be removed nor disabled. It's not just an app to read, copy, move files. SAF framework uses it as a provider e.g. for the file selection dialog (which is also needed by other apps, not only for NB). Afawk other file managers can't do that.

It is not yet known if other SAF supporting file manager can take over that job, e.g. if they are installed as system app, or if there's more to it (feel free to report if you find a way to replace the default file manager).

A lot of users (including some contributors to NB) use e.g. MiXplorer (also see this [issue](https://github.com/Magisk-Modules-Alt-Repo/MiXplorer/issues/11)) for file management too, as it is much more advanced than the stock one, but still the default one stays. It doesn't hurt anyway.

The "Files" app doesn't take any resources that would be freed by disabling it. So it's not worth to even think about it.

When weighing the risk of damaging something vs. the expected gain (~zero) it should be very clear.

Some allegories which may help to better understand it:

* It might help to think of if it as of the Windows Explorer. Did you ever uninstall it?
* "I never used the roof of my car, I don't need it and it's bloat... so I removed it. Now why is it wet in my car, when it rains? What a shitty car ..."

## I do not see the app which is currently backed up in the notification during batch or scheduled backups

*obsolete beginning with version 8* - see also [What does the notification of schedules and batch jobs tell me?](#what-does-the-notification-of-schedules-and-batch-jobs-tell-me)

To optimize the performance of scheduled and batch backups, these tasks are executed in parallel, based on the amount of cores, your SOC's CPU contains.
So notification always shows the last app backup, which was started on whatever free core of your SOC's CPU.

## At restore the data directory of the app does not exist

You got an error like "...failed root command...directory `/data/user/0/`*the.package.name* does not exist...".

You look into `/data/user/0/` with a root file manager and the directory *the.package.name* is missing
(and maybe there are only a few directories, but there should be one directory for each of the packages in the system, even for system apps).

&rarr; Check if you use namespace isolation for root commands in Magisk (other root solutions might have this, too).

It's recommended to use something like "global namespace for all root commands".
Eventually something like "inherit namespace..." could also work, never tested this.

Background:
If mounts of root applications are isolated, each cannot see the mounts of others.
In newer Android versions every app directory is mounted and usually only visible to its own user id.

## How does NB stop / pause / (un)suspend apps during backup?

#### backup

NB is using the pair

```bash
kill -STOP PID
...
kill -CONT PID
```

around a backup, if `backupPauseApps` is enabled in developer settings.

It also uses

```bash
pm suspend the.package.name
...
pm unsuspend the.package.name
```

if `backupSuspendApps` is set additionally.

There is a risk, that NB stops processes that it needs itself, this could result in a dead lock (NB waiting for a result of a process that is stopped).

To prevent this, NB excludes system processes and known package name patterns for "providers" and similar processes that provide services to other apps.

This can never be perfect, so some risk remains.

#### restore

NB uses

```bash
am stop-app the.package.name || am force-stop the.package-name
```

before a restore if `restoreKillApps` is enabled.

This means, it first tries to gracefully stop the app (which needs a recent Android version).
If that fails (or stop-app doesn't exist) the stop is forced.

### Do I need to pause apps?

Originally backup software is used when the system is properly shutdown and not running.
This is the best method, because everything is in a consistent state.

Some modern systems use snapshot file systems to freeze the state of all files.
Writes to the frozen file system are saved to a temporary storage and updated to the frozen file system when it is unfrozen.
This way files are consistent, but not necessarily complete.

NB can only run its backups, while the file system is still active.
You should be aware of inconsistent files.

When looking at the files, that are open, when a backup runs, there are many less critical files like logs etc.

Other files are written quickly and then closed. The chance to run the backup while the file is written is small.

Then there are databases.

Databases are usually handled in a way, that they always contain consistent data (by writing data first and then validating it via a log).

But I think, *copying* a database is different. E.g. the copy can become inconsistent when you start reading and there is something changed in the portion you just read. When you then read the log (often at the end) you are saving a validation state that doesn't correspond to the data you read, because it was changed afterwards.
That's because databases are usually written to randomly ("random access"), instead of only appending data at the end.

Pausing all processes that write to the open database files at least increases the chance to copy the database and its log in a consistent state.
However, it does not prevent files being incompletely written.

#### conclusion

With pausing, you increase the chance for a consistent backup.

In general you should run backups, when you don't use your system, because there is always a risk of incomplete files.

In practice, pausing seems to matter less than expected, **if** you don't use your system while the backup runs.

## Are you going to support older Android versions?

No, Non, No, No, Nein, Nej, Niet, La... in see-able future, maybe this would change in the far future...
Android 7 "Nougat" and older Android version support dropped in NB (OABX) v3.1.0.
Last v7 supporting release is [3.0.0](https://github.com/NeoApplications/Neo-Backup/releases/tag/3.0.0)
Still the minimum SDK in that Version might be an issue (to install it on a newer Android Version), so probably not really helpful.

See also - [Which Android Versions are supported?](#which-android-versions-are-supported)

## Can I use NB to switch to a new device / new OS / new Custom ROM / new major release of my ROM?

If you're trying to backup, flash, restore and go on with the new installation (more or less exactly) as you left your previous setup, then there is bad news for you:

* The system configuration is not only done with apps and app's data. [NB can only backup apps+related data](#what-is-neo-backup) and certain system files, via [special backups](#what-are-special-backups).
* for system apps and their configuration, also see the chapter: [Why is it not recommended to backup system apps?](#why-is-it-not-recommended-to-backup-system-apps)

So the best you can do, is backing up all your user apps + all data types and also try the special backups (see the related chapter what is currently supported) and start flashing.
Note: If you're using banking apps, TAN generators etc., the [app may use Android Keystore](#why-do-i-have-to-loginregister-to-app-x-y-z-again-after-restore) and won't launch once /data partition was deleted because they won't get their secret keys anymore. So be prepared, that you might have to link and login apps again.

More detailed articles about - how to properly backup your device - would be great, but it comes with lots of caveats due to variety of devices and versions. So it is a challenge to keep/word this as general as possible, as well as keeping it up to date (which is also true for this FAQ itself). If you have the knowledge and time to prepare such guidance for anyone, who's really interested in doing it right, we are happy about any participation.

You probably want to look into [app-restore-experiences.txt](app-restore-experiences.txt), where we started to loosly document, how apps react on the restore. You should get a picture what to expect and how imperfect a restore will be in general. The times to get a complete restore with one click are definitely gone.

## Why do I have to login/register to app x y z again after restore?

All apps which use the Android Keystore can basically not be backup up, as the Keystore is encrypted. Data restore might work but login have to be performed again (same for phone number registration for messengers)
Here are several examples - e.g.:

* Nextcloud
* Signal
* Threema
* Whatsapp (?)
* Facebook
* you name it

## Why is it not recommended to backup system apps?

* System apps can change over the android version. They are often tighter bound to the system they were designed for.
* Therefore restoring a system app, especially to a different system, might de-stabilize the system **or make it unbootable**.
* If your backup was done on 4.0.0, then you should place the data you want to restore at the same directory as when they got backed up.
  * --> In 5.0.0 this is already fixed.

## Can I backup and restore the settings of NB?

It's somewhere on the [todo list](#is-there-a-roadmap--an-overview-of-features-planned-to-be-implemented) ...

### preferences

There are experimental `savePreferences` and `loadPreferences` tools in `DevTools/tools` that save and load the preferences. NOTE: this does **not** include schedules and the global blocklist.

### schedules

Schedules can be exported to a folder in the backup directory

### blocklist

You can use "selections" (see another entry here) via the context menu.

Save:

* Get / blocklist (at the end of the menu)
* Put / "filename"
  (e.g. "blocklist-saved", don't confuse with the blocklist itself)

Restore:

* Get / "filename"
* Put / blocklist

Edit:

* save it first to a file like above
* Deselect All
* Put / blocklist
  (otherwise you don't see the current blocklist items)
* Get / "filename" (from save)
* change something...
* Put / "filename"
* restore from the file saved above

### alternative way - save some files

You're all root users, so capable of copying files, right?

All preferences excluding the password are stored in:

```path
    /data/user/0/com.machiav3lli.backup/shared_prefs/com.machiav3lli.backup_preferences.xml
```

This file contains the password (in case you use encryption) and it's encrypted by a key stored in Keystore, so no backup possible, you need to enter it again:

```path
    /data/user/0/com.machiav3lli.backup/shared_prefs/com.machiav3lli.backup.xml
```

These files include the database tables for the global blocklist and the schedules etc. (there were individual databases at some point in the past...):

```path
    /data/user/0/com.machiav3lli.backup/databases/main.db
    /data/user/0/com.machiav3lli.backup/databases/main.db-shm
    /data/user/0/com.machiav3lli.backup/databases/main.db-wal
```

These files are runtime data, the queue that is persistent through boot, you might want to delete them, if you want to stop NB executing remaining jobs (or you enable the preference to cancel them on startup):

```path
    /data/user/0/com.machiav3lli.backup/no_backup/androidx.work.workdb-shm
    /data/user/0/com.machiav3lli.backup/no_backup/androidx.work.workdb-wal
    /data/user/0/com.machiav3lli.backup/no_backup/androidx.work.workdb
```

You can look into the databases with an SQLite editor app, e.g. MixPlorer can do it or you may use the app "SQLite Editor" (com.speedsoftware.sqleditor) or any other sqlite app, also possible from a PC.

You should kill NB before doing such manipulations and start it afterwards.

### alternative way 2 - use another NB variant to backup

You can use a NB variant with a different package name (e.g. use neo to backup release) to backup/restore NB.

## Is there a roadmap / an overview of features planned to be implemented?

A rough backlog (without any guarantee that the devs will 100% stick to it) can be found in the [Kanban board](https://tree.taiga.io/project/machiav3lli-neo-backup/kanban)

Not all features will be listed there, but maybe the bigger / frequently asked ones.

## Where do I find the so called "DevTools"?

in versions >= 8.3.2 you can long press the page title.
in older versions you first need to enable the long press by enabling showInfoLogBar in preferences/advanced/developer settings.
Note: DevTools contain a bunch of possibilities without explaination and without translations.
You should know what it means, before using anything in there.
Many things are experiments for development purposes, e.g. for work in progress and new features, or to compare alternative implementations.
There are also options to fake backups and schedules or crash the app for test purposes.
Read the Telegram group.

## I read about selection and context menu in the Telegram group, where do I find this?

* long press a list entry in Homepage, this will select the first item.
* Selection mode is on, if any item is selected.
* In selection mode you can select further items by single click.
* The selection mode finishes when nothing is selected anymore.
* You will see a menu button in the lower right with a selection count.
* You can also enable this button all the time (= even with a count of zero) in developer settings.
* The menu can be reached by pressing that menu button or by long pressing a selected item.
* Menu items with "..." at the end will ask something (yes/no or asking for further info) before executing an action.
* Without "..." they will immediately execute the action.
* De/select "Visible" means the elements shown in the filtered list
* A Selection can also contain invisible items, even items that do not exist at all (e.g. if you uninstall packages they can still be in the selection)
* That's the reason why there is a "Deselect All"
* You can "add" filtered items to the selection by "Select Visible"
* You can subtract filtered items by "Deselect Visible"
* The (current) selection can be "Put" into a file or into a schedule etc.
* "Get" replaces the current selection by the source
* Selections are stored in `!-SELECTIONS` folder in the backup directory
* These files are simple text files with a package name per line, so you can use tools like editors, grep, meld, windiff, etc. or even scripts to process or build them

## Can I control NB from scripts or Tasker or similar?

yes, NB has a broadcast receiver for Android "intents", that reacts on commands (note, it's still experimental, so subject to change, also consider security, it's not protected).

The Intent must contain the package name, that is:

| apk | package name |
| :- | :- |
| release | `com.machiav3lli.backup` |
| neo | `com.machiav3lli.backup.neo` |
| pumpkin | `com.machiav3lli.backup.hg42` |
| debug| `com.machiav3lli.backup.debug` (e.g. if you compile yourself) |

The broadcast receiver is:

```url
    com.machiav3lli.backup.manager.services.CommandReceiver
```

note, this is a fixed name, `com.machiav3lli.backup` is a namespace not a package name.

Additional data is given in so called `extras` of the intent.

Intents can also be sent by a su shell command like this:

```bash
am -a COMMAND -e EXTRANAME1 EXTRADATA1 -e EXTRANAME2 EXTRADATA2 ... -n PACKAGE/BROADCASTRECEIVER
```

With tasker et.al. the parameters of the intent must be entered in the corresponding fields.

COMMAND can be one of these:

**schedule**

trigger a schedule (like pressing the run button)

* extras
  * name = the name of the schedule
* example: start a specific schedule

    ```bash
    am broadcast -a schedule -e name "the name of the schedule" -n com.machiav3lli.backup/com.machiav3lli.backup.manager.services.CommandReceiver
    ```

**cancel**

cancel a schedule or all schedules

* extras:
  * name = the name of the schedule (without, it cancels **all** schedules)
* example: for canceling all schedules

  ```bash
  am broadcast -a cancel -n com.machiav3lli.backup/com.machiav3lli.backup.services.CommandReceiver
  ```

* example: canceling a specific schedule:

  ```bash
  am broadcast -a cancel -e name "the name of the schedule" -n com.machiav3lli.backup/com.machiav3lli.backup.services.CommandReceiver
  ```

**reschedule**

set a new time of a schedule

* extras:
  * name = the name of the schedule
  * time = new time in HH:MM format, e.g. 12:34
* example: set a schedule to the time 12:34

  ```bash
  am broadcast -a reschedule -e name "the name of the schedule" -e time 12:34 -n com.machiav3lli.backup/com.machiav3lli.backup.services.CommandReceiver
  ```

### now I want to detect the end of the operation

For a single app you can check the properties file, because this is only written, when the backup is finished and successful.

Though, this doesn't help much for a batch backup of several apps, unless you know which are included.

Also note, that the name of the properties file to look for includes the time, so it changes on each backup.

You can use the logcat command to tail the messages written by NB and check either for the end of a batch or the end of all batches.
You need to enable logToSystemLogcat perference, otherwise the messages are only stored internal.

## What is the difference to implementations like Seedvault?

The main difference is that NB uses root to create a copy of the apps [APK and it's data](#what-are-all-these-backup-parts-icons--which-parts-does-a-backup-of-an-app-consist-of) while Seedvault relies on Google's api to backup (without forcing the user to backup to the Google-Cloud).
Seedvault repo can be found [here](https://github.com/seedvault-app/seedvault) - check it for latest information as the infos/comparison in the table below might be outdated.

|  | NB  | Seedvault  |
| ---: | :--- | :--- |
| root necessary? | [yes](#do-i-need-a-rooted-phone) | no |
| encrypted backup | yes (optional) | yes |
| automatic backup | yes (individual schedules) | yes (daily) |
| usable For | [A8-A13](#which-android-versions-are-supported) | A13; but branches for from A9 to A12; also integrated into known Custom ROMs CalyxOS, GrapheneOS, [LOS](https://www.xda-developers.com/lineageos-seedvault-open-source-backup-solution/) |
| Considers 'allowbackup' flag of apps? | no | yes |
| OS-/Rom integrated | no (dedicated app) | yes |
| Choose backup location possibility? | yes | yes |

## What is the difference to the famous Titanium Backup?

Users tend to compare NB and Titanium Backup (TB). There are a lot of comments in the chat.

Feel free to share your thoughts and edit this FAQ (e.g. the table below) to provide more details for this comparison.

The main points to are mention:

* TB did not get an update for some years now (since [November 2019](https://play.google.com/store/apps/details?id=com.keramidas.TitaniumBackup))
* TB does not use [SAF](#why-is-nb-so-slow) and so is probably faster as long as google does not improve it)
* TB is not Open-Source.

All the TB features are listed [here](https://www.titaniumtrack.com/titanium-backup.html)

Here is a quick status overview, what NB is capable of - to be edited.
<details><summary>Click here to show the comparison table ...</summary>

| TB feature  | NB status |
| :--- | :--- |
| Very fast app listing (~1 second for 300 apps) | this isn't because of SAF, it's probably because NB uses official interfaces of Android. I guess, TB simply scans the directories in /data/data/ or a similar technique. <br/>Reasoning: it sees com.google.android.trichromelibrary* which most backup solutions don't see. |
| Sort apps by name / last backup / backup frequency | YES / YES / NO (don't remember what frequency means) |
| Filter apps by name / type / status / Apps Organizer labels (also affects Batch operations) | YES / YES(?) / YES(?) / NO (not yet) |
| Backup/restore regular apps + their settings | YES |
| Backup/restore protected apps + their settings | PROBABLY (what is it exactly? - maybe device protected data? -> YES) |
| Backup/restore system apps + their settings (incl. Wi-Fi AP list) | YES (some system data, called special backups, "package" names special.*, label starts with $). (Restoring system **apps** is not recommended, can de-stabilise system or make it unbootable) |
| Backup/restore (migrate) some system data (eg: SMS/MMS) across incompatible ROMs | YES (only SMS/MMS/call logs) |
| Backup/restore external app data | YES (external_data, obb, media) |
| Restores the Market links when restoring apps | NO |
| Zero-click background batch backup | YES (batch backup, but what is zero click?) |
| Interactive batch restore | NO |
| Many batch scenarios (eg: if more than N days since last backup, etc) | NO (only backup and restore) |
| Zero-click app un-installer | YES (but again, what is zero click?) |
| Zero-click system app un-installer | YES (it's there, not sure if it works, never tried ... but again, what is zero click?) |
| Move app to/from SD card | NO |
| Move app data to/from SD card (needs ext2/3/4 partition) | NO |
| Desktop widgets | YES (but I guess it only works for stock (like) launchers) |
| A single weekly or biweekly scheduled backup | YES (any schedules) |
| User-defined apps lists with filtering, coloring and scheduling support | TB has better filtering support...e.g. tags |
| Built-in Android Market information viewer (Android 2.0+) | ??? |
| Ability to remove orphan app data | NO (at least not as a batch, can TB do this? don't remember) |
| Multiple backups per app (history length can be chosen) | YES (TB can also throw backups for a filtered list) |
| Zero-click background batch restore | YES (but again, what is zero click?) |
| Encryption of your backups (asymmetric crypto: the passphrase is needed on restore only) | YES |
| Backup/restore SMS, MMS, call log, bookmarks and Wi-Fi networks in the portable XML format | NB > v7 ... YES, YES, YES, NO (for which browser?), NO |
| Multi-user support for some apps (eg: games) with a widget for quick switching! | NO |
| Batch verification of your backups | NO |
| Create an “update.zip” file containing apps+data, which can be flashed in recovery mode to restore everything in one shot | NO |
| Convert users apps to system apps | NO |
| Ultra fast HyperShell (much faster for almost everything) | NO (not sure how fast this is, NB currently uses toybox, which might also be fast) |
| App freezer can disable an app (and make it invisible) without un-installing it | YES |
| Batch app freezing/defrosting | NO (TB can do many operations in batch mode with all it's power of filtering. NB can't yet) |
| Full support for paid apps that must normally be installed through the Market! | not sure, what is necessary for that? |
| Market “auto updating” manager, to easily verify, enable or disable auto updates on several apps at once! | don't remember what this is |
| Unlimited, independent scheduled backups (each of which can run 1 to 7 times a week) | YES (TB is more flexible with it's filtering) |
| Market Doctor can retrieve or re-create any broken Market links (for your updates to appear in Market again) | NO |
| Dalvik cache cleaner can free up precious internal memory | NO |
| Integrate updated system apps directly into your ROM to free up even more internal memory | NO |
| Synchronize all (or some of) your backups to Dropbox, Box and Google Drive | NO (only via external methods like rclone and SAF providers) |
| Retrieve all your backups from Dropbox, Box and Google Drive (in case of lost phone or SD card failure) | NO (only via external methods like rclone and SAF providers) |
| TB Web Server: download/upload all your backups as a single ZIP file on your computer through a Web interface | NO |
| Load/Save a Filter (from the Filters screen) and use it in Widgets/Schedules | NO |
| Change the device’s Android ID, restore it from a backup or after a factory reset | NO |
| Protect backup against deletion | NO |
| Send backup (by e-mail / Google Drive / Dropbox) which can be imported/restored very easily | NO (useful) |
| CSV data export from any app’s DB, to e-mail or Google Docs | NO (very useful) |
| Convert app data to/from Gingerbread’s faster WAL DB format | NO (as far as I know, WAL was also used as a portable format) |
| Brand the app with your name | NO |

</details>

## Do I need encryption?

Well, it depends...

Over all it's about risking that others might access your data visa risking your backups by a single point of failure (the password and/or the properties file).

Here are some things to consider:

* Non-encrypted backups are simple tar archives that can be used with any archiver (e.g. to extract single files).
* If encryption is necessary, you could also use encrypted filesystems or similar.

* Internal storage is encrypted.
* Apps are isolated, only NB is granted the right to write to the backup folder.
* Root apps can access everything, but they can also access your data directly, no need to access the backup for this.
* Even with a stolen device it's the same for the real data and the backup. Either internal storage is readable or not.

* External storage is not encrypted.

* Remote storage is often not encrypted.

* You can loose/forget your password (this really happens among users)
* There is a certain risk to loose or damage properties file(s) (not sure why, but it also happens among our users). Then you are doomed. The properties file contains a key (called "iv") that is necessary to decrypt this particular backup. I think it is there to protect against statistical attacks (not sure if that works when the iv vector is readable, at least it's more difficult).

ask yourself:

* Which attack do I want to prevent?
* How much worth has the secret? (e.g. life threat, money, privacy)
* Then look at the technical properties of the protection.
* How much risk does it have to loose the data?
* How much does losing the data cost (not only money)?

Properties of the protection:

* E.g. drive encryption is usually accessible as long as you are logged in (or the drive is unlocked).

* In contrast, with single file encryption you could unlock only the data that you need right now.

* All encryption does not help, if the password (or whatever is the key) can be stolen.

Often it's quite different as you think.

As an example, it seems to be more secure to ask for a password on each access instead of storing it.

However, if an intruder can watch you typing it (lots of possibilities, e.g. phishing, or looking at your screen, or some spy code inserted into a library), he can easily get your password. While a securely stored password  (e.g. hardware based in keystore) can only be stolen by inserted spy code when it is decrypted.

## How can I open encrypted backups on my computer?

You can find the encryption algorithm and setup in this class: [Neo-Backup - Crypto.kt · GitHub](https://github.com/NeoApplications/Neo-Backup/blob/main/app/src/main/java/com/machiav3lli/backup/utils/CryptoUtils.kt) . The rest depends on the version you used.

One of the contributors (Nils: [Pizze](https://github.com/Tiefkuehlpizze)) took the last Java version of this Crypto class and built a wrapper around it:

[OABXDecrypt](https://github.com/Tiefkuehlpizze/OABXDecrypt)

So for those who really want to decrypt the backups on their PCs, this might be a good start and a helpful tool.

UPDATE:

There is now an experimental script that scans a whole directory and creates decrypted and renamed copies of the included backups:

see [decrypt-all](https://github.com/NeoApplications/Neo-Backup/tree/main/etc/decrypt)

It is derived from Nils' experimental `decrypt.py` found in his OABXDecrypt github repo.
Don't forget to read the `README.md` before doing anything with it and create a backup of the backups.

Note: decryption needs the `iv` vector from the properties file (a random value), so it is impossible to decrypt anything without the corresponding properties file (e.g. you can create a fake properties file, unless you encrypted the backup).

## What does the notification of schedules and batch jobs tell me?

(*this decribes the notification beginning with version 8*)

**While it is running...**

* the notification will show in its name / heading:
  * The NB-Logo followed by "Neo-Backup"
  * :runner: - app-backups currently running
  * :people_holding_hands: - app-backups queued/outstanding/not yet started
* below the notification headline you will see
  * the name (in case it is a schedule) and the start-time
* when you expand the notification you will see more details of the parallel tasks which will run (parallelism is based on the amount of cores of the devices SoCs CPU)
  * the first 3 characters show what NB is currently working on (**pre**paration, **apk**, **ext**ernal data, **obb**, **med**ia, etc.)
  * after the dot you will see the package name

**When it's finished...**

* heading:
  * green NB-Icon in case there were no error
  * red NB-Icon in case some backups failed
  * count of processed apps and overall apps in the job
* below it will show
  * the name again (in case it is a schedule)
  * start-time
  * "OK" (in case no errors)
* below that the overall time it took

**Details regarding start-time:**

Time after the name tells when the schedule was really started (or more precise, when the jobs were queued). It does not necessarily tell when the Android WorkManager starts the queued jobs. In "most" cases it start immediately.

The current defaults are "non-exact timing" and we think it's exact enough (with priority battery life), but feel free to report your timing experiences in the channel.

If you want it more exact, you can enable useExactAlarm or may be even more exact useAlarmClock (not sure if this makes a difference) in the developer settings (under Advanced preferences).

If anyone experiences jobs that are not finishing, please report.

However up to now, they were broken in some other way, e.g. tar hanging on dot-dot-names or exceptions. But if you think the old behavior could have been better, you can enable useForeground, which then has both methods to keep the service running, the foreground notification and a wake lock (the new way to do it, copied from the official AOSP clock app, which we think is a good reference).

## Does NB support multi-user setups / work-profile?

Disabled / not support for now as it led to strange behavior that apk and/or data was overwritten in both profiles.
For now NB will only handle the main profile / user and ignore the others.

So this also answers if it works together with Apps which utilize the work profile (like Shelter or Island).
You can try to clone NB into it, but it is not recommended.

Root should be enable in work-profile by setting - Superuser "Multi-User Mode" to "Device Owner Managed" or "User-Independent" in Magisk App settings.

## Does NB support remote backup locations?

NB does not directly support this.
There are already tools for these tasks.

Unlike commercial apps philosophy (lists of features, even if they are not very well supported),
open source should follow the unix philosophy (write programs that do one thing and do it well).
Just use the appropriate tool for a different task, instead of putting all of it into one tool.

You can sync the backup directory to the remote directory using tools like syncthing, which is recommended.
If you have space for a complete backup, this is the preferred way (syncthing is known to work well).

However, in cases when you don't have enough local storage, it is possible to

* use the Storage Access Framework (SAF) to access remote locations or
* use a remote directory mounted to the local file system.

This means, writing to the local directory actually writes to the remote directory.

Note, that Neo Backup (as derived from it's predecessors) was developed with local storage in mind. So it is not optimized for remote storage. It will need a lot of work on it's mechanisms to change that.
It's kind of open when or even if this will happen.

When using a mounted directory, you should at least enable the `flatStructure` preference in developer settings / alternates, because Neo Backup does housekeeping after each application backup. Scanning the backup directory reads all directories that may contain `*.properties` files. Reading a directory is usually the most expensive remote operation.

The `flatStructure` option ideally reduces this to only one directory scan. Note, that `flatStructure` backups can be mixed with others and the speed advantage doesn't fully show up, if there are still non-flat backups. You may want to start with a clean directory.

For some more info about `flatStructure` see `NOTES.md`.

#### **SAF** remote access

works by installing so called document providers.
Unfortunately these apps are rare, we only know these two pure providers:

[SSH](https://play.google.com/store/apps/details?id=ru.nsu.bobrofon.easysshfs)

[CIFS (Windows network)](https://play.google.com/store/apps/details?id=com.wa2c.android.cifsdocumentsprovider)

In theory Google Drive should also be possible, but practically it does not work. It is not offered as a remote location.

**Attention:** the CIFS app seems to have a critical bug (our user Michael tested it with 2.1.1 and 2.2.0 doesn't mention a solution)

the bug is described in this fork:

[SMB Documents Provider](https://github.com/fuenor/smb-documents-provider)

the workaround is an older version, which you probably should use until the bug is solved:

[SMB Documents Provider 1.6.0](https://github.com/fuenor/smb-documents-provider/releases/tag/Ver.1.6.0)

#### **Mounting** remote file systems to local folders

works with

* unix tools (command line), e.g. via Termux
* `rclone` (command line), an awesome command line program that can also mount several remote storage types.

[Round-Sync](https://github.com/newhinton/Round-Sync) (former RCX, extRact) is kind of hybrid.
It's an app that uses rclone to mount remote storage and can also make it accessible via SAF.

Neo backup should use the SAF option.
To enable SAF in RoundSync, you need to configure some options:

```text
    File Access

    [ ] Enable SAF Client Preview
        EXPERIMENTAL: Access SD cards and other storage devices

    [x] Refresh local drives
        Automatically refresh local storage media when starting

    [x] Enable Content Provider Preview
        EXPERIMENTAL: Allows you to grant other apps access to rclone remotes

    [x] Declare as local provider
        Some apps (e.g. Google docs) otherwise can't access files

    [ ] Allow any app to access your remotes
        WARNING: This will allow any app to read and write any path on any configured remote.
```

"Enable Content Provider Preview" is the necessary part to make the RoundSync SAF provider available in the sidebar of the directory selection.

"Declare as local provider" might also be necessary, at least it doesn't hurt.

Then you connect by choosing a remote backup directory via the provider entry in the DocumentsUI directory selector.

<details><summary>Click here to show more details (collection of some old messages) ...</summary>

from Harald = hg42 on github = hg42x on Telegram

this is a loose collection of messages I wrote on Telegram about the topic
(partly edited and rearranged):

> rsync vs rclone

rsync uses another rsync on the remote side, then the two rsyncs move the data.
So the remote rsync must be listening on a port or the local rsync must be able to start the remote via ssh.

Instead rclone can use the protocols of a lot of remote "file systems", like Google Drive, sftp, whatever to connect to them.
It can "mount" these file systems or "sync" them or list files or stream data to/from a file etc.

RCX / extRact is a frontend to rclone.
With rclone you have control over when you mount and unmount the remote directory.
The UIs do this on their own, I am not sure if they keep the mount correctly while it is used...
At least with RCX I had a feeling, that it uses a timeout

> Is it somehow possible to back up to a remote storage? For example directly back up to my PC via cable or via FTP or similar

usually the basic intent of this question is, to avoid storing data on the device first (e.g. no space left?) and then sync.
So you want to "mount" a remote directory to the device and use this.

my last test was with [extRact](https://github.com/newhinton/extRact) (a successor of RCX, which also works but seems to be unmaintained for some time)

I didn't use it lately, so I don't know about the current state.

You need to change some settings to make it work with SAF.

...

"Enable Content Provider Preview" is the necessary part to allow to select the extRact entry in the sidebar of the directory selection.
"Declare as local provider" might also be necessary, at least it doesn't hurt.

Then you connect by choosing a remote backup directory via the extRact entry in the DocumentsUI directory selector.

It works, but it's still suboptimal. Especially getting the list of backups takes a looong time.
And it probably reads it more often than necessary (e.g. while running backups on each deltetion of old directories).
For remote usage some of this could be delayed or combined. This will need some restructuring.

Though it has become much better lately. E.g. the time to read the list is now 1/4 or better.

There might also be limits in the number of packages in one batch etc.
It's possible, that such things are caused by timeouts in Round-Sync/extRact/RCX.

You might try rclone directly (maybe starting it manually or via Tasker or similar).
You can also trigger NB by a shell command using intents (see at the end of this text)

NB uses SAF, this allows to use SAF providers.
An rclone based solution is [Round-Sync](https://github.com/newhinton/Round-Sync)
(the newest incarnation of the former RCX and it's later successor extRact).
They use rclone to integrate a bunch of remote storages into SAF.

I also know these two SAF providers:

[CIFS (Windows network)](https://play.google.com/store/apps/details?id=com.wa2c.android.cifsdocumentsprovider)

[SSH](https://play.google.com/store/apps/details?id=ru.nsu.bobrofon.easysshfs)

Unfortunately the naming of such storage providers isn't very defined, "Document(s) Provider" or "Storage Access Provider", "File System" instead of "Provider" etc. So you never know if you found all.

Google Drive should be a provider, too, but it seems the set of features does not match and NB doesn't see it.
There are only a few apps that show Google Drive in the sidebar of the file selector.

Those providers might have a different behavior... I never tried them more than a small test.

note this is experimental, I don't use remote access regularly, so I don't know if it works flawlessly.
I never restored from it, so I don't know, if the backups are 100% ok or if restore even works.

In my last test backup stopped after 50 min, with about 300 of the 400 apps backed up.
Not sure what happened. At least it seems that less apps could eventually work...
At least the tar files looked ok from looking at some samples.

</details>

## I need label/tags/similar on some backups to identify them

that's not implemented (yet?).

As a workaround you can rename the backup or easier put both in a sub-folder with a useful name. The subfolder could also be reused later for other backups of that "label".

* you always need to handle folder and properties file equally, so that they live at the same directory level and have the same name like this:

  ```path
  path/backupfolder/
  path/backupfolder.properties
  ```

* any subdirectories and file name additions below the basic backup directory (that one chosen in the preferences) will be shown after the date of the backup
* in this name
  * the package name will be replaced by the package symbol
  * the date and time will be removed
* the date and time used in the entry is that from inside the properties file (which should always be equal to that in the file name)

e.g.

```path
<backupdir>/xyz/abc-the.package.name-def/<date-and-time>-some-file-name-addition.properties
```

will be shown like

```path
xyz/abc-📦-def/some-file-name-addition
```

## How do I filter on changed data?

you can't...

We know, Titanium Backup had this feature. We also discussed it.

Some Android versions ago, we checked how much data changes every day.
We found that almost all apps have changed data.

* Those that don't, have a very small data size (apps like bubble level, calculators etc.), so they do not matter much.
* Those with bigger data usually update it frequently (especially messaging apps).
* The number of files is also much bigger today. Scanning all those data files does not make much sense.

It is easier to have categories (= different schedules) of important and less important apps/data with corresponding backup intervals.

## How do I create a support log?

use `preferences / Tools / Terminal / SUPPORT` (...wait...) then share the file (or find it in `<backupdir>/!-LOGS`)

## Troubleshooting

* DevTools: for trouble shooting:
  * long press on Title will show the DevTools popup for faster access to developer settings, log,
    terminal and other tools
* option to autosave a log if an unexpected exception happens, even if it is catched later, so we
  get better info from the inner circles
  * → `logging/autoLogExceptions`
* option to autosave a support log after each schedule, because it's difficult to do this manually
  * → `logging/autoLogAfterSchedule`
* option to autosave a support log on suspicious events, e.g. duplicate schedules
  * situations that are not necessarily an error, only "interesting" sometimes to have a look at
  * → `logging/autoLogSuspicious`
* `SUPPORT` button in DevTools or Terminal
  * `prepareSupport` enables the usual preferences to trace what happens
  * `shareSupport` button saves the text in the terminal and opens share menu
    * if you want to review it, cancel the menu and goto the "View the log" tool instead, you can
      still share from there
  * `afterSupport` reset them to defaults (less intensive tracing)
* you may enable `logToSystemLogcat`, if the internal log isn't long enough and you need to use an app to capture a longer log (or catch it in case of crashes etc.)

Also see TROUBLESHOOTING.md document...
