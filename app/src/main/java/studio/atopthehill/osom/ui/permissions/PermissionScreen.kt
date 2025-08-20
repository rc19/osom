package studio.atopthehill.osom.ui.permissions

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Multi-step permission wizard that guides users through all required permissions.
 * 
 * This replaces the single-permission screen to ensure users grant all necessary
 * permissions during onboarding, preventing issues with missing functionality.
 */
@Composable
fun PermissionScreen(
    navigateToNextScreen: () -> Unit,
    viewModel: PermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // State to track which permission step we're on
    var currentStep by remember { mutableStateOf(0) }
    
    // Check if all permissions are already granted
    LaunchedEffect(Unit) {
        if (viewModel.areAllPermissionsGranted(context)) {
            navigateToNextScreen()
        }
    }

    // Permission steps data
    val permissionSteps = listOf(
        PermissionStep(
            title = "Accessibility Service",
            icon = Icons.Default.Accessibility,
            description = "Monitor screen content from your selected apps to detect tasks and reminders",
            detailText = "This allows Osom to read text from apps you choose to monitor. Your data never leaves your device.",
            isGranted = { viewModel.isAccessibilityPermissionGranted(context) },
            onRequest = { 
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                // We'll handle the launcher in the UI
            }
        ),
        PermissionStep(
            title = "Usage Stats Access",
            icon = Icons.Default.AccessTime,
            description = "Track your app usage time to provide insights and better reminders",
            detailText = "This helps Osom understand your app usage patterns to give more relevant suggestions.",
            isGranted = { viewModel.isUsageStatsPermissionGranted(context) },
            onRequest = { 
                // Will be handled by the launcher
            }
        ),
        PermissionStep(
            title = "Display Over Other Apps",
            icon = Icons.Default.Visibility,
            description = "Show reminders and notifications over other apps when needed",
            detailText = "This allows Osom to show important reminders even when you're using other apps.",
            isGranted = { viewModel.isOverlayPermissionGranted(context) },
            onRequest = { 
                // Will be handled by the launcher
            }
        ),
        PermissionStep(
            title = "Show Notifications",
            icon = Icons.Default.Notifications,
            description = "Send important notifications like app update reminders",
            detailText = "This allows Osom to notify you when permissions need to be restored after app updates.",
            isGranted = { viewModel.isNotificationPostingPermissionGranted(context) },
            onRequest = { 
                // Will be handled by the launcher with ActivityCompat.requestPermissions
            }
        )
    )

    // Generic launcher that can handle different permission types
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if current permission was granted, if so move to next step
        if (currentStep < permissionSteps.size && permissionSteps[currentStep].isGranted()) {
            if (currentStep < permissionSteps.size - 1) {
                currentStep++
            } else {
                // All permissions granted
                navigateToNextScreen()
            }
        }
    }

    // Check permissions status and auto-advance if granted
    LaunchedEffect(currentStep) {
        // Skip already granted permissions
        while (currentStep < permissionSteps.size && permissionSteps[currentStep].isGranted()) {
            currentStep++
        }
        
        // If we've gone through all steps, navigate to next screen
        if (currentStep >= permissionSteps.size) {
            navigateToNextScreen()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        
        // Progress indicator
        ProgressHeader(
            currentStep = currentStep,
            totalSteps = permissionSteps.size
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Current permission step
        if (currentStep < permissionSteps.size) {
            val currentPermission = permissionSteps[currentStep]
            
            PermissionStepCard(
                step = currentPermission,
                onGrantClick = {
                    when (currentStep) {
                        0 -> { // Accessibility
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            permissionLauncher.launch(intent)
                        }
                        1 -> { // Usage Stats
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            permissionLauncher.launch(intent)
                        }
                        2 -> { // Overlay
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            permissionLauncher.launch(intent)
                        }
                        3 -> { // POST_NOTIFICATIONS (This was the bug - step 3 should be POST_NOTIFICATIONS, not notification listener)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                // Use the regular permission request launcher for POST_NOTIFICATIONS
                                ActivityCompat.requestPermissions(
                                    context as android.app.Activity,
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    101
                                )
                            }
                            // For older versions, this permission is granted automatically
                            // Check if permission was granted and advance
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == 
                                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                
                                if (currentStep < permissionSteps.size - 1) {
                                    currentStep++
                                } else {
                                    navigateToNextScreen()
                                }
                            }
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permission status overview
        PermissionStatusOverview(
            steps = permissionSteps,
            currentStep = currentStep
        )
    }
}

/**
 * Data class representing a permission step in the wizard.
 */
data class PermissionStep(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val detailText: String,
    val isGranted: () -> Boolean,
    val onRequest: () -> Unit
)

/**
 * Header showing progress through permission steps.
 */
@Composable
private fun ProgressHeader(
    currentStep: Int,
    totalSteps: Int
) {
    Column {
        Text(
            text = "Set up permissions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Step ${minOf(currentStep + 1, totalSteps)} of $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = (currentStep + 1).toFloat() / totalSteps.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Card displaying information about a single permission step.
 */
@Composable
private fun PermissionStepCard(
    step: PermissionStep,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Permission title
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Permission description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Detailed explanation
            Text(
                text = step.detailText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action button (no skip option - all permissions required)
            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note about required permissions
            Text(
                text = "This permission is required for Osom to function properly",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Overview showing the status of all permission steps.
 */
@Composable
private fun PermissionStatusOverview(
    steps: List<PermissionStep>,
    currentStep: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permission Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (step.isGranted()) Icons.Default.CheckCircle else step.icon,
                        contentDescription = null,
                        tint = if (step.isGranted()) {
                            MaterialTheme.colorScheme.primary
                        } else if (index == currentStep) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (step.isGranted()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else if (index == currentStep) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (step.isGranted()) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}