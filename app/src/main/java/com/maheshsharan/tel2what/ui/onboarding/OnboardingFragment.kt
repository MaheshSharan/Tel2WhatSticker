package com.maheshsharan.tel2what.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.maheshsharan.tel2what.R

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        btnNext = view.findViewById(R.id.btnNext)
        btnSkip = view.findViewById(R.id.btnSkip)

        val adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUI(position)
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < 2) {
                viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }

        updateUI(0)
    }

    private fun updateUI(position: Int) {
        when (position) {
            0, 1 -> {
                btnNext.text = "Next"
                btnSkip.visibility = View.VISIBLE
            }
            2 -> {
                btnNext.text = "Get Started"
                btnSkip.visibility = View.GONE
            }
        }
    }

    private fun completeOnboarding() {
        val prefs = requireContext().getSharedPreferences("tel2what_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
    }
}
