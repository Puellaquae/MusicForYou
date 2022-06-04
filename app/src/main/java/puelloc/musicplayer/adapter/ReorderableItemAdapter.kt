package puelloc.musicplayer.adapter

import android.icu.text.Transliterator
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.BindableAndHighlightableViewHolder
import puelloc.musicplayer.trait.Equatable
import puelloc.musicplayer.trait.IViewHolderBuilder

abstract class ReorderableItemAdapter<T, VH>(
    recyclerView: RecyclerView,
    getItemId: (T) -> Long,
    viewHolderBuilder: IViewHolderBuilder<VH>
) : ItemAdapter<T, VH>(
    getItemId,
    viewHolderBuilder
) where T : Any,
        T : Equatable,
        VH : RecyclerView.ViewHolder,
        VH : BindableAndHighlightableViewHolder<T> {
    private val itemTouchCallback =
        object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.END
                )
            }

            private var startPosition = -1
            private var endPosition = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                Log.d("ReorderableItemAdapter", "$fromPosition to $endPosition")
                endPosition = toPosition
                notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                remove(position)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    startPosition = viewHolder.bindingAdapterPosition
                    endPosition = -1
                }
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (startPosition != -1 && endPosition != -1) {
                        preventNextTimeDiffCallback = true
                        swap(startPosition, endPosition)
                    }
                }
            }
        }
    private val itemTouchHelper = ItemTouchHelper(itemTouchCallback)

    init {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    abstract fun swap(fromPosition: Int, toPosition: Int)
    abstract fun remove(position: Int)
}