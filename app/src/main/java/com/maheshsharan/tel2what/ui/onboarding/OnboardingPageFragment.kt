package com.maheshsharan.tel2what.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.maheshsharan.tel2what.R

class OnboardingPageFragment : Fragment(R.layout.fragment_onboarding_page) {

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): OnboardingPageFragment {
            val fragment = OnboardingPageFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = arguments?.getInt(ARG_POSITION) ?: 0

        val imgIllustration: ImageView = view.findViewById(R.id.imgIllustration)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtDescription: TextView = view.findViewById(R.id.txtDescription)

        when (position) {
            0 -> {
                imgIllustration.setImageResource(R.drawable.onboarding1)
                txtTitle.text = "Import Made Easy"
                txtDescription.text = "Simply paste any Telegram sticker pack link and import your favorite stickers in seconds"
            }
            1 -> {
                imgIllustration.setImageResource(R.drawable.onboarding2)
                txtTitle.text = "Fast & Offline"
                txtDescription.text = "All processing happens on your device. No internet required after download. Your privacy matters"
            }
            2 -> {
                imgIllustration.setImageResource(R.drawable.onboarding3)
                txtTitle.text = "WhatsApp Ready"
                txtDescription.text = "Select up to 30 stickers, customize your pack, and export directly to WhatsApp with one tap"
            }
        }
    }
}
