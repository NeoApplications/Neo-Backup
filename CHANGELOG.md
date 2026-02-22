CHANGELOG
=========

8.3.17 (22.02.2026) +40 Commits,  +20 Translations
------------

### Function

- Add: Setting to enable/disable storage check on backup action
- Add: Fallback options for suGetOwnerGroupContext
- Add: AppState flow to AppVM
- Add: ExtrasTags DB view
- Fix: Restore on wifi backups on newer Android versions
- Fix: Backup equality checks
- Fix: Duplicate backups (possible racing condition)
- Update: Rebase RootFile on SuFile
- Update: Replace buttons boolean positive with ColoringState enum
- Update: Improve reliability of running async functions in receivers

### UI & UX

- Add: Launcher shortcuts support for running schedules
- Add: KeepAndroidOpen banner
- Add: Backup sharing button
- Add: Existing tags suggestion in the adding dialog
- Fix: Restore Andy in placeholder icons
- Fix: Block list indicator on button in schedule page
- Update: Revamp app page, backup items and refresh button
- Update: Make Batch page's bottom bar floating
- Update: Add action button to permission items
- Update: Revamp search bar and the top bars

8.3.16 (03.01.2026) 10 Commits,  14 Translations
------------

### Function

- Add: Scene overlays and respective strategy
- Fix: Running ScheduleReceiver on reboot (credits @Jenna-66)
- Update: Replace BottomSheetScaffold with Nav3 bottom sheet navigation

8.3.15 (28.12.2025) +70 Commits, +20 Translations
------------

### Function

- Add: SchedulerState and ScheduleState to respective VM and page
- Add: Check if available storage enough for backup
- Add: Checkroot alt-variants (for user-testing)
- Add: Worker to refresh backups
- Fix: Reduce checkroot false-negatives
- Fix: Removing lock page from stack when unlocked
- Update: Run ScheduleWork directly from ScheduleReceiver bridging ScheduleService out
- Update: Migrate navigation management to Nav3
- Update: Split MainVM workload into 4 VMs
- Update: Make Blocklist entity more stable
- Update: Improve blocklist repository flows
- Update: Make VM-states compose-compiler-stable
- Update: Revamp CoroutinesAsyncTask and relevant classes
- Update: Simplify package icon composable
- Update: Move setting to exact schedules to advanced settings
- Update: Revamp PackageRepository abstracting BackupsCache and moving collection to VMs
- Update: Collect flow instead of forwarding viemodel in tools
- Update: Move secondary functions out of BackendController to BackendUtils or PackageInfo extension
- Update: Rebase Home context menu to avoid injecting view models
- Update: Move notification channel creation into WorkHandler
- Update: Reduce recompositions of recyclers where possible
- Remove: ScheduleService

### UI & UX

- Add: Random name for new schedules
- Update: Revamp root missing page
- Update: Simplify multiple UI components
- Update: Revamp expandable block layout
- Update: Revamp button and chip layouts
- Update: Fix back-navigation icon on secondary pages
- Update: Revamp sort filter sheet top bar
- Update: Info page links to point to codeberg where needed
- Update: Separate enabled and disabled schedule lists
- Remove: Animated background option

8.3.14 (17.10.2025) +5 Commits, +10 Translations
------------

### Function

- Fix: Running ScheduleReceiver on reboot
- Update: Make HelpSheet a page
- TargetSDK 36

### UI & UX

- Add: Tags' filter
- Fix: Moving dialogs on launch
- Update: Add top bar actions to sub-pages

8.3.13 (05.10.2025) +40 Commits, +40 Translations
------------

### Function

- Fix: ScheduleService not being started after reboot (credits @jenna-66)
- Fix: Getting fitting shell command on Android 9 - or earlier.
- Update: Make respective DAO and repository functions suspend
- Update: Don't kill the app on pressing back in home page
- Update: Revamp schedule packages' filtering
- CompileSDK 36

### UI & UX

- Add: Details on custom list, block list and packages-to-backup when manually starting a schedule
- Add: Warning dialog on adding app to global block list or deleting schedule
- Fix: Predictive back gesture for preferences page
- Fix: Transition animation of opening/closing app/schedule pages
- Update: Default to user apps as main sorting filter and for schedules
- Update: Reduce slide pages pre-composition to default (0)
- Update: Revamp welcome page, app page, schedule page and help sheet layouts
- Update: Replace the top-bottom-blocks layout with one-block
- Update: Revamp chips, tags and buttons layouts

8.3.12 (22.03.2025) +120 Commits, +40 Translations
------------

### Function

- Add: Indices to room entities
- Add: Strict mode (logging) for unsafe intents
- Add: Su-command(s) for APatch (credits @hg42)
- Add: Update package map after finishing a batch/schedule
- Add: Tool to clean up backup directory of damaged backups
- Add: Tool to enforce the backups limit on all apps
- Add: Pref to use PGP key for encryption
- Fix: Missing/overdoing delete of backups
- Fix: Scheduling (Return to ScheduleService scheduling)
- Fix: Launching on refused magisk/root permission
- Fix: Check working su-command if pref_suCommand is empty (credits @hg42)
- Fix: Recomposing navigation suite on changing pager state
- Fix: Loose checkroot (accepted finding one entry in /data/user) (credits @hg42)
- Fix: BackupDir not working at start (credits @hg42)
- Fix: ScheduleWork not running to end
- Fix: Deleting all backups
- Fix: Not showing backups of uninstalled packages
- Fix: Updating package cache even when app page is closed
- Fix: File name value on renaming
- Fix: Schedules not respecting backup limits when in background
- Fix: Avoid removing sort/filter-related prefs on update
- Update: Improve robustness of enabling/disabling specials logic
- Update: Improve NeoPref logic
- Update: Replace backups' map with a repository instance
- Remove: CDATA usage in strings
- TargetSDK 35

### UI & UX

