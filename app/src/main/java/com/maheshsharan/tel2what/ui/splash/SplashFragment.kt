package com.maheshsharan.tel2what.ui.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.maheshsharan.tel2what.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Animate splash elements
        val logo = view.findViewById<View>(R.id.splashLogo)
        val title = view.findViewById<View>(R.id.splashTitle)
        val subtitle = view.findViewById<View>(R.id.splashSubtitle)
        val version = view.findViewById<View>(R.id.splashVersion)

        // Set initial alpha to 0
        logo?.alpha = 0f
        title?.alpha = 0f
        subtitle?.alpha = 0f
        version?.alpha = 0f

        // Animate logo
        logo?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(600)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()

        // Animate title
        title?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setStartDelay(200)
            ?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()

        // Animate subtitle
        subtitle?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setStartDelay(300)
            ?.setDuration(500)
            ?.setInterpolator(android.view.animation.DecelerateInterpolator())
            ?.start()

        // Animate version text
        version?.animate()
            ?.alpha(0.8f) // max alpha is 0.8
            ?.setStartDelay(500)
            ?.setDuration(400)
            ?.start()

        viewLifecycleOwner.lifecycleScope.launch {
            // Splash screen duration: 1.5 seconds
            delay(1500)
            
            // Check if onboarding is completed
            val prefs = requireContext().getSharedPreferences("tel2what_prefs", android.content.Context.MODE_PRIVATE)
            val isFirstLaunch = !prefs.getBoolean("onboarding_complete", false)
            
            if (isFirstLaunch) {
                findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
            }
        }
    }
}
