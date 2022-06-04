package puelloc.musicplayer.ui.viewholder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemItemBinding
import puelloc.musicplayer.trait.BindableAndHighlightableViewHolder
import puelloc.musicplayer.trait.IViewHolderBuilder

class SimpleItemViewHolder<T>(
    val getItemTitle: (item: T) -> String,
    val getItemSubtitle: (item: T) -> String,
    val getItemImage: (item: T) -> Any,
    @DrawableRes val defaultItemImage: Int,
    private val onClick: (item: T) -> Unit,
) : IViewHolderBuilder<SimpleItemViewHolder<T>.ViewHolder> {
    inner class ViewHolder(private val itemBinding: ItemItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), BindableAndHighlightableViewHolder<T> {
        override fun bind(item: T, isHighlight: Boolean) {
            itemBinding.apply {
                if (isHighlight) {
                    root.background.setTint(
                        ContextCompat.getColor(root.context, R.color.selected)
                    )
                } else {
                    root.background.setTint(Color.TRANSPARENT)
                }
                itemTitle.text = getItemTitle(item)
                itemSubtitle.text = getItemSubtitle(item)
                val image = getItemImage(item)
                if (image is Int) {
                    // https://github.com/bumptech/glide/issues/3778
                    Glide
                        .with(root)
                        .asDrawable()
                        .load(ContextCompat.getDrawable(root.context, image))
                        .placeholder(defaultItemImage)
                        .fallback(defaultItemImage)
                        .into(itemImage)
                } else {
                    Glide
                        .with(root)
                        .load(image)
                        .theme(root.context.theme)
                        .placeholder(defaultItemImage)
                        .fallback(defaultItemImage)
                        .into(itemImage)
                }
                root.setOnClickListener {
                    onClick(item)
                }
            }
        }
    }

    override fun getViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
}