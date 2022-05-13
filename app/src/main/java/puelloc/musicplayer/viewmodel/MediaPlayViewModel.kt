package puelloc.musicplayer.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MediaPlayViewModel: ViewModel() {
    val singId = MutableLiveData<Long>()
}