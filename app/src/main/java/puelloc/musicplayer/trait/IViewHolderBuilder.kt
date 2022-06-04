package puelloc.musicplayer.trait

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface IViewHolderBuilder<VH : RecyclerView.ViewHolder> {
    fun getViewHolder(parent: ViewGroup, viewType: Int): VH
}