package com.machiav3lli.backup.utils.extensions

import androidx.core.text.isDigitsOnly

// define dirty values and functions
// e.g. hardcoded, possibly version dependent values
// not necessarily portable or future-proof assumptions
// a.k.a. HACKS!
//TODO revamp: all these might be improved by using more future-proof strategies
class Dirty {
    companion object {
        val FIRST_APPLICATION_GID =
            android.os.Process.FIRST_APPLICATION_UID  // ASSUMPTION: gid==uid (usually 10000)
        val LAST_APPLICATION_GID =
            android.os.Process.LAST_APPLICATION_UID   // ASSUMPTION: gid==uid (usually 19999)

        //val FIRST_APPLICATION_CACHE_GID = FIRST_APPLICATION_GID * 2         // ASSUMPTION: *2 or +10000
        val FIRST_APPLICATION_CACHE_GID =
            LAST_APPLICATION_GID + 1             // ASSUMPTION: cache group following normal group

        // cache groups seem to be offset by 10000 to the normal app group
        fun appGidToCacheGid(appGid: Int) =
            appGid - FIRST_APPLICATION_GID + FIRST_APPLICATION_CACHE_GID

        fun appGidToCacheGid(appGid: String): String {
            // in case the toybox is compiled statically, it usually returns numeric uid/gid
            // because Glibc uses dlopen to implement nss switching (/etc/nsswitch.conf)
            // and dynamic linking in static linked binaries restricts the loadable library versions
            //TODO hg42 could also try to get the name for the gid and add "_cache" to it,
            // but I didn't find a num->name function
            return if (appGid.isDigitsOnly()) {
                val numGid = appGid.toInt()
                if (numGid !in FIRST_APPLICATION_GID..LAST_APPLICATION_GID) // something special like sdcard_rw
                    appGid
                else
                    "${appGidToCacheGid(numGid)}"
            } else {
                val numGid = android.os.Process.getGidForName(appGid)
                if (numGid !in FIRST_APPLICATION_GID..LAST_APPLICATION_GID) // something special like sdcard_rw
                    appGid
                else
                    "${appGid}_cache"
            }
        }
    }
}
