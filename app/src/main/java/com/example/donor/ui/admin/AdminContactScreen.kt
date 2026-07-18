package com.example.donor.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donor.data.model.ContactPerson
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactPerson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingContact by remember { mutableStateOf<ContactPerson?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ContactPerson?>(null) }

    val dbRef = remember { FirebaseDatabase.getInstance().getReference("contactPersons") }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contacts = snapshot.children.mapNotNull { child ->
                    child.getValue(ContactPerson::class.java)?.copy(uid = child.key ?: "")
                }
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) { isLoading = false }
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    // ── Add / Edit dialog ────────────────────────────────────────────────────
    if (showAddDialog || editingContact != null) {
        val existing = editingContact
        ContactEditDialog(
            initial = existing,
            onDismiss = { showAddDialog = false; editingContact = null },
            onSave = { name, phone ->
                if (existing != null) {
                    dbRef.child(existing.uid).setValue(
                        mapOf("name" to name, "phone" to phone)
                    )
                } else {
                    val newRef = dbRef.push()
                    newRef.setValue(mapOf("name" to name, "phone" to phone))
                }
                showAddDialog = false
                editingContact = null
            }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteTarget?.let { contact ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove Contact", fontWeight = FontWeight.SemiBold) },
            text  = { Text("Remove ${contact.name} from the Contact Us list?") },
            confirmButton = {
                Button(
                    onClick = { dbRef.child(contact.uid).removeValue(); deleteTarget = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("Remove", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Us Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RedPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add contact")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedPrimary)
                    }
                }
                contacts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No contacts yet. Tap + to add one.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(contacts, key = { it.uid }) { contact ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape)
                                            .background(RedPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            contact.name.firstOrNull()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Bold,
                                            color = RedPrimary
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text(contact.phone, color = TextSecondary, fontSize = 13.sp)
                                    }





                                    Spacer(Modifier.width(6.dp))

                                    // ── Edit / Delete ─────────────────────────
                                    IconButton(onClick = { editingContact = contact }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RedPrimary)
                                    }
                                    IconButton(onClick = { deleteTarget = contact }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFB71C1C))
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

@Composable
private fun ContactEditDialog(
    initial: ContactPerson?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Contact" else "Edit Contact", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 10) phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    isError = phone.isNotEmpty() && phone.length < 10,
                    supportingText = {
                        if (phone.isNotEmpty() && phone.length < 10)
                            Text("Must be 10 digits", color = MaterialTheme.colorScheme.error)

                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onSave(name, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}