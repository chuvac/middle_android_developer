package ru.skillbranch.skillarticles.ui.articles

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.ui.custom.ArticleItemView

class ArticlesAdapter(
    private val listener: (ArticleItem) -> Unit,
    private val bookmarkListener: (String, Boolean) -> Unit):
    PagedListAdapter<ArticleItem, ArticleVH>(ArticleDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleVH {
        val containerView = ArticleItemView(parent.context)
        return ArticleVH(containerView)
    }

    override fun onBindViewHolder(holder: ArticleVH, position: Int) {
        holder.bind(getItem(position), listener, bookmarkListener)
    }
}

class ArticleDiffCallback: DiffUtil.ItemCallback<ArticleItem>() {
    override fun areItemsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ArticleItem, newItem: ArticleItem): Boolean = oldItem == newItem
}

class ArticleVH(private val containerView: View): RecyclerView.ViewHolder(containerView) {
    fun bind(
        item: ArticleItem?,
        listener: (ArticleItem) -> Unit,
        bookmarkListener: (String, Boolean) -> Unit
    ) {
        //if use placeholder item may be null
        (containerView as ArticleItemView).bind(item!!) { id, isBookmark ->
            bookmarkListener(id, isBookmark)
        }
        itemView.setOnClickListener {listener(item!!)}
    }
}