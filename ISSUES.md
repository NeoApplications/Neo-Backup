## Apps:

* Apps which use android's keystore to identify users e.g. Signal, Element.

* Some Termux plugins which have cyclic symlinks

* Some system apps and all system-less apps (Magisk Modules) aren't meant to be backed up/restored so just be careful not breaking the system while playing with those

* MIUI optimizations (and some other OSes) don't allow the restore in most scenarios, it should be turned off.

* Restore of apps that use Magisk's systemless installation could cause the system crashing fully.

## OABX specific:

* Backups from 4.0.0 can only be restored when they get placed at the same address/directory when they got backed up (this is fixed in 5.0.0)

* Asking for storage permissions(ask for backup directory) on A10/A11 on each launch or restart (only on 4.0.0)
