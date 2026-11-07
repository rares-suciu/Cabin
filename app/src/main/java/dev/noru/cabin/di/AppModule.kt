package dev.noru.cabin.di

import androidx.room.Room
import dev.noru.cabin.data.image.ImageDownloader
import dev.noru.cabin.data.local.db.AppDatabase
import dev.noru.cabin.data.parsing.ContentParser
import dev.noru.cabin.data.remote.NetworkFetcher
import dev.noru.cabin.data.repository.ArticleRepository
import dev.noru.cabin.ui.flightreadiness.FlightReadinessViewModel
import dev.noru.cabin.ui.library.LibraryViewModel
import dev.noru.cabin.ui.reader.ReaderViewModel
import dev.noru.cabin.worker.SaveArticleWorker
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // OkHttp
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Network & Parsing
    single { NetworkFetcher(get()) }
    single { ContentParser() }
    single { ImageDownloader(androidContext(), get()) }

    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "cabin.db"
        ).build()
    }

    single { get<AppDatabase>().articleDao() }

    // Repository
    single { ArticleRepository(androidContext(), get()) }

    // ViewModels
    viewModel { LibraryViewModel(get()) }
    viewModel { ReaderViewModel(get(), get()) }
    viewModel { FlightReadinessViewModel(androidContext(), get()) }

    // Worker
    worker { SaveArticleWorker(get(), get(), get(), get(), get(), get()) }
}
