package puelloc.musicplayer.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.trait.BindableAndHighlightableViewHolder
import puelloc.musicplayer.trait.Equatable
import puelloc.musicplayer.trait.IViewHolderBuilder

open class ItemAdapter<T, VH>(
    getItemId: (item: T) -> Long,
    private val viewHolderBuilder: IViewHolderBuilder<VH>
) :
    AsyncDiffAdapter<T, VH>(getItemId)
        where T : Any,
              T : Equatable,
              VH : RecyclerView.ViewHolder,
              VH : BindableAndHighlightableViewHolder<T> {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return viewHolderBuilder.getViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun getItemId(position: Int): Long {
        return getItemId(getItem(position))
    }
}