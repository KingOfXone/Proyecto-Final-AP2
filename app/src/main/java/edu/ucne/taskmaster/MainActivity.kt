package edu.ucne.taskmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import edu.ucne.taskmaster.navigation.NavHostApp
import edu.ucne.taskmaster.presentation.sign_in.GoogleAuthUiClient
import edu.ucne.taskmaster.ui.theme.TaskMasterTheme
import edu.ucne.taskmaster.util.BottomNavigationBar
import com.google.android.gms.auth.api.identity.Identity

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskMasterTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                BottomNavigationBar(navController = navController) {
                    NavHostApp(navHostController = navController, drawerState = drawerState, googleAuthUiClient = googleAuthUiClient)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TaskMasterTheme {
    }
}