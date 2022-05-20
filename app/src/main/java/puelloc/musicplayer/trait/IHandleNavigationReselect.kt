package puelloc.musicplayer.trait

interface IHandleNavigationReselect {
    /**
     * Handle when bottom navigation reselected. Return true if handle and won't pass to parent.
     * If not implemented, will do scroll to top as default behavior
     */
    fun onNavigationReselect(): Boolean
}