- Add: Search bar to block and custom list dialogs
- Add: About card to help sheet and welcome page (revamp layout)
- Add: Contrast themes
- Add: Encryption prefs page
- Fix: Dialogs on targetSDK 35 don't respect system insets
- Fix: Reactivity of selectable rows in pref dialogs
- Fix: Behaviour of navigating back on pages with details
- Fix: Reactivity on enforcing backups limit in AppPage
- Update: Restore main NB icon colors
- Update: Make last backup choices default for backup dialog
- Update: Improve Dialog box's padding
- Update: Main themes colors
- Update: Revamp coloring and style of some components
- Update: Only use colored pref icons for more informative purposes (credits @hg42)
- Update: Animate updated package items
- Update: Navigation icon and text follow standards

8.3.11 (10.01.2025) +15 Commits, 6 Translations
------------

#### Function

- Add: Scope logging to all ViewModels
- Fix: Crash of using repository database calls on UI thread
- Fix: Koin calls and scoping
- Update: Improve coroutines handling in repositories
- Update: Improve StateFlow usages
- Update: Reduce context usage and calls

#### UI & UX

- Update: Make alternative block design default
- Update: Revamp block borders' design
- Update: Make preference's surface and switch interactively connected
- Update: Improve preferences coloring

8.3.10 (12.12.2024) +15 Commits
------------

#### Function

- Add: Option to group invalid backups (pref_createInvalidBackups)
- Fix: Repeated unscheduled run of scheduled schedules
- Fix: Pulling timeleft for disabled schedules
- Update: Replace WorkHandler & WorkManager calls with inject
- Update: Make ViewModels singleton

#### UI & UX

- Fix: Blocking launching apps after force dismiss
- Fix: Reactivity of schedule timer
- Fix: Reactivity on deleting backups

8.3.9 (10.12.2024) +230 Commits & +80 Translations
------------

#### Function

- Add: Logic repositories in view models
- Add: Scheduling worker (revamp scheduling logic)
- Fix: Neo Backup not being on default SMS list (credit @dl200010)
- Fix: InvalidBackup directory & packageNameFixed
- Fix: Root access not working and fallback to SAF
- Fix: Specials restore
- Fix: Synchronizing backups
- Fix: Empty properties files
- Fix: ShadowRoot sub-folder not working
- Fix: Possible deletion of built-in plugin
- Update: Revamp & single-inject all view models
- Update: Revamp permissions handling
- Update: Revamp SortFilterModel handling
- Update: (DevTools) allow search operators (and='+' or=',')
- Update: (DevTools) ignore single char search pattern
- Update: Simplify DAOs
- Update: Replace unsafe string context menu action with enum
- Update: Set pauseApps default to false (true causing increasingly problems)
- Remove: Proxy usages of viewmodels, powermanager over OABX
- Fix: Sorting locales for reproducible build (@obfusk)
- CompileSdk 35

#### UI & UX

- Add: Different sorting/filtering for different pages
- Add: Adaptive layout in Main & PrefsPage
- Add: ListDetail layout for Home & Scheduler pages
- Add: App & data size to sort/filter sheet
- Add: Version code to BackupItem
- Add: Data partial selection to context menu
- Add: Share action to plugin editor
- Add: Use Preferences button to jump to DevTools on RootMissing
- Add: Make invalid backups visible
- Fix: Empty terminal text
- Fix: (DevTools) closing when there is no real activity
- Update: Revamp sheets layouts
- Update: Revamp backups layouts
- Update: Split BlockBorder to Top & Bottom
- Update: Hide schedule Tags filters when empty
- Update: Replace AlertDialog usage with composables
- Update: Make schedule backup types initially open and on top
- Update: Animate Permission cards

8.3.8 (04.08.2024) +30 Commits & +90 Translations
------------

#### Function

- Add: enableOnBackInvokedCallback to Manifest
- Add: Inject DB and WorkHandler using Koin
- Fix: Plugins
- Update: Move regex and sh files to the plugins
- Update: Get versionName, versionCode from manifest

#### UI & UX

- Add: Plugins page (dev)
- Fix: Running jobs count
- Fix: Pref search in dev panel
- Update: Show seconds in schedule counter only when < 10min remaining time 
- Update: Color the password icon when (in)active
- Update: Replace legacy Encryption & Error AlertDialogues with compose
- Update: Revamp all dialog buttons

8.3.7 (24.07.2024) +100 Commits & +20 Translations
------------

#### Function

- Add: Zstd compression option
- Fix: Crash on after giving permissions
- Fix: Reduce many unneeded recompositions
- Fix: Showing restore-preferences in backup page
- Fix: App sheet and context menu actions respect user
- Fix: Profile handling for pm commands
- Fix: Work profile support
- Fix: StorageFile related crash
- Fix: StorageFile duplication if renaming to the same name
- Update: Split excludeCache to backupCache, restoreCache
- Update: Rebase SwitchChip on segmented buttons
- Update: Migrate most sheets to scaffold
- Update: Restore directories without owner

#### UI & UX

- Add: Respecting tags & notes when searching
- Add: Tags filter for schedules
- Add: Notes to backups
- Add: Button to search for uninstalled app in store
- Add: Original text to error for better reinterpretation
- Add: Share button to terminal text
- Add: Option for alternative page blocks layout
- Fix: Alternative navigation bar item
- Update: Backup items' layout info
- Update: OAndBackup-attribution icon
- Update: Revamp Batch actions bar
- Update: Revamp coloring of UI
- Update: Revamp dialog layouts

8.3.6 (12.01.2024) 6 Commits
------------

#### Function

- Add: ForegroundServiceType for works (Required on A14)
- Fix: Crash on backup/restore
- Update: TargetSDK 34

#### UI

- Remove: Theme color options temporarily
- Update: Revamp theme colors


8.3.5 (12.01.2024) +40 Commits & +60 Translations
------------

#### Function

- Add: Enforce backups limit button to AppSheet
- Add: Allow restoring old apps (older than Android 6) on Android 14+
- Fix: KernelSU support
- Update: Move to one-activity structure
- Update: Revamp permissions management

#### UI

- Add: Animated switch of pages
- Add: Different dynamic and black themes
- Fix: Pages popup animation
- Fix: Crash on resume after launching from AppSheet
- Fix: Navigation bar item's ripple
- Fix: StatusBar visibility in custom dynamic themes
- Update: Revamp AppSheet's buttons
- Update: Make Prefs backgrounds a bit transparent
- Update: Improve colors contrast to background

#### UX

