package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import puelloc.musicplayer.adapter.SongAdapter
import puelloc.musicplayer.databinding.FragmentSongBinding
import puelloc.musicplayer.entity.Song

class SongFragment : Fragment() {

    private var _binding: FragmentSongBinding? = null;
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SongAdapter()
        binding.musicList.adapter = adapter

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
        )
        val cursor = requireContext().contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + " != 0",
            null,
            null
        )!!

        val songs = ArrayList<Song>()

        while (cursor.moveToNext()) {
            songs.add(Song(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)))
        }

        cursor.close()

        adapter.submitList(songs)
    }
}