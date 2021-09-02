package ru.skillbranch.skillarticles.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.*
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.dao.ArticleContentsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlePersonalInfosDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlesDao
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.*
import ru.skillbranch.skillarticles.extensions.data.toArticleContent
import java.lang.Thread.sleep
import kotlin.math.abs
import androidx.annotation.VisibleForTesting
import ru.skillbranch.skillarticles.data.local.dao.ArticleCountsDao
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.remote.req.MessageReq

interface IArticleRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun isAuth(): LiveData<Boolean>
    fun updateSettings(copy: AppSettings)

    suspend fun toggleLike(articleId: String)
    suspend fun toggleBookmark(articleId: String)
    suspend fun decrementLike(articleId: String)
    suspend fun incrementLike(articleId: String)
    suspend fun sendMessage(articleId: String, message: String, answerToSlug: String?)
    suspend fun fetchArticleContent(articleId: String)
    fun loadCommentsByRange(slug: String?, size: Int, articleId: String): List<CommentItemData>

    fun findArticleCommentCount(articleId: String): LiveData<Int>
    fun loadAllComments(
        articleId: String,
        total: Int,
        errHandler: (Throwable) -> Unit
    ): CommentsDataFactory

}

object ArticleRepository : IArticleRepository {
    private val network = NetworkManager.api
    private val preferences = PrefManager
    private var articlesDao = db.articlesDao()
    private var articlePersonalDao = db.articlePersonalInfosDao()
    private var articleCountsDao = db.articleCountsDao()
    private var articleContentDao = db.articleContentsDao()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setupTestDao(
        articlesDao: ArticlesDao,
        articlePersonalDao: ArticlePersonalInfosDao,
        articleCountsDao: ArticleCountsDao,
        articleContentDao: ArticleContentsDao
    ) {
        this.articlesDao = articlesDao
        this.articlePersonalDao = articlePersonalDao
        this.articleCountsDao = articleCountsDao
        this.articleContentDao = articleContentDao
    }

    override fun findArticle(articleId: String): LiveData<ArticleFull> {
        return articlesDao.findFullArticle(articleId)
    }

    override fun getAppSettings(): LiveData<AppSettings> =
        preferences.appSettings //from preferences

    override fun isAuth(): LiveData<Boolean> = preferences.isAuthLive

    override fun updateSettings(settings: AppSettings) {
        preferences.isBigText = settings.isBigText
        preferences.isDarkMode = settings.isDarkMode
    }

    override suspend fun toggleLike(articleId: String) {
        articlePersonalDao.toggleLikeOrInsert(articleId)
    }

    override suspend fun toggleBookmark(articleId: String) {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
    }

    override suspend fun decrementLike(articleId: String) {
        //check auth locally
        if (preferences.accessToken!!.isEmpty()){
            articleCountsDao.decrementLike(articleId)
            return
        }

        try {
            val res = network.decrementLike(articleId, preferences.accessToken!!)
            articleCountsDao.updateLike(articleId, res.likeCount)
        } catch (e: Throwable) {
            if (e is NoNetworkError) {
                articleCountsDao.decrementLike(articleId)
                return
            }
            throw e
        }
    }

    override suspend fun incrementLike(articleId: String) {
        //check auth locally
        if (preferences.accessToken!!.isEmpty()){
            articleCountsDao.incrementLike(articleId)
            return
        }

        try {
            val res = network.incrementLikes(articleId, preferences.accessToken!!)
            articleCountsDao.updateLike(articleId, res.likeCount)
        } catch (e: Throwable) {
            if (e is NoNetworkError) {
                articleCountsDao.incrementLike(articleId)
                return
            }
            throw e
        }
    }

    override suspend fun sendMessage(
        articleId: String,
        message: String,
        answerToMessageId: String?
    ) {
        val (_, messageCount) = network.sendMessage(
            articleId,
            MessageReq(message = message, answerTo = answerToMessageId),
            preferences.accessToken!!
        )
        articleCountsDao.updateCommentsCount(articleId, messageCount)
    }

    override fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId).apply { sleep(1500) }
        articleContentDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> {
        return articleCountsDao.getCommentsCount(articleId)
    }

    override fun loadCommentsByRange(
        slug: String?,
        size: Int,
        articleId: String
    ): List<CommentItemData> {
        val data = network.commentsData.getOrElse(articleId) {
            mutableListOf()
        }
        return when {
            slug == null -> data.take(size)

            size > 0 -> data.dropWhile { it.slug != slug }
                .drop(1)
                .take(size)

            size < 0 -> data.dropLastWhile { it.slug != slug }
                .dropLast(1)
                .takeLast(abs(size))

            else -> emptyList()
        }.apply { sleep(500) }
    }


    override fun loadAllComments(articleId: String, totalCount: Int): CommentsDataFactory =
        CommentsDataFactory(
            itemProvider = ::loadCommentsByRange,
            articleId = articleId,
            totalCount = totalCount
        )



}

class CommentsDataFactory(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : DataSource.Factory<String, CommentItemData>() {
    override fun create(): DataSource<String, CommentItemData> =
        CommentsDataSource(itemProvider, articleId, totalCount)

}

class CommentsDataSource(
    private val itemProvider: (String?, Int, String) -> List<CommentItemData>,
    private val articleId: String,
    private val totalCount: Int
) : ItemKeyedDataSource<String, CommentItemData>() {
    override fun getKey(item: CommentItemData): String = item.slug

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentItemData>
    ) {
        val result = itemProvider(params.requestedInitialKey, params.requestedLoadSize, articleId)
        Log.e(
            "ArticleRepository",
            "loadInitial: key > ${params.requestedInitialKey} size > ${result.size} totalCount > $totalCount"
        )
        callback.onResult(
            if (totalCount > 0) result else emptyList(),
            0,
            totalCount
        )
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, params.requestedLoadSize, articleId)
        Log.e("ArticleRepository", "loadAfter: key > ${params.key} size > ${result.size}")
        callback.onResult(result)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<CommentItemData>) {
        val result = itemProvider(params.key, -params.requestedLoadSize, articleId)
        Log.e("ArticleRepository", "loadBefore: key > ${params.key} size > ${result.size}")
        callback.onResult(result)
    }

}