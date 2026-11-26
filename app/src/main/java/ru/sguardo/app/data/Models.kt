package ru.sguardo.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Зеркало src/lib/types.ts бэкенда. Почти всё необязательное: сервер меняется, приложение не должно падать.

val JsonCfg = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class Folder(val id: String, val key: String? = null, val name: String = "", val color: String? = null)

@Serializable
data class Attachment(
    val kind: String = "file",
    val name: String? = null,
    val url: String = "",
    val mediaId: String? = null,
    val mime: String? = null,
    val round: Boolean? = null,
    val waveform: List<Int>? = null,
    val duration: Double? = null,
)

@Serializable
data class Translation(val lang: String? = null, val text: String = "", val from: String? = null)

@Serializable
data class ReplyRef(val id: String, val text: String = "", val dir: String = "in")

@Serializable
data class Reaction(val emoji: String, val mine: Boolean = false)

@Serializable
data class Transcript(val text: String = "", val lang: String? = null)

@Serializable
data class Message(
    val id: String,
    val dir: String = "in",
    val text: String = "",
    val at: String = "",
    val author: String? = null,
    val authorId: String? = null,
    val attachments: List<Attachment>? = null,
    val translation: Translation? = null,
    val replyTo: ReplyRef? = null,
    val transcript: Transcript? = null,
    val reactions: List<Reaction>? = null,
    // только в приложении: отправляется / изменено / переслано
    val pending: Boolean = false,
    val edited: Boolean = false,
    val forwardedFrom: String? = null,
) {
    val isOut get() = dir == "out"
    val time: Long get() = parseIso(at)
    val att get() = attachments?.firstOrNull()
    val kind: String
        get() = when {
            att == null -> "text"
            att!!.kind == "video" && att!!.round == true -> "circle"
            att!!.kind == "audio" -> "voice"
            att!!.kind == "image" -> "photo"
            att!!.kind == "video" -> "video"
            else -> "file"
        }
    val snippet: String
        get() = text.ifBlank {
            when (kind) {
                "photo" -> "Фото"; "circle" -> "Видеосообщение"; "voice" -> "Голосовое сообщение"
                "video" -> "Видео"; "file" -> att?.name ?: "Файл"; else -> ""
            }
        }
}

@Serializable
data class Conversation(
    val id: String,
    val contactId: String = "",
    val network: String = "telegram",
    val messages: List<Message> = emptyList(),
    val older: Int? = null,
    val unread: Int = 0,
    val channelId: String? = null,
    val externalId: String? = null,
    val meta: Map<String, String>? = null,
) {
    val last get() = messages.lastOrNull()
    val pinnedAt get() = meta?.get("pinned")?.takeIf { it.isNotBlank() }
    val archived get() = !meta?.get("archived").isNullOrBlank()
    val muted get() = !meta?.get("muted").isNullOrBlank()
    val trashed get() = !meta?.get("trashed").isNullOrBlank()
    val blocked get() = meta?.get("blocked") == "1"
    val isGroup get() = meta?.get("kind") == "group"
    val pins: List<String> get() = meta?.get("pins")?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
}

@Serializable
data class Contact(
    val id: String,
    val name: String = "",
    val handle: String? = null,
    val hue: Double? = null,
    val source: String? = null,
    val networks: List<String>? = null,
    val folderId: String? = null,
    val assignee: String? = null,
    val assigneeId: String? = null,
    val note: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val tags: List<String>? = null,
    val lang: String? = null,
    val avatar: String? = null,
    val firstContactAt: String? = null,
)

@Serializable
data class Product(val id: String, val name: String = "", val price: Long = 0)

@Serializable
data class Deal(
    val id: String,
    val contactId: String = "",
    val productId: String? = null,
    val amount: Long = 0,
    val status: String = "paid",
    val date: String = "",
    val dueAt: String? = null,
)

@Serializable
data class Reminder(val id: String, val contactId: String = "", val at: String = "", val text: String = "", val done: Boolean = false)

