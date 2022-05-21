package puelloc.musicplayer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.databinding.ItemItemBinding
import puelloc.musicplayer.trait.Equatable

class ItemAdapter<T>(
    val getItemId: (item: T) -> Long,
    val getItemTitle: (item: T) -> String,
    val getItemSubtitle: (item: T) -> String,
    val getItemImage: (item: T) -> Any,
    @DrawableRes val defaultItemImage: Int,
    private val onClick: (item: T) -> Unit
) :
    ListAdapter<T, ItemAdapter<T>.ViewHolder>(object :
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

    inner class ViewHolder(
        private val itemBinding: ItemItemBinding
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: T) {
            itemBinding.apply {
                root.background.setTint(Color.TRANSPARENT)
                itemTitle.text = getItemTitle(item)
                itemSubtitle.text = getItemSubtitle(item)
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
        holder.bind(item)
    }

    override fun getItemId(position: Int): Long {
        return getItemId(getItem(position))
    }
}