- Add: Opt-in dev-option to ignore locked backups in housekeeping
- Add: Warning about password on save/loadPreferences dev-tool
- Update: Revamp resume logic (relock on each resume)


8.3.4 (13.10.2023) +10 Commits & +10 Translations
------------

#### Function

- Fix: Running schedules when app is not open
- Fix: Missing stub for restorePackage in specials

#### UI

- Fix: NavBar overlap of sheets
- Add: Animate navigation bar buttons

#### UX

- Fix: Crash on restarting app with open AppSheet


8.3.3 (11.09.2023) +30 Commits & +30 Translations
------------

#### Function

- Fix: Force stopping an app
- Fix: Deleting schedule
- Fix: Crash on context menu/Put
- Fix: AutoLog after schedule in case of empty filtered list
- Update: Abstract Main pages from Activity
- Update: Generate Kotlin code using Room

#### UI

- Update: Pre-load all pages
- Update: Improve animation of updated apps bar

#### UX

- Fix: Add workaround to NavBar overlap of sheets on specific Android versions

8.3.2 (23.06.2023) +260 Commits & +70 Translations
------------

#### Function

- Add: Experimental export/import preferences (in DevTools/tools/)
- Add: Singular backups restore (atomic restoration)
- Add: AppInfo+schedule database dumps to terminal
- Add: Selinux status to support log
- Add: Recreating activities on specific changes
- Add: Schedule id to all schedule tracing messages
- Fix: Refreshing package & backup on launch
- Fix: Cache excluded even if it's enabled
- Fix: File duplication with SAF (file! not directory), redesigned/hardened duplicate protection (to
  be tested more)
- Fix: Deleted file/directory sometimes not recognized as deleted (to be tested more)
- Fix: File stream not closed
- Fix: Missing uninstalled packages with backups after startup
- Fix: Using nsenter method of global mount namespace in some cases (credit @H1mJT)
- Fix: With backupDir not accessible, refresh did not work
- Fix: deleteDocument deletes a parent directory
- Fix: setLanguage to work multiple times
- Fix: Startup single backup scans
- Fix: Do not disable password preference, if encryption is disabled, as still necessary for restore
- Fix: Crash on "non-null is null"-exception
- Fix: Crash on adding tag
- Fix: Batch backing up updated apps
- Fix: Reset defaults of yaml prefs
- Update: /data/local/toybox is now prioritized over others
- Update: Integrate Intro into Splash & Main
- Update: Use nsenter to run commands in the global mount namespace
- Update: TargetSDK 33
- Update: Move basic functionality to BaseActivity
- Update: Generalize BatchAction dialog usage
- Update: Replace apk-/dataCheckedList usage with apk-/dataBackupCheckedList
- Update: Make prefs reactive
- Update: Simplify ShellCommands init
- Update: Make exclude asset files react on no_backup preferences
- Update: Debug prefs must default to user value
- Update: CompileSdk 34
- Remove: IntroActivity
- Remove: Premature refreshing
- Remove: Automatically added ACCESS NETWORK STATE permission
- Remove: Usage of AppSheet in tasks

#### UI

- Add: Version text for screenshots and other purposes (preference versionOpacity)
- Add: PostNotifications permission
- Add: BlockBorder to pages
- Add: Option to use alternative NavBar item layout
- Fix: Help sheet & Welcome page transparent backgrounds
- Fix: Scrollable message when long in Actions DialogUI
- Fix: Check all StateChips ripple
- Fix: Batch recycler not filling page
- Update: Set default colors & main icon according to variant
- Update: Revamp pages layout
- Update: Revamp Terminal page layout
- Update: Revamp BatchPrefs, Help & SortFilter sheets into composables
- Update: Revamp all layouts
- Update: Theme-based prefs corner size
- Update: Migrate sheets to composable only
- Update: Replace legacy dialogs with composables
- Update: Revamp all items using ListItem
- Update: Make TopBar transparent
- Update: Revamp search bar layout
- Update: Limit BusyBackground to interaction pages (excluding top and bottom bars)

#### UX

- Add: Singular backups restore layout
- Add: Get/Put selections from/to Schedule custom/block lists
- Add: Long press on title always opens DevTools
- Add: Option in root missing dialog to share a support log
- Add: Indicator of backup revisions set limit
- Add: Indicator if password is set (***) vs. unset (---)
- Add: Extended special filters
- Add: Option to include new user apps with the updated apps notification
- Fix: Close context menu if choosing "no"
- Fix: Batch backups not working when singular backups layout is enable
- Fix: Check all apk/data not working
- Fix: Asking for permissions for special backups without restart
- Fix: duplicate directory picker
- Fix: Ghost clicking settings when search is expanded
- Fix: Checking Special backups for apk batch backup
- Update: Get/Put replaces Load/Save in context menu
- Update: Keyboard actions in dialogs with editable text (focus, tab, return, done etc.)
- Update: Allow showing value and description of prefs
- Update: Move pref to show background laser to UserPrefs

8.3.1 (21.02.2023) +80 Commits & +10 Translations
------------

#### Function

