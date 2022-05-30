package puelloc.musicplayer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemItemBinding

open class NoDiffItemAdapter<T>(
    val getItemTitle: (item: T) -> String,
    val getItemSubtitle: (item: T) -> String,
    val getItemImage: (item: T) -> Any,
    @DrawableRes val defaultItemImage: Int,
    val data: List<T>,
    private val onClick: (item: T) -> Unit
) :
    RecyclerView.Adapter<NoDiffItemAdapter<T>.ViewHolder>() where T : Any {

    inner class ViewHolder(
        private val itemBinding: ItemItemBinding
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: T, isHighlight: Boolean = false) {
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
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = data.size
}