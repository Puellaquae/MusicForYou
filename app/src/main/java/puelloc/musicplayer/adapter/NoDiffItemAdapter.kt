package puelloc.musicplayer.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.BindableAndHighlightableViewHolder
import puelloc.musicplayer.trait.IViewHolderBuilder

open class NoDiffItemAdapter<T, VH>(
    val data: List<T>,
    private val viewHolderBuilder: IViewHolderBuilder<VH>
) :
    RecyclerView.Adapter<VH>()
        where T : Any,
              VH : RecyclerView.ViewHolder,
              VH : BindableAndHighlightableViewHolder<T> {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return viewHolderBuilder.getViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = data.size
}