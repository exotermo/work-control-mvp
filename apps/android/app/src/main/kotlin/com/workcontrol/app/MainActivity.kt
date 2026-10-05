package com.workcontrol.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.workcontrol.app.core.designsystem.WorkControlTheme
import com.workcontrol.app.data.auth.AuthSession
import com.workcontrol.app.data.prelo.PreloApi
import com.workcontrol.app.feature.session.SessionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var session: AuthSession
    @Inject lateinit var prelo: PreloApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkControlTheme { SessionScreen(this, session, prelo) }
        }
    }
}
