package puelloc.musicplayer.adapter

import androidx.recyclerview.widget.*
import puelloc.musicplayer.trait.Equatable

abstract class AsyncDiffAdapter<T, VH>(
    val getItemId: (item: T) -> Long
) :
    RecyclerView.Adapter<VH>()
        where T : Any,
              T : Equatable,
              VH : RecyclerView.ViewHolder {
    private val diffCallback: DiffUtil.ItemCallback<T> = object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
            getItemId(oldItem) == getItemId(newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }

    protected var preventNextTimeDiffCallback = false

    private val differ = AsyncListDiffer(
        object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                if (!preventNextTimeDiffCallback) {
                    notifyItemRangeInserted(position, count)
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                if (!preventNextTimeDiffCallback) {
                    notifyItemRangeRemoved(position, count)
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                if (!preventNextTimeDiffCallback) {
                    notifyItemMoved(fromPosition, toPosition)
                }
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                if (!preventNextTimeDiffCallback) {
                    notifyItemRangeChanged(position, count, payload)
                }
            }
        },
        AsyncDifferConfig.Builder(diffCallback).build()
    )

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    val currentList: List<T> get() = differ.currentList

    open fun submitList(data: List<T>) {
        differ.submitList(data) {
            preventNextTimeDiffCallback = false
        }
    }

    open fun getItem(position: Int): T {
        return differ.currentList[position]
    }
}