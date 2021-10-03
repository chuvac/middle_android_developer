package ru.skillbranch.skillarticles.ui.dialogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.R


class CategoryAdapter(private val listener: (String, Boolean) -> Unit):
ListAdapter<CategoryDataItem, CategoryVH>(CategoryDiffCallback()){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH = CategoryVH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_category_dialog, parent, false),
        listener
    )

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        holder.bind(getItem(position))
    }
}

class CategoryVH(override val containerView: View, val listener: (String, Boolean) -> Unit):
        RecyclerView.ViewHolder(containerView), LayoutInflater {
            fun  bind(item: CategoryDataItem) {
                ch_select.senOnCheckedChangeListener(null)

            }
        }