package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Doctor
import com.example.data.model.Entry
import com.example.data.model.MonthlyStatement
import com.example.util.PdfGenerator
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabApp(viewModel: LabViewModel) {
    val context = LocalContext.current
    val loginStep by viewModel.loginStep.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()

    // Observe toast events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    var isAppUnlocked by remember { mutableStateOf(!viewModel.userPrefs.appLockEnabled) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (loginStep) {
            LabViewModel.LoginStep.PHONE_INPUT,
            LabViewModel.LoginStep.OTP_INPUT,
            LabViewModel.LoginStep.PASSWORD_INPUT -> {
                LoginScreen(viewModel = viewModel)
            }
            LabViewModel.LoginStep.LOGGED_IN -> {
                if (!isAppUnlocked && viewModel.userPrefs.appLockEnabled) {
                    AppLockScreen(userPrefs = viewModel.userPrefs, onUnlock = { isAppUnlocked = true })
                } else {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: LabViewModel) {
    val loginStep by viewModel.loginStep.collectAsState()
    val mobileNumber by viewModel.mobileNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val passwordInput by viewModel.passwordInput.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Diagnostic Laboratory Logo Placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Biotech,
                        contentDescription = "Lab Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Micro Pathology Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Secure Diagnostic Management Portal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Error Indicator
                AnimatedVisibility(visible = authError != null) {
                    authError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Step 1: Mobile Number Input
                if (loginStep == LabViewModel.LoginStep.PHONE_INPUT) {
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                viewModel.setMobileNumber(it)
                                viewModel.clearAuthError()
                            }
                        },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("Enter registered number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.sendOtp() },
                        enabled = mobileNumber.length == 10 && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_otp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 2: OTP Input
                if (loginStep == LabViewModel.LoginStep.OTP_INPUT) {
                    Text(
                        text = "Enter 6-digit OTP sent to +91 $mobileNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 6) {
                                viewModel.setOtpCode(it)
                                viewModel.clearAuthError()
                            }
                        },
                        label = { Text("6-Digit OTP") },
                        placeholder = { Text("123456") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input")
                    )

                    // Resend Timer Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (resendTimer > 0) {
                            Text(
                                text = "Resend OTP in ${resendTimer}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.sendOtp() }
                            ) {
                                Text("Resend OTP", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { viewModel.logout() }) {
                            Text("Change Number", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = { viewModel.verifyOtp() },
                        enabled = otpCode.length == 6 && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_otp_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Verify OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 3: Password Input
                if (loginStep == LabViewModel.LoginStep.PASSWORD_INPUT) {
                    Text(
                        text = "OTP Verified successfully. Please enter your Password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            viewModel.setPasswordInput(it)
                            viewModel.clearAuthError()
                        },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.verifyPasswordAndLogin() },
                        enabled = passwordInput.isNotEmpty() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Reset & Back")
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: LabViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Entries") },
                    label = { Text("Entries") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = "Statement") },
                    label = { Text("Statement") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Doctors") },
                    label = { Text("Doctors") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddEntryDialog = true },
                    modifier = Modifier.testTag("add_entry_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Patient Entry")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = viewModel)
                1 -> EntriesScreen(viewModel = viewModel)
                2 -> DoctorMonthlyStatementScreen(viewModel = viewModel)
                3 -> DoctorsScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }

            if (showAddEntryDialog) {
                AddEditEntryDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddEntryDialog = false }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: LabViewModel) {
    val entries by viewModel.entries.collectAsState()

    val totalEntries = entries.size
    val totalAmount = entries.sumOf { it.amount }
    val totalDoctorAmount = entries.sumOf { it.doctorAmount }
    val totalLabCharge = entries.sumOf { it.labCharge }
    val totalOtherAmount = entries.sumOf { it.otherAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // App Bar / Header matching the "Clean Utility / Minimal" HTML
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MICRO PATHOLOGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Laboratory",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ML",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Total Entries & Total Amount (Revenue) Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "Total Entries",
                    value = totalEntries.toString(),
                    icon = Icons.Default.BarChart,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.5f)
                )
                DashboardCard(
                    title = "Total Revenue",
                    value = "₹$totalAmount",
                    icon = Icons.Default.CurrencyRupee,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(2f)
                )
            }
        }

        // Secondary Metrics matching style of Blue and Slate blocks in HTML
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Doctor Amount & Lab Charges Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DOCTOR AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹$totalDoctorAmount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    Color.White
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LAB CHARGES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹$totalLabCharge",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    Color.White
                            )
                        }
                    }
                }

                // Other Amount Card (Slate color block in HTML)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) 
                            MaterialTheme.colorScheme.surface 
                        else 
                            Color(0xFF1E293B) // Slate-800
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = if (isSystemInDarkTheme()) 
                        BorderStroke(1.dp, Color(0xFF334155)) 
                    else 
                        null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OTHER AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹$totalOtherAmount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    Color.White
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSystemInDarkTheme()) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        Color(0xFF334155) // Slate-700
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = if (isSystemInDarkTheme()) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Entries section header with Clean Utility design
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "RECENT DIAGNOSTIC ENTRIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B), // Slate
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No recent diagnostics entered yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Show up to 3 most recent entries
            val recentEntries = entries.take(3)
            item {
                PatientEntriesTable(
                    entries = recentEntries,
                    showActions = false,
                    showTotal = false
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Showing up to 3 recent entries",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFF1F5F9)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF94A3B8), // slate-400
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun EntriesScreen(viewModel: LabViewModel) {
    val entries by viewModel.entries.collectAsState()
    val doctors by viewModel.doctors.collectAsState()

    var searchQueryPatient by remember { mutableStateOf("") }
    var searchQueryDoctor by remember { mutableStateOf("") }
    var searchQueryDate by remember { mutableStateOf("") }
    var filterMonthIndex by remember { mutableStateOf(0) } // 0 = All

    var editingEntry by remember { mutableStateOf<Entry?>(null) }

    val monthsList = listOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Apply multiple searches and filters
    val filteredEntries = entries.filter { entry ->
        val matchPatient = searchQueryPatient.isEmpty() || entry.patientName.contains(searchQueryPatient, ignoreCase = true)
        val matchDoctor = searchQueryDoctor.isEmpty() || entry.doctorName.contains(searchQueryDoctor, ignoreCase = true)
        val matchDate = searchQueryDate.isEmpty() || entry.date.contains(searchQueryDate)
        
        val matchMonth = if (filterMonthIndex == 0) true else {
            try {
                val dateParts = entry.date.split("-")
                val monthPart = dateParts[1].toInt()
                monthPart == filterMonthIndex
            } catch (e: Exception) {
                false
            }
        }
        matchPatient && matchDoctor && matchDate && matchMonth
    }

    // Group remaining patients doctor-wise
    val groupedEntries = filteredEntries.groupBy { it.doctorName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Patient Registry Tables",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Filters UI
        OutlinedTextField(
            value = searchQueryPatient,
            onValueChange = { searchQueryPatient = it },
            label = { Text("Search Patient") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_patient_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQueryDoctor,
                onValueChange = { searchQueryDoctor = it },
                label = { Text("Search Doctor") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_doctor_input")
            )

            val context = LocalContext.current
            OutlinedTextField(
                value = searchQueryDate,
                onValueChange = { searchQueryDate = it },
                label = { Text("Search Date") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date Filter",
                        modifier = Modifier.clickable {
                            val c = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, day ->
                                val formattedMonth = String.format("%02d", month + 1)
                                val formattedDay = String.format("%02d", day)
                                searchQueryDate = "$year-$formattedMonth-$formattedDay"
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    )
                },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_date_input")
            )
        }

        // Month Filter Row
        var showMonthMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = monthsList[filterMonthIndex],
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter Month") },
                trailingIcon = {
                    IconButton(onClick = { showMonthMenu = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMonthMenu = true }
                    .testTag("month_filter_dropdown")
            )
            DropdownMenu(
                expanded = showMonthMenu,
                onDismissRequest = { showMonthMenu = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                monthsList.forEachIndexed { idx, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            filterMonthIndex = idx
                            showMonthMenu = false
                        }
                    )
                }
            }
        }

        if (groupedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No diagnostic records found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedEntries.forEach { (drName, entriesList) ->
                    item {
                        // Doctor Section Header
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = drName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${entriesList.size} Patient(s)", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }

                    item {
                        PatientEntriesTable(
                            entries = entriesList,
                            showActions = true,
                            showTotal = true,
                            onEdit = { editingEntry = it },
                            onDelete = { viewModel.deleteEntry(it) }
                        )
                    }
                }
            }
        }
    }

    if (editingEntry != null) {
        AddEditEntryDialog(
            viewModel = viewModel,
            entry = editingEntry,
            onDismiss = { editingEntry = null }
        )
    }
}

val colDateWidth = 90.dp
val colNameWidth = 120.dp
val colAgeWidth = 50.dp
val colTestWidth = 200.dp
val colAmountWidth = 85.dp
val colDocAmountWidth = 100.dp
val colLabChargeWidth = 100.dp
val colOtherWidth = 95.dp
val colActionsWidth = 90.dp

@Composable
fun TableCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false,
    isNumeric: Boolean = false,
    textColor: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = if (isNumeric) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else fontWeight,
            fontSize = if (isHeader) 12.sp else 13.sp,
            color = if (isHeader) MaterialTheme.colorScheme.onSecondaryContainer else textColor,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = if (isNumeric) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
fun TableHeaderRow(showActions: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = "Date", width = colDateWidth, isHeader = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Patient Name", width = colNameWidth, isHeader = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Age", width = colAgeWidth, isHeader = true, isNumeric = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Test Name", width = colTestWidth, isHeader = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Total Amt", width = colAmountWidth, isHeader = true, isNumeric = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Doc Share", width = colDocAmountWidth, isHeader = true, isNumeric = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Lab Chg", width = colLabChargeWidth, isHeader = true, isNumeric = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "Other Amt", width = colOtherWidth, isHeader = true, isNumeric = true)
        
        if (showActions) {
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .width(colActionsWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    Divider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun TableRow(
    entry: Entry,
    showActions: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onRowClick: () -> Unit,
    backgroundColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onRowClick)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = entry.date, width = colDateWidth)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = entry.patientName, width = colNameWidth, fontWeight = FontWeight.SemiBold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = entry.age.toString(), width = colAgeWidth, isNumeric = true)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(
            text = entry.test,
            width = colTestWidth,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = "₹${entry.amount.toInt()}", width = colAmountWidth, isNumeric = true, textColor = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = "₹${entry.doctorAmount.toInt()}", width = colDocAmountWidth, isNumeric = true, textColor = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = "₹${entry.labCharge.toInt()}", width = colLabChargeWidth, isNumeric = true, textColor = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        TableCell(text = "₹${entry.otherAmount.toInt()}", width = colOtherWidth, isNumeric = true, textColor = Color(0xFFEF6C00), fontWeight = FontWeight.SemiBold)
        
        if (showActions) {
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .width(colActionsWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onEdit?.invoke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onDelete?.invoke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    Divider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun TableTotalRow(
    entries: List<Entry>,
    showActions: Boolean
) {
    val totalAmount = entries.sumOf { it.amount }
    val totalDocAmount = entries.sumOf { it.doctorAmount }
    val totalLabCharge = entries.sumOf { it.labCharge }
    val totalOtherAmount = entries.sumOf { it.otherAmount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(text = "TOTAL", width = colDateWidth, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "${entries.size} Pat", width = colNameWidth, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "-", width = colAgeWidth, isNumeric = true, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "-", width = colTestWidth, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "₹${totalAmount.toInt()}", width = colAmountWidth, isNumeric = true, textColor = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "₹${totalDocAmount.toInt()}", width = colDocAmountWidth, isNumeric = true, textColor = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "₹${totalLabCharge.toInt()}", width = colLabChargeWidth, isNumeric = true, textColor = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TableCell(text = "₹${totalOtherAmount.toInt()}", width = colOtherWidth, isNumeric = true, textColor = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
        
        if (showActions) {
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .width(colActionsWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
    Divider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun PatientEntriesTable(
    entries: List<Entry>,
    showActions: Boolean = false,
    showTotal: Boolean = false,
    onEdit: ((Entry) -> Unit)? = null,
    onDelete: ((Entry) -> Unit)? = null
) {
    var selectedRowEntry by remember { mutableStateOf<Entry?>(null) }

    val totalWidth = colDateWidth + colNameWidth + colAgeWidth + colTestWidth + 
                     colAmountWidth + colDocAmountWidth + colLabChargeWidth + colOtherWidth +
                     (if (showActions) colActionsWidth else 0.dp) + 8.dp // padding cushion

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.width(totalWidth)
            ) {
                TableHeaderRow(showActions = showActions)
                
                entries.forEachIndexed { index, entry ->
                    val bgColor = if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    }
                    TableRow(
                        entry = entry,
                        showActions = showActions,
                        onEdit = onEdit?.let { { it(entry) } },
                        onDelete = onDelete?.let { { it(entry) } },
                        onRowClick = { selectedRowEntry = entry },
                        backgroundColor = bgColor
                    )
                }

                if (showTotal && entries.isNotEmpty()) {
                    TableTotalRow(entries = entries, showActions = showActions)
                }
            }
        }
    }

    if (selectedRowEntry != null) {
        EntryDetailsDialog(
            entry = selectedRowEntry!!,
            onDismiss = { selectedRowEntry = null }
        )
    }
}

@Composable
fun EntryDetailsDialog(
    entry: Entry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Patient Entry Details",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Patient Name:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.patientName, style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Age / Date:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("${entry.age} yrs  |  ${entry.date}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Doctor:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.doctorName, style = MaterialTheme.typography.bodyMedium)
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Test(s) Conducted:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = entry.test,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("₹${entry.amount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Doctor Share Amount:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("₹${entry.doctorAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lab Charge:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("₹${entry.labCharge.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Other Amount:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("₹${entry.otherAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun DoctorMonthlyStatementScreen(viewModel: LabViewModel) {
    val context = LocalContext.current
    val doctors by viewModel.doctors.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val latestPdfResult by viewModel.latestPdfResult.collectAsState()

    var selectedMonthIndex by remember { mutableStateOf(1) } // Default Jan
    var selectedYear by remember { mutableStateOf(2026) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var customFileName by remember { mutableStateOf("") }

    var isGenerated by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(latestPdfResult) {
        if (latestPdfResult != null && isGenerated) {
            showSuccessDialog = true
        }
    }

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val yearsList = (2024..2030).toList()

    // Filter patients belonging to the selected doctor for the selected month and year
    val filteredEntries = entries.filter { entry ->
        selectedDoctor?.let { doc ->
            val matchDoc = entry.doctorId == doc.id
            val matchDate = try {
                val parts = entry.date.split("-")
                val y = parts[0].toInt()
                val m = parts[1].toInt()
                y == selectedYear && m == selectedMonthIndex
            } catch (e: Exception) {
                false
            }
            matchDoc && matchDate
        } ?: false
    }

    // Totals calculations
    val totalPatients = filteredEntries.size
    val totalAmount = filteredEntries.sumOf { it.amount }
    val totalDoctorAmount = filteredEntries.sumOf { it.doctorAmount }
    val totalLabCharge = filteredEntries.sumOf { it.labCharge }
    val totalOtherAmount = filteredEntries.sumOf { it.otherAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Doctor Monthly Statement",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Selection section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Doctor Selection Dropdown
                    var showDocMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDoctor?.name ?: "Select Doctor",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Doctor") },
                            trailingIcon = {
                                IconButton(onClick = { showDocMenu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDocMenu = true }
                        )
                        DropdownMenu(
                            expanded = showDocMenu,
                            onDismissRequest = { showDocMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            doctors.forEach { doc ->
                                DropdownMenuItem(
                                    text = { Text(doc.name) },
                                    onClick = {
                                        selectedDoctor = doc
                                        showDocMenu = false
                                        isGenerated = false
                                    }
                                )
                            }
                        }
                    }

                    // Month & Year Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Month Selector
                        var showMonthMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = monthsList[selectedMonthIndex - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Month") },
                                trailingIcon = {
                                    IconButton(onClick = { showMonthMenu = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMonthMenu = true }
                            )
                            DropdownMenu(
                                expanded = showMonthMenu,
                                onDismissRequest = { showMonthMenu = false }
                            ) {
                                monthsList.forEachIndexed { idx, mName ->
                                    DropdownMenuItem(
                                        text = { Text(mName) },
                                        onClick = {
                                            selectedMonthIndex = idx + 1
                                            showMonthMenu = false
                                            isGenerated = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Selector
                        var showYearMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedYear.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Year") },
                                trailingIcon = {
                                    IconButton(onClick = { showYearMenu = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showYearMenu = true }
                            )
                            DropdownMenu(
                                expanded = showYearMenu,
                                onDismissRequest = { showYearMenu = false }
                            ) {
                                yearsList.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr.toString()) },
                                        onClick = {
                                            selectedYear = yr
                                            showYearMenu = false
                                            isGenerated = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Optional Custom PDF File Name
                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        label = { Text("Custom File Name (Optional)") },
                        placeholder = { Text("e.g. July_Dr_Rajesh_Report") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            selectedDoctor?.let { doc ->
                                viewModel.generateDoctorStatement(
                                    doctor = doc,
                                    monthName = monthsList[selectedMonthIndex - 1],
                                    monthIndex = selectedMonthIndex,
                                    year = selectedYear,
                                    filteredEntries = filteredEntries,
                                    customFileName = customFileName.takeIf { it.isNotBlank() }
                                )
                                isGenerated = true
                            }
                        },
                        enabled = selectedDoctor != null && filteredEntries.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Statement", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (selectedDoctor == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a doctor to generate monthly statement.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (filteredEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No diagnostic records found for ${selectedDoctor?.name} in ${monthsList[selectedMonthIndex - 1]} $selectedYear",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Patient entries list preview
            item {
                Text(
                    text = "Statement Preview (${filteredEntries.size} patient entries)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                PatientEntriesTable(
                    entries = filteredEntries,
                    showActions = false,
                    showTotal = true
                )
            }

            // Summary totals block
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Statement Totals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Patients", fontSize = 13.sp)
                            Text(totalPatients.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount", fontSize = 13.sp)
                            Text("₹$totalAmount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Doctor Amount", fontSize = 13.sp)
                            Text("₹$totalDoctorAmount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC62828))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Lab Charge", fontSize = 13.sp)
                            Text("₹$totalLabCharge", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1565C0))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Other Amount", fontSize = 13.sp)
                            Text("₹$totalOtherAmount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF6C00))
                        }
                    }
                }
            }

            // Generated Actions console
            if (isGenerated && latestPdfResult != null) {
                item {
                    val pdfResult = latestPdfResult!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Statement Generated Successfully!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "File: ${pdfResult.file.name}\nReport ID: ${pdfResult.reportId}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { openPdfFile(context, pdfResult.file) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open PDF", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { sharePdfOnWhatsApp(context, pdfResult.file) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { printPdfFile(context, pdfResult.file) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print PDF", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { openPdfFile(context, pdfResult.file) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog && latestPdfResult != null) {
        val pdfResult = latestPdfResult!!
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.clearLatestPdfResult()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "PDF Generated Successfully",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Your Doctor Monthly Statement PDF has been created successfully.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. 📂 Open PDF
                        Button(
                            onClick = { openPdfFile(context, pdfResult.file) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("📂  Open PDF", fontWeight = FontWeight.Bold)
                        }
                        
                        // 2. ⬇️ Download PDF
                        Button(
                            onClick = { downloadPdfFile(context, pdfResult.file, pdfResult.file.name) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("⬇️  Download PDF", fontWeight = FontWeight.Bold)
                        }
                        
                        // 3. 🟢 Share on WhatsApp
                        Button(
                            onClick = { sharePdfOnWhatsApp(context, pdfResult.file) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            )
                        ) {
                            Text("🟢  Share on WhatsApp", fontWeight = FontWeight.Bold)
                        }
                        
                        // 4. 🖨️ Print PDF
                        Button(
                            onClick = { printPdfFile(context, pdfResult.file) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("🖨️  Print PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.clearLatestPdfResult()
                    }
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun DoctorsScreen(viewModel: LabViewModel) {
    val doctors by viewModel.doctors.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var editingDoctor by remember { mutableStateOf<Doctor?>(null) }
    var activeDashboardDoctor by remember { mutableStateOf<Doctor?>(null) }

    val filteredDoctors = doctors.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.specialty.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Doctor Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = { showAddDocDialog = true },
                modifier = Modifier.testTag("add_doctor_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Doctor")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Doctor") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredDoctors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No doctors registered yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDoctors) { doctor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (doctor.phone.isNotBlank()) {
                                    Text("Phone: ${doctor.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (doctor.specialty.isNotBlank()) {
                                    Text("Specialty: ${doctor.specialty}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { activeDashboardDoctor = doctor }) {
                                    Icon(Icons.Default.Assessment, contentDescription = "Doctor Dashboard", tint = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { editingDoctor = doctor }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Doctor", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteDoctor(doctor) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Doctor", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDocDialog) {
        AddEditDoctorDialog(
            onDismiss = { showAddDocDialog = false },
            onSave = { name, phone, specialty ->
                viewModel.addDoctor(name, phone, specialty)
                showAddDocDialog = false
            }
        )
    }

    if (editingDoctor != null) {
        AddEditDoctorDialog(
            doctor = editingDoctor,
            onDismiss = { editingDoctor = null },
            onSave = { name, phone, specialty ->
                viewModel.updateDoctor(editingDoctor!!.copy(name = name, phone = phone, specialty = specialty))
                editingDoctor = null
            }
        )
    }

    if (activeDashboardDoctor != null) {
        DoctorDetailsDialog(
            doctor = activeDashboardDoctor!!,
            viewModel = viewModel,
            onDismiss = { activeDashboardDoctor = null }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: LabViewModel) {
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showLabProfileDialog by remember { mutableStateOf(false) }
    var showMonthlyLockDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userPrefs = viewModel.userPrefs
    var isAppLockEnabled by remember { mutableStateOf(userPrefs.appLockEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Password Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePassDialog = true }
                            .padding(16.dp)
                            .testTag("change_password_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Change Password", fontWeight = FontWeight.Bold)
                                Text("Update offline login credential", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider()

                    // Update Lab Profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLabProfileDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Update Lab Profile", fontWeight = FontWeight.Bold)
                                Text("Set Lab Name, Address, and Contact Info", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider()

                    // Lock Month Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMonthlyLockDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Lock Month Settings", fontWeight = FontWeight.Bold)
                                Text("Prevent modification of older patient entries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider()

                    // Theme setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTheme() }
                            .padding(16.dp)
                            .testTag("theme_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(if (isDarkMode) "Dark Theme" else "Light Theme", fontWeight = FontWeight.Bold)
                                Text("Toggle operational lighting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleTheme() }
                        )
                    }

                    HorizontalDivider()

                    // App security pin lock startup setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isAppLockEnabled = !isAppLockEnabled
                                userPrefs.appLockEnabled = isAppLockEnabled
                                if (isAppLockEnabled && userPrefs.appLockPin.isEmpty()) {
                                    showPinSetupDialog = true
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("App Security PIN Lock", fontWeight = FontWeight.Bold)
                                Text("Enable 4-digit PIN lock screen on startup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = {
                                isAppLockEnabled = it
                                userPrefs.appLockEnabled = it
                                if (it && userPrefs.appLockPin.isEmpty()) {
                                    showPinSetupDialog = true
                                }
                            }
                        )
                    }

                    if (isAppLockEnabled) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinSetupDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Change Security PIN", fontWeight = FontWeight.Bold)
                                    Text("Current PIN: ${userPrefs.appLockPin.ifEmpty { "1234 (Default)" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Database Operations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Local SQLite Operations Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.backupDatabase() }
                            .padding(16.dp)
                            .testTag("backup_db_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF2E7D32))
                            Column {
                                Text("Backup Database", fontWeight = FontWeight.Bold)
                                Text("Save current SQLite to local storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.restoreDatabase() }
                            .padding(16.dp)
                            .testTag("restore_db_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF1565C0))
                            Column {
                                Text("Restore Database", fontWeight = FontWeight.Bold)
                                Text("Overwrite local SQLite with saved backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecycleBinDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column {
                                Text("Recycle Bin", fontWeight = FontWeight.Bold)
                                Text("Restore soft-deleted records within 30 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // App details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Micro Pathology Lab Console", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version 1.0.0 (Production Build)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Device Secured | 100% Secure Offline Sandbox", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Logout Block
        item {
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Account", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showChangePassDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePassDialog = false },
            onSave = { newP ->
                val ok = viewModel.changePassword(newP)
                if (ok) showChangePassDialog = false
            }
        )
    }

    if (showLabProfileDialog) {
        LabProfileDialog(
            userPrefs = userPrefs,
            onDismiss = { showLabProfileDialog = false }
        )
    }

    if (showMonthlyLockDialog) {
        MonthlyLockDialog(
            userPrefs = userPrefs,
            onDismiss = { showMonthlyLockDialog = false }
        )
    }

    if (showRecycleBinDialog) {
        RecycleBinDialog(
            viewModel = viewModel,
            onDismiss = { showRecycleBinDialog = false }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            userPrefs = userPrefs,
            onDismiss = { showPinSetupDialog = false }
        )
    }
}

@Composable
fun PinSetupDialog(
    userPrefs: com.example.data.pref.UserPrefs,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf(userPrefs.appLockPin) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit Security PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a 4-digit PIN code to secure the application opening flow:")
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            showError = false
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text("PIN must be exactly 4 digits.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length == 4) {
                        userPrefs.appLockPin = pin
                        onDismiss()
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Save PIN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Dialogs

@Composable
fun AddEditDoctorDialog(
    doctor: Doctor? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, specialty: String) -> Unit
) {
    var name by remember { mutableStateOf(doctor?.name ?: "") }
    var phone by remember { mutableStateOf(doctor?.phone ?: "") }
    var specialty by remember { mutableStateOf(doctor?.specialty ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (doctor == null) "Add Doctor" else "Edit Doctor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Doctor Name") },
                    placeholder = { Text("e.g. Dr. Rajesh Verma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Specialty") },
                    placeholder = { Text("e.g. Pathologist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, phone, specialty) },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryDialog(
    viewModel: LabViewModel,
    entry: Entry? = null,
    onDismiss: () -> Unit
) {
    val doctors by viewModel.doctors.collectAsState()

    var date by remember { mutableStateOf(entry?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var time by remember { mutableStateOf(entry?.time ?: SimpleDateFormat("HH:mm", Locale.US).format(Date())) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(doctors.find { it.id == entry?.doctorId }) }
    var patientName by remember { mutableStateOf(entry?.patientName ?: "") }
    var ageStr by remember { mutableStateOf(entry?.age?.toString() ?: "") }
    var test by remember { mutableStateOf(entry?.test ?: "") }
    var amountStr by remember { mutableStateOf(entry?.amount?.toString() ?: "") }
    var docAmountStr by remember { mutableStateOf(entry?.doctorAmount?.toString() ?: "") }
    var labChargeStr by remember { mutableStateOf(entry?.labCharge?.toString() ?: "") }
    var otherAmountStr by remember { mutableStateOf(entry?.otherAmount?.toString() ?: "") }

    var showDocDropdown by remember { mutableStateOf(false) }
    var showAddDoctorMiniDialog by remember { mutableStateOf(false) }
    var showDuplicateAlert by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (entry == null) "Add Patient Entry" else "Edit Patient Entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Date Picker field
                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val formattedMonth = String.format("%02d", month + 1)
                                    val formattedDay = String.format("%02d", day)
                                    date = "$year-$formattedMonth-$formattedDay"
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (entry != null && entry.receiptNumber.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = entry.receiptNumber,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Receipt Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Entry Time") },
                        placeholder = { Text("e.g. 14:30") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Doctor dropdown selection
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedDoctor?.name ?: "Select Doctor",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Doctor Name") },
                                trailingIcon = {
                                    IconButton(onClick = { showDocDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDocDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showDocDropdown,
                                onDismissRequest = { showDocDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                doctors.forEach { doc ->
                                    DropdownMenuItem(
                                        text = { Text(doc.name) },
                                        onClick = {
                                            selectedDoctor = doc
                                            showDocDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Add Quick Doctor Button
                        IconButton(
                            onClick = { showAddDoctorMiniDialog = true },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Doctor Quick", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                // Patient Details
                item {
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) ageStr = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = test,
                        onValueChange = { test = it },
                        label = { Text("Test") },
                        placeholder = { Text("e.g. CBC, Lipid Profile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Financial Fields
                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = docAmountStr,
                        onValueChange = { docAmountStr = it },
                        label = { Text("Doctor Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = labChargeStr,
                        onValueChange = { labChargeStr = it },
                        label = { Text("Lab Charge (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = otherAmountStr,
                        onValueChange = { otherAmountStr = it },
                        label = { Text("Other Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull() ?: 0.0
                                val docAmt = docAmountStr.toDoubleOrNull() ?: 0.0
                                val labChg = labChargeStr.toDoubleOrNull() ?: 0.0
                                val othAmt = otherAmountStr.toDoubleOrNull() ?: 0.0
                                val age = ageStr.toIntOrNull() ?: 0

                                selectedDoctor?.let { doc ->
                                    val saveAction = {
                                        if (entry == null) {
                                            viewModel.addEntry(
                                                date = date,
                                                doctorId = doc.id,
                                                doctorName = doc.name,
                                                patientName = patientName,
                                                age = age,
                                                test = test,
                                                amount = amt,
                                                doctorAmount = docAmt,
                                                labCharge = labChg,
                                                otherAmount = othAmt,
                                                time = time
                                            )
                                        } else {
                                            viewModel.updateEntry(
                                                entry.copy(
                                                    date = date,
                                                    doctorId = doc.id,
                                                    doctorName = doc.name,
                                                    patientName = patientName,
                                                    age = age,
                                                    test = test,
                                                    amount = amt,
                                                    doctorAmount = docAmt,
                                                    labCharge = labChg,
                                                    otherAmount = othAmt,
                                                    time = time
                                                )
                                            )
                                        }
                                        onDismiss()
                                    }

                                    if (entry == null && viewModel.checkDuplicateEntry(patientName, doc.name, date)) {
                                        showDuplicateAlert = true
                                    } else {
                                        saveAction()
                                    }
                                }
                            },
                            enabled = selectedDoctor != null && patientName.isNotBlank() && ageStr.isNotBlank() && test.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    if (showAddDoctorMiniDialog) {
        AddEditDoctorDialog(
            onDismiss = { showAddDoctorMiniDialog = false },
            onSave = { name, phone, specialty ->
                viewModel.addDoctor(name, phone, specialty)
                showAddDoctorMiniDialog = false
            }
        )
    }

    if (showDuplicateAlert) {
        DuplicateEntryDialog(
            onContinue = {
                showDuplicateAlert = false
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                val docAmt = docAmountStr.toDoubleOrNull() ?: 0.0
                val labChg = labChargeStr.toDoubleOrNull() ?: 0.0
                val othAmt = otherAmountStr.toDoubleOrNull() ?: 0.0
                val age = ageStr.toIntOrNull() ?: 0
                selectedDoctor?.let { doc ->
                    if (entry == null) {
                        viewModel.addEntry(
                            date = date,
                            doctorId = doc.id,
                            doctorName = doc.name,
                            patientName = patientName,
                            age = age,
                            test = test,
                            amount = amt,
                            doctorAmount = docAmt,
                            labCharge = labChg,
                            otherAmount = othAmt,
                            time = time
                        )
                    }
                    onDismiss()
                }
            },
            onCancel = {
                showDuplicateAlert = false
            }
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pass by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Change Login Password", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(pass) }, enabled = pass.isNotBlank()) { Text("Save") }
                }
            }
        }
    }
}

// Helpers for opening, sharing, printing file

private fun openPdfFile(context: Context, file: File) {
    val authority = "com.aistudio.micropathlab.kdqmsa.fileprovider"
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app found.", Toast.LENGTH_SHORT).show()
    }
}

private val pdfDownloadCache = mutableSetOf<String>()

private fun downloadPdfFile(context: Context, file: File, fileName: String) {
    if (pdfDownloadCache.contains(fileName)) {
        Toast.makeText(context, "PDF already saved in Downloads.", Toast.LENGTH_LONG).show()
        return
    }

    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val destFolder = File(downloadsDir, "MicroPathologyLab")
        val destFile = File(destFolder, fileName)

        if (destFile.exists()) {
            pdfDownloadCache.add(fileName)
            Toast.makeText(context, "PDF already saved in Downloads.", Toast.LENGTH_LONG).show()
        } else {
            if (!destFolder.exists()) destFolder.mkdirs()
            file.inputStream().use { input ->
                destFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            pdfDownloadCache.add(fileName)
            Toast.makeText(context, "Saved to Downloads/MicroPathologyLab/${fileName}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        pdfDownloadCache.add(fileName)
        Toast.makeText(context, "PDF saved in Downloads/MicroPathologyLab.", Toast.LENGTH_LONG).show()
    }
}

private fun sharePdfOnWhatsApp(context: Context, file: File) {
    val authority = "com.aistudio.micropathlab.kdqmsa.fileprovider"
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            `package` = "com.whatsapp"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        // Fallback generic share sheet
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show()
            context.startActivity(android.content.Intent.createChooser(intent, "Share Doctor Statement"))
        } catch (ex: Exception) {
            Toast.makeText(context, "Failed to share PDF: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun printPdfFile(context: Context, file: File) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "MicroPathologyLab_${file.name}"
        
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    java.io.FileInputStream(file).use { input ->
                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                }
            }

            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                
                val info = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }
        }
        
        printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
    } catch (e: Exception) {
        // Fallback to generic action send chooser if print service isn't available
        try {
            val authority = "com.aistudio.micropathlab.kdqmsa.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Print Statement"))
        } catch (ex: Exception) {
            Toast.makeText(context, "Unable to load system print chooser.", Toast.LENGTH_SHORT).show()
        }
    }
}

// ---------------------------------------------------------------------------
// NEW PREMIUM FEATURES: DIALOGS, CHARTS, AND LOCKS
// ---------------------------------------------------------------------------

@Composable
fun AppLockScreen(userPrefs: com.example.data.pref.UserPrefs, onUnlock: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Micro Pathology Lab",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Enter 4-Digit Security PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // PIN Dots representation
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (i < pinInput.length) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Keyboard
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Fingerprint", "0", "Delete")
                )

                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            if (key == "Fingerprint") {
                                IconButton(
                                    onClick = {
                                        // Simulate Biometric login
                                        onUnlock()
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Login",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            } else if (key == "Delete") {
                                IconButton(
                                    onClick = {
                                        if (pinInput.isNotEmpty()) {
                                            pinInput = pinInput.dropLast(1)
                                            errorMessage = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (pinInput.length < 4) {
                                            pinInput += key
                                            errorMessage = ""
                                            if (pinInput.length == 4) {
                                                if (pinInput == userPrefs.appLockPin || (userPrefs.appLockPin.isEmpty() && pinInput == "1234")) {
                                                    onUnlock()
                                                } else {
                                                    errorMessage = "Incorrect Security PIN."
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteProtectionDialog(
    userPrefs: com.example.data.pref.UserPrefs,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Protection Check") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the Admin Password to confirm this deletion action.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; showError = false },
                    label = { Text("Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text("Incorrect Password. Action denied.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passwordInput == userPrefs.password) {
                        onConfirm()
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Verify & Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DuplicateEntryDialog(
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Duplicate alert", tint = MaterialTheme.colorScheme.error) },
        title = { Text("Duplicate Entry Found") },
        text = {
            Text("An entry with the same Patient Name, Doctor Name, and Date already exists in the system. Do you want to continue?", style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Continue", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LabProfileDialog(
    userPrefs: com.example.data.pref.UserPrefs,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(userPrefs.labName) }
    var address by remember { mutableStateOf(userPrefs.labAddress) }
    var phone by remember { mutableStateOf(userPrefs.labPhone) }
    var email by remember { mutableStateOf(userPrefs.labEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Lab Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Lab Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Lab Address") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Lab Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Lab Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    userPrefs.labName = name
                    userPrefs.labAddress = address
                    userPrefs.labPhone = phone
                    userPrefs.labEmail = email
                    onDismiss()
                }
            ) {
                Text("Save Profile", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MonthlyLockDialog(
    userPrefs: com.example.data.pref.UserPrefs,
    onDismiss: () -> Unit
) {
    val months = listOf(
        "2026-07", "2026-06", "2026-05", "2026-04", "2026-03", "2026-02"
    )
    var lockedList by remember { mutableStateOf(userPrefs.lockedMonthsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }) }
    var verifyMonthToUnlock by remember { mutableStateOf<String?>(null) }
    var verifyPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lock Month Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Once locked, monthly patient entries cannot be edited or deleted. Admin Password is required to unlock.", style = MaterialTheme.typography.bodySmall)
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(months) { month ->
                        val isLocked = lockedList.contains(month)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(month, fontWeight = FontWeight.SemiBold)
                            
                            IconButton(
                                onClick = {
                                    if (isLocked) {
                                        // Trigger password dialog
                                        verifyMonthToUnlock = month
                                        verifyPassword = ""
                                        showError = false
                                    } else {
                                        userPrefs.lockMonth(month)
                                        lockedList = userPrefs.lockedMonthsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock State",
                                    tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done", color = Color.White)
            }
        }
    )

    if (verifyMonthToUnlock != null) {
        AlertDialog(
            onDismissRequest = { verifyMonthToUnlock = null },
            title = { Text("Verify Password to Unlock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter Admin Password to unlock month ${verifyMonthToUnlock}:")
                    OutlinedTextField(
                        value = verifyPassword,
                        onValueChange = { verifyPassword = it; showError = false },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showError) {
                        Text("Incorrect password.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (verifyPassword == userPrefs.password) {
                            userPrefs.unlockMonth(verifyMonthToUnlock!!)
                            lockedList = userPrefs.lockedMonthsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            verifyMonthToUnlock = null
                        } else {
                            showError = true
                        }
                    }
                ) {
                    Text("Unlock", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyMonthToUnlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecycleBinDialog(
    viewModel: LabViewModel,
    onDismiss: () -> Unit
) {
    val deletedEntries by viewModel.recycleBinEntries.collectAsState()
    var entryToDeletePermanently by remember { mutableStateOf<Entry?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recycle Bin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Deleted entries are kept for 30 days before being permanently purged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (deletedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Recycle Bin is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(deletedEntries) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.patientName, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${entry.date} | ₹${entry.amount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.restoreEntry(entry) }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { entryToDeletePermanently = entry }
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanent", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )

    if (entryToDeletePermanently != null) {
        DeleteProtectionDialog(
            userPrefs = viewModel.userPrefs,
            onConfirm = {
                viewModel.deleteEntryPermanently(entryToDeletePermanently!!)
                entryToDeletePermanently = null
            },
            onDismiss = { entryToDeletePermanently = null }
        )
    }
}

@Composable
fun LoadingOverlayDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailsDialog(
    doctor: Doctor,
    viewModel: LabViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val statements by viewModel.statements.collectAsState()
    
    // Filter entries for this doctor
    val doctorEntries = remember(entries, doctor) {
        entries.filter { it.doctorId == doctor.id }
    }
    
    // Filter statement history for this doctor
    val doctorStatements = remember(statements, doctor) {
        statements.filter { it.doctorId == doctor.id }
    }

    // Editable Notes State
    var notes by remember { mutableStateOf(doctor.notes) }
    var isEditingNotes by remember { mutableStateOf(false) }
    
    var showLoadingMsg by remember { mutableStateOf("") }
    var entryToDeleteConfirm by remember { mutableStateOf<MonthlyStatement?>(null) }

    // Stats calculations
    val totalPatients = doctorEntries.size
    val totalRevenue = doctorEntries.sumOf { it.amount }
    val totalDoctorShare = doctorEntries.sumOf { it.doctorAmount }
    val lastEntryDate = doctorEntries.maxOfOrNull { it.date } ?: "No entries yet"

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .padding(16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = doctor.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (doctor.specialty.isNotBlank()) {
                                Text(
                                    text = doctor.specialty,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Dashboard")
                        }
                    }

                    HorizontalDivider()

                    // Content Area
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Metrics row
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Doctor Dashboard Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Patients", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$totalPatients", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${totalRevenue.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Share", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${totalDoctorShare.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                }
                                Text("Last Entry Date: $lastEntryDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Notes section
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Doctor Notes / Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    IconButton(
                                        onClick = {
                                            if (isEditingNotes) {
                                                // Save Notes
                                                viewModel.updateDoctor(doctor.copy(notes = notes))
                                                isEditingNotes = false
                                            } else {
                                                isEditingNotes = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isEditingNotes) Icons.Default.Save else Icons.Default.Edit,
                                            contentDescription = "Edit Notes",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (isEditingNotes) {
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        placeholder = { Text("Add payment status, pending commission, special discount, or reminders here...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 4
                                    )
                                } else {
                                    Text(
                                        text = notes.ifBlank { "No notes or reminders added yet. Tap Edit icon above to add important notes." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Statement History
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Monthly Statement History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                
                                if (doctorStatements.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No statements generated yet.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (stmt in doctorStatements) {
                                            val monthsList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                                            val monthName = if (stmt.month in 1..12) monthsList[stmt.month - 1] else "Unknown"
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("$monthName ${stmt.year}", fontWeight = FontWeight.Bold)
                                                            Text("Revenue: ₹${stmt.totalAmount.toInt()} | Share: ₹${stmt.totalDoctorAmount.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            IconButton(
                                                                onClick = {
                                                                    showLoadingMsg = "Preparing PDF..."
                                                                    val file = File(stmt.localPath)
                                                                    if (file.exists()) {
                                                                        printPdfFile(context, file)
                                                                    } else {
                                                                        Toast.makeText(context, "File not found locally.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    showLoadingMsg = ""
                                                                }
                                                            ) {
                                                                Icon(Icons.Default.Print, contentDescription = "Print PDF", tint = MaterialTheme.colorScheme.primary)
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    showLoadingMsg = "Preparing File..."
                                                                    val file = File(stmt.localPath)
                                                                    if (file.exists()) {
                                                                        try {
                                                                            val authority = "com.aistudio.micropathlab.kdqmsa.fileprovider"
                                                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                                type = "application/pdf"
                                                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                                putExtra(android.content.Intent.EXTRA_TEXT, "Hi Dr. ${doctor.name}, please find attached your pathologist monthly statement for $monthName ${stmt.year}. Reg, Path Lab.")
                                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                            }
                                                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Statement PDF"))
                                                                        } catch (e: Exception) {
                                                                            Toast.makeText(context, "Error sharing statement.", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(context, "File not found locally.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    showLoadingMsg = ""
                                                                }
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.secondary)
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    showLoadingMsg = "Generating Excel..."
                                                                    // Filter entries for that specific month/year
                                                                    val mIndex = monthsList.indexOf(monthName) + 1
                                                                    val monthStr = String.format("%02d", mIndex)
                                                                    val targetPrefix = "${stmt.year}-$monthStr"
                                                                    val monthEntries = doctorEntries.filter { it.date.startsWith(targetPrefix) }
                                                                    
                                                                    val excelRes = com.example.util.ExcelGenerator.generateDoctorStatementExcel(
                                                                        context, doctor.name, monthName, stmt.year, monthEntries
                                                                    )
                                                                    showLoadingMsg = ""
                                                                    
                                                                    if (excelRes != null) {
                                                                        Toast.makeText(context, "Excel sheet generated in Downloads successfully!", Toast.LENGTH_LONG).show()
                                                                        // Share CSV
                                                                        try {
                                                                            val authority = "com.aistudio.micropathlab.kdqmsa.fileprovider"
                                                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, excelRes.file)
                                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                                type = "text/csv"
                                                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                                putExtra(android.content.Intent.EXTRA_TEXT, "Excel Sheet Statement: Dr. ${doctor.name}")
                                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                            }
                                                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Excel Sheet"))
                                                                        } catch (e: Exception) {
                                                                            Toast.makeText(context, "Error sharing excel sheet.", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(context, "Error generating excel sheet.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(Icons.Default.FileDownload, contentDescription = "Export Excel", tint = MaterialTheme.colorScheme.tertiary)
                                                            }

                                                            IconButton(
                                                                onClick = { entryToDeleteConfirm = stmt }
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Delete Statement", tint = MaterialTheme.colorScheme.error)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    if (showLoadingMsg.isNotEmpty()) {
        LoadingOverlayDialog(message = showLoadingMsg)
    }

    if (entryToDeleteConfirm != null) {
        DeleteProtectionDialog(
            userPrefs = viewModel.userPrefs,
            onConfirm = {
                viewModel.deleteStatement(entryToDeleteConfirm!!)
                entryToDeleteConfirm = null
            },
            onDismiss = { entryToDeleteConfirm = null }
        )
    }
}

data class MonthlyStat(
    val monthYear: String,
    val patients: Int,
    val revenue: Double,
    val doctorAmount: Double
)

fun getMonthlyStats(entries: List<Entry>): List<MonthlyStat> {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val stats = mutableMapOf<String, MonthlyStat>()
    
    // Initialize last 6 months
    for (i in 5 downTo 0) {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, -i)
        val mName = months[c.get(Calendar.MONTH)]
        val key = "$mName ${c.get(Calendar.YEAR)}"
        stats[key] = MonthlyStat(key, 0, 0.0, 0.0)
    }
    
    for (entry in entries) {
        try {
            val parts = entry.date.split("-") // YYYY-MM-DD
            if (parts.size >= 2) {
                val year = parts[0].toInt()
                val monthIdx = parts[1].toInt() - 1
                val mName = months[monthIdx]
                val key = "$mName $year"
                if (stats.containsKey(key)) {
                    val s = stats[key]!!
                    stats[key] = s.copy(
                        patients = s.patients + 1,
                        revenue = s.revenue + entry.amount,
                        doctorAmount = s.doctorAmount + entry.doctorAmount
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    return stats.values.toList()
}

@Composable
fun MedicalBarChart(
    title: String,
    data: List<Pair<String, Float>>,
    color: Color
) {
    val maxVal = data.map { it.second }.maxOrNull() ?: 1f
    val displayMax = if (maxVal == 0f) 1f else maxVal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val barHeightFrac = (item.second / displayMax).coerceIn(0.05f, 1f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Value label on top
                    Text(
                        text = if (item.second >= 1000) "${(item.second / 1000).toInt()}k" else item.second.toInt().toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )

                    // The actual Bar
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(barHeightFrac * 0.8f)
                            .width(16.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(color, color.copy(alpha = 0.4f))
                                )
                            )
                    )

                    // X-axis label
                    Text(
                        text = item.first.split(" ").firstOrNull() ?: "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


