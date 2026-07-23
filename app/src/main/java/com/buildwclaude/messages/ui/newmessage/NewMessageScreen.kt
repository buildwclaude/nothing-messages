package com.buildwclaude.messages.ui.newmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.components.Avatar
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.data.telephony.ContactsRepository
import com.buildwclaude.messages.domain.model.Recipient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewMessageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val contacts: ContactsRepository,
) : ViewModel() {
    val prefillBody: String = savedState.get<String>("body") ?: ""
    val query = MutableStateFlow("")
    val selected = MutableStateFlow<List<Recipient>>(emptyList())

    val results = query
        .debounce(150)
        .mapLatest { contacts.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(r: Recipient) {
        selected.value = if (selected.value.any { it.address == r.address }) {
            selected.value.filterNot { it.address == r.address }
        } else {
            selected.value + r
        }
    }
}

@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onStartConversation: (addresses: List<String>, body: String) -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left), "Back",
                    tint = palette.TextSecondary,
                )
            }
            Text(
                "New message",
                style = DesignType.screenTitle,
                color = palette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty() || query.any { it.isDigit() }) {
                Text(
                    "Next",
                    style = DesignType.itemTitle,
                    color = palette.Blue,
                    modifier = Modifier
                        .clickable {
                            val addresses = selected.map { it.address }.ifEmpty {
                                listOf(query.trim())
                            }
                            if (addresses.isNotEmpty() && addresses[0].isNotBlank()) {
                                onStartConversation(addresses, viewModel.prefillBody)
                            }
                        }
                        .padding(12.dp),
                )
            }
        }

        if (selected.isNotEmpty()) {
            LazyColumn(modifier = Modifier.height(60.dp)) {
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        selected.forEach { r ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(palette.Blue)
                                    .clickable { viewModel.toggle(r) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(r.displayName, style = DesignType.body, color = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painterResource(R.drawable.ic_x), "Remove",
                                    tint = Color.White, modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text("Name or phone number", style = DesignType.body) },
            textStyle = DesignType.bodyLarge,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            if (query.any { it.isDigit() } && query.length >= 3) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartConversation(listOf(query.trim()), viewModel.prefillBody) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_message_circle), null,
                            tint = palette.Blue, modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Send to ${query.trim()}",
                            style = DesignType.itemTitle,
                            color = palette.Blue,
                        )
                    }
                    HorizontalDivider(color = palette.Divider)
                }
            }
            items(results) { r ->
                val isSelected = selected.any { it.address == r.address }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggle(r) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Avatar(r, size = 44.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.displayName, style = DesignType.itemTitle, color = palette.TextPrimary)
                        Text(r.address, style = DesignType.body, color = palette.TextSecondary)
                    }
                    if (isSelected) {
                        Icon(
                            painterResource(R.drawable.ic_check_circle), "Selected",
                            tint = palette.Blue, modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
