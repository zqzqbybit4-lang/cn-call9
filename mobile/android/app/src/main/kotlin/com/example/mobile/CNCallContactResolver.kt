package com.example.mobile

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object CNCallContactResolver {
    fun resolveDisplayName(
        context: Context,
        callerId: String,
        serverCallerName: String?,
    ): String {
        val cleanCallerId = callerId.trim()
        val cleanServerName = serverCallerName?.trim().orEmpty()

        // 1. Try local contact lookup by caller_id if READ_CONTACTS permission is granted
        if (cleanCallerId.isNotEmpty()) {
            val contactName = queryContactName(context, cleanCallerId)
            if (!contactName.isNullOrEmpty() && contactName != cleanCallerId) {
                return contactName
            }
        }

        // 2. Server caller_name fallback (valid display name, not empty and not equal to callerId)
        if (cleanServerName.isNotEmpty() && cleanServerName != cleanCallerId) {
            return cleanServerName
        }

        // 3. Final fallback
        return "مستخدم CN CALL"
    }

    private fun queryContactName(context: Context, callerId: String): String? {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CONTACTS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(callerId),
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        val name = cursor.getString(nameIdx)?.trim()
                        if (!name.isNullOrEmpty()) {
                            return name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("[CN CALL][CONTACT_RESOLVER] Lookup failed for callerId=$callerId: ${e.message}")
        }
        return null
    }
}
