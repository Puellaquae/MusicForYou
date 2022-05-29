package puelloc.musicplayer.adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.Equatable

abstract class ReorderableItemAdapter<T>(
    recyclerView: RecyclerView,
    getItemId: (T) -> Long,
    getItemTitle: (T) -> String,
    getItemSubtitle: (T) -> String,
    getItemImage: (T) -> Any,
    defaultItemImage: Int,
    onClick: (T) -> Unit
) : ItemAdapter<T>(
    getItemId,
    getItemTitle,
    getItemSubtitle,
    getItemImage,
    defaultItemImage,
    onClick
) where T : Any, T : Equatable {
    private val itemTouchCallback =
        object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
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
                endPosition = toPosition
                notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No Need implementation
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    startPosition = viewHolder.bindingAdapterPosition
                }
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    preventNextTimeDiffCallback = true
                    swap(startPosition, endPosition)
                }
            }
        }
    private val itemTouchHelper = ItemTouchHelper(itemTouchCallback)

    init {
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    abstract fun swap(fromPosition: Int, toPosition: Int)
}