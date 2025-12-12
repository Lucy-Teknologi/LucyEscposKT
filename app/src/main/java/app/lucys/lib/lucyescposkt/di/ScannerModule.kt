package app.lucys.lib.lucyescposkt.di

import android.content.Context
import app.lucys.lib.lucyescposkt.data.AndroidBluetoothPrinterScanner
import app.lucys.lib.lucyescposkt.data.BluetoothPrinterScanner
import app.lucys.lib.lucyescposkt.data.CoilImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides
    @Singleton
    fun bindsBTScanner(@ApplicationContext context: Context): BluetoothPrinterScanner =
        AndroidBluetoothPrinterScanner(context)

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): CoilImageLoader =
        CoilImageLoader(context)
}