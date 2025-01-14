package co.netguru.baby.monitor.client.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import co.netguru.baby.monitor.client.BuildConfig
import co.netguru.baby.monitor.client.R
import co.netguru.baby.monitor.client.common.base.BaseFragment
import co.netguru.baby.monitor.client.feature.communication.websocket.MessageController
import co.netguru.baby.monitor.client.feature.server.ServerViewModel
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_server_settings.*

class ServerSettingsFragment : BaseFragment() {
    override val layoutResource = R.layout.fragment_server_settings

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val configurationViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ConfigurationViewModel::class.java]
    }
    private val serverViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ServerViewModel::class.java]
    }
    private val settingsViewModel by lazy {
        ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
    }

    private fun setupObservers() {
        configurationViewModel.resetState.observe(
            viewLifecycleOwner,
            Observer { resetState ->
                when (resetState) {
                    is ChangeState.InProgress -> setupResetButton(true)
                    is ChangeState.Failed -> setupResetButton(false)
                }
            },
        )
    }

    private fun setupViews() {
        sendRecordingsSw.isChecked = configurationViewModel.isUploadEnabled()

        rateUsBtn.setOnClickListener {
            settingsViewModel.openMarket(requireActivity())
        }

        secondPartTv.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.company_url))))
        }

        closeIbtn.setOnClickListener {
            serverViewModel.toggleDrawer(false)
        }

        resetAppBtn.setOnClickListener {
            resetApp()
        }

        sendRecordingsSw.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.setUploadEnabled(isChecked)
        }

        version.text =
            getString(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }

    private fun resetApp() {
        configurationViewModel.resetApp(requireActivity() as? MessageController)
    }

    private fun setupResetButton(resetInProgress: Boolean) {
        resetAppBtn.apply {
            isClickable = !resetInProgress
            text = if (resetInProgress) "" else resources.getString(R.string.reset)
        }
        resetProgressBar.isVisible = resetInProgress
    }
}
