package ru.skillbranch.skillarticles.data.repositories

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import ru.skillbranch.skillarticles.data.local.DbManager.db
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.local.dao.ArticleContentsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticleCountsDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlePersonalInfosDao
import ru.skillbranch.skillarticles.data.local.dao.ArticlesDao
import ru.skillbranch.skillarticles.data.local.entities.ArticleFull
import ru.skillbranch.skillarticles.data.models.AppSettings
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.remote.req.MessageReq
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.extensions.data.toArticleContent

interface IArticleRepository {
    fun findArticle(articleId: String): LiveData<ArticleFull>
    fun getAppSettings(): LiveData<AppSettings>
    fun isAuth(): LiveData<Boolean>
    fun updateSettings(copy: AppSettings)

    suspend fun toggleLike(articleId: String): Boolean
    suspend fun toggleBookmark(articleId: String): Boolean
    suspend fun decrementLike(articleId: String)
    suspend fun incrementLike(articleId: String)
    suspend fun sendMessage(articleId: String, message: String, answerToMessageId: String?)
    suspend fun refreshCommentsCount(articleId: String)
    suspend fun fetchArticleContent(articleId: String)

    fun findArticleCommentCount(articleId: String): LiveData<Int>
    fun loadAllComments(articleId: String, total: Int, errHandler: (Throwable) -> Unit): CommentsDataFactory

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

    override suspend fun toggleLike(articleId: String): Boolean {
        articlePersonalDao.toggleLikeOrInsert(articleId)
        return articlePersonalDao.isLiked(articleId)
    }

    override suspend fun toggleBookmark(articleId: String): Boolean {
        articlePersonalDao.toggleBookmarkOrInsert(articleId)
        return articlePersonalDao.isBookmarked(articleId)
    }

    override suspend fun decrementLike(articleId: String) {
        //check auth locally
        if (preferences.accessToken.isEmpty()) {
            articleCountsDao.decrementLike(articleId)
            return
        }

        try {
            val res = network.decrementLike(articleId, preferences.accessToken)
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
        if (preferences.accessToken.isEmpty()) {
            articleCountsDao.incrementLike(articleId)
            return
        }

        try {
            val res = network.incrementLike(articleId, preferences.accessToken)
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
            preferences.accessToken
        )
        articleCountsDao.updateCommentsCount(articleId, messageCount)
    }

    override suspend fun refreshCommentsCount(articleId: String) {
        val counts = network.loadArticleCounts(articleId)
        articleCountsDao.updateCommentsCount(articleId, counts.comments)
    }

    override suspend fun fetchArticleContent(articleId: String) {
        val content = network.loadArticleContent(articleId)
        articleContentDao.insert(content.toArticleContent())
    }

    override fun findArticleCommentCount(articleId: String): LiveData<Int> {
        return articleCountsDao.getCommentsCount(articleId)
    }


    override fun loadAllComments(
        articleId: String,
        totalCount: Int,
        errHandler: (Throwable) -> Unit
    ): CommentsDataFactory =
        CommentsDataFactory(
            itemProvider = network,
            articleId = articleId,
            totalCount = totalCount,
            errHandler = errHandler
        )

    suspend fun addBookmark(articleId: String) {
        if(preferences.accessToken.isEmpty()) {
            return
        }

        try {
            network.addBookmark(articleId, preferences.accessToken)
        } catch (e: Exception) {
            if (e is NoNetworkError) return
            throw e
        }
    }

    suspend fun removeBookmark(articleId: String) {
        if(preferences.accessToken.isEmpty()) {
            return
        }

        try {
            network.removeBookmark(articleId, preferences.accessToken)
        } catch (e: Exception) {
            if (e is NoNetworkError) return
            throw e
        }
    }


}

class CommentsDataFactory(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private var errHandler: (Throwable) -> Unit
) : DataSource.Factory<String?, CommentRes>() {
    override fun create(): DataSource<String?, CommentRes> =
        CommentsDataSource(itemProvider, articleId, totalCount, errHandler)

}

class CommentsDataSource(
    private val itemProvider: RestService,
    private val articleId: String,
    private val totalCount: Int,
    private var errHandler: (Throwable) -> Unit
) : ItemKeyedDataSource<String, CommentRes>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<CommentRes>
    ) {
        try {
            val result = itemProvider.loadComments(
                articleId,
                params.requestedInitialKey,
                params.requestedLoadSize
            ).execute()

            callback.onResult(
                if (totalCount > 0) result.body()!! else emptyList(),
                0,
                totalCount
            )
        } catch (e: Throwable) {
            errHandler(e)
        }
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<CommentRes>) {
        try {
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                params.requestedLoadSize
            ).execute()
            callback.onResult(result.body()!!)
        } catch (e: Throwable) {
            errHandler(e)
        }
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<CommentRes>) {
        try {
            val result = itemProvider.loadComments(
                articleId,
                params.key,
                -params.requestedLoadSize
            ).execute()
            callback.onResult(result.body()!!)
        } catch (e: Throwable) {
            errHandler(e)
        }
    }

    override fun getKey(item: CommentRes): String = item.id

}