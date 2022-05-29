package puelloc.musicplayer.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.Equatable

abstract class DiffListAdapter<T, VH : RecyclerView.ViewHolder>(val getItemId: (item: T) -> Long) :
    ListAdapter<T, VH>(object :
        DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(
            oldItem: T,
            newItem: T
        ): Boolean =
            getItemId(oldItem) == getItemId(newItem)

        override fun areContentsTheSame(
            oldItem: T,
            newItem: T
        ): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) where T : Any, T : Equatable