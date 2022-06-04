

package puelloc.musicplayer.trait

@Suppress("SpellCheckingInspection")
interface BindableAndHighlightableViewHolder<T> {
    fun bind(item: T, isHighlight: Boolean = false)
}