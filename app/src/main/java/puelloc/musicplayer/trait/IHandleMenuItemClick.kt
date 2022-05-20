package puelloc.musicplayer.trait

import android.view.MenuItem

interface IHandleMenuItemClick {
    fun onMenuItemClicked(menuItem: MenuItem): Boolean
}