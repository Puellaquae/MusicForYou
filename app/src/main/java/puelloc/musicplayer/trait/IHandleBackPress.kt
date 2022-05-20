package puelloc.musicplayer.trait

interface IHandleBackPress {
    /**
     * Return true if handled back press event, and won't pass event to parent.
     * So you should check the child view and let it do onBackPressed first.
     */
    fun onBackPressed(): Boolean
}