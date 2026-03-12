package com.gymapp.core.drivebackup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveBackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupSnapshotStore(
        impl: RoomBackupSnapshotStore,
    ): BackupSnapshotStore

    @Binds
    @Singleton
    abstract fun bindDriveBackupRepository(
        impl: GoogleDriveBackupRepository,
    ): DriveBackupRepository
}
