package ru.skillbranch.skillarticles.viewmodels.bookmarks

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class BookmarksViewModel(handle: SavedStateHandle) :
    BaseViewModel<BookmarksState>(handle, BookmarksState()) {
    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(10)
            .setPrefetchDistance(30)
            .setInitialLoadSizeHint(50)
            .build()
    }
    private val repository = ArticlesRepository
    private val listData = Transformations.switchMap(state) {
        when {
            it.isSearch && !it.searchQuery.isNullOrBlank() -> buildPageList(repository.searchBookmarks(it.searchQuery))
            else -> buildPageList(repository.toggleBookmark())
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<ArticleItem>) -> Unit
    ) {
        listData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPageList(
        dataFactory: ArticlesDataFactory
    ): LiveData<PagedList<ArticleItem>> {
        val builder = LivePagedListBuilder<Int, ArticleItem>(
            dataFactory,
            listConfig
        )

//        if (dataFactory.strategy is ArticleStrategy.BookmarkArticles) {
//            builder.setBoundaryCallback(BookmarksBoundaryCallback(
//                ::zeroLoadingHandle,
//                ::itemAtEndHandle
//            ))
//        }

        return builder
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    private fun itemAtEndHandle(lastLoadArticles: ArticleItem) {
        Log.e("BookmarksViewModel", "itemAtEndHandle: ")
        viewModelScope.launch(Dispatchers.IO) {
            val items = repository.loadArticlesFromNetwork(
                start = lastLoadArticles.id.toInt().inc(),
                size = listConfig.pageSize
            )
            if (items.isNotEmpty()) {
                repository.insertArticlesToDb(items)
                listData.value?.dataSource?.invalidate()
            }

            withContext(Dispatchers.Main) {
                notify(
                    Notify.TextMessage("Load from network articles from ${items.firstOrNull()?.id} " +
                        "to ${items.lastOrNull()?.id}"))
            }
        }
    }

    private fun zeroLoadingHandle() {
        Log.e("ArticlesViewModel", "zeroLoadingHandle: ")
        notify(Notify.TextMessage("Storage is empty"))
        viewModelScope.launch(Dispatchers.IO) {
            val items = repository.loadArticlesFromNetwork(start = 0, size = listConfig.initialLoadSizeHint)
            if (items.isNotEmpty()) {
                repository.insertArticlesToDb(items)
                listData.value?.dataSource?.invalidate()
            }
        }
    }

    fun handleSearchMode(isSearch: Boolean) {
        updateState { it.copy(isSearch = isSearch) }
    }

    fun handleSearch(query: String?) {
        query ?: return
        updateState { it.copy(searchQuery = query) }
    }

    fun handleToggleBookmark(id: String){
        repository.toggleBookmark(id)
        listData.value?.dataSource?.invalidate()
    }
}

data class BookmarksState(
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val isLoading: Boolean = true
): IViewModelState

class BookmarksBoundaryCallback(
    private val zeroLoadingHandle: () -> Unit,
    private val itemAtEndHandle: (ArticleItem) -> Unit
): PagedList.BoundaryCallback<ArticleItem>() {
    override fun onZeroItemsLoaded() {
        zeroLoadingHandle()
    }

    override fun onItemAtEndLoaded(itemAtEnd: ArticleItem) {
        itemAtEndHandle(itemAtEnd)
    }

}