@Serializable
data class AppEvent(val id: String, val type: String = "", val contactId: String = "", val at: String = "", val meta: String? = null)

@Serializable
data class Template(val id: String, val title: String = "", val text: String = "")

@Serializable
data class Permissions(
    val channels: kotlinx.serialization.json.JsonElement? = null,
    val onlyAssigned: Boolean = false,
    val canPay: Boolean = true,
    val canExport: Boolean = false,
    val seeMoney: Boolean = true,
)

@Serializable
data class TeamMember(
    val id: String,
    val name: String = "",
    val role: String = "agent",
    val email: String = "",
    val permissions: Permissions? = null,
)

@Serializable
data class Channel(val id: String, val type: String = "", val mode: String? = null, val name: String = "", val status: String = "", val error: String? = null)

@Serializable
data class WorkspaceInfo(val currency: String? = null, val translate: Boolean? = null)

@Serializable
data class AppState(
    val workspace: WorkspaceInfo? = null,
    val channels: List<Channel> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val products: List<Product> = emptyList(),
    val deals: List<Deal> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val events: List<AppEvent> = emptyList(),
    val templates: List<Template> = emptyList(),
    val team: List<TeamMember> = emptyList(),
)

@Serializable
data class User(
    val id: String = "",
    val workspaceId: String? = null,
    val email: String = "",
    val name: String = "",
    val role: String = "owner",
    val locale: String? = null,
    val tz: String? = null,
) {
    val isOwner get() = role == "owner"
}

@Serializable
data class StateResponse(val state: AppState? = null, val user: User? = null, val now: String? = null)

@Serializable
data class PartialState(val partial: Boolean = false, val conversations: List<Conversation> = emptyList(), val gone: List<String> = emptyList())

@Serializable
data class MeResponse(val user: User)

@Serializable
data class HistoryResponse(val messages: List<Message> = emptyList(), val hasMore: Boolean = false, val total: Int = 0)

@Serializable
data class AccountInfo(
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val plan: String? = null,
    val status: String? = null,
    val workspaceName: String? = null,
    val paidUntil: String? = null,
    val trialEndsAt: String? = null,
    val lastLoginAt: String? = null,
    val sessions: Int? = null,
)

@Serializable
data class UploadResponse(val attachment: Attachment)

@Serializable
data class ScheduledMessage(
    val id: String,
    val conversationId: String = "",
    val text: String = "",
    val sendAt: String = "",
    val status: String = "pending",
    val error: String? = null,
)

@Serializable
data class ScheduledList(val items: List<ScheduledMessage> = emptyList())

@Serializable
data class Lesson(
    val id: String,
    val conversationId: String = "",
    val contactId: String = "",
    val startsAt: String = "",
    val endsAt: String = "",
    val status: String = "planned",
    val seriesId: String? = null,
)

@Serializable
data class LessonsResponse(val lessons: List<Lesson> = emptyList(), val duration: Int? = null)

@Serializable
data class PresenceItem(val online: Boolean = false, val lastSeen: String? = null)

@Serializable
data class PresenceResponse(val presence: Map<String, PresenceItem> = emptyMap())

@Serializable
data class DraftItem(val text: String = "", val at: String = "")

@Serializable
data class DraftsResponse(val drafts: Map<String, DraftItem> = emptyMap())

@Serializable
data class LinkSuggestion(val aId: String, val bId: String, val aName: String = "", val bName: String = "", val reason: String = "")

@Serializable
data class LinkSuggestions(val items: List<LinkSuggestion> = emptyList())

@Serializable
data class ErrorBody(val error: String? = null, val warning: String? = null)

fun parseIso(s: String?): Long {
    if (s.isNullOrBlank()) return 0L
    return try {
        java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try { java.time.Instant.parse(s).toEpochMilli() } catch (_: Exception) { 0L }
    }
}

fun isoOf(ms: Long): String = java.time.Instant.ofEpochMilli(ms).toString()
