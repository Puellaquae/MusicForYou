package puelloc.musicplayer.adapter

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.BindableAndHighlightableViewHolder
import puelloc.musicplayer.trait.Equatable
import puelloc.musicplayer.trait.IViewHolderBuilder

/**
 * this will auto set adapter to recyclerView
 */
class SelectableItemAdapter<T, VH>(
    recyclerView: RecyclerView,
    getItemId: (item: T) -> Long,
    viewHolderBuilder: IViewHolderBuilder<VH>
) :
    ItemAdapter<T, VH>(getItemId, viewHolderBuilder)
        where T : Any,
              T : Equatable,
              VH : RecyclerView.ViewHolder,
              VH : BindableAndHighlightableViewHolder<T> {

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
                        return object : ItemDetails<Long>() {
                            override fun getPosition(): Int {
                                return viewHolder.bindingAdapterPosition
                            }

                            override fun getSelectionKey(): Long {
                                return viewHolder.itemId
                            }
                        }
                    }
                    return null
                }
            },
            StorageStrategy.createLongStorage()
        ).build()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectionTracker.isSelected(getItemId(item)))
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

    fun getSelection(): List<Long> = selectionTracker.selection.toList()
}