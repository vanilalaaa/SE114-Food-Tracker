package com.SE114.food_tracker.di

import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ProfileRepository
import com.SE114.food_tracker.data.repository.SupabaseAuthRepository
import com.SE114.food_tracker.data.repository.SupabaseProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository
}
