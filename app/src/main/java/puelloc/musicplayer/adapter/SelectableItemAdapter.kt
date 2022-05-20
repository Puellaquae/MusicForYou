package puelloc.musicplayer.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemItemBinding
import puelloc.musicplayer.trait.Equatable

class SelectableItemAdapter<T>(
    recyclerView: RecyclerView,
    private val getItemId: (item: T) -> Long,
    private val getItemTitle: (item: T) -> String,
    private val getItemSubtitle: (item: T) -> String,
    private val getItemImage: (item: T) -> Any,
    @DrawableRes private val defaultItemImage: Int,
    private val onClick: (item: T) -> Unit
) :
    ListAdapter<T, SelectableItemAdapter<T>.ViewHolder>(object :
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
    }) where T : Any, T : Equatable {

    companion object {
        private val TAG = SelectableItemAdapter::class.java.simpleName
    }

    inner class ViewHolder(
        private val itemBinding: ItemItemBinding
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: T, selected: Boolean) {
            itemBinding.apply {
                itemTitle.text = getItemTitle(item)
                itemSubtitle.text = getItemSubtitle(item)
                if (selected) {
                    root.background.setTint(
                        ContextCompat.getColor(root.context, R.color.selected)
                    )
                } else {
                    root.background.setTint(Color.TRANSPARENT)
                }
                Glide
                    .with(root)
                    .load(getItemImage(item))
                    .placeholder(defaultItemImage)
                    .fallback(defaultItemImage)
                    .into(itemImage)
                root.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }

    private val selectionTracker: SelectionTracker<Long>

    init {
        setHasStableIds(true)
        recyclerView.adapter = this
        selectionTracker = SelectionTracker.Builder(
            "selection-$recyclerView",
            recyclerView,
            object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
                override fun getKey(position: Int): Long {
                    return getItemId(position)
                }

                override fun getPosition(key: Long): Int {
                    return currentList.indexOfFirst { getItemId(it) == key }
                }
            },
            object : ItemDetailsLookup<Long>() {
                override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
                    val childView = recyclerView.findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val viewHolder = recyclerView.getChildViewHolder(childView)
                        if (viewHolder is SelectableItemAdapter<*>.ViewHolder) {
                            return object : ItemDetails<Long>() {
                                override fun getPosition(): Int {
                                    return viewHolder.bindingAdapterPosition
                                }

                                override fun getSelectionKey(): Long {
                                    return viewHolder.itemId
                                }
                            }
                        }
                    }
                    return null
                }
            },
            StorageStrategy.createLongStorage()
        ).build()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectionTracker.isSelected(getItemId(item)))
    }

    override fun getItemId(position: Int): Long {
        return getItemId(getItem(position))
    }

    fun clearSelection() {
        selectionTracker.clearSelection()
    }

    fun selectAll() {
        selectionTracker.setItemsSelected(currentList.map { getItemId(it) }, true)
    }

    fun selectionCount() = selectionTracker.selection.size()

    fun hasSelection() = selectionTracker.hasSelection()

    fun addSelectionObserver(selectionObserver: SelectionTracker.SelectionObserver<Long>) {
        selectionTracker.addObserver(selectionObserver)
    }
}