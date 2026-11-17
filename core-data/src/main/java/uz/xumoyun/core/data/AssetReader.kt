package uz.xumoyun.core.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** assets/ papkasidan matn o'qish. Testlarda fayl tizimidan o'qiydigan soxta versiya beriladi. */
interface AssetSource {
    fun readText(path: String): String
}

@Singleton
class AndroidAssetSource @Inject constructor(@ApplicationContext private val context: Context) : AssetSource {
    override fun readText(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AssetModule {
    @Binds abstract fun assets(impl: AndroidAssetSource): AssetSource
}
