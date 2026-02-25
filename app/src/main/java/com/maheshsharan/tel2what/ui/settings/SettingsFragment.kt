package com.maheshsharan.tel2what.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.maheshsharan.tel2what.BuildConfig
import com.maheshsharan.tel2what.R

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val txtVersion: TextView = view.findViewById(R.id.txtVersion)
        val cardGithub: MaterialCardView = view.findViewById(R.id.cardGithub)
        val cardPrivacyPolicy: MaterialCardView = view.findViewById(R.id.cardPrivacyPolicy)
        val cardTerms: MaterialCardView = view.findViewById(R.id.cardTerms)
        val cardLicenses: MaterialCardView = view.findViewById(R.id.cardLicenses)

        // Set version
        txtVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        cardGithub.setOnClickListener {
            openUrl("https://github.com/MaheshSharan")
        }

        cardPrivacyPolicy.setOnClickListener {
            openUrl("https://maheshsharan.github.io/Tel2WhatSticker/privacy-policy.html")
        }

        cardTerms.setOnClickListener {
            openUrl("https://maheshsharan.github.io/Tel2WhatSticker/terms-of-service.html")
        }

        cardLicenses.setOnClickListener {
            openUrl("https://maheshsharan.github.io/Tel2WhatSticker/open-source-licenses.html")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
