package com.example.doomflame.icon_select

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.doomflame.R
import io.reactivex.rxjava3.processors.PublishProcessor

data class IconSelectItem(val title: String, val payload: Int)

class IconSelectViewHolder(
    root: ViewGroup,
    private val clickStream: PublishProcessor<Int>,
) : ViewHolder(
    LayoutInflater.from(root.context).inflate(R.layout.item_icon_select, root, false)
) {
    private val button = itemView as AppCompatButton
    private var item: IconSelectItem? = null

    init {
        button.setOnClickListener {
            item?.payload?.let {
                clickStream.offer(it)
            }
        }
    }

    fun bind(item: IconSelectItem) {
        this.item = item
        button.text = item.title
    }
}

private object IconSelectCallback : ItemCallback<IconSelectItem>() {
    override fun areItemsTheSame(oldItem: IconSelectItem, newItem: IconSelectItem): Boolean =
        false

    override fun areContentsTheSame(oldItem: IconSelectItem, newItem: IconSelectItem): Boolean =
        oldItem == newItem
}

class IconSelectAdapter(
    private val clickStream: PublishProcessor<Int>,
) : ListAdapter<IconSelectItem, IconSelectViewHolder>(IconSelectCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconSelectViewHolder =
        IconSelectViewHolder(parent, clickStream)

    override fun onBindViewHolder(holder: IconSelectViewHolder, position: Int) =
        holder.bind(getItem(position))
}