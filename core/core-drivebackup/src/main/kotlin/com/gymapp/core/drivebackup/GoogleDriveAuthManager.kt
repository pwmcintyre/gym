package com.gymapp.core.drivebackup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val driveScope = Scope(DriveScopes.DRIVE_APPDATA)

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(driveScope)
        .build()

    fun createSignInIntent(): Intent =
        GoogleSignIn.getClient(context, signInOptions).signInIntent

    fun currentAccountState(): DriveAccountState {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return DriveAccountState(
            accountLabel = account?.email ?: account?.displayName,
            isAuthorized = account != null && GoogleSignIn.hasPermissions(account, driveScope),
        )
    }

    fun requireAuthorizedAccount(): GoogleSignInAccount {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        check(account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
            "Connect Google Drive in Settings first."
        }
        return account
    }

    fun handleSignInResult(data: Intent?): Result<DriveAccountState> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)

        check(GoogleSignIn.hasPermissions(account, driveScope)) {
            "Google Drive access was not granted."
        }

        DriveAccountState(
            accountLabel = account.email ?: account.displayName,
            isAuthorized = true,
        )
    }.recoverCatching { error ->
        val apiException = error as? ApiException
        if (apiException != null) {
            throw IllegalStateException(
                "Google sign-in failed (${apiException.statusCode}). Check the Android OAuth client and Drive API setup.",
                apiException,
            )
        }
        throw error
    }
}