- Add: busy to renameDamagedToERROR etc.
- Add: DevTools openBackupDir (works only with SAF capable file managers, so only Files/DocumentsUI)
- Add: Dev-Prefs: autoLogUnInstallBroadcast, toolbarOpacity, prettyJson
- Add: maxJobs to change the default (changing needs real kill + restart),
- Add: killThisApp (app is killed, alarms are kept, unlike force-close)
- Add: Stopping schedule service on finished
- Fix: renameDamagedToERROR missing some important damages
- Fix: hidden Lucky Patcher issue (please test, we don't use it)
- Fix: missing empty line after log header
- Fix: all backups running at once
- Fix: Phh su + inherited+enforcing, directly check if su has --mount-master (github issue #562)
- Update: Pretty print properties files
- Update: improves prevention of duplicate schedules handling (should no more trigger detection)
- Update: Scan depth first in findBackups (= add directory contents at front of queue)
- Update: Remove all xxx dir for xxx.properties before queueing directory content in findBackups
- Update: terminal button log/rel to extract lines that are related to NB from logcat
    - currently machiav3lli.backup + NeoBackup, also used in SUPPORT. Note, log/app is PID related,
      so only from the running NB, not from the one before, if it was restarted)
- Remove: finishWork -> simplification

#### UI

- Add: New app icon
- Add: busy handling + refresh (indicator) button to Logs tab
- Update: Revamp NavBar & SearchBar
- Update: Another option for busy background with grey fade
- Update: Allow BottomSheet to extend over StatusBar
- Update: Icon & theme colors
- Update: Cleaner splash icon
- Remove: Overriding background color in light theme

#### UX

- Add: Rotating refresh button (turn time and scale in devsettings/adv)
- Add: search field in DevTools/devsett, searches all settings, but only key names, not the label
- Add: support infos for mount master etc.
- Fix: renameDamagedToERROR + undoDamagedToERROR blocking the UI
- Fix: a delete/rename backup glitch
- Fix: progress notifications
- Fix: missing refresh of packages on start

8.3.0 (30.01.2023) +350 Commits & +60 Translations
------------

#### Usability / UX

- Add: `hideBackupLabels`, `menuButtonAlwaysVisible` developer options
- Add: Confirmation to context menu "add to blocklist"
- Add: Context menu item "Deselect Not Visible"
- Add: Enable backup/restore in context menu
- Add: Button to run schedule in Schedule item
- Fix: Duplicate entries in LogPage
- Fix: Remove and dismiss AppSheet of uninstalled package with no backups
- Update: Revamp the backend for faster reading and loading of the apps' list
- Update: More informative log file format
- Update: Replace busy progress bar by a more background animation

#### UI

- Add: Context menu floating button
- Add: Info on who's the build is signed by in HelpSheet
- Add: Separate Black theme preference
- Fix: Grey out app icon when uninstalled
- Update: Improve background theming
- Update: Actions buttons layout
- Update: ScheduleSheet bottom layout

#### Function

- Add: Database accessor on OABX
- Add: An experimental flatStructure scheme (read more in [developer notes document](NOTES.md))
- Add: DevTools for advanced users (read more in [developer notes document](NOTES.md))
- Fix: Run only numCores threads in parallel on menu actions
- Fix: Duplicated schedules
- Fix: Blocking other parts until full scan is ready
- Fix: Thread safety for parallel processes
- Fix: Duplicate searching time for backups of packages that don't have backups
- Fix: Failing backup after cleaning up the backup list
- Update: Use parallel processing where appropriate
- Update: Speedy cached/prefetched icons
- Update: Organize dev options in groups

8.2.5 (03.12.2022) +15 Commits & +20 Translations
------------------

#### Function

- Fix: Schedules ignoring specials in blocklists
- Fix: Running schedules twice
- Add: Fake schedules
- Add: More tracing prefs

#### UI

- Fix: Add space to info chips

8.2.4 (25.11.2022) +10 Commits
------------------

#### Function

- Fix: BlockLists in schedules
- Fix: Repetitive running schedules
- Update: Default disable tracing

#### UI

- Fix: Restore full height of terminal

#### Usability/UX

- Update: Separate service prefs into backup and restore ones

8.2.3 (25.11.2022) +25 Commits
------------------

#### Function

- Fix: Note & Tags in AppSheet
- Fix: Log not updated when deleting items
- Update: Separate PrefGroups for logging and tracing

#### UI

- Fix: Schedule filters
- Fix: Height calculation of LogItem/TerminalText, button colors
- Update: Revamp Chips layout

#### Usability/UX

- Add: Batch settings Sheet
- Add: Tooltips to check all buttons in Batch

8.2.2 (23.11.2022) +160 Commits
------------------

#### Function

- Fix: Schedules handling

#### UI

- Add: System theme based icon

8.2.1 (18.11.2022) +290 Commits
------------------

#### Function

- Add: Persisting backups
- Add: Dev Prefs: restoreKillApps, pref_refreshOnStart, pref_logToSystemLogcat
- Add: Dev Prefs: Trace options
- Add: Terminal for support (retrieving infos by buttons, e.g. toybox versions, su version, text can
  be saved to Log)
- Fix: Support of non-magisk SU implementations (`su 0` now is the only feature su needs to have)
- Fix: Bug in `ls -l` of statically linked toybox variants (e.g. toybox-ext Magisk module)
- Fix: Handling of `ls -l` numeric user ids output (xxx_cache group adds 10000 if numeric)
- Fix: Quoting/escaping problems of Magisk su
- Fix: Do not use restorecon if selinux context cannot be retrieved from the file system
- Fix: Don't change group of a cache directory if it'S a system group (e.g. sdcard_rw)
- Fix: A StorageFile bug resulting more issues
- Fix: Make startup more robust in case NB was restored by backup software
- Fix: Cache invalidation
- Fix: Restoring (un)compressed data/specials
- Fix: Specials and data: don't restorecon in case of selinux context = "?"
- Fix: Updating a Package on (Un)Install when there's backups
- Fix: Delete any password accidentally saved to non-encrypted preferences
- Update: Special folders start with `!-` to put them at the top of a directory listing
- Update: Activities hold VMs (avoid memory duplicates)
- Update: Simplify files creation
- Update: Improve tar error output handling
- Update: Dev prefs: pauseApps + pmSuspend is now backupPauseApps + backupSuspendApps
- Update: Toybox scores
- Update: Migrate fields from LiveData to Flow
- Update: Context menu actions to non-blocking
- Update: Replace AppInfo of uninstalled packages that left backups

#### UI

- Add: Missing buttons to legend on HelpSheet
- Add: Save button to error dialogs
- Fix: Share button on Logs works now
- Fix: LogsPage
- Fix: ScheduleSheet custom & block lists state update
- Update: Allow swiping between pages
- Update: Minimize & simplify UI layouts
- Update: Hide NavBar when in a Tool page
- Update: Log entries have a maximum height and are scrollable
- Update: Global loading/busy bar
- Update: Migrate updatedApps bar into expandable FAB
- Update: Make StatusBar transparent

#### Usability/UX

- Add: Tooltips to AppSheet action buttons
- Add: Save/Load context menu selections
- Fix: Dependent preferences should now work flawless
- Fix: Re-initiating app state when rotating screen
- Fix: Show Launch action only for launchable apps
- Update: Alphabetical sort respects Locale
- Update: Short touch on the app icon will select it, a long touch opens context menu
- Update: Hide irrelevant actions for Specials
- Update: Search term is applied again when changing tab
- Update: Make SortFilterSheet immediately updating counts

8.2.0 (22.10.2022) +150 Commits
------------------

- Add: Support for themed icon on A13
- Add: Option to backup no_backup files
- Add: Option for multiline InfoChips Pref
- Add: Option to Squeeze NavItems' text instead of ellipsis
- Add: Note about downgrading being only supported by debuggable packages
- Add: Selectable Home items with context menu (needs heavy testing)
- Update: New icons (Phosphor icons)
- Update: Allow to backup the app
- Update: TopBar layout
- Update: Set better fitting view composition strategy for Sheets
- Update: Make progress bars global
- Update: Migrate SplashActivity to compose
- Update: BackupItem's layout to better fit low dpi and huge fonts
- Update: Improve error messages to include a log
- Update: Revamp password dialog
- Update: Show error message when passwords don't match
- Fix: Handling when apk is not called base.apk
- Fix: Auto updating AppSheet after uninstall/enable/disable/restore
- Fix: Crash on recreating app with a Sheet initiated
- Fix: Crash on prefs having wrong data
- Fix: Forwarding to Main directly from Intro
- Fix: Tonal surface follows set accent color
- Fix: Not respecting default Backup prefs
- Fix: StringPref value not shown
- Fix: Password field not being marked as password
- Fix: Device/biometric lock prompt throwing exception
- Fix: SeekBar's layout
- Fix: TopBar title for Exports & Logs
- Fix: Version name alignment in HelpSheet
- Remove: Tint for Prefs' icons
- Remove: Xml-based unused resources (replaced with compose-based)

8.1.3 (20.09.2022) +50 Commits
------------------

- Update: Migrate navigation fully to Compose
- Update: Convert AppInfo to chips
- Update: Layouts of BackupItem, Welcome Page, Permissions Page and AppSheet
- Update: Unify TopBar and sort management by Main
- Update: Apply Sort/Filter options to sheet's stats
- Update: Make sheets fill available size
- Update: Tag editor animation
- Update: Revamp prefs UI
- Update: Matrix link
- Update: NavBar items UI
- Fix: Inconsistent default theme pref (@hg42)
- Fix: Cropping package name in AppSheet

8.1.2 (12.09.2022) +30 Commits
------------------

- Fix: Wrong default for pmSuspend (@hg42)
- Fix: Disabled app's text color in app sheet (@hg42)
- Fix: List item selection
- Fix: Pref's slider text color (@hg42)
- Add: Pref's slider adaptive steps (@hg42)
- Add: Prefs' dynamic title
- Update: Prefs' BottomNavBar UI

8.1.1 (07.08.2022) +65 Commits
------------------

- Fix: Sheets scrolling
- Fix: Clumsy updates button on devices with low dpi
- Fix: Restrict process pausing detection
- Fix: Cleaning search bar on close
- Update: Revamp Prefs (based on Compose)
- Update: Revamp export/import tool
- Update: Backups retention limit to 100
- Update: Dynamic theme

8.1.0 (23.07.2022) +300 Commits
------------------

- Add: Dynamic color support (aka Material You)
- Add: Backup size (applies only to new backups)
- Add: Tags & Note
- Add: Optional info log in the topBar (@hg42)
- Add: Installer package name
- Add: Session installer (in anticipation of A13 restrictions)
- Add: New sort criteria
- Add: Gray coloring of app type's label
- Add: Loading and batch progress bars
- Add: Filter permissions list
- Add: Version name in HelpSheet (@hg42)
- Add: Repeat restore try without permissions once if it fails
- Fix: Data reset after reboot (@hg42)
- Fix: Initial Root check (@hg42)
- Fix: (catch&ignore) List-related irregular crash
- Fix: DocumentInfo/size of files (@hg42)
- Fix: Obey search with select all (@hg42)
- Fix: Use proper first user appId instead of hardcoded one (@hg42)
- Fix: Schedule's special filter visibility
- Fix: OAndBackupX to Neo Backup in some strings (@whalehub)
- Fix: Packages placeholder icons
- Update: Use cancelable SnackBar for popup info
- Update: Migrate sheets to stateful Compose
- Update: Adapt --mount-master option to what libsu detected (@hg42)
- Update: Show encryption reminder only three times
- Update: Improve SAF access performance (@hg42)
- Update: Select utilBox by score (@hg42)

8.0.2 (29.04.2022) 2 Commits
------------------

- Fix: Auto-updating AppSheet package

8.0.1 (29.04.2022) 2 Commits
------------------

- Fix: Showing uninstalled apps

8.0.0 (29.04.2022, a hot-fix for the unintended release of an alpha on F-Droid) +700 Commits
------------------
NEW BACKUP STRUCTURE: Older backups (v6-v7) are experimentally supported. It's nevertheless
preferred to use a new backup directory for a cleaner experience.
NEW FILTER FORMAT: Requires resetting sort/filter manually.

#### UI

- Update: Migrate fragments & sheets to Compose and update UI
- Add: Auto update list on external install/uninstall
- Removed: Tags & Notes disabled for now
- Update: Improve the app restart on theme/language change
- Fix: Welcome fragment scrolling view
- Update: Place state text under actions in AppSheet

#### Function

- Add: Backup & restore permissions
- Update: Refactor batch actions (@hgx42)
- Add: Made the compression level configurable (@pizze)
- Add: Fake developer settings (@hg42, mostly for debugging/testing reasons)
- Add: Call log special backup (@dl200010)
- Fix: SMS/MMS special backup (@dl200010)
- Add: Generic support for older backups(v6-v7) (@hg42)
- Update: Complete rework of the running notification. (@hgx42)
- Fix: Freezing more essential packages (@hg42)
- Fix: (Not) restoring package to both/all profiles
- Fix: Handling backups with dot-dot-dirs (@hg42)
- Add: Option to disable compression (@hg42)
- Add: Special backups to schedules
- Fix: Using toybox on SDK26 (@hg42)
- Fix: More strict hard links (@hg42)
- Fix: Messed up scheduling after rebooting device
- Add: Simple switch where to place the Wifi config file (@pizze)
- Add: Retry mechanism to detect when PackageManager is not ready after installing an APK (@pizze)

#### Usability/UX

- Add: Backend's lazy loading (half-baked)
- Add: Loading toasts for refresh.
- Update: Improve UX for search, scrolling & AppSheet
- Fix: Resetting search box on work finished
- Update: Improve error reporting (@hg42)
- Update Translations

7.0.0 (09.10.2021) +300 Commits
------------------
BACKUPS HAS NEW VARIABLE: making new backups incompatible with old versions of OABX
SCHEDULES REVAMPED: schedules will be deleted on update and old exports aren't usable

#### UI

* Add: Theming engine
* Add: Transition animations
* Add: Elevation/shadows to UI (viewable in light theme)
* Add: PrimaryDark color to system's navigation bar
* Update: Revamp UI (Material design 3 & new icons set)
* Update: Replace the snackbar with a simple text on top of activity and appsheet
* Fix: Crashing on changing theme
* Fix: Sort/Filter and Blocklist buttons colliding with lower dpi

#### Function

* Add: Media files backup
* Add: Randomized IV for the Cipher
* Update: TargetSDK 30
* Update: Exclude cache size from data size
* Fix: Postpone chmod to do it after restore (@hg42)
* Fix: Special characters in ls output and quoting(@hg42x)
* Fix: Sorting by data size
* Fix: Improved exception handling with TarUtils (@hg42)
* Fix: Firefox restore (@hg42)
* Fix: Including uninstalled apps in scheduled backups

#### Usability/UX

* Add: Extras [Note and Tags]
* Add: Blocklist shortcut to all navigation fragments
* Add: Animated placeholders while loading apps' list
* Add: Schedule special filter for old apps
* Add: Disabled apps filter
* Add: Backup all updated apps button
* Add: Support for countries' specific locales
* Add languages: Chinese(traditional/Taiwan), Portuguese(Brazil), Lithuanian.
* Update translations: Chinese(simplified), Vietnamese, Ukrainian, Russian, Arabic, German, French,
  Polish, Norwegian, Italian, Swedish.
* Update: Automate locals generation
* Update: Revamp app's navigation (Activities and Fragments)
* Update: Revamp filters and modes
* Update: Improve readability of app's info in app sheet
* Update: More informative error messages
* Update: Grey out encryption password preferences instead of hiding it
* Remove: Main special filter split
* Remove: Info shortcut from Main fragments
* Fix: Hide Navigation Bar while in tools

6.0.1 (03.06.2021)
------------------

* Fixed: Exclude system apps checkbox in the schedule sheet
* Fixed: Import the val of exclude system apps on schedules' import
* Added: Vietnamese, Hindi & Ukrainian
* Updated translations:Arabic, Spanish, Italian, Portuguese, Russian, Catalan, Turkish, Indonesian

6.0.0 (05.05.2021)
------------------
CLEAR DATA NEEDED

* Added: Encrypt private preferences including encryption password (using Android keystore)
* Added: Export/import of schedules
* Added: Global blocklist applies to Main
* Added: Ascending/descending sort support
* Added: Warning text to backup directory picker
* Added: Direct share button for logs
* Added: Better error handling
* Updated: Make backup dialog checks response to the service's active options
* Updated: Use modes' checkboxes in the backup/restore/schedule dialogs
* Updated: Schedule action on first day if possible
* Updated: Replace Blacklist with Blocklist
* Fixed: Don't schedule when disabled
* Fixed: Crash when scheduled list of apps is empty
* Fixed: Schedules not firing on time
* Fixed: Delayed notifications of some restored apps (exclude the push notifications' ID from
  backups)
* Fixed: Backup instance folder left untouched after backup failing
* Fixed: Hiding device lock option when there's no lock
* Added: Catalan language
* Updated translation: Arabic, Spanish, Indonesian, Polish, Russian, Japanese, Chinese, Dutch,
  French

5.1.0 (18.03.2021)
------------------

* Added: Tool to save apps' list to a file
* Added: Option to use device credentials as lock
* Added: Names to schedules
* Added: Force kill and launch to App Sheet
* Added: Updated apps bar
* Added: Snackbar on single Backup/Restore actions
* Added: Warning text to enable special backups preference
* Updated: UI/UX revamp (in co-op with @opepp)
* Updated: STOP-CONT default is on
* Fixed: Scheduling backups
* Fixed: Log items sdk release name
* Fixed: Crash on batch actions with nothing checked
* Updated Translations: Norwegian, German, Chinese, Spanish, Polish, Indonesian, Malayalam, French,
  Turkish, Arabic
* A ton of other small fixes

5.0.2 (24.02.2021)
------------------

* Fixed: Message display on error (@hg42)
* Fixed: Quote issue in backup process (@hg42)

5.0.1 (13.02.2021)
------------------

* Updated: Improving shell commands function (@hg42)
* Fixed: Correct SELinux context (@jakeler & @hg42)
* Fixed: Missing files for some special backups (@hg42)
* Fixed: Throwing exception on sockets and other special files (@hg42)
* Fixed: Failed backups with files with names with double spaces (@jakeler)
* Fixed: STOP-CONT log messages (@hg42)
* Updated: Gradle & Dependencies
* Updated translations: Polish, Portuguese, Chinese, Greek, German, French, Norwegian, Dutch,
  Spanish, Italian, Indonesian, Japanese, Russian
* Clean up

5.0.0 (03.01.2021)
------------------

MIGRATED TO KOTLIN, NEW ENCRYPTION ALGORITHM: clean install & doesn't support restore of previous
encrypted backups.
SCHEDULED ACTIONS CAN ONLY LAST 10 MINUTES (SYSTEM CONSTRAINT): so partition your group of apps.
will be fixed in next minor releases.

* Added: Individual modes for apps in batch action dialog
* Added: Launchable apps filter
* Added: Ability to install test builds
* Added: Option to restore with all asked permissions
* Added: Option to allow downgrading apps
* Added: Progress indicator for batch actions
* Added: Filter black list based on schedule's filter
* Added: Refresh after scheduled actions or changing backup folder (no visual indicator)
* Added: Copy app's own APK to tools
* Added: Stats to sort/filter sheet
* Added: Blacklists to scheduled actions
* Added: Shortcut to known issues in Help
* Added: Notification on missing storage permission of scheduled backups
* Added: Separate Option to backup obb files
* Updated: Multi-threading batch & scheduled actions (big improvement to performance)
* Updated: Encryption algorithm and parameters
* Updated: Log the errors on most actions
* Updated: New log viewer system
* Updated: Choosing a backup directory doesn't create a child folder anymore
* Updated: Migrate from Java to Kotlin (99.99%)
* Updated: Far less unneeded refreshing
* Updated: Schedules mode/sub-mode to filter/mode
* Updated: Standardize modes and filters variables
* Updated: Scheduling from Alarm-based to Job-based
* Updated: Migrated to Gradle Kotlinscript
* Updated: Batch & scheduled actions to WorkManager-based
* Updated: Don't use cmd package anymore
* Updated: Migrate from Log to Timber
* Updated: Turn some utils and handlers classes to objects
* Updated: Integrate Backup- & RestoreFragment in BatchFragment
* Updated: Migrate custom list to DialogFragment
* Updated: Color the shortcuts icons
* Updated: Migrate blacklist DB to Room
* Updated: Using ViewModel to manage data in Main and Scheduler
* Updated: Batch items reflect their backup mode
* Updated: Show Blacklist's & Customlist's apps with labels (@hg42)
* Updated: Format of the time left for scheduled backups
* Updated: Return special backups setting to advanced
* Updated: Set default disable usb verification to true
* Updated: Improve the details in start schedule now dialog
* Updated: Safer backup history initialization (@hg42)
* Fixed: APK/data not found error
* Fixed: Persistant storage permission
* Fixed: Ability to query for all apps (on A11)
* Fixed: Asking for permission to manage all files (on A11)
* Fixed: Backup directory being asked again after a reboot
* Fixed: Crashing on picking up new backup directory from settings
* Fixed: Enable/disable apps
* Fixed: Running scheduled backup with empty list gives an error
* Fixed: Backup items restoring the latest backup not the chosen one
* Fixed: Crash when deleting an app that has no backups
* Fixed: Ghost clicking when the app is long in the background
* Fixed: Not able to read the full text of error notifications
* Fixed: AppInfo's latestBackup & isUpdated
* Fixed: RememberFiltering option
* Fixed: Length of the time left string
* Fixed: Updated tag not updating
* Fixed: Scheduling repeated actions
* Fixed: Try to read logs as backups
* Fixed: CheckApk,-Data & -All behavior
* Fixed: Blacklist saving wrong selected apps
* Fixed: AppInfo constructor missing AppMetaInfo
* Fixed: Exclude system apps not showing correctly
* Fixed: Crashing on delete all backups
* Fixed: Restore of apps with device-protected data (@fantostisch)
* Fixed: Exception hardening (@hg42)
* Removed: Copying app's own apk on backup actions
* Removed: Quick reboot tool
* Removed: Toasts on batch actions
* Removed: Usage of Wakelocks
* Removed: HandleMessages
* Updated Translations: Estonian, Indonesian, Hindi, Greek, Italian, Norwegian, Russian, Chinese,
  German, French, Spanish, Dutch, Arabic, Polish, Portuguese

4.0.0 (02.11.2020)
------------------

* Added: Allow parallel batch tasks
* Added: Disable verification of apps over USB before installing and re-enable it afterwards
* Added: APK & Data checkboxes in Batch
* Added: Storage Access Framework support (Credits @tiefkuehlpizze)
* Added: Symlink/Pipe Support (Credits @hg42 and @tiefkuehlpizze)
* Added: Multi Backup support (Credits @tiefkuehlpizze)
* Added: HelpSheet to deliver help easily
* Added: More clear messages informing user about running processes
* Added: Arabic, Bosnian and Malayalam
* Updated: Tags system
* Updated: Use STOP/CONT to make sure no background processes ruin the backup (Credits @hg42 &
  @tiefkuehlpizze)
* Updated: Restructure Schedules
* Updated: Backups directory is /OABackupX now
* Updated: Backup Structure (Credits @tiefkuehlpizze)
* Updated: Whole new layouts for UI elements
* Updated: Encryption warning leads directly to its preferences
* Updated: Translations
* Fixed: Log viewer
* Fixed: Create Log when errors occur on backups
* Removed: Annimon stream dependency
* Too many other improvements, fixes and cleanups

3.2.0 (23.09.2020)
------------------

* Added: new Navigation
* Added: badge for updated apps in Main
* Fixed: save checked items in Batch
* Fixed: sync of check all in Batch
* Fixed: search filtering unexpected behavior(caused by caching)
* Fixed: resume behavior
* Updated: improve refresh performance
* Updated: improve on visual performance (Action instances and Sheets launch speed)
* Updated: Translations (Norwegian, Dutch, Turkish, Chinese, Portuguese)

3.1.1 (11.09.2020)
------------------

* Added: the Intro
* Added: encryption password check field
* Removed: Busybox path preference
* Fixed: showing the present date instead of the last backup
* Fixed: not updating the items' update tag after new backup
* Updated: Translations (Norwegian, Russian, Italian, French, Dutch, German)
* Updated: set remember sort/filter default to true
* Updated: set biometric lock default to off
* Updated: hide log viewer

3.1.0 (25.08.2020)
-------------------

* Removed: dropped Android Nougat support
* Added: new tags system
* Added: biometric lock preference
* Added: persistent checked items in Batch
* Updated: the whole UI
* Updated: change schedule custom app list to show labels instead of package names
* Updated: reduce repeatability of encryption AlertDialog
* Updated: set default external data and obb preference to off
* Updated: show only the Date of latest backup in Main
* Fixed: crash on launching batch fast
* Fixed: crashing when tapping outside of ProgressDialog in Main
* Refactored the code improving quality and performance
* Moved to Affero GPL v3

3.0.0 (04.08.2020)
-------------------

**THIS RELEASE ISN'T COMPATIBLE WITH THE VERSIONS BEFORE**

* Changed: default backup directory to /OABX
* Rewritten Encryption Solution (Credits @Pizze)
* Added: sort by Data Size
* Added: alert dialog about enabling encryption
* Updated: revamped Help UI
* Added: tooltips for all Buttons' Icons
* Updated: AppSheet adaptive UI
* Added: wipe Cache button to AppSheet
* Changed: clearCache preference to excludeCache
* Fixed: edge cases for all Adapters
* Fixed: fluid Checked in Batch
* Fixed: not connected Prefs
* Fixed: some Filters not working
* Update: rewritten the whole shell commands functions (Credits @Pizze)
* Updated: German (Credits @elea11)
* a lot of performance and UI tweaks

2.0.0 (06.07.2020)
-------------------

* updated: switched to sulib (Credits @Tiefkuehlpizze)
* removed oab-utils for good (Credits @Tiefkuehlpizze)
* update: new UI elments for App-, Filter- & ScheduleSheet
* fixed scheduled backups for data(alone)
* fixed zipping external/deData/obb according to Prefs
* fixed: zipping empty external/deData/obb folders
* updated: clean cache function
* some other UI and Performance tweaks

1.3.0 (25.06.2020)
-------------------

* updated: new Filters System
* updated: reduced Splash waiting time
* added: Split-APK support (needs more testing, Credits @Tiefkuehlpizze)
* updated: Search supports Package Name
* fixed: cache not found/ Symlinks errors
* added: OBB support(needs more testing)
* added languages: Italian, Chinese, Turkish, Polish (Credits @Urnyx05 @tuchit @scrubjay55
  @Antyradek)
* other: UI and performance tweaks

1.2.0 (10.06.2020)
-------------------

* updated: UI
* fixed: share crashing
* added: options to backup Device Protected Data and External Data

1.1.1 (04.06.2020)
-------------------

* added: Exodus report shortcut to AppSheet
* fixed: not launching if there's an app with icon dimensions <= 0
* changed: default is set now to copy OAndBackupX to parent backup folder
* added: Greek (Credits @tsiflimagas)
* update: Capitalisation and English Strings (Credits: @atrate)
* update: generalize Chinese, Dutch, Polish and Swedish

1.1.0 (29.05.2020)
-------------------

* added: data backup encryption
* fixed: schedules
* updated: notifications' format
* tweaked some UI elements
* and improved performance

1.0.2 (24.05.2020)
-------------------

* added: Item click feedback
* updated: scrolling eXperience
* fixed: Splash screen
* fixed: swipe to refresh behavior
* fixed: Statusbar theme
* and some other UI tweaks

1.0.0 (18.05.2020)
-------------------

* added: option to clean cache before data backup

0.9.14 (14.05.2020)
-------------------

* reverted: back to cleaning cache

0.9.13 (14.05.2020)
-------------------

* reverted: back to cp (rsync isn't bundled with all ROMs)

0.9.12 (14.05.2020)
-------------------

* added: app's info shortcut in App Sheet
* updated: punch of UI elements
* updated: new default backup directory /OAndBackupX
* fixed: behaviour of adapter after doing actions
* fixed: adapter's crashing
* switched: from cleaning app's cache to excluding it from backup(Credit @icewind1991)
* tweaked: behaviour of different UI elements

0.9.11 (11.05.2020)
-------------------

* added: clear cache before backing up
* updated: chips styling
* added: prompt for battery optimization exclusion
* fixed: the profiles bug (needs testing)
* switched: from FilePicker to SAF
* tweaked up the code

0.9.10 (07.05.2020)
-------------------

* rewritten: Scheduler (need to be tested extensively)
* fixed: links in help
* clean up the code
* UI tweaks here and there

0.9.9 (06.05.2020)
-------------------

* removed: check for Update (preparing for F-Droid Release)

0.9.8 (01.05.2020)
-------------------

* fixed: remember filter
* fixed: picking the value for old backups
* fixed: empty filtered list causing crash
* tweaked: Scheduler UI
* small other tweaks

0.9.7 (27.04.2020)
-------------------

* fixed: update logic
* removed: PGP encryption and follow symbolic links
* cleaned up

0.9.6 (25.04.2020)
-------------------

* added: check for Update
* fixed: theme consistency
* fixed: some strings

0.9.5 (23.04.2020)
-------------------

* fixed refresh issue after restore and backup from AppSheet
* made the UI more simple
* fixed press back on Main
* some other small tweaks here and there

0.9.4 (20.04.2020)
-------------------

* added support for protected data backup
* switched to local date format for apps in Main
* some other small changes

0.9.3 (18.04.2020)
-------------------

* added Theme's settings: Dark/Light/System
* fixed Settings padding
* other small tweaks

0.9.2 (13.04.2020)
-------------------

* added fast scroll bars to Main & Batch
* converted the icons .png to .xml

0.9.1 (10.04.2020)
-------------------

* fixed the reported Oab-utils-test error

0.9.0 (09.04.2020)
-------------------

* adapted FastAdapter for Main and Batch
* rewritten Main and Batch
* added AppSheet for apps
* added Intro to handle permissions
* restructured the app
* UI tweaks all over the app
* Androidx-ify the Dialogs
* rewritten some Handlers
* clean up job
* shot the encryption down for now
* other small tweaks

0.2.7 (31.03.2020)
-------------------

* optimized sort/filter chips' visual behavior
* other small UI tweaks

0.2.6 (29.03.2020)
-------------------

* rewritten Preferences fully
* replaced the file browser with a more initiative one
* fixed toolbar issue for Scheduler and Preferences
* integrated Help and Tools in the Preferences

0.1.3 (27.03.2020)
-------------------

* new UI for Main, Batch and Scheduler
* fixed: missing "both" as description of backed up mode

0.1.2 (27.03.2020)
-------------------

* new Main UI
* fixed refresh in Main

0.1.1 (25.03.2020)
-------------------

* initial X release
* restructured and cleaned the app: adding Bottombar-based navigation and Sort and Filter FAB(for
  now: only the basic filters)
* rewritten Batch-(Activity, Adapter and Sorter) and most the layouts
* partially rewritten Main-(Activity and Sorter)
* completed the German translation
* had to shut the preferences out for now: to solve the compatibility issues, I have to fully
  restructure and rewrite the preferences and that'll be one of the next steps(fix priority high).
* other drawback: setting a toolbar on some activities(scheduler, help and tools) is causing a
  crash, so they have to do with no toolbar for now(fix priority minimal).
