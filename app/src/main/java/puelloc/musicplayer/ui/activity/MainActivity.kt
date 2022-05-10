package puelloc.musicplayer.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.ui.fragment.PlaylistFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                isUserInputEnabled = false

                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount(): Int = 3

                    override fun createFragment(position: Int): Fragment = when (position) {
                        0 -> ForYouFragment()
                        1 -> SongFragment()
                        2 -> PlaylistFragment()
                        else -> throw RuntimeException()
                    }
                }

                registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding.bottomNavigation.menu.getItem(position).isChecked = true
                    }
                })
            }

            bottomNavigation.apply {
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_SELECTED
                setOnItemSelectedListener {
                    when(it.itemId) {
                        R.id.nav_song -> binding.viewPager.setCurrentItem(1, true)
                        R.id.nav_for_you -> binding.viewPager.setCurrentItem(0, true)
                        R.id.nav_playlist -> binding.viewPager.setCurrentItem(2, true)
                    }
                    true
                }
                selectedItemId = R.id.nav_song
            }
        }
    }
}