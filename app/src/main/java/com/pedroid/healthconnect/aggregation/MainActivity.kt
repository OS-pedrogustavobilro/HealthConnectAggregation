package com.pedroid.healthconnect.aggregation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.pedroid.healthconnect.aggregation.ui.theme.HealthConnectAggregationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val LOG_TAG = "HealthConnectAggr"

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            Log.d(LOG_TAG, "granted permissions - $granted")
        }

    private val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission<HeartRateRecord>(),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthConnectAggregationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Screen(
                        modifier = Modifier.padding(innerPadding),
                        onButtonClick = this::aggregate
                    )
                }
            }
        }
        healthConnectClient = HealthConnectClient.getOrCreate(this)
        requestPermission()
    }

    private fun requestPermission() {
        lifecycleScope.launch {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            if (!grantedPermissions.containsAll(permissions)) {
                requestPermissions.launch(permissions)
            }
        }
    }

    private fun aggregate() {
        lifecycleScope.launch(Dispatchers.Default) {
            val splitNumber = 20
            val startDate = LocalDateTime.now().minusMonths(6L).truncatedTo(ChronoUnit.HOURS)
            val endDate = LocalDateTime.now()
            val totalDuration = Duration.between(startDate, endDate)
            val stepSeconds = totalDuration.seconds / (splitNumber - 1)
            var splitStartDate = startDate
            (1..splitNumber).forEach { index ->
                val splitEndDate =
                    splitStartDate.plusSeconds(stepSeconds).truncatedTo(ChronoUnit.HOURS)
                Log.d(LOG_TAG, "$index, splitStart=$splitStartDate; splitEnd=$splitEndDate")
                healthConnectClient.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG),
                        timeRangeFilter = TimeRangeFilter.Companion.between(
                            splitStartDate,
                            splitEndDate
                        ),
                        timeRangeSlicer = Duration.ofHours(1)
                    )
                )
                splitStartDate = splitEndDate
            }
        }
    }
}

@Composable
fun Screen(
    modifier: Modifier = Modifier,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Testing Health Connect Aggregation By Duration (Hour) - Crashes in Android 14+\n\nThis example uses Heart Rate. You can change the source code to request a different kind of data",
        )
        Button(onClick = onButtonClick) {
            Text("Click to Test Aggregate Health Data")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HealthConnectAggregationTheme {
        Screen(modifier = Modifier, onButtonClick = {})
    }
}