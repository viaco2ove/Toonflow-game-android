package com.toonflow.game

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import coil.compose.AsyncImage
import org.json.JSONArray
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.VoiceBindingDraft
import com.toonflow.game.data.VoiceMixItem
import com.toonflow.game.data.WorldItem
import com.toonflow.game.viewmodel.MainViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Base64
import androidx.compose.ui.unit.min
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private val bgDark = Color(0xFF081424)
private val textSoft = Color(0xFFEAF3FF)
private val pageGray = Color(0xFFF1F3F7)
private val lightLine = Color(0xFFD3DEEF)
private val warnYellow = Color(0xFFFFE600)
private val activeOrange = Color(0xFFFF8E2B)
private const val RUNTIME_VOICE_CACHE_LIMIT = 60

private fun sanitizeSpeakableText(input: String): String {
  return input
    .replace(Regex("（[^）]*）"), "")
    .replace(Regex("\\([^)]*\\)"), "")
    .replace(Regex("【[^】]*】"), "")
    .replace(Regex("\\[[^\\]]*]"), "")
    .replace(Regex("《[^》]*》"), "")
    .replace(Regex("〈[^〉]*〉"), "")
    .replace(Regex("〔[^〕]*〕"), "")
    .replace(Regex("(^|\\n)[：:，,；;、]+"), "$1")
    .replace(Regex("[ \\t]+\\n"), "\n")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
}

private fun normalizePlayableSpeakableText(input: String): String {
  val text = sanitizeSpeakableText(input).replace("\r", "").trim()
  if (text.isBlank()) return ""
  val compact = text.replace(Regex("\\s+"), "")
  val meaningful = compact.replace(Regex("[0-9０-９.,!?;:，。！？；：、…·\"'“”‘’`~!@#$%^&*()\\-_=+\\[\\]{}<>/\\\\|]+"), "")
  return if (meaningful.isBlank()) "" else text
}

private fun splitSpeakableSegments(input: String): List<String> {
  val text = normalizePlayableSpeakableText(input)
  if (text.isBlank()) return emptyList()
  val result = mutableListOf<String>()
  val buffer = StringBuilder()
  fun flush() {
    val value = normalizePlayableSpeakableText(buffer.toString())
    if (value.isNotBlank()) {
      result += value
    }
    buffer.clear()
  }
  text.forEach { ch ->
    buffer.append(ch)
    val compactLength = buffer.toString().replace(Regex("\\s+"), "").length
    if (ch == '。' || ch == '！' || ch == '？' || ch == '!' || ch == '?' || ch == '；' || ch == ';' || ch == '\n') {
      flush()
    } else if (compactLength >= 40) {
      flush()
    }
  }
  flush()
  return result
}

private fun inferRuntimeFallbackPreset(roleType: String, name: String = "", description: String = ""): String {
  if (roleType == "narrator") return "story_narrator"
  val text = "$name $description".lowercase()
  return if (Regex("[女姐妈妹娘妃后妻她]|female|woman|girl|lady").containsMatchIn(text)) {
    "story_std_female"
  } else {
    "story_std_male"
  }
}

private fun inferRuntimeAudioExt(url: String): String {
  val raw = url.substringBefore('?').lowercase()
  return when {
    raw.endsWith(".wav") -> "wav"
    raw.endsWith(".ogg") -> "ogg"
    raw.endsWith(".webm") -> "webm"
    raw.endsWith(".m4a") || raw.endsWith(".mp4") -> "m4a"
    raw.endsWith(".aac") -> "aac"
    else -> "mp3"
  }
}

private fun sha1(input: String): String {
  return MessageDigest.getInstance("SHA-1")
    .digest(input.toByteArray())
    .joinToString("") { byte -> "%02x".format(byte) }
}

private fun <T> setLimitedCacheValue(
  cache: MutableMap<String, T>,
  key: String,
  value: T,
  onEvict: ((T) -> Unit)? = null,
) {
  cache.remove(key)
  cache[key] = value
  while (cache.size > RUNTIME_VOICE_CACHE_LIMIT) {
    val oldestKey = cache.entries.firstOrNull()?.key ?: break
    val removed = cache.remove(oldestKey)
    if (removed != null) {
      onEvict?.invoke(removed)
    }
  }
}

private data class SettingsManufacturerOption(
  val value: String,
  val label: String,
  val website: String = "",
  val textBaseUrl: String = "",
  val imageBaseUrl: String = "",
  val voiceBaseUrl: String = "",
)

private val settingsManufacturers = listOf(
  SettingsManufacturerOption(
    value = "ai_voice_tts",
    label = "ai_voice_tts",
    website = "https://github.com/viaco2ove/ai_voice_tts",
    voiceBaseUrl = "http://127.0.0.1:8000",
  ),
  SettingsManufacturerOption(
    value = "volcengine",
    label = "火山引擎",
    website = "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey",
    textBaseUrl = "https://ark.cn-beijing.volces.com/api/v3",
    imageBaseUrl = "https://ark.cn-beijing.volces.com/api/v3/images/generations",
  ),
  SettingsManufacturerOption(
    value = "deepseek",
    label = "DeepSeek",
    website = "https://platform.deepseek.com",
    textBaseUrl = "https://api.deepseek.com/v1",
  ),
  SettingsManufacturerOption(
    value = "openai",
    label = "OpenAI",
    website = "https://platform.openai.com/api-keys",
    textBaseUrl = "https://api.openai.com/v1",
    imageBaseUrl = "https://api.openai.com/v1/images/generations",
  ),
  SettingsManufacturerOption(
    value = "gemini",
    label = "Gemini",
    website = "https://ai.google.dev/gemini-api/docs/api-key?hl=zh-cn",
    textBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
    imageBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
  ),
  SettingsManufacturerOption(
    value = "bria",
    label = "Bria",
    website = "https://platform.bria.ai",
    imageBaseUrl = "https://engine.prod.bria-api.com/v2/image/edit",
  ),
  SettingsManufacturerOption(
    value = "aliyun_imageseg",
    label = "阿里云视觉",
    website = "https://ram.console.aliyun.com/manage/ak",
    imageBaseUrl = "https://imageseg.cn-shanghai.aliyuncs.com",
  ),
  SettingsManufacturerOption(
    value = "tencent_ci",
    label = "腾讯云数据万象",
    website = "https://console.cloud.tencent.com/cam/capi",
    imageBaseUrl = "",
  ),
  SettingsManufacturerOption(
    value = "local_birefnet",
    label = "BiRefNet 本地",
    website = "https://github.com/ZhengPeng7/BiRefNet",
    imageBaseUrl = "",
  ),
  SettingsManufacturerOption(
    value = "t8star",
    label = "t8star",
    textBaseUrl = "https://ai.t8star.cn/v1",
    imageBaseUrl = "https://ai.t8star.cn/v1/images/generations",
  ),
  SettingsManufacturerOption(
    value = "zhipu",
    label = "智谱",
    website = "https://bigmodel.cn/usercenter/proj-mgmt/apikeys",
    textBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
  ),
  SettingsManufacturerOption(
    value = "qwen",
    label = "阿里千问",
    website = "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key",
    textBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
  ),
  SettingsManufacturerOption(
    value = "aliyun",
    label = "local阿里云",
    website = "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key",
    voiceBaseUrl = "http://127.0.0.1:8000",
  ),
  SettingsManufacturerOption(
    value = "aliyun_direct",
    label = "阿里云直连",
    website = "https://bailian.console.aliyun.com/cn-beijing/?tab=model#/api-key",
    voiceBaseUrl = "https://dashscope.aliyuncs.com",
  ),
  SettingsManufacturerOption(
    value = "other",
    label = "其他",
  ),
)

private fun settingsManufacturerLabel(value: String): String {
  return settingsManufacturers.firstOrNull { it.value == value }?.label ?: value.ifBlank { "未知厂商" }
}

private fun settingsManufacturerWebsite(value: String): String {
  return settingsManufacturers.firstOrNull { it.value == value }?.website.orEmpty()
}

private fun isVoiceDesignSlot(slot: MainViewModel.SettingsModelSlot): Boolean {
  return slot.configType == "voice_design" || slot.key == "storyVoiceDesignModel"
}

private fun isVoiceDesignManufacturer(value: String): Boolean {
  return value.trim().equals("qwen", ignoreCase = true)
}

private fun isVoiceDesignModelName(model: String): Boolean {
  val normalized = model.trim().lowercase(Locale.ROOT)
  return normalized.isNotBlank() && (
    normalized == "qwen-voice-design" ||
      normalized.startsWith("qwen3-tts-vd") ||
      normalized == "voice-enrollment" ||
      normalized.startsWith("cosyvoice-v3") ||
      normalized.startsWith("cosyvoice-v3.5")
    )
}

private fun settingsManufacturersFor(type: String): List<SettingsManufacturerOption> {
  if (type == "voice_design") {
    return settingsManufacturers.filter { it.value == "qwen" }
  }
  return settingsManufacturers.filter {
    if (type == "voice") {
      it.value != "qwen"
    } else {
      it.value != "ai_voice_tts"
        && it.value != "aliyun"
        && it.value != "aliyun_direct"
        && it.value != "bria"
        && it.value != "aliyun_imageseg"
        && it.value != "tencent_ci"
        && it.value != "local_birefnet"
    }
  }
}

private fun settingsManufacturersForSlot(slot: MainViewModel.SettingsModelSlot): List<SettingsManufacturerOption> {
  if (isVoiceDesignSlot(slot)) {
    return settingsManufacturers.filter { it.value == "qwen" }
  }
  if (slot.key == "storyAvatarMattingModel") {
    return settingsManufacturers.filter { it.value == "bria" || it.value == "aliyun_imageseg" || it.value == "tencent_ci" || it.value == "local_birefnet" }
  }
  return settingsManufacturersFor(slot.configType)
}

private fun isAvatarMattingManufacturer(value: String): Boolean {
  return value.trim().equals("bria", ignoreCase = true)
    || value.trim().equals("aliyun_imageseg", ignoreCase = true)
    || value.trim().equals("tencent_ci", ignoreCase = true)
    || value.trim().equals("local_birefnet", ignoreCase = true)
}

private fun defaultSettingsModelType(type: String): String {
  return when (type) {
    "image" -> "t2i"
    "voice" -> "tts"
    "voice_design" -> "voice_design"
    else -> "text"
  }
}

private fun defaultSettingsModelTypeForSlot(slot: MainViewModel.SettingsModelSlot): String {
  return when {
    isVoiceDesignSlot(slot) -> "voice_design"
    slot.configType == "voice" && slot.key == "storyAsrModel" -> "asr"
    else -> defaultSettingsModelType(slot.configType)
  }
}

private fun defaultSettingsManufacturer(type: String): String {
  if (type == "voice_design") return "qwen"
  return if (type == "voice") "ai_voice_tts" else "volcengine"
}

private fun defaultSettingsManufacturerForSlot(slot: MainViewModel.SettingsModelSlot): String {
  return when {
    isVoiceDesignSlot(slot) -> "qwen"
    slot.key == "storyAvatarMattingModel" -> "bria"
    slot.configType == "voice" && slot.key == "storyAsrModel" -> "aliyun_direct"
    else -> defaultSettingsManufacturer(slot.configType)
  }
}

private fun defaultSettingsBaseUrl(manufacturer: String, type: String, modelType: String = defaultSettingsModelType(type)): String {
  if (type == "voice_design" && manufacturer == "qwen") {
    return "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/customization"
  }
  if (type == "voice" && manufacturer == "aliyun_direct") {
    return if (modelType == "asr") "https://dashscope.aliyuncs.com/compatible-mode" else "https://dashscope.aliyuncs.com"
  }
  val row = settingsManufacturers.firstOrNull { it.value == manufacturer } ?: return ""
  return when (type) {
    "image" -> row.imageBaseUrl
    "voice" -> row.voiceBaseUrl
    else -> row.textBaseUrl
  }
}

private fun defaultSettingsModelName(manufacturer: String, type: String, modelType: String = defaultSettingsModelType(type)): String {
  if (type == "voice_design" && manufacturer == "qwen") {
    return "qwen3-tts-vd-2026-01-26"
  }
  if (type == "image" && manufacturer == "bria") {
    return "RMBG-2.0"
  }
  if (type == "image" && manufacturer == "aliyun_imageseg") {
    return "SegmentCommonImage"
  }
  if (type == "image" && manufacturer == "tencent_ci") {
    return "AIPortraitMatting"
  }
  if (type == "image" && manufacturer == "local_birefnet") {
    return "birefnet-portrait"
  }
  if (type == "voice" && manufacturer == "ai_voice_tts") {
    return if (modelType == "tts") "ai_voice_tts" else ""
  }
  if (type == "voice" && manufacturer == "aliyun") {
    return if (modelType == "asr") "fun-asr-realtime" else "cosyvoice-v3-flash"
  }
  if (type == "voice" && manufacturer == "aliyun_direct") {
    return if (modelType == "asr") "qwen3-asr-flash" else "cosyvoice-v3-flash"
  }
  return ""
}

private fun defaultSettingsModelNameForSlot(
  slot: MainViewModel.SettingsModelSlot,
  manufacturer: String,
  modelType: String = defaultSettingsModelType(slot.configType),
): String {
  if (isVoiceDesignSlot(slot) && manufacturer == "qwen") {
    return "qwen3-tts-vd-2026-01-26"
  }
  return defaultSettingsModelName(manufacturer, slot.configType, modelType)
}

private fun settingsApiKeyRequired(manufacturer: String, type: String): Boolean {
  return !(type == "voice" && manufacturer == "ai_voice_tts")
    && !(type == "image" && manufacturer == "local_birefnet")
}

private fun settingsRowMatchesSlot(slot: MainViewModel.SettingsModelSlot, row: com.toonflow.game.data.ModelConfigItem): Boolean {
  if (isVoiceDesignSlot(slot)) {
    return isVoiceDesignManufacturer(row.manufacturer) && isVoiceDesignModelName(row.model)
  }
  if (slot.key == "storyAvatarMattingModel") {
    return isAvatarMattingManufacturer(row.manufacturer)
  }
  if (slot.configType == "image" && isAvatarMattingManufacturer(row.manufacturer)) {
    return false
  }
  if (slot.configType != "voice") return true
  val modelType = row.modelType.ifBlank { "tts" }
  return if (slot.key == "storyAsrModel") modelType == "asr" else modelType != "asr"
}

private fun settingsModelKindLabel(type: String, slotKey: String = ""): String {
  if (slotKey == "storyVoiceDesignModel" || type == "voice_design") return "语音设计模型"
  return when (type) {
    "image" -> "图像模型"
    "voice" -> "语音模型"
    else -> "文本模型"
  }
}

private fun settingsModelTypeLabel(type: String, slotKey: String = ""): String {
  if (slotKey == "storyVoiceDesignModel" || type == "voice_design") return "voice_design"
  return type
}

private fun settingsModelTypeOptionsForSlot(slot: MainViewModel.SettingsModelSlot): List<Pair<String, String>> {
  if (isVoiceDesignSlot(slot)) return listOf("voice_design" to "语音设计")
  return when (slot.configType) {
    "image" -> listOf("t2i" to "文生图", "i2i" to "图生图")
    "voice" -> listOf("tts" to "语音tts", "asr" to "语音识别")
    else -> listOf("text" to "通用文本", "deepThinkingText" to "深度思考")
  }
}

private fun settingsShouldShowModelType(slot: MainViewModel.SettingsModelSlot): Boolean {
  return !isVoiceDesignSlot(slot) && slot.configType != "image"
}

private fun settingsModelTypeOptionLabel(slot: MainViewModel.SettingsModelSlot, modelType: String): String {
  return when {
    isVoiceDesignSlot(slot) -> "语音设计"
    modelType == "voice_design" -> "语音设计"
    modelType == "deepThinkingText" -> "深度思考"
    modelType == "i2i" -> "图生图"
    modelType == "asr" -> "语音识别"
    modelType == "tts" -> "语音tts"
    slot.configType == "image" -> "文生图"
    else -> "通用文本"
  }
}

private fun settingsApiKeyPlaceholder(slot: MainViewModel.SettingsModelSlot, manufacturer: String): String {
  if (!settingsApiKeyRequired(manufacturer, slot.configType)) return "本地 ai_voice_tts 可留空"
  if (slot.key == "storyAvatarMattingModel" && manufacturer == "aliyun_imageseg") {
    return "AccessKeyId|AccessKeySecret"
  }
  if (slot.key == "storyAvatarMattingModel" && manufacturer == "tencent_ci") {
    return "SecretId|SecretKey"
  }
  if (slot.key == "storyAvatarMattingModel" && manufacturer == "local_birefnet") {
    return "本地模型无需填写"
  }
  return "请输入 API Key"
}

private fun settingsApiKeyHint(slot: MainViewModel.SettingsModelSlot, manufacturer: String): String {
  if (slot.key != "storyAvatarMattingModel") return ""
  return when (manufacturer) {
    "aliyun_imageseg" -> "阿里云视觉这里请填写 AccessKeyId|AccessKeySecret，或填写 {\"accessKeyId\":\"...\",\"accessKeySecret\":\"...\"}。"
    "bria" -> "Bria 这里填写平台生成的 API token。"
    "tencent_ci" -> "腾讯云这里请填写 SecretId|SecretKey；Base URL 请填标准 COS 桶域名，例如 https://bucket-appid.cos.ap-shanghai.myqcloud.com。"
    "local_birefnet" -> "本地 BiRefNet 不需要 Base URL 或 API Key。首次选择会提示安装 Python 依赖和模型文件，安装完成后即可直接使用。"
    else -> ""
  }
}

private fun isImportantNotice(text: String): Boolean {
  if (text.isBlank()) return false
  val keywords = listOf("失败", "错误", "失效", "请先", "不能为空", "不一致", "无法", "不可用", "未登录", "未选择")
  return keywords.any { text.contains(it) }
}

private data class StoryPromptUiMeta(
  val agentLabel: String,
  val tsLabel: String,
)

private fun storyPromptUiMeta(code: String): StoryPromptUiMeta {
  return when (code) {
    "story-main" -> StoryPromptUiMeta("story_main", "src/agents/story/main.ts")
    "story-orchestrator" -> StoryPromptUiMeta("story_orchestrator", "src/agents/story/orchestrator/index.ts")
    "story-speaker" -> StoryPromptUiMeta("story_speaker", "src/agents/story/speaker/index.ts")
    "story-memory" -> StoryPromptUiMeta("memory_manager", "src/agents/story/memory_manager/index.ts")
    "story-chapter" -> StoryPromptUiMeta("chapter_judge", "src/agents/story/chapter_judge/index.ts")
    "story-mini-game" -> StoryPromptUiMeta("mini_game_agent", "src/agents/story/mini_game/index.ts")
    "story-safety" -> StoryPromptUiMeta("safety_agent", "src/agents/story/safety/index.ts")
    else -> StoryPromptUiMeta(code, "src/agents/story/unknown.ts")
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgDark) {
          PrototypeAndroidApp()
        }
      }
    }
  }
}

@Composable
private fun PrototypeAndroidApp(vm: MainViewModel = viewModel()) {
  var createStep by remember { mutableIntStateOf(0) }
  var showUserEditor by remember { mutableStateOf(false) }
  var showNpcEditor by remember { mutableStateOf(false) }
  var playMode by remember { mutableStateOf("live") }
  var autoVoice by remember { mutableStateOf(vm.autoVoiceEnabled()) }
  var showDialogMenu by remember { mutableStateOf(false) }
  var showStorySettingDetail by remember { mutableStateOf(false) }
  val toggleAutoVoice = {
    val next = !autoVoice
    autoVoice = next
    vm.setAutoVoiceEnabled(next)
  }
  val storyAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateStoryPlayerAvatarFromUri(uri.toString())
    }
  }
  val storyAvatarVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateStoryPlayerAvatarFromVideoUri(uri.toString())
    }
  }
  val accountAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateAccountAvatarFromUri(uri.toString())
    }
  }
  val accountAvatarVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateAccountAvatarFromVideoUri(uri.toString())
    }
  }

  val bottomTab = when (vm.activeTab) {
    "故事大厅" -> "home"
    "游玩" -> "chat"
    "设置" -> "my"
    "主页" -> "home"
    "创建" -> "create"
    "聊过" -> "chat"
    else -> "my"
  }
  val currentNotice = vm.notice
  val importantNotice = remember(currentNotice) { isImportantNotice(currentNotice) }

  LaunchedEffect(currentNotice) {
    if (currentNotice.isBlank() || importantNotice) return@LaunchedEffect
    kotlinx.coroutines.delay(1200)
    if (vm.notice == currentNotice) {
      vm.notice = ""
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(pageGray)) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(1f)) {
        when (vm.activeTab) {
          "主页" -> HomeScene(vm = vm, autoVoice = autoVoice, onToggleVoice = toggleAutoVoice)
          "故事大厅" -> HallScene(vm = vm)
          "创建" -> CreateScene(
            vm = vm,
            step = createStep,
            onNext = { createStep = 1 },
            onBackToStory = { createStep = 0 },
            showUserEditor = showUserEditor,
            onOpenUserEditor = { showUserEditor = true },
            onCloseUserEditor = { showUserEditor = false },
            showNpcEditor = showNpcEditor,
            onOpenNpcEditor = { showNpcEditor = true },
            onCloseNpcEditor = { showNpcEditor = false },
            onPickAvatar = { storyAvatarPicker.launch("image/*") },
            onPickAvatarVideoGif = { storyAvatarVideoPicker.launch("video/mp4") },
          )
          "聊过" -> HistoryScene(vm = vm)
          "游玩" -> PlayScene(
            vm = vm,
            mode = playMode,
            onModeChange = { playMode = it },
            autoVoice = autoVoice,
            onToggleVoice = toggleAutoVoice,
            showDialogMenu = showDialogMenu,
            onOpenDialogMenu = { showDialogMenu = true },
            onCloseDialogMenu = { showDialogMenu = false },
            showStorySettingDetail = showStorySettingDetail,
            onToggleStorySettingDetail = { showStorySettingDetail = !showStorySettingDetail },
            onCloseSetting = { playMode = "live" },
            onExitDebug = {
              vm.leaveDebugMode()
              vm.startNewStoryDraft()
              playMode = "live"
            },
          )
          "我的" -> ProfileScene(
            vm = vm,
            onPickAvatar = { accountAvatarPicker.launch("image/*") },
            onPickAvatarVideoGif = { accountAvatarVideoPicker.launch("video/mp4") },
          )
          "设置" -> SettingsScene(vm = vm)
          else -> HomeScene(vm = vm, autoVoice = autoVoice, onToggleVoice = toggleAutoVoice)
        }
      }

      BottomNav(
        active = bottomTab,
        onClick = { key ->
          when (key) {
            "home" -> vm.setTab("主页")
            "create" -> vm.startNewStoryDraft()
            "chat" -> vm.setTab("聊过")
            "my" -> vm.setTab("我的")
          }
        },
      )
    }

    if (vm.notice.isNotBlank()) {
      Card(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(horizontal = 10.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (importantNotice) Color(0xFFFFF1F1) else Color(0xFFE8E0F8)),
        shape = RoundedCornerShape(10.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalArrangement = Arrangement.spacedBy(if (importantNotice) 6.dp else 0.dp),
        ) {
          Text(
            text = vm.notice,
            color = if (importantNotice) Color(0xFF8B3C3C) else Color(0xFF4B4470),
            style = MaterialTheme.typography.bodySmall,
          )
          if (importantNotice) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              TextButton(onClick = { vm.notice = "" }) {
                Text("关闭", color = Color(0xFF8B3C3C), fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HomeScene(vm: MainViewModel, autoVoice: Boolean, onToggleVoice: () -> Unit) {
  val rec = vm.recommendedWorld()
  val coverPath = rec?.let { vm.worldCoverPath(it).trim().ifBlank { null } }

  Box(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1830)))
    if (coverPath != null) {
      AsyncImage(
        model = coverPath,
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .clickable {
            vm.startFromWorld(rec, vm.quickInput.trim())
            vm.quickInput = ""
          },
        contentScale = ContentScale.Crop,
      )
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0x3D0A1424), Color(0x7A0A1424), Color(0xE00A1424)),
          ),
        ),
    )

    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 10.dp, end = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CircleGhostBtn(icon = Icons.Outlined.Search, contentDescription = "进入故事大厅") { vm.setTab("故事大厅") }
      CircleGhostBtn(
        icon = if (autoVoice) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
        contentDescription = "切换语音",
      ) {
        onToggleVoice()
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 14.dp, start = 12.dp),
    ) {
      Text("主页", color = Color(0xFFF2F6FF), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
      Text("项目：${vm.selectedProjectName()}", color = Color(0xFFBFD3F1), style = MaterialTheme.typography.labelSmall)
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (rec != null) {
        Column(
          modifier = Modifier
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(Color(0x32131F35))
              .clickable {
                vm.startFromWorld(rec, vm.quickInput.trim())
                vm.quickInput = ""
              }
              .padding(14.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .background(Color(0x2AFFFFFF))
                  .padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                Text("随机推荐故事", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
              Text(
                text = rec.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = rec.intro.ifBlank { "随机展示一个已发布故事，点主视觉或下方输入区都能进入。" },
                color = Color(0xFFE6EEF9),
                style = MaterialTheme.typography.bodyMedium,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyPill("章节 ${rec.chapterCount ?: 0}")
                TinyPill("会话 ${rec.sessionCount ?: 0}")
                TinyPill("全站已发布")
              }
            }
          }
        }
      } else {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x6620344B)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("暂无可游玩故事", color = textSoft, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("当前还没有已发布且有章节的故事，可以先去故事大厅看看。", color = textSoft, style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC10233D)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("输入一句话开始故事", color = Color(0xFFF4F7FD), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
            Text(
              if (rec != null) "会从当前随机推荐的已发布故事开始。" else "暂无推荐故事，可先去故事大厅选择。",
              color = Color(0xFFBFD2EF),
              style = MaterialTheme.typography.bodySmall,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
          ) {
            OutlinedTextField(
              value = vm.quickInput,
              onValueChange = { vm.quickInput = it },
              modifier = Modifier.weight(1f),
              placeholder = { Text("输入一句话，点击右侧按钮进入故事") },
              singleLine = false,
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x172C4468),
                unfocusedContainerColor = Color(0x172C4468),
                focusedBorderColor = Color(0x5E9BD1FF),
                unfocusedBorderColor = Color(0x3B7D9FCC),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color(0x8FD4E0F5),
                unfocusedPlaceholderColor = Color(0x70D4E0F5),
              ),
              minLines = 3,
            )

            Button(
              onClick = { vm.quickStart() },
              enabled = rec != null,
              modifier = Modifier.height(112.dp).width(98.dp),
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD84A), contentColor = Color(0xFF2B3240)),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                Text("进入故事", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TinyPill(text: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(Color(0x2A0E1B30))
      .border(1.dp, Color(0x24E2EBF7), RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Text(text, color = Color(0xFFF3F7FF), style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun HallScene(vm: MainViewModel) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF4F6FA))
      .padding(10.dp),
  ) {
    HeaderTitle(title = "故事大厅", rightText = "返回") { vm.setTab("主页") }

    OutlinedTextField(
      value = vm.hallKeyword,
      onValueChange = { vm.hallKeyword = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("搜索感兴趣内容") },
      shape = RoundedCornerShape(999.dp),
      singleLine = true,
    )

    Row(
      modifier = Modifier
        .padding(top = 8.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      HallTag(text = "全部", active = vm.hallCategory == "all") { vm.hallCategory = "all" }
      HallTag(text = "可游玩", active = vm.hallCategory == "hasChapter") { vm.hallCategory = "hasChapter" }
      HallTag(text = "热门", active = vm.hallCategory == "noChapter") { vm.hallCategory = "noChapter" }
    }

    val list = vm.filteredHallWorlds()
    if (list.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无匹配故事", color = Color(0xFF60708C))
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        gridItems(list, key = { it.id }) { item ->
          HallStoryCard(
            world = item,
            coverPath = vm.worldCoverPath(item).ifBlank { null },
            onPlay = { vm.startFromWorld(item) },
            canEdit = vm.canEditWorld(item),
            onEdit = { vm.openWorldForEdit(item) },
          )
        }
      }
    }
  }
}

@Composable
private fun HallTag(text: String, active: Boolean, onClick: () -> Unit) {
  val bg = if (active) Color(0xFFFFE347) else Color(0xFFEAF0FA)
  val fg = if (active) Color(0xFF39424F) else Color(0xFF415775)
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(bg)
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Text(text, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun HallStoryCard(world: WorldItem, coverPath: String?, canEdit: Boolean, onPlay: () -> Unit, onEdit: () -> Unit) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    modifier = Modifier.fillMaxWidth(),
  ) {
    StoryCoverImage(
      title = world.name,
      coverPath = coverPath,
      modifier = Modifier.fillMaxWidth().height(98.dp),
    )
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(world.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
      Text(
        world.intro.ifBlank { "暂无简介" },
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF495C79),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        "章节 ${world.chapterCount ?: 0} · 会话 ${world.sessionCount ?: 0}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF6E82A1),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniBtn(text = "游玩", primary = true, onClick = onPlay)
        if (canEdit) {
          MiniBtn(text = "编辑", onClick = onEdit)
        }
      }
    }
  }
}

@Composable
private fun CreateScene(
  vm: MainViewModel,
  step: Int,
  onNext: () -> Unit,
  onBackToStory: () -> Unit,
  showUserEditor: Boolean,
  onOpenUserEditor: () -> Unit,
  onCloseUserEditor: () -> Unit,
  showNpcEditor: Boolean,
  onOpenNpcEditor: () -> Unit,
  onCloseNpcEditor: () -> Unit,
  onPickAvatar: () -> Unit,
  onPickAvatarVideoGif: () -> Unit,
) {
  var npcName by remember { mutableStateOf("") }
  var npcDesc by remember { mutableStateOf("") }
  var npcVoice by remember { mutableStateOf("") }
  var npcVoiceMode by remember { mutableStateOf("text") }
  var npcVoicePresetId by remember { mutableStateOf("") }
  var npcVoiceReferenceAudioPath by remember { mutableStateOf("") }
  var npcVoiceReferenceAudioName by remember { mutableStateOf("") }
  var npcVoiceReferenceText by remember { mutableStateOf("") }
  var npcVoicePromptText by remember { mutableStateOf("") }
  var npcVoiceMixVoices by remember { mutableStateOf(listOf(VoiceMixItem(weight = 0.7))) }
  var npcSample by remember { mutableStateOf("") }
  var npcAvatarPath by remember { mutableStateOf("") }
  var npcAvatarBgPath by remember { mutableStateOf("") }
  var npcAvatarDraftKey by remember { mutableStateOf("draft_npc_${System.currentTimeMillis()}") }
  var editingNpcIndex by remember { mutableIntStateOf(-1) }
  var showAdvanced by remember { mutableStateOf(false) }
  var showAvatarActionDialog by remember { mutableStateOf(false) }
  var showNpcAvatarActionDialog by remember { mutableStateOf(false) }
  var showUserAvatarPreviewDialog by remember { mutableStateOf(false) }
  var showNpcAvatarPreviewDialog by remember { mutableStateOf(false) }
  var showCoverActionDialog by remember { mutableStateOf(false) }
  var showChapterBgActionDialog by remember { mutableStateOf(false) }
  var showUserVoiceDialog by remember { mutableStateOf(false) }
  var showNpcVoiceDialog by remember { mutableStateOf(false) }
  var showNarratorVoiceDialog by remember { mutableStateOf(false) }
  var showDeleteNpcConfirm by remember { mutableStateOf(false) }
  var showImageGenerateDialog by remember { mutableStateOf(false) }
  var imageGenerateTarget by remember { mutableStateOf("") }
  var imageGeneratePrompt by remember { mutableStateOf("") }
  var imageGenerateStyleKey by remember { mutableStateOf("general_3") }
  val imageGenerateReferenceUris = remember { mutableStateListOf<String>() }
  val mentionRoles = vm.mentionRoleNames().distinct().filter { it.isNotBlank() }.ifEmpty { listOf("用户", "旁白") }
  val resolvedOpeningRole = if (vm.chapterOpeningRole in mentionRoles) vm.chapterOpeningRole else mentionRoles.first()
  val chapterUsed = vm.chapterContent.length
  val currentEditorChapterSort = vm.selectedChapterId?.let { selectedId ->
    vm.chapters.firstOrNull { it.id == selectedId }?.sort
  } ?: if (vm.chapters.isNotEmpty()) {
    (vm.chapters.maxOfOrNull { it.sort } ?: 0) + 1
  } else {
    1
  }
  val showChapterOpeningEditor = currentEditorChapterSort <= 1
  data class ChapterTabView(
    val id: Long?,
    val label: String,
    val draft: Boolean,
  )
  val chapterTabs = buildList {
    vm.chapters
      .sortedBy { it.sort }
      .forEach { chapter ->
        add(
          ChapterTabView(
            id = chapter.id,
            label = chapter.title.ifBlank { "第 ${chapter.sort} 章" },
            draft = false,
          ),
        )
      }
    if (vm.selectedChapterId == null && vm.chapters.isNotEmpty()) {
      add(
        ChapterTabView(
          id = null,
          label = "${vm.chapterTitle.ifBlank { "第 $currentEditorChapterSort 章" }}（草稿）",
          draft = true,
        ),
      )
    }
  }
  val hasUserAvatarPreview = vm.userAvatarPath.isNotBlank() || vm.userAvatarBgPath.isNotBlank()
  val hasNpcAvatarPreview = npcAvatarPath.isNotBlank() || npcAvatarBgPath.isNotBlank()
  var autoPersistReady by remember { mutableStateOf(false) }
  val autoPersistFingerprint = buildString {
    append(step).append('|')
    append(vm.worldName).append('|')
    append(vm.worldIntro).append('|')
    append(vm.worldCoverPath).append('|')
    append(vm.worldCoverBgPath).append('|')
    append(vm.playerName).append('|')
    append(vm.playerDesc).append('|')
    append(vm.playerVoice).append('|')
    append(vm.playerVoicePresetId).append('|')
    append(vm.playerVoiceMode).append('|')
    append(vm.playerVoiceReferenceAudioPath).append('|')
    append(vm.playerVoiceReferenceAudioName).append('|')
    append(vm.playerVoiceReferenceText).append('|')
    append(vm.playerVoicePromptText).append('|')
    append(vm.playerVoiceMixVoices.joinToString(";") { "${it.voiceId}:${it.weight}" }).append('|')
    append(vm.narratorName).append('|')
    append(vm.narratorVoice).append('|')
    append(vm.narratorVoicePresetId).append('|')
    append(vm.narratorVoiceMode).append('|')
    append(vm.narratorVoiceReferenceAudioPath).append('|')
    append(vm.narratorVoiceReferenceAudioName).append('|')
    append(vm.narratorVoiceReferenceText).append('|')
    append(vm.narratorVoicePromptText).append('|')
    append(vm.narratorVoiceMixVoices.joinToString(";") { "${it.voiceId}:${it.weight}" }).append('|')
    append(vm.globalBackground).append('|')
    append(vm.allowRoleView).append('|')
    append(vm.allowChatShare).append('|')
    append(vm.chapterTitle).append('|')
    append(vm.chapterContent).append('|')
    append(vm.chapterEntryCondition).append('|')
    append(vm.chapterCondition).append('|')
    append(vm.chapterOpeningRole).append('|')
    append(vm.chapterOpeningLine).append('|')
    append(vm.chapterBackground).append('|')
    append(vm.chapterMusic).append('|')
    append(vm.chapterConditionVisible).append('|')
    vm.npcRoles.forEach { role ->
      append(role.id).append(':')
      append(role.name).append(':')
      append(role.avatarPath).append(':')
      append(role.avatarBgPath).append(':')
      append(role.description).append(':')
      append(role.voice).append(':')
      append(role.voiceMode).append(':')
      append(role.voicePresetId).append(':')
      append(role.voiceReferenceAudioPath).append(':')
      append(role.voiceReferenceAudioName).append(':')
      append(role.voiceReferenceText).append(':')
      append(role.voicePromptText).append(':')
      append(role.voiceMixVoices.joinToString(";") { "${it.voiceId}:${it.weight}" }).append(':')
      append(role.sample).append('|')
    }
  }
  LaunchedEffect(autoPersistFingerprint) {
    if (!autoPersistReady) {
      autoPersistReady = true
      return@LaunchedEffect
    }
    vm.scheduleStoryEditorAutoPersist()
  }
  val npcAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      val roleKey = if (editingNpcIndex >= 0) {
        vm.npcRoles.getOrNull(editingNpcIndex)?.id?.ifBlank { npcAvatarDraftKey } ?: npcAvatarDraftKey
      } else {
        npcAvatarDraftKey
      }
      vm.importRoleAvatar(uri.toString(), "npc_${vm.selectedProjectId}_$roleKey") { fg, bg ->
        npcAvatarPath = fg
        npcAvatarBgPath = bg
      }
    }
  }
  val npcAvatarVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.importRoleAvatarVideoGif(uri.toString()) { fg, bg ->
        npcAvatarPath = fg
        npcAvatarBgPath = bg
      }
    }
  }
  val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.importWorldCover(uri.toString())
    }
  }
  val chapterBgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.importChapterBackground(uri.toString())
    }
  }
  val chapterMusicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.importChapterMusic(uri.toString())
    }
  }
  val imageGenerateRefPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
    if (uris.isNotEmpty()) {
      uris.map { it.toString() }.forEach { uri ->
        if (!imageGenerateReferenceUris.contains(uri)) {
          imageGenerateReferenceUris.add(uri)
        }
      }
      showImageGenerateDialog = true
    }
  }

  fun openImageGenerate(target: String, prompt: String, refs: List<String> = emptyList()) {
    imageGenerateTarget = target
    imageGeneratePrompt = prompt
    imageGenerateStyleKey = when (target) {
      "cover" -> "cinema"
      "chapter" -> "guofeng"
      "npc" -> "general_3"
      else -> "general_3"
    }
    imageGenerateReferenceUris.clear()
    imageGenerateReferenceUris.addAll(refs)
    showImageGenerateDialog = true
  }

  fun submitImageGenerate() {
    val styledPrompt = buildStyledImagePrompt(imageGenerateStyleKey, imageGeneratePrompt)
    if (imageGeneratePrompt.isBlank()) {
      showImageGenerateDialog = false
      return
    }
    when (imageGenerateTarget) {
      "user" -> vm.generateUserAvatar(styledPrompt, imageGenerateReferenceUris.toList())
      "npc" -> {
        val roleKey = if (editingNpcIndex >= 0) {
          vm.npcRoles.getOrNull(editingNpcIndex)?.id?.ifBlank { npcAvatarDraftKey } ?: npcAvatarDraftKey
        } else {
          npcAvatarDraftKey
        }
        vm.generateRoleAvatar(
          prompt = styledPrompt,
          referenceUris = imageGenerateReferenceUris.toList(),
          storageKey = "npc_${vm.selectedProjectId}_$roleKey",
          roleName = npcName.ifBlank { "角色" },
        ) { fg, bg ->
          npcAvatarPath = fg
          npcAvatarBgPath = bg
        }
      }
      "cover" -> vm.generateWorldCover(styledPrompt, imageGenerateReferenceUris.toList())
      "chapter" -> vm.generateChapterBackground(styledPrompt, imageGenerateReferenceUris.toList())
    }
    showImageGenerateDialog = false
  }

  @Composable
  fun RenderImageGenerateDialog() {
    if (showImageGenerateDialog) {
      ImageGenerateDialog(
        title = when (imageGenerateTarget) {
          "user" -> "创建角色"
          "npc" -> "创建角色"
          "cover" -> "创建故事封面"
          else -> "创建章节背景"
        },
        prompt = imageGeneratePrompt,
        referenceImageCount = imageGenerateReferenceUris.size,
        loading = vm.aiGenerating,
        onPromptChange = { imageGeneratePrompt = it },
        selectedStyleKey = imageGenerateStyleKey,
        onStyleChange = { imageGenerateStyleKey = it },
        onAddReferenceImages = { imageGenerateRefPicker.launch("image/*") },
        onClearReferenceImages = { imageGenerateReferenceUris.clear() },
        onDismiss = {
          if (!vm.aiGenerating) showImageGenerateDialog = false
        },
        onConfirm = { submitImageGenerate() },
      )
    }
  }

  when {
    showUserEditor -> {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(pageGray)
          .padding(12.dp),
      ) {
        HeaderTitle(title = "用户扮演角色的资料", rightText = "完成") { onCloseUserEditor() }
        ProtoInputCard(label = "头像（可上传 / AI 生成）") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MediumEditableAvatar(
              path = vm.userAvatarPath.trim().ifBlank { null },
              backgroundPath = vm.userAvatarBgPath.trim().ifBlank { vm.userAvatarPath.trim().ifBlank { null } },
              fallbackName = vm.playerName.ifBlank { vm.userName.ifBlank { "用户" } },
              loading = vm.storyPlayerAvatarProcessing,
              onClick = { showAvatarActionDialog = true },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(if (vm.storyPlayerAvatarProcessing) "头像处理中..." else "点击头像更换", color = Color(0xFF3F5476), fontWeight = FontWeight.SemiBold)
              Text("支持 PNG / GIF / MP4，保存时会自动标准化。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
              Text("可选：上传、GIF、AI 文生图、AI 图生图、图标查看大图。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MiniBtn(text = "上传") { onPickAvatar() }
            MiniBtn(text = "GIF") { onPickAvatarVideoGif() }
            MiniBtn(text = "AI 生图") { openImageGenerate("user", vm.playerDesc) }
            MiniIconBtn(icon = Icons.Outlined.Visibility, contentDescription = "查看头像大图") {
              if (hasUserAvatarPreview) {
                showUserAvatarPreviewDialog = true
              } else {
                vm.notice = "当前还没有头像可查看"
              }
            }
          }
          if (showAvatarActionDialog) {
            AvatarActionDialog(
              onDismiss = { showAvatarActionDialog = false },
              onUpload = {
                showAvatarActionDialog = false
                onPickAvatar()
              },
              onUploadVideoGif = {
                showAvatarActionDialog = false
                onPickAvatarVideoGif()
              },
              onAiGenerate = {
                openImageGenerate("user", vm.playerDesc)
                showAvatarActionDialog = false
              },
            )
          }
          if (showUserAvatarPreviewDialog) {
            AvatarPreviewDialog(
              title = vm.playerName.ifBlank { "用户头像" },
              foregroundPath = vm.userAvatarPath.trim().ifBlank { null },
              backgroundPath = vm.userAvatarBgPath.trim().ifBlank { vm.userAvatarPath.trim().ifBlank { null } },
              fallbackName = vm.playerName.ifBlank { vm.userName.ifBlank { "用户" } },
              onDismiss = { showUserAvatarPreviewDialog = false },
            )
          }
        }
        ProtoInputCard(label = "角色名（选填）") {
          OutlinedTextField(
            value = vm.playerName,
            onValueChange = { vm.playerName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入用户在故事里扮演的角色名") },
          )
        }
        ProtoInputCard(label = "角色设定（选填）", top = 10.dp) {
          ScrollableOutlinedTextField(
            value = vm.playerDesc,
            onValueChange = { vm.playerDesc = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("用户的身份、性格等") },
            minLines = 4,
          )
        }
        ProtoInputCard(label = "角色音色（选填）", top = 10.dp) {
          VoicePickerField(
            value = vm.playerVoice.ifBlank { "未选择音色" },
            onClick = { showUserVoiceDialog = true },
          )
        }
        Text(
          "用户是固定角色，不可删除。参数卡会在保存后自动生成到 parameterCardJson。",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7286A4),
          modifier = Modifier.padding(top = 8.dp),
        )
        if (showUserVoiceDialog) {
          VoicePickerDialog(
            vm = vm,
            title = "选择用户音色",
            initialLabel = vm.playerVoice,
            initialPresetId = vm.playerVoicePresetId,
            initialMode = vm.playerVoiceMode,
            initialReferenceAudioPath = vm.playerVoiceReferenceAudioPath,
            initialReferenceAudioName = vm.playerVoiceReferenceAudioName,
            initialReferenceText = vm.playerVoiceReferenceText,
            initialPromptText = vm.playerVoicePromptText,
            initialMixVoices = vm.playerVoiceMixVoices,
            onDismiss = { showUserVoiceDialog = false },
            onConfirm = { binding ->
              vm.setPlayerVoiceBinding(binding)
              showUserVoiceDialog = false
            },
          )
        }
        RenderImageGenerateDialog()
      }
      return
    }

    showNpcEditor -> {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(pageGray)
          .padding(12.dp),
      ) {
        HeaderTitle(title = if (editingNpcIndex >= 0) "编辑角色资料" else "新增角色资料", rightText = "确定") {
          if (npcName.isNotBlank()) {
            if (editingNpcIndex >= 0) {
              vm.setNpcRoleAvatar(editingNpcIndex, npcAvatarPath, npcAvatarBgPath)
              vm.setNpcRoleName(editingNpcIndex, npcName)
              vm.setNpcRoleDescription(editingNpcIndex, npcDesc)
              vm.setNpcRoleVoice(
                editingNpcIndex,
                VoiceBindingDraft(
                  label = npcVoice,
                  presetId = npcVoicePresetId,
                  mode = npcVoiceMode,
                  referenceAudioPath = npcVoiceReferenceAudioPath,
                  referenceAudioName = npcVoiceReferenceAudioName,
                  referenceText = npcVoiceReferenceText,
                  promptText = npcVoicePromptText,
                  mixVoices = npcVoiceMixVoices,
                ),
              )
              vm.setNpcRoleSample(editingNpcIndex, npcSample)
            } else {
              vm.addNpcRole()
              val idx = vm.npcRoles.lastIndex
              if (idx >= 0) {
                vm.setNpcRoleAvatar(idx, npcAvatarPath, npcAvatarBgPath)
                vm.setNpcRoleName(idx, npcName)
                vm.setNpcRoleDescription(idx, npcDesc)
                vm.setNpcRoleVoice(
                  idx,
                  VoiceBindingDraft(
                    label = npcVoice,
                    presetId = npcVoicePresetId,
                    mode = npcVoiceMode,
                    referenceAudioPath = npcVoiceReferenceAudioPath,
                    referenceAudioName = npcVoiceReferenceAudioName,
                    referenceText = npcVoiceReferenceText,
                    promptText = npcVoicePromptText,
                    mixVoices = npcVoiceMixVoices,
                  ),
                )
                vm.setNpcRoleSample(idx, npcSample)
              }
            }
          }
          npcName = ""
          npcDesc = ""
          npcVoice = ""
          npcVoiceMode = "text"
          npcVoicePresetId = ""
          npcVoiceReferenceAudioPath = ""
          npcVoiceReferenceAudioName = ""
          npcVoiceReferenceText = ""
          npcVoicePromptText = ""
          npcVoiceMixVoices = listOf(VoiceMixItem(weight = 0.7))
          npcSample = ""
          npcAvatarPath = ""
          npcAvatarBgPath = ""
          npcAvatarDraftKey = "draft_npc_${System.currentTimeMillis()}"
          editingNpcIndex = -1
          onCloseNpcEditor()
        }
        ProtoInputCard(label = "头像（可上传 / AI 生成）") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MediumEditableAvatar(
              path = npcAvatarPath.trim().ifBlank { null },
              backgroundPath = npcAvatarBgPath.trim().ifBlank { npcAvatarPath.trim().ifBlank { null } },
              fallbackName = npcName.ifBlank { "角色" },
              loading = vm.roleAvatarProcessing,
              onClick = { showNpcAvatarActionDialog = true },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(if (vm.roleAvatarProcessing) "头像处理中..." else "点击头像更换", color = Color(0xFF3F5476), fontWeight = FontWeight.SemiBold)
              Text("支持 PNG / GIF / MP4，保存时会自动标准化。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
              Text("可选：上传、GIF、AI 文生图、AI 图生图、图标查看大图。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MiniBtn(text = "上传") { npcAvatarPicker.launch("image/*") }
            MiniBtn(text = "GIF") { npcAvatarVideoPicker.launch("video/mp4") }
            MiniBtn(text = "AI 生图") { openImageGenerate("npc", npcDesc) }
            MiniIconBtn(icon = Icons.Outlined.Visibility, contentDescription = "查看头像大图") {
              if (hasNpcAvatarPreview) {
                showNpcAvatarPreviewDialog = true
              } else {
                vm.notice = "当前还没有头像可查看"
              }
            }
          }
          if (showNpcAvatarActionDialog) {
            AvatarActionDialog(
              onDismiss = { showNpcAvatarActionDialog = false },
              onUpload = {
                showNpcAvatarActionDialog = false
                npcAvatarPicker.launch("image/*")
              },
              onUploadVideoGif = {
                showNpcAvatarActionDialog = false
                npcAvatarVideoPicker.launch("video/mp4")
              },
              onAiGenerate = {
                openImageGenerate("npc", npcDesc)
                showNpcAvatarActionDialog = false
              },
            )
          }
          if (showNpcAvatarPreviewDialog) {
            AvatarPreviewDialog(
              title = npcName.ifBlank { "角色头像" },
              foregroundPath = npcAvatarPath.trim().ifBlank { null },
              backgroundPath = npcAvatarBgPath.trim().ifBlank { npcAvatarPath.trim().ifBlank { null } },
              fallbackName = npcName.ifBlank { "角色" },
              onDismiss = { showNpcAvatarPreviewDialog = false },
            )
          }
        }
        ProtoInputCard(label = "角色名") {
          OutlinedTextField(value = npcName, onValueChange = { npcName = it }, modifier = Modifier.fillMaxWidth())
        }
        ProtoInputCard(label = "角色设定(性别,年龄,性格,外貌,音色特点,技能,物品,装备,等级,血量,蓝量,金钱,其他)", top = 10.dp) {
          ScrollableOutlinedTextField(
            value = npcDesc,
            onValueChange = { npcDesc = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
          )
        }
        ProtoInputCard(label = "角色音色", top = 10.dp) {
          VoicePickerField(
            value = npcVoice.ifBlank { "未选择音色" },
            onClick = { showNpcVoiceDialog = true },
          )
        }
        ProtoInputCard(label = "台词示例", top = 10.dp) {
          OutlinedTextField(value = npcSample, onValueChange = { npcSample = it }, modifier = Modifier.fillMaxWidth())
        }
        Text(
          "参数卡会在保存角色后自动生成到 parameterCardJson。",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7286A4),
          modifier = Modifier.padding(top = 8.dp),
        )
        if (editingNpcIndex >= 0) {
          TextButton(
            onClick = { showDeleteNpcConfirm = true },
          ) {
            Text("删除当前角色", color = Color(0xFFC64242), fontWeight = FontWeight.SemiBold)
          }
        }
        if (showDeleteNpcConfirm) {
          AlertDialog(
            onDismissRequest = { showDeleteNpcConfirm = false },
            title = { Text("确认删除角色") },
            text = { Text("删除后无法恢复，确认删除当前角色吗？") },
            dismissButton = {
              TextButton(onClick = { showDeleteNpcConfirm = false }) {
                Text("取消")
              }
            },
            confirmButton = {
              TextButton(
                onClick = {
                  vm.removeNpcRole(editingNpcIndex)
                  showDeleteNpcConfirm = false
                  npcName = ""
                  npcDesc = ""
                  npcVoice = ""
                  npcVoiceMode = "text"
                  npcVoicePresetId = ""
                  npcVoiceReferenceAudioPath = ""
                  npcVoiceReferenceAudioName = ""
                  npcVoiceReferenceText = ""
                  npcVoicePromptText = ""
                  npcVoiceMixVoices = listOf(VoiceMixItem(weight = 0.7))
                  npcSample = ""
                  npcAvatarPath = ""
                  npcAvatarBgPath = ""
                  npcAvatarDraftKey = "draft_npc_${System.currentTimeMillis()}"
                  editingNpcIndex = -1
                  onCloseNpcEditor()
                },
              ) {
                Text("删除", color = Color(0xFFC64242), fontWeight = FontWeight.Bold)
              }
            },
          )
        }
        if (showNpcVoiceDialog) {
          VoicePickerDialog(
            vm = vm,
            title = "选择角色音色",
            initialLabel = npcVoice,
            initialPresetId = npcVoicePresetId,
            initialMode = npcVoiceMode,
            initialReferenceAudioPath = npcVoiceReferenceAudioPath,
            initialReferenceAudioName = npcVoiceReferenceAudioName,
            initialReferenceText = npcVoiceReferenceText,
            initialPromptText = npcVoicePromptText,
            initialMixVoices = npcVoiceMixVoices,
            onDismiss = { showNpcVoiceDialog = false },
            onConfirm = { binding ->
              npcVoice = binding.label
              npcVoiceMode = binding.mode
              npcVoicePresetId = binding.presetId
              npcVoiceReferenceAudioPath = binding.referenceAudioPath
              npcVoiceReferenceAudioName = binding.referenceAudioName
              npcVoiceReferenceText = binding.referenceText
              npcVoicePromptText = binding.promptText
              npcVoiceMixVoices = binding.mixVoices.ifEmpty { listOf(VoiceMixItem(weight = 0.7)) }
              showNpcVoiceDialog = false
            },
          )
        }
        RenderImageGenerateDialog()
      }
      return
    }
  }

  if (step == 1) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(pageGray)
        .verticalScroll(rememberScrollState())
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      HeaderTitle(title = "基本信息", rightText = "返回") { onBackToStory() }
      if (vm.canUndoStoryAutoPersist()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = { vm.undoStoryAutoPersist() }) {
            Text("撤回", color = Color(0xFF6E5DB6), fontWeight = FontWeight.Bold)
          }
        }
      }
      ProtoInputCard(label = "故事图标") {
        Box(modifier = Modifier.fillMaxWidth().clickable { showCoverActionDialog = true }) {
          StoryCoverImage(
            title = vm.worldName.ifBlank { "故事" },
            coverPath = vm.worldCoverPath.trim().ifBlank { null },
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(1.dp, lightLine, RoundedCornerShape(10.dp)),
          )
        }
        if (showCoverActionDialog) {
          AvatarActionDialog(
            onDismiss = { showCoverActionDialog = false },
            onUpload = {
              showCoverActionDialog = false
              coverPicker.launch("image/*")
            },
            onUploadVideoGif = null,
            onAiGenerate = {
              openImageGenerate("cover", vm.worldIntro)
              showCoverActionDialog = false
            },
          )
        }
      }
      ProtoInputCard(label = "故事名称") {
        OutlinedTextField(value = vm.worldName, onValueChange = { vm.worldName = it }, modifier = Modifier.fillMaxWidth())
      }
      ProtoInputCard(label = "故事简介") {
        ScrollableOutlinedTextField(
          value = vm.worldIntro,
          onValueChange = { vm.worldIntro = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 5,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MiniBtn(text = "返回", onClick = onBackToStory)
        MiniBtn(text = "存草稿", onClick = { vm.saveStoryEditor(publish = false, successNotice = "故事草稿已保存") })
        MiniBtn(text = "发布", primary = true, onClick = {
          vm.saveStoryEditor(publish = true, successNotice = "故事已发布并可游玩")
        })
      }
    }
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageGray)
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    HeaderTitle(title = "故事设定", rightText = "下一步") {
      vm.saveStoryEditor(publish = false, successNotice = null)
      onNext()
    }
    if (vm.canUndoStoryAutoPersist()) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { vm.undoStoryAutoPersist() }) {
          Text("撤回", color = Color(0xFF6E5DB6), fontWeight = FontWeight.Bold)
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("角色", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          AvatarItem(
            label = "用户",
            icon = Icons.Outlined.Person,
            avatarPath = vm.userAvatarPath.trim().ifBlank { null },
            avatarBgPath = vm.userAvatarBgPath.trim().ifBlank { null },
            onClick = onOpenUserEditor,
            showEditBadge = true,
            iconTint = Color(0xFF6B55A2),
          )
          vm.npcRoles.forEachIndexed { index, role ->
            AvatarItem(
              label = role.name.ifBlank { "新角色" },
              icon = Icons.Outlined.AccountCircle,
              avatarPath = role.avatarPath.trim().ifBlank { null },
              avatarBgPath = role.avatarBgPath.trim().ifBlank { null },
              showEditBadge = true,
              onClick = {
                npcName = role.name
                npcAvatarPath = role.avatarPath
                npcAvatarBgPath = role.avatarBgPath
                npcAvatarDraftKey = role.id.ifBlank { "draft_npc_${System.currentTimeMillis()}" }
                npcDesc = role.description
                npcVoice = role.voice
                npcVoiceMode = role.voiceMode
                npcVoicePresetId = role.voicePresetId
                npcVoiceReferenceAudioPath = role.voiceReferenceAudioPath
                npcVoiceReferenceAudioName = role.voiceReferenceAudioName
                npcVoiceReferenceText = role.voiceReferenceText
                npcVoicePromptText = role.voicePromptText
                npcVoiceMixVoices = role.voiceMixVoices.ifEmpty { listOf(VoiceMixItem(weight = 0.7)) }
                npcSample = role.sample
                editingNpcIndex = index
                onOpenNpcEditor()
              },
            )
          }
          AvatarItem(
            label = "新增",
            icon = Icons.Outlined.Add,
            onClick = {
              npcName = ""
              npcAvatarPath = ""
              npcAvatarBgPath = ""
              npcAvatarDraftKey = "draft_npc_${System.currentTimeMillis()}"
              npcDesc = ""
              npcVoice = ""
              npcVoiceMode = "text"
              npcVoicePresetId = ""
              npcVoiceReferenceAudioPath = ""
              npcVoiceReferenceAudioName = ""
              npcVoiceReferenceText = ""
              npcVoicePromptText = ""
              npcVoiceMixVoices = listOf(VoiceMixItem(weight = 0.7))
              npcSample = ""
              editingNpcIndex = -1
              onOpenNpcEditor()
            },
            iconTint = Color(0xFF8EA4C9),
          )
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("全局背景（选填）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        ScrollableOutlinedTextField(
          value = vm.globalBackground,
          onValueChange = { vm.globalBackground = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 4,
          placeholder = { Text("多章节故事时可填写世界背景，提及时请使用角色名或“用户”。") },
        )
        MentionRow(vm = vm, onClick = { vm.appendGlobalMention(it) })
      }
    }

    if (showChapterOpeningEditor) {
      Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("故事描述", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
          Text("开场白", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7B8EA8))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFF8FBFF))
              .border(1.dp, Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
              .clickable {
                val curr = mentionRoles.indexOf(resolvedOpeningRole).let { if (it < 0) 0 else it }
                vm.chapterOpeningRole = mentionRoles[(curr + 1) % mentionRoles.size]
              }
              .padding(horizontal = 12.dp, vertical = 12.dp),
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("选择开场白发言角色", color = Color(0xFF5E7395))
              Text("${resolvedOpeningRole}  >", color = Color(0xFF4D6285), fontWeight = FontWeight.Bold)
            }
          }
          ScrollableOutlinedTextField(
            value = vm.chapterOpeningLine,
            onValueChange = { vm.chapterOpeningLine = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("作为选定角色/旁白的第一句话开启整个故事") },
          )
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("章节背景图片", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        Box(modifier = Modifier.fillMaxWidth().clickable { showChapterBgActionDialog = true }) {
          StoryCoverImage(
            title = vm.chapterTitle.ifBlank { "章节背景" },
            coverPath = vm.chapterBackground.trim().ifBlank { null },
            modifier = Modifier
              .fillMaxWidth()
              .height(92.dp)
              .clip(RoundedCornerShape(10.dp))
              .border(1.dp, lightLine, RoundedCornerShape(10.dp)),
            emptyText = "上传 / AI 生成章节背景图",
          )
        }
      }
    }

    if (showChapterBgActionDialog) {
      AvatarActionDialog(
        onDismiss = { showChapterBgActionDialog = false },
        onUpload = {
          showChapterBgActionDialog = false
          chapterBgPicker.launch("image/*")
        },
        onUploadVideoGif = null,
        onAiGenerate = {
          openImageGenerate("chapter", vm.chapterContent)
          showChapterBgActionDialog = false
        },
      )
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("故事内容（章节内容）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
          Text(
            "调试",
            color = Color(0xFF1E3FFF),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.clickable { vm.debugCurrentChapter() },
          )
        }
        Text(
          "提及用户扮演的角色时，请用“用户”一词称呼",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7B8EA8),
        )
        ScrollableOutlinedTextField(
          value = vm.chapterContent,
          onValueChange = { vm.chapterContent = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 4,
          placeholder = { Text("描述主要情节，包括用户在故事中和其他角色的互动。提及时请使用角色原名或用户，不要使用“你”来代称。") },
        )
        MentionRow(vm = vm, onClick = { vm.appendChapterMention(it) })
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("还可输入1500字", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A8C0))
          Text("$chapterUsed/1500", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A8C0))
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("成功条件（章节结局）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        ScrollableOutlinedTextField(
          value = vm.chapterCondition,
          onValueChange = { vm.chapterCondition = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          placeholder = { Text("只有用户达成该条件才进入下一章节。为空代表无结束，AI 持续编排。") },
        )
        SettingSwitchRow(
          label = "结局条件对用户可见",
          checked = vm.chapterConditionVisible,
          onToggle = { vm.chapterConditionVisible = !vm.chapterConditionVisible },
        )
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("背景音乐（可选）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
            .clickable { chapterMusicPicker.launch("audio/*") }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(displayStorageName(vm.chapterMusic, "可选预设音乐（现在无），也可以上传"), color = Color(0xFF5E7395))
            Text("上传  >", color = Color(0xFF4D6285), fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { vm.saveStoryEditor(publish = false, startNextDraft = true, successNotice = "当前章节已保存，并已新建下一章节草稿") },
      colors = CardDefaults.cardColors(containerColor = Color.White),
      shape = RoundedCornerShape(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("添加下一章节", color = Color(0xFF41597D))
        Icon(Icons.Outlined.Add, contentDescription = "添加", tint = Color(0xFF6D7FA0))
      }
    }

    if (chapterTabs.size > 1) {
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        chapterTabs.forEach { chapter ->
          val active = if (chapter.draft) vm.selectedChapterId == null else chapter.id == vm.selectedChapterId
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(if (active) Color(0xFF314F7E) else Color(0xFFF2F7FF))
              .border(1.dp, if (active) Color(0xFF2A426A) else Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
              .clickable(enabled = chapter.id != null) {
                vm.saveCurrentChapterAndSelect(chapter.id)
              }
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              chapter.label,
              color = if (active) Color.White else Color(0xFF2E466A),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("旁白面板", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
            .clickable { showNarratorVoiceDialog = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("旁白音色", color = Color(0xFF5E7395))
            Text("${vm.narratorVoice.ifBlank { "混合（清朗温润）" }}  >", color = Color(0xFF4D6285), fontWeight = FontWeight.Bold)
          }
        }
        if (showNarratorVoiceDialog) {
          VoicePickerDialog(
            vm = vm,
            title = "选择旁白音色",
            initialLabel = vm.narratorVoice,
            initialPresetId = vm.narratorVoicePresetId,
            initialMode = vm.narratorVoiceMode,
            initialReferenceAudioPath = vm.narratorVoiceReferenceAudioPath,
            initialReferenceAudioName = vm.narratorVoiceReferenceAudioName,
            initialReferenceText = vm.narratorVoiceReferenceText,
            initialPromptText = vm.narratorVoicePromptText,
            initialMixVoices = vm.narratorVoiceMixVoices,
            onDismiss = { showNarratorVoiceDialog = false },
            onConfirm = { binding ->
              vm.setNarratorVoiceBinding(binding)
              showNarratorVoiceDialog = false
            },
          )
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      shape = RoundedCornerShape(12.dp),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvanced = !showAdvanced },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("高级设定", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
          Text(if (showAdvanced) "收起" else "展开", color = Color(0xFF6B5CA2), style = MaterialTheme.typography.labelSmall)
        }
        if (showAdvanced) {
          SettingSwitchRow(
            label = "他人可查看角色设定",
            checked = vm.allowRoleView,
            onToggle = { vm.allowRoleView = !vm.allowRoleView },
          )
          SettingSwitchRow(
            label = "他人可分享对话剧情",
            checked = vm.allowChatShare,
            onToggle = { vm.allowChatShare = !vm.allowChatShare },
          )
        }
      }
    }

    Button(
      onClick = {
        vm.saveStoryEditor(publish = false, successNotice = null)
        onNext()
      },
      modifier = Modifier.fillMaxWidth().height(44.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = warnYellow, contentColor = Color(0xFF1F2128)),
    ) {
      Text("下一步", fontWeight = FontWeight.ExtraBold)
    }

    RenderImageGenerateDialog()

    Spacer(modifier = Modifier.height(60.dp))
  }
}

@Composable
private fun MentionRow(vm: MainViewModel, onClick: (String) -> Unit) {
  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    vm.mentionRoleNames().distinct().filter { it.isNotBlank() }.forEach { role ->
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(Color(0xFFEEF3FC))
          .border(1.dp, Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
          .clickable { onClick(role) }
          .padding(horizontal = 9.dp, vertical = 4.dp),
      ) {
        Text(role, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4F6281))
      }
    }
  }
}

@Composable
private fun HistoryScene(vm: MainViewModel) {
  var pendingDeleteSession by remember { mutableStateOf<SessionItem?>(null) }
  Column(modifier = Modifier.fillMaxSize().background(pageGray).padding(10.dp)) {
    HeaderTitle(title = "聊过", rightText = "刷新") { vm.reloadAll() }
    if (vm.sessions.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无会话记录", color = Color(0xFF60708C))
      }
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
        items(vm.sessions, key = { it.sessionId }) { item ->
          val world = vm.worlds.firstOrNull { it.id == item.worldId }
          HistoryCard(
            item = item,
            coverPath = vm.resolveMediaPath(item.worldCoverPath).ifBlank { vm.worldCoverPath(world) }.ifBlank { null },
            onClick = { vm.continueSessionForWorld(item.worldId, item.sessionId) },
            onWatch = { vm.continueSessionForWorld(item.worldId, item.sessionId, playback = true, playbackIndex = 0) },
            onDelete = { pendingDeleteSession = item },
          )
        }
      }
    }
  }
  pendingDeleteSession?.let { item ->
    AlertDialog(
      onDismissRequest = { pendingDeleteSession = null },
      title = { Text("删除会话") },
      text = { Text("确认删除会话「${item.title.ifBlank { item.worldName.ifBlank { "未命名会话" } }}」吗？删除后无法恢复。") },
      confirmButton = {
        TextButton(onClick = {
          vm.deleteSession(item.sessionId)
          pendingDeleteSession = null
        }) {
          Text("删除", color = Color(0xFFC43F3F))
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteSession = null }) {
          Text("取消")
        }
      },
    )
  }
}

@Composable
private fun HistoryCard(
  item: SessionItem,
  coverPath: String?,
  onClick: () -> Unit,
  onWatch: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable { onClick() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StoryCoverImage(
        title = if (item.title.isBlank()) item.worldName else item.title,
        coverPath = coverPath,
        modifier = Modifier.size(66.dp).clip(RoundedCornerShape(10.dp)),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (item.title.isBlank()) item.worldName else item.title,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val metaLine = listOfNotNull(
          item.worldName.ifBlank { null },
          item.chapterTitle.ifBlank { null },
        ).joinToString(" · ")
        if (metaLine.isNotBlank()) {
          Text(
            text = metaLine,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7084A2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
          )
        }
        Text(
          text = item.latestMessage?.content ?: "点击继续聊",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF4F6281),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 4.dp),
        )
        if (item.worldIntro.isNotBlank()) {
          Text(
            text = item.worldIntro,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7B8DA9),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
        Row(
          modifier = Modifier.padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onClick,
            modifier = Modifier.weight(1f).height(34.dp),
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F7FD), contentColor = Color(0xFF2E466A)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
          ) {
            Text("继续", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
          MiniIconBtn(icon = Icons.Outlined.Visibility, contentDescription = "观看回放", onClick = onWatch)
          MiniIconBtn(icon = Icons.Outlined.Delete, contentDescription = "删除会话", onClick = onDelete)
        }
      }
    }
  }
}

@Composable
private fun PlayScene(
  vm: MainViewModel,
  mode: String,
  onModeChange: (String) -> Unit,
  autoVoice: Boolean,
  onToggleVoice: () -> Unit,
  showDialogMenu: Boolean,
  onOpenDialogMenu: () -> Unit,
  onCloseDialogMenu: () -> Unit,
  showStorySettingDetail: Boolean,
  onToggleStorySettingDetail: () -> Unit,
  onCloseSetting: () -> Unit,
  onExitDebug: () -> Unit,
) {
  if (vm.currentSessionId.isBlank()) {
    Box(modifier = Modifier.fillMaxSize().background(pageGray), contentAlignment = Alignment.Center) {
      Text("请先从主页或聊过进入故事", color = Color(0xFF60708C))
    }
    return
  }

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var selectedMessage by remember(vm.currentSessionId) { mutableStateOf<MessageItem?>(null) }
  var deletingMessage by remember(vm.currentSessionId) { mutableStateOf<MessageItem?>(null) }
  var inputMode by remember(vm.currentSessionId) { mutableStateOf("text") }
  var voiceListening by remember(vm.currentSessionId) { mutableStateOf(false) }
  var voiceTranscribing by remember(vm.currentSessionId) { mutableStateOf(false) }
  var pendingVoiceStartAfterPermission by remember(vm.currentSessionId) { mutableStateOf(false) }
  var recorder by remember(vm.currentSessionId) { mutableStateOf<MediaRecorder?>(null) }
  var recordFile by remember(vm.currentSessionId) { mutableStateOf<File?>(null) }
  var playbackPlayer by remember(vm.currentSessionId) { mutableStateOf<MediaPlayer?>(null) }
  var playbackRequestId by remember(vm.currentSessionId) { mutableStateOf(0) }
  val runtimeVoicePreviewCache = remember(vm.currentSessionId) { mutableMapOf<String, String>() }
  val runtimeVoicePreviewInflight = remember(vm.currentSessionId) { mutableMapOf<String, CompletableDeferred<String>>() }
  val runtimeVoiceAudioPathCache = remember(vm.currentSessionId) { mutableMapOf<String, String>() }
  val runtimeVoiceAudioInflight = remember(vm.currentSessionId) { mutableMapOf<String, CompletableDeferred<String>>() }
  val runtimeVoiceFallbackBindingCache = remember(vm.currentSessionId) { mutableMapOf<String, VoiceBindingDraft>() }
  val runtimeVoiceWarmCache = remember(vm.currentSessionId) { mutableSetOf<String>() }
  var systemTts by remember { mutableStateOf<TextToSpeech?>(null) }
  var systemTtsInit by remember { mutableStateOf<CompletableDeferred<TextToSpeech>?>(null) }
  val revealedMessages = remember(vm.currentSessionId) { mutableStateListOf<MessageItem>() }
  var playbackCursor by remember(vm.currentSessionId) { mutableIntStateOf(0) }
  var playbackPlaying by remember(vm.currentSessionId) { mutableStateOf(false) }
  var playbackRunId by remember(vm.currentSessionId) { mutableIntStateOf(0) }
  var debugAutoAdvancing by remember(vm.currentSessionId) { mutableStateOf(false) }
  var debugAutoAdvanceJob by remember { mutableStateOf<Job?>(null) }
  var dialogMenuAnchorBounds by remember(vm.currentSessionId) { mutableStateOf<Rect?>(null) }
  var runtimeVoiceMessageKey by remember(vm.currentSessionId) { mutableStateOf("") }
  var runtimeVoicePhase by remember(vm.currentSessionId) { mutableStateOf("") }
  var runtimeVoiceIndicator by remember(vm.currentSessionId) { mutableStateOf(".") }
  var runtimeVoiceAutoJob by remember(vm.currentSessionId) { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()
  val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) {
      if (!pendingVoiceStartAfterPermission) return@rememberLauncherForActivityResult
      pendingVoiceStartAfterPermission = false
      scope.launch {
        startRuntimeVoiceRecording(
          context = context,
          onStart = { mediaRecorder, output ->
            recorder = mediaRecorder
            recordFile = output
            voiceListening = true
          },
          onFailure = { message -> vm.notice = message },
        )
      }
    } else {
      pendingVoiceStartAfterPermission = false
      vm.notice = "未授予录音权限"
    }
  }
  fun stopRuntimePlayback(invalidate: Boolean = true) {
    if (invalidate) {
      playbackRequestId += 1
    }
    systemTts?.stop()
    playbackPlayer?.let { player ->
      runCatching {
        player.stop()
      }
      player.release()
    }
    playbackPlayer = null
    runtimeVoiceMessageKey = ""
    runtimeVoicePhase = ""
    runtimeVoiceIndicator = "."
  }

  fun stopRuntimeAutoVoiceQueue(invalidate: Boolean = true) {
    runtimeVoiceAutoJob?.cancel()
    runtimeVoiceAutoJob = null
    stopRuntimePlayback(invalidate)
  }

  suspend fun ensureSystemTts(): TextToSpeech {
    systemTts?.let { return it }
    systemTtsInit?.let { return it.await() }
    val deferred = CompletableDeferred<TextToSpeech>()
    systemTtsInit = deferred
    var createdTts: TextToSpeech? = null
    createdTts = TextToSpeech(context.applicationContext) { status ->
      val engine = createdTts
      if (status == TextToSpeech.SUCCESS && engine != null) {
        runCatching {
          val zhResult = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
          if (zhResult == TextToSpeech.LANG_MISSING_DATA || zhResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.CHINESE)
          }
          engine.setSpeechRate(1f)
        }
        systemTts = engine
        if (!deferred.isCompleted) {
          deferred.complete(engine)
        }
      } else {
        engine?.shutdown()
        if (!deferred.isCompleted) {
          deferred.completeExceptionally(IllegalStateException("系统朗读不可用"))
        }
      }
      systemTtsInit = null
    }
    return try {
      deferred.await()
    } catch (err: Throwable) {
      systemTts = null
      throw err
    }
  }

  fun setRuntimeVoiceIndicator(message: MessageItem?, phase: String) {
    if (message == null || phase.isBlank()) {
      runtimeVoiceMessageKey = ""
      runtimeVoicePhase = ""
      runtimeVoiceIndicator = "."
      return
    }
    runtimeVoiceMessageKey = vm.messageUiKey(message)
    runtimeVoicePhase = phase
  }

  fun messageVoiceTail(message: MessageItem): String {
    return if (runtimeVoiceMessageKey == vm.messageUiKey(message) && runtimeVoicePhase.isNotBlank()) {
      runtimeVoiceIndicator
    } else {
      ""
    }
  }

  fun estimatePlaybackTimeoutMs(text: String): Long {
    val normalized = sanitizeSpeakableText(text)
    val estimated = normalized.length * 180L + 6000L
    return estimated.coerceIn(8000L, 45000L)
  }

  fun estimateRevealDelayMs(text: String): Long {
    val normalized = sanitizeSpeakableText(text)
    val estimated = normalized.length * 90L + 1200L
    return estimated.coerceIn(1400L, 4800L)
  }

  LaunchedEffect(runtimeVoiceMessageKey, runtimeVoicePhase) {
    if (runtimeVoiceMessageKey.isBlank() || runtimeVoicePhase.isBlank()) {
      runtimeVoiceIndicator = "."
      return@LaunchedEffect
    }
    val frames = if (runtimeVoicePhase == "playing") listOf(".", "。", ".") else listOf(".", "。")
    var index = 0
    runtimeVoiceIndicator = frames[index]
    while (true) {
      delay(260L)
      index = (index + 1) % frames.size
      runtimeVoiceIndicator = frames[index]
    }
  }

  fun runtimeVoiceBindingKey(binding: VoiceBindingDraft): String {
    val runtimeContextKey = binding.configId ?: vm.sessionDetail?.world?.id ?: vm.currentSessionId.takeIf { it.isNotBlank() } ?: "runtime"
    return buildString {
      append(runtimeContextKey).append('|')
      append(binding.mode).append('|')
      append(binding.presetId).append('|')
      append(binding.referenceAudioPath).append('|')
      append(binding.referenceText).append('|')
      append(binding.promptText).append('|')
      append(binding.mixVoices.joinToString(";") { "${it.voiceId}:${it.weight}" })
    }
  }

  fun runtimeVoicePreviewKey(binding: VoiceBindingDraft, text: String): String {
    return runtimeVoiceBindingKey(binding) + "|" + text
  }

  fun isDeterministicRuntimeVoiceError(error: Throwable): Boolean {
    val message = (error.message ?: error.toString()).lowercase()
    return listOf(
      "detect audio failed",
      "当前语音设计模型与所选故事语音模型不兼容",
      "请先在设置里配置语音设计模型",
      "当前语音模型不支持该绑定模式",
      "克隆模式需要参考音频",
      "提示词模式需要填写提示词",
      "参考音频无法被阿里云解码",
      "语音模型配置不存在",
      "未返回试听音频",
      "http 400",
    ).any { message.contains(it.lowercase()) }
  }

  fun findMessageRole(message: MessageItem) = vm.playStoryRoles().firstOrNull { role ->
    val roleName = message.role.trim()
    (roleName.isNotBlank() && (role.name == roleName || role.id == roleName)) || (roleName.isBlank() && role.roleType == message.roleType)
  } ?: vm.playStoryRoles().firstOrNull { it.roleType == message.roleType }

  fun resolveFallbackVoiceBinding(message: MessageItem, originalBinding: VoiceBindingDraft?): VoiceBindingDraft? {
    if (message.roleType == "player") return null
    if (message.roleType == "narrator") {
      return VoiceBindingDraft(
        label = originalBinding?.label?.ifBlank { "旁白" } ?: "旁白",
        configId = originalBinding?.configId,
        presetId = "story_narrator",
        mode = "text",
        referenceAudioPath = "",
        referenceAudioName = "",
        referenceText = "",
        promptText = "",
        mixVoices = emptyList(),
      )
    }
    val role = findMessageRole(message)
    val roleName = role?.name ?: message.role.trim()
    return VoiceBindingDraft(
      label = originalBinding?.label?.ifBlank { role?.voice ?: roleName } ?: (role?.voice ?: roleName),
      configId = originalBinding?.configId ?: role?.voiceConfigId,
      presetId = inferRuntimeFallbackPreset(
        roleType = role?.roleType ?: message.roleType,
        name = roleName,
        description = role?.description.orEmpty(),
      ),
      mode = "text",
      referenceAudioPath = "",
      referenceAudioName = "",
      referenceText = "",
      promptText = "",
      mixVoices = emptyList(),
    )
  }

  fun shouldDowngradeRuntimeVoiceBinding(binding: VoiceBindingDraft?, error: Throwable): Boolean {
    if (binding == null || binding.mode == "text") return false
    return isDeterministicRuntimeVoiceError(error)
  }

  suspend fun resolveRuntimeVoiceUrl(binding: VoiceBindingDraft, text: String): String {
    val cacheKey = runtimeVoicePreviewKey(binding, text)
    runtimeVoicePreviewCache[cacheKey]?.takeIf { it.isNotBlank() }?.let { return it }
    runtimeVoicePreviewInflight[cacheKey]?.let { return it.await() }
    val deferred = CompletableDeferred<String>()
    runtimeVoicePreviewInflight[cacheKey] = deferred
    try {
      val url = withTimeoutOrNull(15000L) {
        vm.streamVoice(
          configId = binding.configId,
          text = text,
          mode = binding.mode,
          presetId = binding.presetId,
          referenceAudioPath = binding.referenceAudioPath,
          referenceText = binding.referenceText,
          promptText = binding.promptText,
          mixVoices = binding.mixVoices,
          format = "mp3",
          sampleRate = 16000,
        )
      } ?: throw IllegalStateException("语音生成超时")
      if (url.isBlank()) throw IllegalStateException("未返回试听音频")
      setLimitedCacheValue(runtimeVoicePreviewCache, cacheKey, url)
      deferred.complete(url)
      return url
    } catch (err: Throwable) {
      deferred.completeExceptionally(err)
      throw err
    } finally {
      runtimeVoicePreviewInflight.remove(cacheKey)
    }
  }

  suspend fun resolveRuntimeVoicePlaybackPath(audioUrl: String): String {
    runtimeVoiceAudioPathCache[audioUrl]?.takeIf { it.isNotBlank() && File(it).exists() }?.let { return it }
    runtimeVoiceAudioInflight[audioUrl]?.let { return it.await() }
    val deferred = CompletableDeferred<String>()
    runtimeVoiceAudioInflight[audioUrl] = deferred
    try {
      val resolvedAudioUrl = vm.resolveMediaPath(audioUrl)
      val localPath = withTimeoutOrNull(10000L) {
        withContext(Dispatchers.IO) {
          val targetDir = File(context.cacheDir, "runtime_voice_preview").apply { mkdirs() }
          val targetFile = File(targetDir, "${sha1(resolvedAudioUrl)}.${inferRuntimeAudioExt(resolvedAudioUrl)}")
          if (!targetFile.exists() || targetFile.length() <= 0L) {
            URL(resolvedAudioUrl).openStream().use { input ->
              targetFile.outputStream().use { output ->
                input.copyTo(output)
              }
            }
          }
          targetFile.absolutePath
        }
      } ?: throw IllegalStateException("音频下载超时")
      if (localPath.isBlank()) throw IllegalStateException("音频下载失败")
      setLimitedCacheValue(runtimeVoiceAudioPathCache, audioUrl, localPath) { cachedPath ->
        runCatching { File(cachedPath).delete() }
      }
      deferred.complete(localPath)
      return localPath
    } catch (err: Throwable) {
      deferred.completeExceptionally(err)
      throw err
    } finally {
      runtimeVoiceAudioInflight.remove(audioUrl)
    }
  }

  suspend fun warmVoiceBinding(binding: VoiceBindingDraft) {
    if (binding.mode != "text") return
    val bindingKey = runtimeVoiceBindingKey(binding)
    if (!runtimeVoiceWarmCache.add(bindingKey)) return
    runCatching {
      resolveRuntimeVoiceUrl(binding, "你好啊，有什么可以帮到你")
    }
  }

  suspend fun replayWithSystemTts(
    message: MessageItem,
    speakable: String,
    manual: Boolean,
    waitForCompletion: Boolean,
  ): Boolean {
    if (speakable.isBlank()) {
      if (manual) {
        vm.notice = "这条对话没有可重听内容"
      }
      return false
    }
    val requestId = playbackRequestId
    return try {
      val tts = ensureSystemTts()
      val played = withTimeoutOrNull(if (waitForCompletion) estimatePlaybackTimeoutMs(speakable) else 5000L) {
        suspendCancellableCoroutine<Boolean> { continuation ->
          val utteranceId = "runtime_tts_${requestId}_${System.currentTimeMillis()}"
          val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceIdFromEngine: String?) {
              if (utteranceIdFromEngine != utteranceId) return
              if (requestId != playbackRequestId) {
                if (continuation.isActive) continuation.resume(false)
                return
              }
              setRuntimeVoiceIndicator(message, "playing")
              if (manual) {
                vm.notice = "正在本地朗读"
              }
              if (!waitForCompletion && continuation.isActive) {
                continuation.resume(true)
              }
            }

            override fun onDone(utteranceIdFromEngine: String?) {
              if (utteranceIdFromEngine != utteranceId) return
              if (manual) {
                vm.notice = "朗读完成"
              }
              if (continuation.isActive) {
                continuation.resume(true)
              }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceIdFromEngine: String?) {
              onError(utteranceIdFromEngine, TextToSpeech.ERROR)
            }

            override fun onError(utteranceIdFromEngine: String?, errorCode: Int) {
              if (utteranceIdFromEngine != utteranceId) return
              if (manual) {
                vm.notice = "系统朗读失败"
              }
              if (continuation.isActive) {
                continuation.resume(false)
              }
            }
          }
          tts.setOnUtteranceProgressListener(listener)
          continuation.invokeOnCancellation {
            tts.stop()
          }
          val result = tts.speak(speakable, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
          if (result == TextToSpeech.ERROR && continuation.isActive) {
            if (manual) {
              vm.notice = "系统朗读失败"
            }
            continuation.resume(false)
          }
        }
      }
      if (played == null) {
        tts.stop()
        if (manual) {
          vm.notice = "系统朗读超时"
        }
        return false
      }
      played
    } catch (err: Throwable) {
      if (manual) {
        vm.notice = "系统朗读失败: ${err.message ?: "未知错误"}"
      }
      false
    }
  }

  suspend fun playMessageVoiceWithBinding(
    message: MessageItem,
    binding: VoiceBindingDraft,
    speakable: String,
    manual: Boolean,
    waitForCompletion: Boolean,
  ): Boolean {
    stopRuntimePlayback()
    val requestId = playbackRequestId
    if (manual) {
      vm.notice = "正在生成语音"
    }
    val segments = splitSpeakableSegments(speakable)
    if (segments.isEmpty()) return false
    setRuntimeVoiceIndicator(message, "loading")
    for (segment in segments) {
      var segmentPlayed = false
      var lastError: Throwable? = null
      val previewKey = runtimeVoicePreviewKey(binding, segment)
      repeat(3) {
        var shouldRetry = true
        if (segmentPlayed) return@repeat
        if (requestId != playbackRequestId) return false
        setRuntimeVoiceIndicator(message, "loading")
        val audioUrl = runCatching {
          resolveRuntimeVoiceUrl(binding, segment)
        }.getOrElse {
          lastError = it
          shouldRetry = !isDeterministicRuntimeVoiceError(it)
          ""
        }
        if (audioUrl.isBlank()) {
          if (!shouldRetry) return@repeat
          delay(220L)
          return@repeat
        }
        val playbackPath = runCatching {
          resolveRuntimeVoicePlaybackPath(audioUrl)
        }.getOrElse {
          lastError = it
          val messageText = it.message.orEmpty()
          if (messageText.contains("HTTP ", ignoreCase = true) || messageText.contains("音频下载超时")) {
            runtimeVoicePreviewCache.remove(previewKey)
            runtimeVoicePreviewInflight.remove(previewKey)?.cancel()
            runtimeVoiceAudioPathCache.remove(audioUrl)?.let { cachedPath ->
              runCatching { File(cachedPath).delete() }
            }
          }
          ""
        }
        if (playbackPath.isBlank()) {
          delay(220L)
          return@repeat
        }
        if (requestId != playbackRequestId) return false
        val played = withTimeoutOrNull(if (waitForCompletion) estimatePlaybackTimeoutMs(segment) else 8000L) {
          suspendCancellableCoroutine<Boolean> { continuation ->
            val player = MediaPlayer()
            playbackPlayer = player
            fun finish(ok: Boolean, noticeText: String? = null) {
              if (playbackPlayer === player) {
                playbackPlayer = null
              }
              runCatching { player.stop() }
              player.release()
              if (manual && !noticeText.isNullOrBlank()) {
                vm.notice = noticeText
              }
              if (continuation.isActive) {
                continuation.resume(ok)
              }
            }
            continuation.invokeOnCancellation {
              if (playbackPlayer === player) {
                playbackPlayer = null
              }
              runCatching { player.stop() }
              player.release()
            }
            runCatching {
              player.setDataSource(playbackPath)
              player.setOnPreparedListener {
                if (requestId != playbackRequestId) {
                  finish(false)
                  return@setOnPreparedListener
                }
                setRuntimeVoiceIndicator(message, "playing")
                if (manual) vm.notice = "正在播放重听"
                it.start()
                if (!waitForCompletion && continuation.isActive) {
                  continuation.resume(true)
                }
              }
              player.setOnCompletionListener {
                finish(true, "朗读完成")
              }
              player.setOnErrorListener { _, _, _ ->
                finish(false, "语音播放失败")
                true
              }
              player.prepareAsync()
            }.onFailure {
              lastError = it
              finish(false, if (manual) "重听失败: ${it.message ?: "未知错误"}" else null)
            }
          }
        }
        if (played == null) {
          stopRuntimePlayback(invalidate = false)
          if (manual) {
            vm.notice = "语音播放超时"
          }
          return@repeat
        }
        segmentPlayed = played
      }
      if (!segmentPlayed) {
        throw (lastError ?: IllegalStateException("重听失败"))
      }
      delay(120L)
    }
    return true
  }

  suspend fun playMessageVoice(
    message: MessageItem,
    manual: Boolean,
    waitForCompletion: Boolean = manual,
    overrideText: String? = null,
  ): Boolean {
    val speakable = normalizePlayableSpeakableText(overrideText ?: vm.displayContentForMessage(message))
    if (speakable.isBlank()) {
      if (manual) vm.notice = "这条对话没有可重听内容"
      return false
    }
    val binding = vm.playVoiceBindingForMessage(message)
    if (binding == null) {
      if (manual) vm.notice = "当前角色未绑定可用音色"
      return replayWithSystemTts(message, speakable, manual, waitForCompletion)
    }
    val bindingKey = runtimeVoiceBindingKey(binding)
    val preferredBinding = runtimeVoiceFallbackBindingCache[bindingKey] ?: binding
    try {
      return playMessageVoiceWithBinding(message, preferredBinding, speakable, manual, waitForCompletion)
    } catch (err: Throwable) {
      var finalError = err
      if (runtimeVoiceBindingKey(preferredBinding) == bindingKey && shouldDowngradeRuntimeVoiceBinding(preferredBinding, err)) {
        val fallbackBinding = resolveFallbackVoiceBinding(message, binding)
        if (fallbackBinding != null && runtimeVoiceBindingKey(fallbackBinding) != bindingKey) {
          setLimitedCacheValue(runtimeVoiceFallbackBindingCache, bindingKey, fallbackBinding)
          try {
            if (manual) {
              vm.notice = "当前绑定音色不可用，正在切换兼容音色"
            }
            return playMessageVoiceWithBinding(message, fallbackBinding, speakable, manual, waitForCompletion)
          } catch (fallbackError: Throwable) {
            finalError = fallbackError
          }
        }
      }
      val fallbackNotice = if (manual) {
        "后端语音不可用，改用系统朗读"
      } else {
        ""
      }
      if (manual) {
        vm.notice = fallbackNotice
      }
      val systemPlayed = replayWithSystemTts(message, speakable, manual, waitForCompletion)
      if (systemPlayed) {
        return true
      }
      if (!manual) {
        vm.notice = "自动语音失败，已跳过"
      }
      if (manual) {
        vm.notice = "重听失败: ${finalError.message ?: "未知错误"}"
      }
      return false
    } finally {
      if (runtimeVoiceMessageKey == vm.messageUiKey(message)) {
        runtimeVoiceMessageKey = ""
        runtimeVoicePhase = ""
        runtimeVoiceIndicator = "."
      }
    }
  }

  fun stopPlaybackSequence() {
    playbackPlaying = false
    playbackRunId += 1
    stopRuntimeAutoVoiceQueue()
  }

  fun launchRuntimeAutoVoice(message: MessageItem, segments: List<String>) {
    val speakableSegments = segments
      .map(::normalizePlayableSpeakableText)
      .filter { it.isNotBlank() }
    if (speakableSegments.isEmpty()) return
    stopRuntimeAutoVoiceQueue(invalidate = true)
    runtimeVoiceAutoJob = scope.launch {
      try {
        for (segment in speakableSegments) {
          ensureActive()
          playMessageVoice(
            message,
            manual = false,
            waitForCompletion = true,
            overrideText = segment,
          )
          ensureActive()
          delay(120L)
        }
      } catch (_: CancellationException) {
      } finally {
        if (runtimeVoiceAutoJob === this) {
          runtimeVoiceAutoJob = null
        }
      }
    }
  }

  fun continueFromPlayback() {
    stopPlaybackSequence()
    vm.sessionViewMode = "live"
    vm.sessionPlaybackStartIndex = 0
    onModeChange("live")
  }

  suspend fun startPlaybackSequence() {
    val playbackSequence = vm.messages.toList().filterNot(vm::isRuntimeRetryMessage)
    if (playbackSequence.isEmpty()) return
    val runId = playbackRunId + 1
    playbackRunId = runId
    playbackPlaying = true
    for (index in playbackCursor until playbackSequence.size) {
      if (runId != playbackRunId) return
      playbackCursor = index
      vm.sessionPlaybackStartIndex = index
      val message = playbackSequence.getOrNull(index) ?: continue
      playMessageVoice(message, manual = false, waitForCompletion = true)
      if (runId != playbackRunId) return
      delay(120L)
    }
    if (runId == playbackRunId) {
      playbackPlaying = false
    }
  }

  DisposableEffect(context) {
    onDispose {
      runCatching { recorder?.stop() }
      recorder?.release()
      recorder = null
      recordFile?.delete()
      recordFile = null
      voiceListening = false
      voiceTranscribing = false
      stopRuntimeAutoVoiceQueue()
      systemTtsInit?.cancel()
      systemTtsInit = null
      systemTts?.let { engine ->
        runCatching { engine.stop() }
        runCatching { engine.shutdown() }
      }
      systemTts = null
    }
  }

  val sessionTitle = vm.playSessionTitle()
  val currentChapter = vm.playCurrentChapter()
  val allMessages = vm.messages.toList()
  val playbackMessages = remember(allMessages) { allMessages.filterNot(vm::isRuntimeRetryMessage) }
  val allMessageKeysFingerprint = allMessages.map { vm.messageUiKey(it) }.joinToString("|")
  val allMessageProgressFingerprint = allMessages.joinToString("|") { message ->
    buildString {
      append(vm.messageUiKey(message))
      append('_').append(message.content)
      append('_').append(vm.isStreamingRuntimeMessage(message))
      append('_').append(vm.streamingSentenceTexts(message).joinToString("||"))
      append('_').append(vm.runtimeMessageStatus(message))
      append('_').append(message.meta?.toString().orEmpty())
    }
  }
  val isSessionPlaybackMode = !vm.debugMode && vm.sessionViewMode == "playback"
  val playbackMaxIndex = maxOf(0, playbackMessages.lastIndex)
  val playbackCurrentMessage = playbackMessages.getOrNull(playbackCursor)
  val playbackProgressLabel = if (playbackMessages.isEmpty()) {
    "暂无可回放台词"
  } else {
    "${playbackCursor + 1}/${playbackMessages.size} · ${vm.displayNameForMessage(playbackCurrentMessage ?: playbackMessages.last())}"
  }
  val displayMessages = when {
    mode == "history" && isSessionPlaybackMode -> playbackMessages.take(minOf(playbackCursor + 1, playbackMessages.size))
    mode == "history" -> allMessages
    else -> revealedMessages.takeLast(1).toList()
  }
  val latestRevealedMessage = revealedMessages.lastOrNull()
  val canPlayerSpeak = vm.playCanPlayerSpeak()
  val canPlayerInput = vm.playCanPlayerInput()
  val listState = rememberLazyListState()
  var debugPanelOpen by remember(vm.currentSessionId) { mutableStateOf(false) }
  fun latestMessageByKey(messageKey: String): MessageItem? {
    return vm.messages.firstOrNull { vm.messageUiKey(it) == messageKey }
  }
  LaunchedEffect(mode, displayMessages.size) {
    if (displayMessages.isNotEmpty()) {
      listState.scrollToItem(displayMessages.lastIndex)
    }
  }
  LaunchedEffect(vm.currentSessionId) {
    debugAutoAdvanceJob?.cancel()
    debugAutoAdvanceJob = null
    onModeChange(if (vm.sessionViewMode == "playback") "history" else "live")
    revealedMessages.clear()
    playbackCursor = vm.sessionPlaybackStartIndex.coerceAtLeast(0)
    playbackPlaying = false
    playbackRunId += 1
    stopRuntimeAutoVoiceQueue()
    debugAutoAdvancing = false
  }
  LaunchedEffect(vm.currentSessionId, vm.sessionViewMode, playbackMessages.size) {
    playbackCursor = vm.sessionPlaybackStartIndex.coerceIn(0, maxOf(0, playbackMessages.lastIndex))
  }
  LaunchedEffect(vm.currentSessionId, autoVoice, mode) {
    if (!autoVoice || mode == "history" || mode == "tips" || mode == "setting") return@LaunchedEffect
    vm.playNarratorVoiceBinding()?.let { warmVoiceBinding(it) }
  }
  LaunchedEffect(mode, isSessionPlaybackMode, vm.currentSessionId) {
    if (mode != "history" || !isSessionPlaybackMode) {
      stopPlaybackSequence()
    }
  }
  LaunchedEffect(vm.currentSessionId, allMessageProgressFingerprint, mode) {
    if (mode == "history") {
      revealedMessages.clear()
      revealedMessages.addAll(allMessages)
      return@LaunchedEffect
    }
    if (allMessages.isEmpty()) {
      revealedMessages.clear()
      return@LaunchedEffect
    }
    val currentKeys = allMessages.map { vm.messageUiKey(it) }
    val revealedKeys = revealedMessages.map { vm.messageUiKey(it) }
    val mismatched = currentKeys.size < revealedKeys.size || revealedKeys.indices.any { index -> currentKeys[index] != revealedKeys[index] }
    if (mismatched) {
      revealedMessages.clear()
      revealedMessages.addAll(allMessages)
      return@LaunchedEffect
    }
    revealedKeys.indices.forEach { index ->
      val latest = allMessages[index]
      if (revealedMessages[index] != latest) {
        revealedMessages[index] = latest
      }
    }
  }
  LaunchedEffect(vm.currentSessionId, allMessageKeysFingerprint, mode, autoVoice, vm.debugLoading) {
    if (mode == "history") {
      revealedMessages.clear()
      revealedMessages.addAll(allMessages)
      return@LaunchedEffect
    }
    if (mode == "tips" || mode == "setting" || vm.debugLoading) return@LaunchedEffect
    if (allMessages.isEmpty()) {
      revealedMessages.clear()
      return@LaunchedEffect
    }
    val currentKeys = allMessages.map { vm.messageUiKey(it) }
    val revealedKeys = revealedMessages.map { vm.messageUiKey(it) }
    val mismatched = currentKeys.size < revealedKeys.size || revealedKeys.indices.any { index -> currentKeys[index] != revealedKeys[index] }
    if (mismatched) {
      revealedMessages.clear()
      revealedMessages.addAll(allMessages)
      return@LaunchedEffect
    }
    val newMessages = allMessages.drop(revealedKeys.size)
    for (message in newMessages) {
      val messageKey = vm.messageUiKey(message)
      revealedMessages.add(latestMessageByKey(messageKey) ?: message)
      if (revealedMessages.isNotEmpty()) {
        listState.scrollToItem(revealedMessages.lastIndex)
      }
      var currentMessage = latestMessageByKey(messageKey) ?: message
      if (vm.isRuntimeRetryMessage(currentMessage)) {
        continue
      }
      vm.setRuntimeMessageStatus(currentMessage.id, "revealing")
      var streamedSentenceCount = 0
      val queuedVoiceSegments = mutableListOf<String>()
      if (vm.isStreamingRuntimeMessage(currentMessage)) {
        while (true) {
          currentMessage = latestMessageByKey(messageKey) ?: break
          if (!vm.isStreamingRuntimeMessage(currentMessage)) break
          val sentences = vm.streamingSentenceTexts(currentMessage)
          while (streamedSentenceCount < sentences.size) {
            val sentence = sentences[streamedSentenceCount]
            streamedSentenceCount += 1
            if (sentence.isBlank()) continue
            if (autoVoice) {
              queuedVoiceSegments += sentence
            }
          }
          delay(120)
        }
        currentMessage = latestMessageByKey(messageKey) ?: currentMessage
        val finalSentences = vm.streamingSentenceTexts(currentMessage)
        while (streamedSentenceCount < finalSentences.size) {
          val sentence = finalSentences[streamedSentenceCount]
          streamedSentenceCount += 1
          if (sentence.isBlank()) continue
          if (autoVoice) {
            queuedVoiceSegments += sentence
          }
        }
      }
      currentMessage = latestMessageByKey(messageKey) ?: currentMessage
      val displayContent = vm.displayContentForMessage(currentMessage)
      if (currentMessage.roleType == "player") {
        vm.setRuntimeMessageStatus(currentMessage.id, "waiting_player")
        delay(180)
        continue
      }
      if (!autoVoice) {
        vm.setRuntimeMessageStatus(currentMessage.id, if (canPlayerSpeak) "waiting_player" else "waiting_next")
        delay(estimateRevealDelayMs(displayContent))
      } else {
        val voiceSegments = if (queuedVoiceSegments.isNotEmpty()) queuedVoiceSegments.toList() else listOf(displayContent)
        val playableSegments = voiceSegments.map(::sanitizeSpeakableText).filter { it.isNotBlank() }
        if (playableSegments.isNotEmpty()) {
          vm.setRuntimeMessageStatus(currentMessage.id, "voicing")
          launchRuntimeAutoVoice(currentMessage, playableSegments)
        }
        vm.setRuntimeMessageStatus(currentMessage.id, if (canPlayerSpeak) "waiting_player" else "waiting_next")
        delay(if (playableSegments.isNotEmpty()) 260 else estimateRevealDelayMs(displayContent))
      }
    }
  }
  LaunchedEffect(
    vm.currentSessionId,
    mode,
    vm.debugMode,
    vm.debugLoading,
    vm.debugEndDialog,
    canPlayerSpeak,
    latestRevealedMessage?.let { vm.messageUiKey(it) } ?: "",
    latestRevealedMessage?.let { vm.isStreamingRuntimeMessage(it) } ?: false,
    runtimeVoiceMessageKey,
    runtimeVoicePhase,
  ) {
    if (mode != "live" || vm.debugLoading || vm.debugEndDialog != null) {
      return@LaunchedEffect
    }
    val latest = latestRevealedMessage ?: return@LaunchedEffect
    if (latest.roleType == "player" || vm.isRuntimeRetryMessage(latest) || vm.isStreamingRuntimeMessage(latest)) {
      return@LaunchedEffect
    }
    val sameVoiceTarget = runtimeVoiceMessageKey == vm.messageUiKey(latest)
    var latestStatus = vm.runtimeMessageStatus(latest)
    if (!sameVoiceTarget && latestStatus in listOf("", "generated", "revealing", "voicing")) {
      latestStatus = if (canPlayerSpeak) "waiting_player" else "waiting_next"
      vm.setRuntimeMessageStatus(latest.id, latestStatus)
    }
    if (!debugAutoAdvancing && latestStatus == "auto_advancing") {
      latestStatus = if (canPlayerSpeak) "waiting_player" else "waiting_next"
      vm.setRuntimeMessageStatus(latest.id, latestStatus)
    }
    if (canPlayerSpeak) {
      return@LaunchedEffect
    }
    if (latestStatus != "waiting_next") {
      return@LaunchedEffect
    }
    val messageKey = vm.messageUiKey(latest)
    if (messageKey.isBlank() || debugAutoAdvancing || debugAutoAdvanceJob?.isActive == true) {
      return@LaunchedEffect
    }
    debugAutoAdvancing = true
    val latestMessageId = latest.id
    vm.setRuntimeMessageStatus(latestMessageId, "auto_advancing")
    var launchedJob: Job? = null
    launchedJob = scope.launch {
      try {
        val ok = if (vm.debugMode) vm.continueDebugNarrative() else vm.continueSessionNarrative()
        if (!ok) {
          vm.setRuntimeMessageStatus(latestMessageId, "error")
        }
      } finally {
        if (debugAutoAdvanceJob === launchedJob) {
          debugAutoAdvanceJob = null
        }
        debugAutoAdvancing = false
      }
    }
    debugAutoAdvanceJob = launchedJob
  }
  LaunchedEffect(mode) {
    if (mode == "tips" || mode == "setting") {
      debugAutoAdvanceJob?.cancel()
      debugAutoAdvanceJob = null
      debugAutoAdvancing = false
      selectedMessage = null
      dialogMenuAnchorBounds = null
      onCloseDialogMenu()
      stopRuntimeAutoVoiceQueue()
    }
  }
  LaunchedEffect(autoVoice) {
    if (!autoVoice) {
      stopRuntimeAutoVoiceQueue()
    }
  }

  val tipOptions = vm.buildAiTipOptions()
  val statePreview = vm.playStatePreview()
  val playTitle = vm.playWorldName()
  val chapterTitle = vm.playChapterTitle()
  val chapterObjectiveText = vm.playVisibleChapterObjective()
  val chapterObjectivePreview = remember(chapterObjectiveText) {
    val normalized = chapterObjectiveText.replace("\\s+".toRegex(), " ").trim()
    if (normalized.length > 20) "${normalized.take(20)}..." else normalized
  }
  val activeMiniGame = vm.playRuntimeMiniGame()
  val playerAvatarPath = vm.userAvatarPath.trim().ifBlank { null }
  val playerAvatarBgPath = vm.userAvatarBgPath.trim().ifBlank { null }
  val chapterBackgroundPath = vm.playChapterBackgroundPath().trim().ifBlank { null }
  val currentLiveMessage = if (mode == "history") null else displayMessages.lastOrNull()
  val currentLiveFigureFgPath = currentLiveMessage
    ?.takeIf { !vm.isRuntimeRetryMessage(it) }
    ?.let { vm.avatarPathForMessage(it).trim().ifBlank { null } }
  val closeDialogMenu = {
    selectedMessage = null
    dialogMenuAnchorBounds = null
    onCloseDialogMenu()
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF15283F))) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    if (chapterBackgroundPath != null) {
      AsyncImage(
        model = chapterBackgroundPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(modifier = Modifier.fillMaxSize().background(Color(0xAA0F1D31)))
    }
    if (currentLiveFigureFgPath != null) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(0.74f),
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              brush = Brush.radialGradient(
                colors = listOf(Color(0x587EAFF4), Color.Transparent),
                radius = 620f,
              ),
            ),
        )
        if (currentLiveFigureFgPath != null) {
          AsyncImage(
            model = currentLiveFigureFgPath,
            contentDescription = null,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxSize()
              .alpha(0.98f),
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.Fit,
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x120C1828), Color(0xA80B1728)),
              ),
            ),
        )
      }
    }
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth(0.9f) // 90% 宽度
          .then(
            // 最大宽度限制 720.dp
            Modifier.width(min(720.dp, androidx.compose.ui.unit.Dp.Infinity))
          )
          // 圆角
          .clip(RoundedCornerShape(20.dp))
          // 边框
          .border(1.dp, Color(0xFF3A3F50), RoundedCornerShape(20.dp))
          // 背景色
          .background(Color(0xFF1E253A))
          // 内边距
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(0.9f),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircleGhostBtn(
            icon = Icons.Outlined.ArrowBack,
            contentDescription = if (vm.debugMode) "返回编辑" else "返回故事大厅",
          ) {
            if (vm.debugMode) {
              onExitDebug()
            } else {
              vm.setTab("故事大厅")
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              playTitle,
              color = textSoft,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (vm.debugMode) "章节：$chapterTitle（调试）" else "章节：$chapterTitle",
              color = Color(0xFFBED2F0),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
        CircleGhostBtn(
          icon = if (autoVoice) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
          contentDescription = "切换语音",
        ) { onToggleVoice() }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (displayMessages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (vm.sessionOpening) "正在进入故事..." else "当前会话暂无消息，发送一句话开始。", color = Color(0xFFD5E6FF))
          }
        } else if (mode != "history") {
          val msg = displayMessages.last()
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp),
          ) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 206.dp),
            ) {
              Column(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .widthIn(max = 640.dp)
                  .padding(end = 34.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (vm.isRuntimeRetryMessage(msg)) {
                  RuntimeRetryCard(
                    title = vm.displayNameForMessage(msg),
                    content = msg.content.ifBlank { "模型调用失败" },
                    onRetry = { vm.retryRuntimeFailure() },
                    modifier = Modifier.fillMaxWidth(),
                  )
                } else {
                  val displayContent = vm.displayContentForMessage(msg)
                  val loading = vm.isStreamingRuntimeMessage(msg) && displayContent.isBlank()
                  val loadingText = vm.loadingHintForMessage(msg)
                  LiveStoryCard(
                    title = vm.displayNameForMessage(msg),
                    content = displayContent.ifBlank { "（空消息）" },
                    loading = loading,
                    loadingText = loadingText,
                    roleType = msg.roleType,
                    reaction = vm.reactionForMessage(msg),
                    voiceTail = messageVoiceTail(msg),
                    voicePlaying = runtimeVoicePhase == "playing" && messageVoiceTail(msg).isNotBlank(),
                    onOpenMenu = { bounds ->
                      if (vm.isStreamingRuntimeMessage(msg)) return@LiveStoryCard
                      dialogMenuAnchorBounds = bounds
                      selectedMessage = msg
                      onOpenDialogMenu()
                    },
                    modifier = Modifier
                      .fillMaxWidth(),
                  )
                }
              }
              if (mode != "setting" && mode != "tips" && tipOptions.isNotEmpty()) {
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 18.dp),
                ) {
                  TipFloatBtn(onClick = { onModeChange("tips") })
                }
              }
            }
          }
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(displayMessages, key = { "${it.id}_${it.createTime}" }) { msg ->
              if (vm.isRuntimeRetryMessage(msg)) {
                RuntimeRetryCard(
                  title = vm.displayNameForMessage(msg),
                  content = msg.content.ifBlank { "模型调用失败" },
                  onRetry = { vm.retryRuntimeFailure() },
                  modifier = Modifier.fillMaxWidth(),
                )
              } else {
                val displayContent = vm.displayContentForMessage(msg)
                val loading = vm.isStreamingRuntimeMessage(msg) && displayContent.isBlank()
                val loadingText = vm.loadingHintForMessage(msg)
                Bubble(
                  title = vm.displayNameForMessage(msg),
                  content = displayContent,
                  loading = loading,
                  loadingText = loadingText,
                  roleType = msg.roleType,
                  avatarPath = vm.avatarPathForMessage(msg).trim().ifBlank { if (msg.roleType == "player") playerAvatarPath else null },
                  avatarBgPath = vm.avatarBgPathForMessage(msg).trim().ifBlank { if (msg.roleType == "player") playerAvatarBgPath else null },
                  reaction = vm.reactionForMessage(msg),
                  voiceTail = messageVoiceTail(msg),
                  voicePlaying = runtimeVoicePhase == "playing" && messageVoiceTail(msg).isNotBlank(),
                  onOpenMenu = { bounds ->
                    if (vm.isStreamingRuntimeMessage(msg)) return@Bubble
                    dialogMenuAnchorBounds = bounds
                    selectedMessage = msg
                    onOpenDialogMenu()
                  },
                )
              }
            }
          }
        }

        if (mode == "tips") {
          AiTipPanel(
            options = tipOptions,
            onPick = { option ->
              vm.sendText = option
              vm.sendMessage()
              onModeChange("live")
            },
            onBack = { onModeChange("live") },
          )
        }

        if (mode == "setting") {
          StorySettingPanel(
            worldName = vm.playWorldName(),
            worldIntro = vm.playWorldIntro(),
            globalBackground = vm.playGlobalBackground(),
            chapterTitle = chapterTitle,
            chapterOpeningRole = currentChapter?.openingRole.orEmpty(),
            chapterOpeningLine = currentChapter?.openingText.orEmpty(),
            chapterContent = currentChapter?.content?.ifBlank { "暂无章节内容" } ?: "暂无章节内容",
            chapterCondition = vm.playChapterConditionText(),
            roles = vm.playStoryRoles(),
            allowRoleView = vm.playAllowRoleView(),
            statePreview = statePreview,
            showStorySettingDetail = showStorySettingDetail,
            onToggleStorySettingDetail = onToggleStorySettingDetail,
            onClose = onCloseSetting,
          )
        }

        if (showDialogMenu && mode != "tips" && mode != "setting" && selectedMessage != null) {
          val dialogMessage = selectedMessage!!
          val anchorBounds = dialogMenuAnchorBounds
          if (anchorBounds != null) {
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
            val menuWidthPx = with(density) { 248.dp.roundToPx() }
            val menuHeightPx = with(density) { 332.dp.roundToPx() }
            val menuMarginPx = with(density) { 12.dp.roundToPx() }
            val menuGapPx = with(density) { 12.dp.roundToPx() }
            val menuMaxX = (screenWidthPx - menuWidthPx - menuMarginPx).coerceAtLeast(menuMarginPx)
            val menuMaxY = (screenHeightPx - menuHeightPx - menuMarginPx).coerceAtLeast(menuMarginPx)
            val preferredY = anchorBounds.top.toInt() - menuHeightPx - menuGapPx
            val fallbackY = anchorBounds.bottom.toInt() + menuGapPx
            val popupOffset = IntOffset(
              x = anchorBounds.left.toInt().coerceIn(menuMarginPx, menuMaxX),
              y = if (preferredY >= menuMarginPx) {
                preferredY.coerceIn(menuMarginPx, menuMaxY)
              } else {
                fallbackY.coerceIn(menuMarginPx, menuMaxY)
              },
            )
            Popup(
              alignment = Alignment.TopStart,
              offset = popupOffset,
              onDismissRequest = closeDialogMenu,
              properties = PopupProperties(focusable = true),
            ) {
              DialogMenu(
                message = dialogMessage,
                reaction = vm.reactionForMessage(dialogMessage),
                onCopy = {
                  clipboardManager.setText(AnnotatedString(vm.displayContentForMessage(dialogMessage)))
                  vm.notice = "已复制对话内容"
                  closeDialogMenu()
                },
                onReplay = {
                  if (vm.isStreamingRuntimeMessage(dialogMessage)) {
                    vm.notice = "正文仍在生成中"
                    closeDialogMenu()
                    return@DialogMenu
                  }
                  val content = vm.displayContentForMessage(dialogMessage).trim()
                  if (content.isBlank()) {
                    vm.notice = "这条对话没有可重听内容"
                  } else {
                    scope.launch { playMessageVoice(dialogMessage, manual = true) }
                  }
                  closeDialogMenu()
                },
                onLike = {
                  vm.setReactionForMessage(dialogMessage, "like")
                  closeDialogMenu()
                },
                onDislike = {
                  vm.setReactionForMessage(dialogMessage, "dislike")
                  closeDialogMenu()
                },
                onRewrite = {
                  vm.applyRewritePrompt(dialogMessage)
                  if (dialogMessage.roleType == "player" && vm.canDeleteMessage(dialogMessage)) {
                    vm.deleteMessage(dialogMessage)
                  }
                  closeDialogMenu()
                },
                onDelete = if (vm.canDeleteMessage(dialogMessage)) {
                  {
                    deletingMessage = dialogMessage
                    closeDialogMenu()
                  }
                } else {
                  null
                },
                onClose = closeDialogMenu,
              )
            }
          }
        }

        deletingMessage?.let { targetMessage ->
          AlertDialog(
            onDismissRequest = { deletingMessage = null },
            confirmButton = {
              TextButton(
                onClick = {
                  deletingMessage = null
                  vm.deleteMessage(targetMessage)
                },
              ) {
                Text("删除", color = Color(0xFFFF8E8E), fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { deletingMessage = null }) {
                Text("取消", color = Color(0xFF7E8EA8))
              }
            },
            title = { Text("删除这条台词？", fontWeight = FontWeight.Bold) },
            text = {
              Text(
                "当前只支持删除最后一条玩家台词。删除后会回到可重新输入的状态。",
                color = Color(0xFF42546F),
              )
            },
            containerColor = Color.White,
          )
        }

        if (vm.debugLoading) {
          Box(
            modifier = Modifier.fillMaxSize().background(Color(0x800A1626)),
            contentAlignment = Alignment.Center,
          ) {
            Card(
              modifier = Modifier.fillMaxWidth(0.72f),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xF0132740)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(color = Color(0xFFFFD84A), strokeWidth = 3.dp)
                Text("进入调试中", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                  vm.debugLoadingStage.ifBlank { "正在初始化调试上下文..." },
                  color = Color(0xFFD5E3F8),
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }

      FooterBar(
        mode = mode,
        miniGame = activeMiniGame,
        debugMode = vm.debugMode,
        runtimeChatDebug = vm.playLatestRuntimeChatDebug(),
        debugPanelOpen = debugPanelOpen,
        isSessionPlaybackMode = isSessionPlaybackMode,
        playbackProgressLabel = playbackProgressLabel,
        playbackCursor = playbackCursor,
        playbackMaxIndex = playbackMaxIndex,
        playbackPlaying = playbackPlaying,
        playbackCanPlay = playbackMessages.isNotEmpty(),
        storyTitle = playTitle,
        storySubtitle = "@${vm.playStoryRoles().firstOrNull { it.roleType != "player" }?.name ?: "旁白"}",
        chapterObjectivePreview = chapterObjectivePreview,
        canPlayerInput = canPlayerInput,
        sendPending = vm.sendPending,
        inputMode = inputMode,
        voiceListening = voiceListening,
        voiceTranscribing = voiceTranscribing,
        inputPlaceholder = vm.playInputPlaceholder(inputMode == "text"),
        turnHint = vm.playTurnHint(),
        onModeChange = onModeChange,
        onOpenSetting = { onModeChange("setting") },
        onOpenObjective = {
          if (!showStorySettingDetail) {
            onToggleStorySettingDetail()
          }
          onModeChange("setting")
        },
        onShare = {
          clipboardManager.setText(AnnotatedString("$sessionTitle $chapterTitle".trim()))
          vm.notice = "已复制故事标题"
        },
        onComment = { vm.notice = "评论功能待接入" },
        sendText = vm.sendText,
        onSendTextChange = { vm.sendText = it },
        onSend = { vm.sendMessage() },
        onToggleDebugPanel = { debugPanelOpen = !debugPanelOpen },
        onPlaybackCursorChange = { value ->
          playbackCursor = value.coerceIn(0, playbackMaxIndex)
          vm.sessionPlaybackStartIndex = playbackCursor
          stopPlaybackSequence()
        },
        onTogglePlayback = {
          if (playbackPlaying) {
            stopPlaybackSequence()
          } else {
            scope.launch { startPlaybackSequence() }
          }
        },
        onContinueFromPlayback = { continueFromPlayback() },
        onToggleInputMode = {
          inputMode = if (inputMode == "text") "voice" else "text"
          if (inputMode == "text") {
            runCatching { recorder?.stop() }
            recorder?.release()
            recorder = null
            recordFile?.delete()
            recordFile = null
            voiceListening = false
            pendingVoiceStartAfterPermission = false
          }
        },
        onVoicePressStart = {
          if (vm.sendPending) {
            vm.notice = "发送中，请稍候"
          } else if (!vm.playCanPlayerInput()) {
            vm.notice = vm.playTurnHint().ifBlank { "当前还没轮到用户发言" }
          } else if (voiceTranscribing || voiceListening) {
            Unit
          } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) {
              scope.launch {
                startRuntimeVoiceRecording(
                  context = context,
                  onStart = { mediaRecorder, output ->
                    recorder = mediaRecorder
                    recordFile = output
                    voiceListening = true
                  },
                  onFailure = { message -> vm.notice = message },
                )
              }
            } else {
              pendingVoiceStartAfterPermission = true
              recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }
        },
        onVoicePressFinish = { cancel ->
          pendingVoiceStartAfterPermission = false
          if (voiceTranscribing || !voiceListening) {
            Unit
          } else {
            val currentRecorder = recorder
            val currentFile = recordFile
            recorder = null
            recordFile = null
            voiceListening = false
            scope.launch {
              if (cancel) {
                runCatching { currentRecorder?.stop() }
                currentRecorder?.release()
                currentFile?.delete()
                return@launch
              }
              voiceTranscribing = true
              runCatching {
                currentRecorder?.stop()
                currentRecorder?.release()
                val audioFile = currentFile ?: error("录音文件不存在")
                val payload = audioFileToBase64Payload(audioFile)
                val text = vm.transcribeRuntimeVoice(payload, vm.currentSessionId)
                if (text.isBlank()) error("语音识别未返回文本")
                vm.sendText = text
                vm.sendMessage()
              }.onFailure {
                currentRecorder?.release()
                vm.notice = "语音识别失败: ${it.message ?: "未知错误"}"
              }
              currentFile?.delete()
              voiceTranscribing = false
            }
          }
        },
        onMiniGameAction = {
          vm.sendMiniGameAction(it)
        },
      )
    }
  }

  if (vm.debugEndDialog != null) {
    AlertDialog(
      onDismissRequest = { vm.closeDebugDialog(false) },
      title = { Text("章节调试结束") },
      text = {
        Text(
          when (vm.debugEndDialog) {
            "已完结" -> "已完结\n已没有下一个章节。可返回编辑继续补章节。"
            "进入自由剧情" -> "进入自由剧情\n当前章节已完成。继续查看后将进入自由剧情，编排师会继续推进故事。"
            else -> "已失败\n当前调试已结束。"
          },
        )
      },
      confirmButton = {
        TextButton(onClick = {
          vm.closeDebugDialog(true)
          onModeChange("live")
        }) {
          Text("返回编辑")
        }
      },
      dismissButton = {
        TextButton(onClick = { vm.closeDebugDialog(false) }) {
          Text("继续查看")
        }
      },
    )
  }
}

@Composable
private fun ObjectiveChip(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = Color(0x8A091423)),
    shape = RoundedCornerShape(999.dp),
  ) {
    Text(
      text = text,
      color = Color(0xFFF1F7FF),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}

@Composable
private fun LiveStoryCard(
  title: String,
  content: String,
  loading: Boolean = false,
  loadingText: String = "",
  roleType: String = "npc",
  reaction: String = "",
  voiceTail: String = "",
  voicePlaying: Boolean = false,
  onOpenMenu: ((Rect) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isPlayer = roleType == "player"
  val reactionLabel = when (reaction) {
    "like" -> "已点赞"
    "dislike" -> "已点踩"
    else -> ""
  }
  var bubbleBounds by remember { mutableStateOf<Rect?>(null) }
  Box(
    modifier = modifier
      .fillMaxWidth()
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isPlayer) Color(0xE2203657) else Color(0xF6F7FBFF),
      ),
      shape = RoundedCornerShape(22.dp),
    ) {
      Column(
        modifier = Modifier
          .onGloballyPositioned { coordinates ->
            bubbleBounds = coordinates.boundsInWindow()
          }
          .pointerInput(onOpenMenu) {
            detectTapGestures(
              onDoubleTap = { bubbleBounds?.let { bounds -> onOpenMenu?.invoke(bounds) } },
              onLongPress = { bubbleBounds?.let { bounds -> onOpenMenu?.invoke(bounds) } },
            )
          }
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (loading) {
          MessageLoadingDots(
            text = loadingText,
            textColor = if (isPlayer) Color(0xFFE2EEFF) else Color(0xFF4F6486),
            dotColor = if (isPlayer) Color(0x99E2EEFF) else Color(0x8A57709C),
          )
        } else {
          Text(
            text = buildAnnotatedString {
              append(content)
              if (voiceTail.isNotBlank()) {
                withStyle(
                  SpanStyle(
                    color = if (voicePlaying) Color(0xFFF0A91E) else Color(0xFF4D75C7),
                    fontWeight = FontWeight.ExtraBold,
                  ),
                ) {
                  append(" ")
                  append(voiceTail)
                }
              }
            },
            color = if (isPlayer) Color(0xFFE8F2FF) else Color(0xFF20304A),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        if (reactionLabel.isNotBlank()) {
          Surface(
            color = if (isPlayer) Color(0x223BA7FF) else Color(0xFFEAF2FF),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = reactionLabel,
              color = if (isPlayer) Color(0xFFD7E7FF) else Color(0xFF5F7190),
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
    Surface(
      modifier = Modifier.offset(x = 11.dp, y = (-12).dp),
      color = if (isPlayer) Color(0x33FFFFFF) else Color(0xA9314966),
      shape = RoundedCornerShape(10.dp),
    ) {
      Text(
        text = title,
        color = Color(0xFFF0F6FF),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun RuntimeRetryCard(
  title: String,
  content: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxWidth(),
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xF22A1B0D)),
      shape = RoundedCornerShape(22.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = content,
          color = Color(0xFFFFF2E0),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Button(
          onClick = onRetry,
          shape = RoundedCornerShape(999.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFC874),
            contentColor = Color(0xFF35220D),
          ),
        ) {
          Icon(
            imageVector = Icons.Outlined.Replay,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("重试", fontWeight = FontWeight.ExtraBold)
        }
      }
    }
    Surface(
      modifier = Modifier.offset(x = 11.dp, y = (-12).dp),
      color = Color(0xCC5D3A12),
      shape = RoundedCornerShape(10.dp),
    ) {
      Text(
        text = title,
        color = Color(0xFFFFE2B3),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun Bubble(
  title: String,
  content: String,
  loading: Boolean = false,
  loadingText: String = "",
  roleType: String = "npc",
  avatarPath: String? = null,
  avatarBgPath: String? = null,
  reaction: String = "",
  voiceTail: String = "",
  voicePlaying: Boolean = false,
  onOpenMenu: ((Rect) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isPlayer = roleType == "player"
  val reactionLabel = when (reaction) {
    "like" -> "已点赞"
    "dislike" -> "已点踩"
    else -> ""
  }
  val reactionIcon = if (reaction == "dislike") Icons.Outlined.ThumbDown else Icons.Outlined.ThumbUp
  var bubbleBounds by remember { mutableStateOf<Rect?>(null) }
  Column(
    modifier = modifier
      .fillMaxWidth(),
    horizontalAlignment = if (isPlayer) Alignment.End else Alignment.Start,
  ) {
    Text(
      title,
      color = if (isPlayer) Color(0xFFDBE9FF) else textSoft,
      style = MaterialTheme.typography.labelSmall,
    )
    Row(
      modifier = Modifier
        .padding(top = 4.dp)
        .fillMaxWidth(),
      horizontalArrangement = if (isPlayer) Arrangement.End else Arrangement.Start,
      verticalAlignment = Alignment.Top,
    ) {
      if (!isPlayer) {
        SmallAvatar(
          foregroundPath = avatarPath,
          backgroundPath = avatarBgPath,
          title = title,
          size = 42.dp,
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Card(
        modifier = Modifier
          .fillMaxWidth(0.84f)
          .onGloballyPositioned { coordinates ->
            bubbleBounds = coordinates.boundsInWindow()
          }
          .pointerInput(onOpenMenu) {
            detectTapGestures(
              onDoubleTap = { bubbleBounds?.let { bounds -> onOpenMenu?.invoke(bounds) } },
              onLongPress = { bubbleBounds?.let { bounds -> onOpenMenu?.invoke(bounds) } },
            )
          },
        colors = CardDefaults.cardColors(containerColor = if (isPlayer) Color(0xB3274568) else Color(0xE6FFFFFF)),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (loading) {
            MessageLoadingDots(
              text = loadingText,
              textColor = if (isPlayer) Color(0xFFDCEBFF) else Color(0xFF4F6486),
              dotColor = if (isPlayer) Color(0x99DCEBFF) else Color(0x8A57709C),
            )
          } else {
            Text(
              buildAnnotatedString {
                append(content)
                if (voiceTail.isNotBlank()) {
                  withStyle(
                    SpanStyle(
                      color = if (voicePlaying) Color(0xFFF0A91E) else Color(0xFF4D75C7),
                      fontWeight = FontWeight.ExtraBold,
                    ),
                  ) {
                    append(" ")
                    append(voiceTail)
                  }
                }
              },
              color = if (isPlayer) Color(0xFFE8F2FF) else Color(0xFF1F2B40),
              style = MaterialTheme.typography.bodySmall,
            )
          }
          if (reactionLabel.isNotBlank()) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (isPlayer) Color(0x223BA7FF) else Color(0xFFF0F5FF))
                .padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = reactionIcon,
                contentDescription = null,
                tint = if (reaction == "dislike") Color(0xFF6C7A91) else Color(0xFF4E7DD9),
                modifier = Modifier.size(12.dp),
              )
              Text(
                reactionLabel,
                color = if (isPlayer) Color(0xFFD7E7FF) else Color(0xFF5F7190),
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
      if (isPlayer) {
        Spacer(modifier = Modifier.width(6.dp))
        SmallAvatar(
          foregroundPath = avatarPath,
          backgroundPath = avatarBgPath,
          title = title,
          size = 42.dp,
        )
      }
    }
  }
}

@Composable
private fun MessageLoadingDots(
  text: String = "",
  textColor: Color = Color(0xFF4F6486),
  dotColor: Color = Color(0x8A57709C),
) {
  val pulse = rememberInfiniteTransition(label = "message-loading")
  val scales = List(3) { index ->
    pulse.animateFloat(
      initialValue = 0.72f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 620, easing = LinearEasing, delayMillis = index * 140),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "message-loading-$index",
    )
  }
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.defaultMinSize(minHeight = 24.dp),
  ) {
    if (text.isNotBlank()) {
      Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
      )
    }
    scales.forEach { scale ->
      Box(
        modifier = Modifier
          .size(8.dp)
          .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = 0.45f + ((scale.value - 0.72f) / 0.28f) * 0.55f
          }
          .clip(CircleShape)
          .background(dotColor),
      )
    }
  }
}

@Composable
private fun SmallAvatar(
  foregroundPath: String?,
  backgroundPath: String?,
  title: String,
  size: androidx.compose.ui.unit.Dp = 24.dp,
) {
  LayeredAvatarFrame(
    foregroundPath = foregroundPath,
    backgroundPath = backgroundPath,
    modifier = Modifier.size(size),
    backgroundColor = Color(0xFFE5EAF3),
    borderColor = Color(0xFFC7D4E8),
    fallback = {
      Text(
        text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        color = Color(0xFF7D8CA5),
        style = MaterialTheme.typography.labelSmall,
      )
    },
  )
}

@Composable
private fun LayeredAvatarFrame(
  foregroundPath: String?,
  backgroundPath: String?,
  modifier: Modifier,
  backgroundColor: Color,
  borderColor: Color,
  shape: androidx.compose.ui.graphics.Shape = CircleShape,
  fallback: @Composable BoxScope.() -> Unit,
  overlay: @Composable BoxScope.() -> Unit = {},
) {
  Box(
    modifier = modifier
      .clip(shape)
      .border(1.dp, borderColor, shape)
      .background(backgroundColor),
    contentAlignment = Alignment.Center,
  ) {
    if (!backgroundPath.isNullOrBlank()) {
      AsyncImage(
        model = backgroundPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
      )
    }
    if (!foregroundPath.isNullOrBlank()) {
      AsyncImage(
        model = foregroundPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
      )
    } else {
      fallback()
    }
    overlay()
  }
}

@Composable
private fun TipFloatBtn(onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(28.dp)
      .clip(CircleShape)
      .background(Color(0x66122133))
      .border(1.dp, Color(0x70FFFFFF), CircleShape)
      .clickable { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Outlined.AutoAwesome,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(14.dp),
    )
  }
}

@Composable
private fun DialogMenu(
  message: MessageItem,
  reaction: String,
  onCopy: () -> Unit,
  onReplay: () -> Unit,
  onLike: () -> Unit,
  onDislike: () -> Unit,
  onRewrite: () -> Unit,
  onDelete: (() -> Unit)?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.widthIn(min = 196.dp, max = 248.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748)),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = message.role.ifBlank { "当前对话" },
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        text = message.content.ifBlank { "（空消息）" },
        color = Color(0xFFD6E2F2),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Divider(color = Color(0x334B5B72))
      DialogMenuAction(icon = Icons.Outlined.ContentCopy, text = "复制", onClick = onCopy)
      DialogMenuAction(icon = Icons.Outlined.Replay, text = "重听", onClick = onReplay)
      DialogMenuAction(
        icon = Icons.Outlined.ThumbUp,
        text = if (reaction == "like") "取消点赞" else "点赞",
        onClick = onLike,
      )
      DialogMenuAction(
        icon = Icons.Outlined.ThumbDown,
        text = if (reaction == "dislike") "取消点踩" else "点踩",
        onClick = onDislike,
      )
      if (onDelete != null) {
        DialogMenuAction(icon = Icons.Outlined.Delete, text = "删除", onClick = onDelete, tint = Color(0xFFFFA0A0))
      }
      DialogMenuAction(icon = Icons.Outlined.Edit, text = "改写", onClick = onRewrite)
      DialogMenuAction(icon = Icons.Outlined.Close, text = "关闭", onClick = onClose)
    }
  }
}

@Composable
private fun AiTipPanel(options: List<String>, onPick: (String) -> Unit, onBack: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp)
      .offset(y = (-18).dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBFF)),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("AI 提示", color = Color(0xFF233450), fontWeight = FontWeight.Bold)
      options.take(3).forEach { option ->
        MiniBtn(text = option, onClick = { onPick(option) }, full = true)
      }
      Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "返回",
          color = Color(0xFFD6E9FF),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.align(Alignment.CenterEnd).clickable { onBack() },
        )
      }
    }
  }
}

@Composable
private fun DialogMenuAction(icon: ImageVector, text: String, onClick: () -> Unit, tint: Color = Color.White) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 9.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    Text(text = text, color = tint, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun StorySettingPanel(
  worldName: String,
  worldIntro: String,
  globalBackground: String,
  chapterTitle: String,
  chapterOpeningRole: String,
  chapterOpeningLine: String,
  chapterContent: String,
  chapterCondition: String,
  roles: List<com.toonflow.game.data.StoryRole>,
  allowRoleView: Boolean,
  statePreview: String,
  showStorySettingDetail: Boolean,
  onToggleStorySettingDetail: () -> Unit,
  onClose: () -> Unit,
) {
  var selectedRoleId by remember(worldName) { mutableStateOf<String?>(null) }
  var showModePicker by remember(worldName) { mutableStateOf(false) }
  val selectedRole = roles.firstOrNull { it.id == selectedRoleId } ?: roles.firstOrNull()
  val panelScroll = rememberScrollState()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp)
      .heightIn(max = 460.dp)
      .offset(y = (-22).dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xDD0B1A2D)),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier
        .padding(10.dp)
        .verticalScroll(panelScroll),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = worldName,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Center),
        )
        Text(
          text = "关闭",
          color = Color(0xFFD6E9FF),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.align(Alignment.CenterEnd).clickable { onClose() },
        )
      }
      Text(
        "简介：$worldIntro",
        color = Color(0xFFDCEEFF),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
      )
      Text("角色列表", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
      if (roles.isNotEmpty()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          roles.forEach { role ->
            Column(
              modifier = Modifier
                .clickable { selectedRoleId = role.id },
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              SmallAvatar(
                foregroundPath = role.avatarPath.trim().ifBlank { null },
                backgroundPath = role.avatarBgPath.trim().ifBlank { null },
                title = role.name,
                size = 40.dp,
              )
              Text(
                role.name.ifBlank { role.roleType },
                color = Color(0xFFDCEEFF),
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }
      if (!allowRoleView) {
        Text("创作者未开放“他人可查看角色设定”，当前仅展示基础信息。", color = Color(0xFFBFD3F1), style = MaterialTheme.typography.bodySmall)
      } else if (selectedRole != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(selectedRole.name.ifBlank { "未命名角色" }, color = Color.White, fontWeight = FontWeight.Bold)
            Text("角色类型：${selectedRole.roleType}", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
            Text("角色设定：${selectedRole.description.ifBlank { "暂无设定" }}", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
            Text("角色音色：${selectedRole.voice.ifBlank { "未配置" }}", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
            if (selectedRole.sample.isNotBlank()) {
              Text("台词示例：${selectedRole.sample}", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
            }
            selectedRole.parameterCardJson?.let { card ->
              Divider(color = Color(0x22FFFFFF))
              Text("参数卡", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
              ParameterCardDetail(card)
            }
          }
        }
      }
      if (showStorySettingDetail) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("故事设定", color = Color.White, fontWeight = FontWeight.Bold)
            Text("故事背景：${globalBackground.ifBlank { "暂无全局背景" }}", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text("章节：$chapterTitle", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text(
              "开场白：${if (chapterOpeningLine.isNotBlank()) "${chapterOpeningRole.ifBlank { "旁白" }}：$chapterOpeningLine" else "无"}",
              color = Color(0xFFDCEEFF),
              style = MaterialTheme.typography.bodySmall,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
            Text("章节编排：仅供编排师内部使用，游玩时不直接展示。", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text("章节完成条件：$chapterCondition", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text("Runtime状态：$statePreview", color = Color(0xFFAFC6E9), style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
          }
        }
      }
      Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggleStorySettingDetail() },
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
        shape = RoundedCornerShape(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("故事设定", color = Color.White, fontWeight = FontWeight.SemiBold)
          Text(if (showStorySettingDetail) "收起 >" else "> ", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
        }
      }
      Card(
        modifier = Modifier.fillMaxWidth().clickable { showModePicker = !showModePicker },
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
        shape = RoundedCornerShape(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("对话模式", color = Color.White, fontWeight = FontWeight.SemiBold)
          Text("基础模式 >", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
        }
      }
      if (showModePicker) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
          shape = RoundedCornerShape(10.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("✓ 基础模式（当前唯一）", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("当前仅支持基础模式，后续可扩展其他对话模式。", color = Color(0xFFBFD3F1), style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

private fun parameterCardTextValue(value: String): String = value.ifBlank { "未设定" }

private fun parameterCardListValue(values: List<String>): String = if (values.isNotEmpty()) values.joinToString("、") else "未设定"

private fun parameterCardOtherJson(values: List<String>): String = runCatching {
  JSONArray(values).toString()
}.getOrElse { "[]" }

@Composable
private fun ParameterCardField(label: String, value: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(label, color = Color(0xFF93A8C3), style = MaterialTheme.typography.labelSmall)
    val longText = value.length > 120 || label == "原始角色设定"
    if (longText) {
      Box(
        modifier = Modifier
          .heightIn(max = 132.dp)
          .verticalScroll(rememberScrollState()),
      ) {
        Text(value, color = Color(0xFFAFC6E9), style = MaterialTheme.typography.bodySmall)
      }
    } else {
      Text(value, color = Color(0xFFAFC6E9), style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun ParameterCardDetail(card: com.toonflow.game.data.RoleParameterCard) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    ParameterCardField("角色名", parameterCardTextValue(card.name))
    ParameterCardField("原始角色设定", parameterCardTextValue(card.rawSetting))
    ParameterCardField("性别", parameterCardTextValue(card.gender))
    ParameterCardField("年龄", card.age?.toString() ?: "未设定")
    ParameterCardField("等级", card.level.toString())
    ParameterCardField("等级称号", parameterCardTextValue(card.levelDesc))
    ParameterCardField("性格", parameterCardTextValue(card.personality))
    ParameterCardField("外貌", parameterCardTextValue(card.appearance))
    ParameterCardField("音色特点", parameterCardTextValue(card.voice))
    ParameterCardField("技能", parameterCardListValue(card.skills))
    ParameterCardField("物品", parameterCardListValue(card.items))
    ParameterCardField("装备", parameterCardListValue(card.equipment))
    ParameterCardField("血量", card.hp.toString())
    ParameterCardField("蓝量", card.mp.toString())
    ParameterCardField("金钱", card.money.toString())
    ParameterCardField("其他", parameterCardOtherJson(card.other))
  }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun FooterBar(
  mode: String,
  miniGame: MainViewModel.RuntimeMiniGameView?,
  debugMode: Boolean,
  runtimeChatDebug: MainViewModel.RuntimeChatDebugItem?,
  debugPanelOpen: Boolean,
  isSessionPlaybackMode: Boolean,
  playbackProgressLabel: String,
  playbackCursor: Int,
  playbackMaxIndex: Int,
  playbackPlaying: Boolean,
  playbackCanPlay: Boolean,
  storyTitle: String,
  storySubtitle: String,
  chapterObjectivePreview: String,
  canPlayerInput: Boolean,
  sendPending: Boolean,
  inputMode: String,
  voiceListening: Boolean,
  voiceTranscribing: Boolean,
  inputPlaceholder: String,
  turnHint: String,
  onModeChange: (String) -> Unit,
  onOpenSetting: () -> Unit,
  onOpenObjective: () -> Unit,
  onShare: () -> Unit,
  onComment: () -> Unit,
  sendText: String,
  onSendTextChange: (String) -> Unit,
  onSend: () -> Unit,
  onToggleDebugPanel: () -> Unit,
  onPlaybackCursorChange: (Int) -> Unit,
  onTogglePlayback: () -> Unit,
  onContinueFromPlayback: () -> Unit,
  onToggleInputMode: () -> Unit,
  onVoicePressStart: () -> Unit,
  onVoicePressFinish: (Boolean) -> Unit,
  onMiniGameAction: (String) -> Unit,
) {
  val miniGameActive = miniGame != null && mode != "tips" && mode != "setting"
  val playbackModeActive = mode == "history" && isSessionPlaybackMode
  Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (miniGameActive) {
      miniGame?.let { active ->
        MiniGamePanel(miniGame = active, onAction = onMiniGameAction)
      }
    }
    if (chapterObjectivePreview.isNotBlank() && mode != "history" && mode != "tips" && mode != "setting") {
      ObjectiveChip(
        text = "当前目标：$chapterObjectivePreview",
        onClick = onOpenObjective,
        modifier = Modifier
          .padding(bottom = 2.dp)
          .widthIn(max = 220.dp),
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSetting() },
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = storyTitle,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp).rotate(180f),
          )
        }
        Text(
          text = storySubtitle,
          color = Color(0xFFD7E7FF),
          style = MaterialTheme.typography.labelSmall,
        )
        if (runtimeChatDebug != null) {
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .size(18.dp)
              .border(BorderStroke(1.dp, Color(0x47D8E8FF)), CircleShape)
              .clickable { onToggleDebugPanel() },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = if (debugPanelOpen) "隐藏调试状态" else "显示调试状态",
              tint = Color(0xFFDCE9FF),
              modifier = Modifier.size(11.dp),
            )
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        StoryFooterAction(icon = Icons.Outlined.FavoriteBorder, label = "0", onClick = { })
        StoryFooterAction(icon = Icons.Outlined.Share, label = "分享", onClick = onShare)
        StoryFooterAction(icon = Icons.Outlined.ChatBubbleOutline, label = "评论", onClick = onComment)
        StoryFooterAction(
          icon = if (mode == "history") Icons.Outlined.ArrowBack else Icons.Outlined.History,
          label = if (mode == "history") {
            if (isSessionPlaybackMode) "继续聊" else "返回"
          } else {
            "历史"
          },
          onClick = { onModeChange(if (mode == "history") "live" else "history") },
        )
      }
    }
    if (runtimeChatDebug != null && debugPanelOpen) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x70122740)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x33D8E8FF)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "会话 ${runtimeChatDebug.conversationId.take(10)}…  消息 ${runtimeChatDebug.messageId}  序号 ${runtimeChatDebug.lineIndex}",
            color = Color(0xFFFFE2A0),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${runtimeChatDebug.currentRole.ifBlank { "未知角色" }} · ${runtimeStatusLabel(runtimeChatDebug.currentStatus)} · 下一位 ${runtimeChatDebug.nextRole.ifBlank { "当前角色" }}",
            color = Color(0xFFEAF3FF),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }
    if (turnHint.isNotBlank()) {
      Text(turnHint, color = Color(0xFFD7E7FF), style = MaterialTheme.typography.bodySmall)
    }
    if (playbackModeActive) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC132844)),
        shape = RoundedCornerShape(14.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "剧情回放",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              playbackProgressLabel,
              color = Color(0xFFD7E7FF),
              style = MaterialTheme.typography.labelSmall,
            )
          }
          Slider(
            value = playbackCursor.toFloat(),
            onValueChange = { onPlaybackCursorChange(it.roundToInt()) },
            valueRange = 0f..playbackMaxIndex.toFloat(),
            enabled = playbackCanPlay && playbackMaxIndex > 0,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onTogglePlayback,
              enabled = playbackCanPlay,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x221C7DFF),
                contentColor = Color.White,
                disabledContainerColor = Color(0x33221E32),
                disabledContentColor = Color(0x99D7E7FF),
              ),
            ) {
              Icon(
                imageVector = if (playbackPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (playbackPlaying) "暂停" else "播放")
            }
            Button(
              onClick = onContinueFromPlayback,
              enabled = playbackCanPlay,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF7FBFF),
                contentColor = Color(0xFF20304A),
                disabledContainerColor = Color(0x66F7FBFF),
                disabledContentColor = Color(0xFFE3EEFF),
              ),
            ) {
              Text("继续聊")
            }
          }
        }
      }
      Text(
        "当前为剧情回放模式，拖动进度条或点击播放继续观看。",
        color = Color(0xFFD7E7FF),
        style = MaterialTheme.typography.bodySmall,
      )
    } else if (miniGameActive && miniGame?.acceptsTextInput != true) {
      Text(
        "小游戏进行中，请使用上方面板操作。",
        color = Color(0xFFD7E7FF),
        style = MaterialTheme.typography.bodySmall,
      )
    } else if (inputMode == "text") {
      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = sendText,
          onValueChange = onSendTextChange,
          modifier = Modifier.weight(1f),
          enabled = canPlayerInput && !sendPending,
          placeholder = { Text(inputPlaceholder) },
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFBED6FF),
            unfocusedBorderColor = Color(0x88BED6FF),
            disabledBorderColor = Color(0x66BED6FF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color(0xFFD9E7FB),
            focusedPlaceholderColor = Color(0xFFD9E7FB),
            unfocusedPlaceholderColor = Color(0xBFD9E7FB),
            disabledPlaceholderColor = Color(0xBFD9E7FB),
            focusedContainerColor = Color(0x22111E32),
            unfocusedContainerColor = Color(0x22111E32),
            disabledContainerColor = Color(0x33111E32),
            cursorColor = Color(0xFFFFD36A),
          ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        MiniBtn(text = "声", onClick = onToggleInputMode)
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = onSend,
          enabled = canPlayerInput && !sendPending,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF7FBFF),
            contentColor = Color(0xFF20304A),
            disabledContainerColor = Color(0x66F7FBFF),
            disabledContentColor = Color(0xFFE3EEFF),
          ),
        ) {
          Text(if (sendPending) "发送中..." else "发送")
        }
      }
    } else {
      val density = LocalDensity.current
      var holdCancelPending by remember(voiceListening, voiceTranscribing) { mutableStateOf(false) }
      var holdStartY by remember(inputMode, voiceListening) { mutableStateOf(0f) }
      val cancelDistancePx = with(density) { 76.dp.toPx() }
      LaunchedEffect(voiceListening, voiceTranscribing) {
        if (!voiceListening || voiceTranscribing) {
          holdCancelPending = false
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          color = if (canPlayerInput && !sendPending && !voiceTranscribing) Color(0xFFF7FBFF) else Color(0x66F7FBFF),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .pointerInteropFilter { event ->
                if (!canPlayerInput || sendPending || voiceTranscribing) {
                  return@pointerInteropFilter false
                }
                when (event.actionMasked) {
                  MotionEvent.ACTION_DOWN -> {
                    holdStartY = event.rawY
                    holdCancelPending = false
                    onVoicePressStart()
                    true
                  }
                  MotionEvent.ACTION_MOVE -> {
                    if (voiceListening) {
                      holdCancelPending = holdStartY - event.rawY > cancelDistancePx
                    }
                    true
                  }
                  MotionEvent.ACTION_UP -> {
                    onVoicePressFinish(holdCancelPending)
                    holdCancelPending = false
                    true
                  }
                  MotionEvent.ACTION_CANCEL -> {
                    onVoicePressFinish(true)
                    holdCancelPending = false
                    true
                  }
                  else -> false
                }
              }
              .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              when {
                voiceTranscribing -> "识别处理中..."
                sendPending -> "发送中..."
                voiceListening && holdCancelPending -> "松开取消"
                voiceListening -> "松开发送，上滑取消"
                else -> "按住说话"
              },
              color = if (canPlayerInput && !sendPending && !voiceTranscribing) Color(0xFF20304A) else Color(0xFFE3EEFF),
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        MiniBtn(text = "键", onClick = onToggleInputMode)
      }
    }
  }
}

private fun runtimeStatusLabel(status: String): String {
  return when (status.trim()) {
    "waiting_next" -> "等待下一位"
    "waiting_player" -> "等待玩家"
    "auto_advancing" -> "自动推进中"
    "revealing" -> "展示中"
    "streaming" -> "流式生成中"
    "generated" -> "已生成"
    "voicing" -> "语音中"
    "error" -> "异常"
    else -> if (status.isBlank()) "未知" else status
  }
}

private suspend fun startRuntimeVoiceRecording(
  context: Context,
  onStart: (MediaRecorder, File) -> Unit,
  onFailure: (String) -> Unit,
) {
  runCatching {
    val outputFile = File.createTempFile("toonflow_play_", ".m4a", context.cacheDir)
    val mediaRecorder = MediaRecorder().apply {
      setAudioSource(MediaRecorder.AudioSource.MIC)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      setAudioSamplingRate(16000)
      setAudioEncodingBitRate(96000)
      setOutputFile(outputFile.absolutePath)
      prepare()
      start()
    }
    onStart(mediaRecorder, outputFile)
  }.onFailure {
    onFailure("无法开始录音: ${it.message ?: "未知错误"}")
  }
}

private fun audioFileToBase64Payload(file: File): String {
  val ext = file.extension.trim().lowercase()
  val mime = when (ext) {
    "mp3" -> "audio/mpeg"
    "ogg" -> "audio/ogg"
    "webm" -> "audio/webm"
    "aac" -> "audio/aac"
    "m4a", "mp4" -> "audio/mp4"
    else -> "audio/wav"
  }
  return "data:$mime;base64,${Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)}"
}

@Composable
private fun MiniGamePanel(
  miniGame: MainViewModel.RuntimeMiniGameView,
  onAction: (String) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = Color(0xCC132844)),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            miniGame.displayName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            "第 ${if (miniGame.round > 0) miniGame.round else 1} 轮 · ${miniGame.phase.ifBlank { "进行中" }}",
            color = Color(0xFFD1E4FF),
            style = MaterialTheme.typography.labelSmall,
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x223A7BFF))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
          Text(
            miniGame.status.ifBlank { "active" },
            color = Color(0xFFDCEAFF),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
      if (miniGame.ruleSummary.isNotBlank()) {
        Text(
          miniGame.ruleSummary,
          color = Color(0xFFBFD4F1),
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (miniGame.stateItems.isNotEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x18FFFFFF)),
          shape = RoundedCornerShape(10.dp),
        ) {
          Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            miniGame.stateItems.forEach { item ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  item.key,
                  color = Color(0xFF9BB5D7),
                  style = MaterialTheme.typography.labelSmall,
                )
                Text(
                  item.value,
                  color = Color.White,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }
      if (miniGame.narration.isNotBlank()) {
        Text(
          miniGame.narration,
          color = Color(0xFFE6F1FF),
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (!miniGame.pendingExit && miniGame.playerOptions.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          miniGame.playerOptions.forEach { option ->
            MiniBtn(
              text = option.label,
              onClick = { onAction(option.label) },
              full = true,
            )
          }
        }
      }
      if (miniGame.controlOptions.isNotEmpty()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          miniGame.controlOptions.forEach { action ->
            MiniBtn(text = action, onClick = { onAction(action) })
          }
        }
      }
    }
  }
}

@Composable
private fun StoryFooterAction(icon: ImageVector, label: String, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 2.dp, vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(1.dp),
  ) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
    Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun ProfileScene(
  vm: MainViewModel,
  onPickAvatar: () -> Unit,
  onPickAvatarVideoGif: () -> Unit,
) {
  var showAvatarActionDialog by remember { mutableStateOf(false) }
  var showImageGenerateDialog by remember { mutableStateOf(false) }
  var showDraftListPage by remember { mutableStateOf(false) }
  var deletingDraft by remember { mutableStateOf<WorldItem?>(null) }
  var draftMenuWorld by remember { mutableStateOf<WorldItem?>(null) }
  var imageGeneratePrompt by remember { mutableStateOf("") }
  var imageGenerateStyleKey by remember { mutableStateOf("general_3") }
  val imageGenerateReferenceUris = remember { mutableStateListOf<String>() }
  val allWorlds = vm.worldsForSelectedProject()
  val publishedWorlds = allWorlds.filter { vm.isWorldPublished(it) }
  val draftWorlds = allWorlds.filter { !vm.isWorldPublished(it) }
  val latestDraft = draftWorlds.firstOrNull()
  val firstPublished = publishedWorlds.firstOrNull()
  val remainingPublished = publishedWorlds.drop(1)
  val likeCount = publishedWorlds.sumOf { it.sessionCount ?: 0 }
  val followCount = if (publishedWorlds.isEmpty()) 0 else 1
  val fanCount = publishedWorlds.sumOf { it.chapterCount ?: 0 }
  val tagSeeds = publishedWorlds.map { it.name }.filter { it.isNotBlank() }.take(3)
  val tags = tagSeeds
  val imageGenerateRefPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
    if (uris.isNotEmpty()) {
      uris.map { it.toString() }.forEach { uri ->
        if (!imageGenerateReferenceUris.contains(uri)) {
          imageGenerateReferenceUris.add(uri)
        }
      }
      showImageGenerateDialog = true
    }
  }

  if (showDraftListPage) {
    ProfileDraftListPage(
      vm = vm,
      draftWorlds = draftWorlds,
      onBack = { showDraftListPage = false },
      onOpenDraftMenu = { draftMenuWorld = it },
    )
  } else {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF6F7FB))
        .verticalScroll(rememberScrollState())
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("我的", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2B40))
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFC4D0E3), CircleShape)
            .clickable { vm.setTab("设置") },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "设置",
            tint = Color(0xFF60779A),
            modifier = Modifier.size(15.dp),
          )
        }
      }

      Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LayeredAvatarFrame(
          foregroundPath = vm.accountAvatarPath.trim().ifBlank { null },
          backgroundPath = vm.accountAvatarBgPath.trim().ifBlank { null },
          modifier = Modifier
            .size(56.dp)
            .clickable(enabled = !vm.accountAvatarProcessing) { showAvatarActionDialog = true },
          backgroundColor = Color(0xFFE5EAF3),
          borderColor = Color(0xFFCCD8EA),
          fallback = {
            Text(
              text = vm.userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
              color = Color(0xFF7D8CA5),
              fontWeight = FontWeight.Bold,
            )
          },
          overlay = {
            if (vm.accountAvatarProcessing) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color(0x66132134)),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = Color(0xFFFFD071),
                  strokeWidth = 2.2.dp,
                )
              }
            }
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFC7D4E8), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "更换头像",
                tint = Color(0xFF6B7F9F),
                modifier = Modifier.size(12.dp),
              )
            }
          },
        )
        Text(
          vm.userName.ifBlank { "未登录" },
          fontSize = MaterialTheme.typography.headlineSmall.fontSize,
          fontWeight = FontWeight.ExtraBold,
          color = Color(0xFF1F2A3F),
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileStatCell(value = likeCount.toString(), label = "获赞", modifier = Modifier.weight(1f))
        ProfileStatCell(value = followCount.toString(), label = "关注", modifier = Modifier.weight(1f))
        ProfileStatCell(value = fanCount.toString(), label = "粉丝", modifier = Modifier.weight(1f))
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileActionBtn(text = "新建故事", modifier = Modifier.weight(1f)) { vm.startNewStoryDraft() }
        ProfileActionBtn(text = "编辑资料", modifier = Modifier.weight(1f)) { vm.setTab("设置") }
      }

      Text("${publishedWorlds.size} 作品", fontWeight = FontWeight.ExtraBold, color = Color(0xFF313B4C))
      if (tags.isNotEmpty()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          tags.forEach { tag ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFEEF3FC))
                .border(1.dp, Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
              Text(tag, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4F6281))
            }
          }
        }
      } else {
        Text("暂无标签", color = Color(0xFF7D90AE), style = MaterialTheme.typography.bodySmall)
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
          ProfileWorkCard(
            cover = {
              if (latestDraft != null) {
                StoryCoverImage(
                  title = latestDraft.name.ifBlank { "草稿箱" },
                  coverPath = vm.worldCoverPath(latestDraft).ifBlank { null },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                  emptyText = "草稿",
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .background(Color(0xFFF3F6FC)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("暂无草稿", color = Color(0xFF90A3C2))
                }
              }
            },
            meta = if (latestDraft != null) {
              "保存草稿后会显示在这里"
            } else {
              "保存草稿后会显示在这里"
            },
            metaLines = 2,
            onClick = { showDraftListPage = true },
          )
        }
        Box(modifier = Modifier.weight(1f)) {
          ProfileWorkCard(
            cover = {
              if (firstPublished != null) {
                StoryCoverImage(
                  title = firstPublished.name.ifBlank { "故事" },
                  coverPath = vm.worldCoverPath(firstPublished).ifBlank { null },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .clickable { vm.startFromWorld(firstPublished) },
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .background(Color(0xFFF3F6FC)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("暂无已发布故事", color = Color(0xFF90A3C2))
                }
              }
            },
            meta = if (firstPublished != null) {
              firstPublished.name.ifBlank { "未命名故事" }
            } else {
              "暂无已发布故事"
            },
            onClick = null,
            actions = if (firstPublished != null) {
              {
                ProfileActionBtn(text = "编辑", modifier = Modifier.fillMaxWidth()) { vm.reopenPublishedWorldAsDraft(firstPublished) }
              }
            } else {
              null
            },
          )
        }
      }

      remainingPublished.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          row.forEach { world ->
            Box(modifier = Modifier.weight(1f)) {
              ProfileWorkCard(
                cover = {
                  StoryCoverImage(
                    title = world.name.ifBlank { "故事" },
                    coverPath = vm.worldCoverPath(world).ifBlank { null },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(126.dp)
                      .clickable { vm.startFromWorld(world) },
                  )
                },
                meta = world.name.ifBlank { "未命名故事" },
                onClick = null,
                actions = {
                  ProfileActionBtn(text = "编辑", modifier = Modifier.fillMaxWidth()) { vm.reopenPublishedWorldAsDraft(world) }
                },
              )
            }
          }
          if (row.size < 2) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }

  if (showAvatarActionDialog) {
    AvatarActionDialog(
      onDismiss = { showAvatarActionDialog = false },
      onUpload = {
        showAvatarActionDialog = false
        onPickAvatar()
      },
      onUploadVideoGif = {
        showAvatarActionDialog = false
        onPickAvatarVideoGif()
      },
      onAiGenerate = {
        imageGeneratePrompt = vm.userName
        imageGenerateReferenceUris.clear()
        imageGenerateStyleKey = "general_3"
        showImageGenerateDialog = true
        showAvatarActionDialog = false
      },
    )
  }

  if (showImageGenerateDialog) {
    ImageGenerateDialog(
      title = "创建角色",
      prompt = imageGeneratePrompt,
      referenceImageCount = imageGenerateReferenceUris.size,
      loading = vm.aiGenerating,
      onPromptChange = { imageGeneratePrompt = it },
      selectedStyleKey = imageGenerateStyleKey,
      onStyleChange = { imageGenerateStyleKey = it },
      onAddReferenceImages = { imageGenerateRefPicker.launch("image/*") },
      onClearReferenceImages = { imageGenerateReferenceUris.clear() },
      onDismiss = {
        if (!vm.aiGenerating) showImageGenerateDialog = false
      },
      onConfirm = {
        vm.generateAccountAvatar(
          buildStyledImagePrompt(imageGenerateStyleKey, imageGeneratePrompt),
          imageGenerateReferenceUris.toList(),
        )
        showImageGenerateDialog = false
      },
    )
  }

  if (deletingDraft != null) {
    AlertDialog(
      onDismissRequest = { deletingDraft = null },
      title = { Text("删除草稿") },
      text = { Text("确认删除《${deletingDraft?.name?.ifBlank { "未命名故事" } ?: "未命名故事"}》？此操作会删除对应章节和调试会话。") },
      confirmButton = {
        TextButton(
          onClick = {
            deletingDraft?.let { vm.deleteWorld(it) }
            deletingDraft = null
          },
        ) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingDraft = null }) {
          Text("取消")
        }
      },
    )
  }

  if (draftMenuWorld != null) {
    AlertDialog(
      onDismissRequest = { draftMenuWorld = null },
      title = { Text(draftMenuWorld?.name?.ifBlank { "未命名故事" } ?: "未命名故事") },
      text = { Text("选择操作") },
      confirmButton = {
        TextButton(
          onClick = {
            val world = draftMenuWorld
            draftMenuWorld = null
            if (world != null) {
              vm.openWorldForEdit(world)
              showDraftListPage = false
            }
          },
        ) {
          Text("编辑")
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          TextButton(
            onClick = {
              val world = draftMenuWorld
              draftMenuWorld = null
              if (world != null) deletingDraft = world
            },
          ) {
            Text("删除")
          }
          TextButton(onClick = { draftMenuWorld = null }) {
            Text("关闭")
          }
        }
      },
    )
  }
}

@Composable
private fun ProfileStatCell(value: String, label: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(value, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3B4D69))
    Text(label, color = Color(0xFF5B6F8F), style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun ProfileDraftListPage(
  vm: MainViewModel,
  draftWorlds: List<WorldItem>,
  onBack: () -> Unit,
  onOpenDraftMenu: (WorldItem) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFF6F7FB))
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.clickable { onBack() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.ArrowBack,
          contentDescription = "返回",
          tint = Color(0xFF5D7294),
          modifier = Modifier.size(18.dp),
        )
        Text("草稿箱", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2B40))
      }
      ProfileActionBtn(text = "新建故事") { vm.startNewStoryDraft() }
    }

    Text("${draftWorlds.size} 个草稿", color = Color(0xFF6A7E9D), style = MaterialTheme.typography.bodySmall)

    if (draftWorlds.isEmpty()) {
      ProfileWorkCard(
        cover = {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(126.dp)
              .background(Color(0xFFF3F6FC)),
            contentAlignment = Alignment.Center,
          ) {
            Text("暂无草稿", color = Color(0xFF90A3C2))
          }
        },
        meta = "保存草稿后会显示在这里",
        onClick = null,
      )
    } else {
      draftWorlds.chunked(2).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
          row.forEach { world ->
            Box(modifier = Modifier.weight(1f)) {
              ProfileDraftTile(
                world = world,
                coverPath = vm.worldCoverPath(world).ifBlank { null },
                onClick = {
                  vm.openWorldForEdit(world)
                  onBack()
                },
                onMenu = { onOpenDraftMenu(world) },
              )
            }
          }
          if (row.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileDraftTile(
  world: WorldItem,
  coverPath: String?,
  onClick: () -> Unit,
  onMenu: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(250.dp)
      .background(Color(0xFFF7F8FB))
      .clickable { onClick() },
  ) {
    StoryCoverImage(
      title = world.name.ifBlank { "未命名" },
      coverPath = coverPath,
      modifier = Modifier.fillMaxSize(),
      emptyText = "",
    )
    if (coverPath.isNullOrBlank()) {
      Box(
        modifier = Modifier
          .padding(10.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFFB3B3B7))
          .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text("未生图", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
    }
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xBD101620)),
          ),
        )
        .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 10.dp),
    ) {
      Column(modifier = Modifier.align(Alignment.BottomStart)) {
        Text(
          world.name.ifBlank { "未命名" },
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          formatDraftDate(world.updateTime),
          color = Color.White.copy(alpha = 0.88f),
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Text(
        "...",
        color = Color.White,
        fontSize = MaterialTheme.typography.titleLarge.fontSize,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .clickable { onMenu() }
          .padding(horizontal = 6.dp),
      )
    }
  }
}

@Composable
private fun ProfileActionBtn(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = modifier.height(40.dp),
    shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3B4D69)),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
  ) {
    Text(text, fontWeight = FontWeight.Bold)
  }
}

private fun formatDraftDate(timestamp: Long): String {
  if (timestamp <= 0L) return "--.--"
  return runCatching {
    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
  }.getOrDefault("--.--")
}

@Composable
private fun ProfileWorkCard(
  cover: @Composable () -> Unit,
  meta: String,
  metaLines: Int = 1,
  onClick: (() -> Unit)?,
  actions: (@Composable () -> Unit)? = null,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(214.dp)
      .let { base ->
        if (onClick != null) base.clickable { onClick() } else base
      },
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      cover()
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = meta,
          color = Color(0xFF556B8A),
          style = MaterialTheme.typography.bodySmall,
          maxLines = metaLines,
          overflow = TextOverflow.Ellipsis,
        )
        if (actions != null) {
          Box(modifier = Modifier.fillMaxWidth()) {
            actions()
          }
        }
      }
    }
  }
}

@Composable
private fun AvatarActionDialog(
  onDismiss: () -> Unit,
  onUpload: () -> Unit,
  onUploadVideoGif: (() -> Unit)?,
  onAiGenerate: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("选择图片来源") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileActionDialogRow(
          icon = Icons.Outlined.Upload,
          text = "上传图片（支持 PNG / GIF）",
          onClick = onUpload,
        )
        if (onUploadVideoGif != null) {
          ProfileActionDialogRow(
            icon = Icons.Outlined.PlayArrow,
            text = "GIF 上传 MP4",
            onClick = onUploadVideoGif,
            glyph = "GIF",
          )
        }
        ProfileActionDialogRow(
          icon = Icons.Outlined.AutoAwesome,
          text = "AI 生成图片",
          onClick = onAiGenerate,
        )
        Text(
          "不选参考图就是文生图；选了参考图就是图生图。角色头像会标准化并立即刷新；封面和背景图会按场景比例保存。",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF6A7F9F),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("关闭")
      }
    },
  )
}

@Composable
private fun AvatarPreviewDialog(
  title: String,
  foregroundPath: String?,
  backgroundPath: String?,
  fallbackName: String,
  onDismiss: () -> Unit,
) {
  var previewMode by remember(foregroundPath, backgroundPath) {
    mutableStateOf(
      when {
        !foregroundPath.isNullOrBlank() && !backgroundPath.isNullOrBlank() -> AvatarPreviewMode.Composite
        !foregroundPath.isNullOrBlank() -> AvatarPreviewMode.Foreground
        else -> AvatarPreviewMode.Background
      },
    )
  }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFF)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("查看头像", fontWeight = FontWeight.ExtraBold, color = Color(0xFF213958))
          TextButton(onClick = onDismiss) {
            Text("关闭")
          }
        }
        Text(
          title.ifBlank { "角色头像" },
          color = Color(0xFF4D6285),
          style = MaterialTheme.typography.bodySmall,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          AvatarPreviewModePill(
            text = "合成",
            selected = previewMode == AvatarPreviewMode.Composite,
            enabled = !foregroundPath.isNullOrBlank() || !backgroundPath.isNullOrBlank(),
          ) {
            previewMode = AvatarPreviewMode.Composite
          }
          Spacer(modifier = Modifier.width(8.dp))
          AvatarPreviewModePill(
            text = "仅主体",
            selected = previewMode == AvatarPreviewMode.Foreground,
            enabled = !foregroundPath.isNullOrBlank(),
          ) {
            previewMode = AvatarPreviewMode.Foreground
          }
          Spacer(modifier = Modifier.width(8.dp))
          AvatarPreviewModePill(
            text = "仅背景",
            selected = previewMode == AvatarPreviewMode.Background,
            enabled = !backgroundPath.isNullOrBlank(),
          ) {
            previewMode = AvatarPreviewMode.Background
          }
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFFD3DEEF), RoundedCornerShape(28.dp))
            .background(if (previewMode == AvatarPreviewMode.Composite) Color(0xFFEAF1FB) else Color(0xFFF3F7FD)),
          contentAlignment = Alignment.Center,
        ) {
          when (previewMode) {
            AvatarPreviewMode.Composite -> LayeredAvatarFrame(
              foregroundPath = foregroundPath,
              backgroundPath = backgroundPath,
              modifier = Modifier.fillMaxSize(),
              backgroundColor = Color.Transparent,
              borderColor = Color.Transparent,
              shape = RoundedCornerShape(28.dp),
              fallback = {
                Text(
                  text = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                  color = Color(0xFF7D8CA5),
                  fontWeight = FontWeight.Bold,
                )
              },
            )
            AvatarPreviewMode.Foreground -> {
              if (!foregroundPath.isNullOrBlank()) {
                AsyncImage(
                  model = foregroundPath,
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
                  alignment = Alignment.BottomCenter,
                )
              } else {
                Text(
                  text = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                  color = Color(0xFF7D8CA5),
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            AvatarPreviewMode.Background -> {
              if (!backgroundPath.isNullOrBlank()) {
                AsyncImage(
                  model = backgroundPath,
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
                  alignment = Alignment.Center,
                )
              } else {
                Text(
                  text = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                  color = Color(0xFF7D8CA5),
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}

private enum class AvatarPreviewMode {
  Composite,
  Foreground,
  Background,
}

@Composable
private fun AvatarPreviewModePill(
  text: String,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .alpha(if (enabled) 1f else 0.45f)
      .clip(CircleShape)
      .border(
        1.dp,
        if (selected) Color(0xFF8FB0F5) else Color(0xFFD3DEEF),
        CircleShape,
      )
      .background(
        if (selected) Color(0xFFE3EDFF) else Color.White,
      )
      .clickable(enabled = enabled) { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
  ) {
    Text(
      text = text,
      color = if (selected) Color(0xFF2348A6) else Color(0xFF566B89),
      fontWeight = FontWeight.Bold,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun VoicePickerField(value: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFFF8FBFF))
      .border(1.dp, Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 14.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("点击选择音色", color = Color(0xFF5E7395))
      Text("${value.ifBlank { "未选择音色" }}  >", color = Color(0xFF4D6285), fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun VoicePickerDialog(
  vm: MainViewModel,
  title: String,
  initialLabel: String,
  initialPresetId: String,
  initialMode: String,
  initialReferenceAudioPath: String,
  initialReferenceAudioName: String,
  initialReferenceText: String,
  initialPromptText: String,
  initialMixVoices: List<VoiceMixItem>,
  onDismiss: () -> Unit,
  onConfirm: (VoiceBindingDraft) -> Unit,
) {
  fun runtimeStoryVoiceConfigId(): Long? = vm.settingsModelBinding("storyVoiceModel")?.configId?.takeIf { it > 0L }
  fun runtimeVoiceDesignConfigId(): Long? = vm.settingsModelBinding("storyVoiceDesignModel")?.configId?.takeIf { it > 0L }
  val availableModes = listOf(
    "text" to "预设音色",
    "clone" to "克隆音色",
    "mix" to "混合音色",
    "prompt_voice" to "提示词音色",
  )
  var selectedPresetId by remember(initialPresetId, title) { mutableStateOf(initialPresetId) }
  var selectedMode by remember(initialMode, title) {
    mutableStateOf(initialMode.takeIf { mode -> availableModes.any { it.first == mode } } ?: "text")
  }
  var referenceAudioPath by remember(initialReferenceAudioPath, title) { mutableStateOf(initialReferenceAudioPath) }
  var referenceAudioName by remember(initialReferenceAudioName, title) { mutableStateOf(initialReferenceAudioName) }
  var referenceText by remember(initialReferenceText, title) { mutableStateOf(initialReferenceText) }
  var promptText by remember(initialPromptText, title) { mutableStateOf(initialPromptText) }
  val selectedMixVoices = remember(initialMixVoices, title) {
    mutableStateListOf<VoiceMixItem>().apply {
      addAll(initialMixVoices.ifEmpty { listOf(VoiceMixItem(weight = 0.7)) })
    }
  }
  var previewText by remember(title) { mutableStateOf("你好啊，有什么可以帮到你") }
  var previewLoading by remember { mutableStateOf(false) }
  var promptPolishing by remember { mutableStateOf(false) }
  var audioUploading by remember { mutableStateOf(false) }
  var previewStatus by remember { mutableStateOf("") }
  var previewAudioUrl by remember { mutableStateOf("") }
  var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      val displayName = displayNameForUri(context, uri)
      audioUploading = true
      previewStatus = "上传参考音频中..."
      scope.launch {
        runCatching { vm.uploadVoiceReferenceAudio(uri.toString()) }
          .onSuccess { result ->
            referenceAudioPath = result.filePath
            referenceAudioName = displayName
            previewStatus = "参考音频已上传"
          }
          .onFailure {
            previewStatus = "音频上传失败: ${it.message ?: "未知错误"}"
          }
        audioUploading = false
      }
    }
  }

  LaunchedEffect(Unit) {
    vm.ensureSettingsPanelData()
    vm.ensureVoiceModels()
  }
  LaunchedEffect(vm.settingsAiModelMap.size, vm.voiceModels.size) {
    vm.ensureVoicePresets(runtimeStoryVoiceConfigId())
  }

  val effectiveConfigId = runtimeStoryVoiceConfigId()
  val presets = vm.voicePresets(effectiveConfigId)
  val selectedModel = vm.voiceModels.firstOrNull { it.id == effectiveConfigId }
  val hasVoiceDesignModel = runtimeVoiceDesignConfigId() != null

  fun isAliyunDirectCosyVoiceModel(model: String?): Boolean {
    return model?.trim()?.lowercase() in setOf(
      "cosyvoice-v3-flash",
      "cosyvoice-v3-plus",
      "cosyvoice-v3.5-flash",
      "cosyvoice-v3.5-plus",
    )
  }

  fun modelSupportedModeKeys(): Set<String> {
    val declaredModes = selectedModel
      ?.modes
      ?.map { it.trim() }
      ?.filter { it.isNotBlank() }
      ?.toSet()
      .orEmpty()
    if (declaredModes.isNotEmpty()) {
      return declaredModes
    }
    return if (selectedModel?.manufacturer?.trim() == "aliyun_direct" && isAliyunDirectCosyVoiceModel(selectedModel.model)) {
      setOf("text", "clone", "mix")
    } else {
      availableModes.map { it.first }.toSet()
    }
  }

  fun supportedModeKeys(): Set<String> {
    val modes = modelSupportedModeKeys().toMutableSet()
    if (hasVoiceDesignModel) {
      modes.add("prompt_voice")
    } else {
      modes.remove("prompt_voice")
    }
    return modes
  }

  fun unsupportedModeReason(mode: String): String? {
    val normalizedMode = mode.trim()
    if (normalizedMode == "prompt_voice") {
      return if (hasVoiceDesignModel) null else "请先在设置里配置语音设计模型"
    }
    if (normalizedMode.isBlank() || supportedModeKeys().contains(normalizedMode)) {
      return null
    }
    return "当前语音模型不支持该绑定模式"
  }

  fun fallbackSupportedMode(): String {
    val modes = supportedModeKeys()
    return when {
      modes.contains("text") -> "text"
      modes.isNotEmpty() -> modes.first()
      else -> "text"
    }
  }

  val supportedModeKeys = supportedModeKeys()
  val modeSupportNotes = buildList {
    val supportedModelLabels = availableModes
      .filter { it.first != "prompt_voice" && modelSupportedModeKeys().contains(it.first) }
      .map { it.second }
    val unsupportedModelModes = availableModes.filter { it.first != "prompt_voice" && !modelSupportedModeKeys().contains(it.first) }
    if (unsupportedModelModes.isNotEmpty() && supportedModelLabels.isNotEmpty()) {
      add("当前模型仅支持：${supportedModelLabels.joinToString("、")}")
    }
    if (!hasVoiceDesignModel) {
      add("提示词音色需要先在设置里配置语音设计模型")
    }
  }
  val modeSupportNote = modeSupportNotes.joinToString("；")

  LaunchedEffect(presets.size, selectedMode) {
    if (selectedMode == "text" && presets.isNotEmpty() && (selectedPresetId.isBlank() || presets.none { it.voiceId == selectedPresetId })) {
      selectedPresetId = presets.first().voiceId
    }
  }

  LaunchedEffect(selectedModel?.id, effectiveConfigId, title) {
    val reason = unsupportedModeReason(selectedMode)
    if (reason != null) {
      selectedMode = fallbackSupportedMode()
      previewStatus = reason
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      mediaPlayer?.release()
      mediaPlayer = null
    }
  }

  fun buildSelectedLabel(): String {
    return when (selectedMode) {
      "clone" -> {
        if (referenceAudioName.isNotBlank()) "克隆：$referenceAudioName" else "克隆音色"
      }
      "mix" -> {
        val names = selectedMixVoices
          .filter { it.voiceId.isNotBlank() }
          .map { item -> presets.firstOrNull { it.voiceId == item.voiceId }?.name ?: item.voiceId }
        if (names.isEmpty()) "混合音色" else "混合：${names.joinToString(" + ").take(18)}"
      }
      "prompt_voice" -> {
        val prompt = promptText.trim()
        if (prompt.isBlank()) "提示词音色" else "提示词：${prompt.take(12)}"
      }
      else -> presets.firstOrNull { it.voiceId == selectedPresetId }?.name
        ?: initialLabel.ifBlank { "预设音色" }
    }
  }

  fun validateBinding(): String? {
    if (selectedMode == "prompt_voice" && !hasVoiceDesignModel) return "请先在设置里配置语音设计模型"
    if (effectiveConfigId == null) return "请先在设置里配置语音生成模型"
    unsupportedModeReason(selectedMode)?.let { return it }
    return when (selectedMode) {
      "text" -> if (selectedPresetId.isBlank()) "请先选择音色预设" else null
      "clone" -> if (referenceAudioPath.isBlank()) "克隆模式需要上传参考音频" else null
      "mix" -> if (selectedMixVoices.none { it.voiceId.isNotBlank() }) "混合模式至少选择一个音色" else null
      "prompt_voice" -> if (promptText.trim().isBlank()) "提示词模式需要填写提示词" else null
      else -> null
    }
  }

  fun enqueuePreviewDownload() {
    val url = previewAudioUrl.trim()
    if (url.isBlank()) {
      previewStatus = "请先试听后再下载"
      return
    }
    val fileName = buildString {
      append(title.replace("\\s+".toRegex(), "").ifBlank { "voice_preview" })
      append("_")
      append(System.currentTimeMillis())
      append(".wav")
    }
    runCatching {
      val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setDescription("下载音色试听文件")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
      val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
      manager.enqueue(request)
    }.onSuccess {
      previewStatus = "已加入下载队列"
    }.onFailure {
      previewStatus = "下载失败: ${it.message ?: "未知错误"}"
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("绑定模式", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
        availableModes.forEach { (modeKey, modeLabel) ->
          SelectableRow(
            text = if (supportedModeKeys.contains(modeKey)) modeLabel else "${modeLabel}（当前模型不支持）",
            selected = selectedMode == modeKey,
            muted = !supportedModeKeys.contains(modeKey),
            onClick = {
              val reason = unsupportedModeReason(modeKey)
              if (reason != null) {
                previewStatus = reason
              } else {
                selectedMode = modeKey
                previewStatus = ""
              }
            },
          )
        }
        if (modeSupportNote.isNotBlank()) {
          Text(modeSupportNote, color = Color(0xFF9B6A22), style = MaterialTheme.typography.bodySmall)
        }

        when (selectedMode) {
          "clone" -> {
            Text("参考音频", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
            Button(
              onClick = { audioPicker.launch("audio/*") },
              enabled = !audioUploading,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F6FF), contentColor = Color(0xFF314F7E)),
            ) {
              Text(if (audioUploading) "上传中..." else "选择并上传音频")
            }
            Text(
              referenceAudioName.ifBlank { "未选择参考音频" },
              color = Color(0xFF6A7F9F),
              style = MaterialTheme.typography.bodySmall,
            )
            ScrollableOutlinedTextField(
              value = referenceText,
              onValueChange = { referenceText = it },
              modifier = Modifier.fillMaxWidth(),
              minLines = 2,
              placeholder = { Text("参考音频对应文本（可选）") },
            )
          }
          "mix" -> {
            Text("已选混合音色", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
            selectedMixVoices.forEachIndexed { index, item ->
              val label = presets.firstOrNull { it.voiceId == item.voiceId }?.name ?: item.voiceId.ifBlank { "未选择音色" }
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFFF8FBFF))
                  .border(1.dp, Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 10.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(label, color = Color(0xFF294A79), fontWeight = FontWeight.SemiBold)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("权重 ${"%.1f".format(item.weight)}", color = Color(0xFF5E7395))
                    TextButton(
                      onClick = {
                        selectedMixVoices[index] = item.copy(weight = (item.weight - 0.1).coerceAtLeast(0.1))
                      },
                    ) {
                      Text("-")
                    }
                    TextButton(
                      onClick = {
                        selectedMixVoices[index] = item.copy(weight = (item.weight + 0.1).coerceAtMost(1.0))
                      },
                    ) {
                      Text("+")
                    }
                    TextButton(
                      onClick = {
                        selectedMixVoices.removeAt(index)
                        if (selectedMixVoices.isEmpty()) {
                          selectedMixVoices.add(VoiceMixItem(weight = 0.7))
                        }
                      },
                    ) {
                      Text("删除")
                    }
                  }
                }
              }
            }

            Text("可选预设", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
            if (presets.isEmpty()) {
              Text("当前模型还没有返回可用音色。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
            } else {
              presets.forEach { preset ->
                val selectedIndex = selectedMixVoices.indexOfFirst { it.voiceId == preset.voiceId }
                SelectableRow(
                  text = if (selectedIndex >= 0) "${preset.name} · 已加入" else preset.name,
                  selected = selectedIndex >= 0,
                  onClick = {
                    if (selectedIndex >= 0) {
                      selectedMixVoices.removeAt(selectedIndex)
                      if (selectedMixVoices.isEmpty()) {
                        selectedMixVoices.add(VoiceMixItem(weight = 0.7))
                      }
                    } else {
                      val activeCount = selectedMixVoices.count { it.voiceId.isNotBlank() }
                      if (activeCount >= 3) {
                        previewStatus = "最多只能混合 3 个音色"
                      } else {
                        val blankIndex = selectedMixVoices.indexOfFirst { it.voiceId.isBlank() }
                        if (blankIndex >= 0) {
                          selectedMixVoices[blankIndex] = VoiceMixItem(voiceId = preset.voiceId, weight = 0.3)
                        } else {
                          selectedMixVoices.add(VoiceMixItem(voiceId = preset.voiceId, weight = 0.3))
                        }
                      }
                    }
                  },
                )
              }
            }
          }
          "prompt_voice" -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("提示词", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
              TextButton(
                onClick = {
                  val rawText = promptText.trim().ifBlank { initialLabel.trim() }
                  if (rawText.isBlank()) {
                    previewStatus = "请先输入提示词或角色名"
                    return@TextButton
                  }
                  promptPolishing = true
                  previewStatus = ""
                  scope.launch {
                    runCatching {
                      val selectedPresetProvider = presets.firstOrNull { it.voiceId == selectedPresetId }?.provider?.trim()?.takeIf { it.isNotBlank() }
                      vm.polishVoicePrompt(
                        text = rawText,
                        style = listOfNotNull(selectedModel?.model, selectedModel?.manufacturer, selectedPresetProvider).joinToString(" · "),
                      )
                    }.onSuccess {
                      if (it.isNotBlank()) {
                        promptText = it
                        previewStatus = "提示词已润色"
                      } else {
                        previewStatus = "未返回润色结果"
                      }
                    }.onFailure {
                      previewStatus = "AI润色失败: ${it.message ?: "未知错误"}"
                    }
                    promptPolishing = false
                  }
                },
                enabled = !promptPolishing,
              ) {
                Text(if (promptPolishing) "润色中..." else "AI润色")
              }
            }
            ScrollableOutlinedTextField(
              value = promptText,
              onValueChange = { promptText = it },
              modifier = Modifier.fillMaxWidth(),
              minLines = 3,
              placeholder = { Text("例如：温柔、清亮、成熟、治愈、讲故事感") },
            )
          }
          else -> {
            Text("音色预设", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
            when {
              effectiveConfigId == null -> {
                Text("请先在设置里配置语音生成模型。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
              }
              presets.isEmpty() -> {
                Text("当前语音生成配置还没有返回可用音色。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
              }
              else -> {
                presets.forEach { preset ->
                  SelectableRow(
                    text = preset.name,
                    selected = preset.voiceId == selectedPresetId,
                    onClick = { selectedPresetId = preset.voiceId },
                  )
                }
              }
            }
          }
        }

        Text("试听文本", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
        ScrollableOutlinedTextField(
          value = previewText,
          onValueChange = { previewText = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          placeholder = { Text("输入要试听的文本") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(
            onClick = {
              val validateMsg = validateBinding()
              if (validateMsg != null) {
                previewStatus = validateMsg
                return@Button
              }
              if (previewText.trim().isBlank()) {
                previewStatus = "请输入试听文本"
                return@Button
              }
              previewLoading = true
              previewStatus = ""
              scope.launch {
                runCatching {
                  val url = vm.previewVoice(
                    configId = effectiveConfigId,
                    text = previewText.trim(),
                    mode = selectedMode,
                    presetId = selectedPresetId,
                    referenceAudioPath = referenceAudioPath,
                    referenceText = referenceText.trim(),
                    promptText = promptText.trim(),
                    mixVoices = selectedMixVoices.toList(),
                  )
                  if (url.isBlank()) error("未返回试听音频")
                  previewAudioUrl = url
                  mediaPlayer?.release()
                  mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener {
                      previewLoading = false
                      previewStatus = "正在播放试听"
                      start()
                    }
                    setOnCompletionListener {
                      previewStatus = "试听完成"
                    }
                    setOnErrorListener { mp, _, _ ->
                      mp.release()
                      mediaPlayer = null
                      previewLoading = false
                      previewStatus = "试听播放失败"
                      true
                    }
                    prepareAsync()
                  }
                }.onFailure {
                  previewLoading = false
                  previewStatus = "试听失败: ${it.message ?: "未知错误"}"
                }
              }
            },
            enabled = !previewLoading,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF314F7E), contentColor = Color.White),
          ) {
            Text(if (previewLoading) "加载中..." else "试听")
          }
          TextButton(
            onClick = {
              mediaPlayer?.pause()
              previewStatus = "已停止试听"
            },
            enabled = mediaPlayer != null || previewAudioUrl.isNotBlank(),
          ) {
            Text("停止")
          }
          TextButton(
            onClick = { enqueuePreviewDownload() },
            enabled = previewAudioUrl.isNotBlank(),
          ) {
            Text("下载")
          }
        }
        if (previewStatus.isNotBlank()) {
          Text(previewStatus, color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val validateMsg = validateBinding()
          if (validateMsg != null) {
            previewStatus = validateMsg
            return@TextButton
          }
          onConfirm(
            VoiceBindingDraft(
              label = buildSelectedLabel(),
              presetId = selectedPresetId,
              mode = selectedMode,
              referenceAudioPath = referenceAudioPath,
              referenceAudioName = referenceAudioName,
              referenceText = referenceText.trim(),
              promptText = promptText.trim(),
              mixVoices = selectedMixVoices.filter { it.voiceId.isNotBlank() },
            ),
          )
        },
        enabled = !previewLoading && !audioUploading,
      ) {
        Text("确定")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    },
  )
}

private fun displayNameForUri(context: Context, uri: Uri): String {
  val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (columnIndex >= 0 && cursor.moveToFirst()) cursor.getString(columnIndex) else null
  }?.trim().orEmpty()
  if (displayName.isNotBlank()) return displayName
  return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':').orEmpty().ifBlank { "参考音频" }
}

private fun displayStorageName(raw: String, fallback: String): String {
  val value = raw.trim()
  if (value.isBlank()) return fallback
  return value.substringBefore('?').substringBefore('#').substringAfterLast('/').ifBlank { fallback }
}

private data class ImageStylePreset(
  val key: String,
  val title: String,
  val subtitle: String,
  val promptHint: String,
  val accent: Color,
  val colors: List<Color>,
)

private fun imageStylePresets(): List<ImageStylePreset> {
  return listOf(
    ImageStylePreset("general_3", "通用 3.0", "高质角色", "高质量角色立绘，五官清晰，光影干净，完整上半身", Color(0xFF6B55A2), listOf(Color(0xFFEDE4FF), Color(0xFFD8C8FF))),
    ImageStylePreset("general_1", "通用 1.0", "自然真实", "自然真实风格，清晰人物主体，干净背景，细节完整", Color(0xFF4E7DBD), listOf(Color(0xFFDFF0FF), Color(0xFFBFD8FF))),
    ImageStylePreset("romance", "言情漫画", "二次元感", "言情漫画风，精致五官，柔和上色，清透皮肤，甜美氛围", Color(0xFFFF7A98), listOf(Color(0xFFFFE2EC), Color(0xFFFFC2D3))),
    ImageStylePreset("pixel", "像素画", "游戏像素", "像素风角色，游戏感强，像素颗粒清晰，配色明快", Color(0xFF5BA86E), listOf(Color(0xFFDFF7E6), Color(0xFFBDECCB))),
    ImageStylePreset("thick", "细腻厚涂", "厚涂质感", "细腻厚涂，层次丰富，皮肤与布料质感明显，光影立体", Color(0xFFB26C3F), listOf(Color(0xFFFFE8D8), Color(0xFFFFD0A8))),
    ImageStylePreset("guofeng", "国风", "东方审美", "国风角色，东方审美，服饰纹样精致，气质端庄", Color(0xFFC0732A), listOf(Color(0xFFFFF0D9), Color(0xFFF7D79D))),
    ImageStylePreset("cinema", "电影感", "大片构图", "电影感构图，真实光影，景深明显，人物表情细腻", Color(0xFF365B8C), listOf(Color(0xFFD6E6FF), Color(0xFFB8C9E8))),
    ImageStylePreset("dark", "暗黑写实", "强对比", "暗黑写实风，强对比光影，气氛感强，角色轮廓锐利", Color(0xFF4B5568), listOf(Color(0xFFDCE2EC), Color(0xFFB6C0CF))),
  )
}

private fun imageStylePresetByKey(key: String?): ImageStylePreset {
  val presets = imageStylePresets()
  return presets.firstOrNull { it.key == key } ?: presets.first()
}

private fun buildStyledImagePrompt(styleKey: String?, prompt: String): String {
  val style = imageStylePresetByKey(styleKey)
  val text = prompt.trim()
  return if (text.isBlank()) style.promptHint else "${style.promptHint}。$text"
}

@Composable
private fun SelectableRow(text: String, selected: Boolean, muted: Boolean = false, onClick: () -> Unit) {
  val backgroundColor = when {
    selected -> Color(0xFFE8F0FF)
    muted -> Color(0xFFF5F7FB)
    else -> Color(0xFFF8FBFF)
  }
  val borderColor = when {
    selected -> Color(0xFF9EB5DE)
    muted -> Color(0xFFE1E8F2)
    else -> Color(0xFFD8E3F3)
  }
  val textColor = when {
    selected -> Color(0xFF294A79)
    muted -> Color(0xFF97A6BC)
    else -> Color(0xFF5E7395)
  }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    Text(text, color = textColor)
  }
}

@Composable
private fun ImageGenerateDialog(
  title: String,
  prompt: String,
  referenceImageCount: Int,
  loading: Boolean,
  onPromptChange: (String) -> Unit,
  selectedStyleKey: String,
  onStyleChange: (String) -> Unit,
  onAddReferenceImages: () -> Unit,
  onClearReferenceImages: () -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  val presets = imageStylePresets()
  val scrollState = rememberScrollState()
  val selectedPreset = presets.firstOrNull { it.key == selectedStyleKey } ?: presets.first()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Color(0xFFF3F5FA),
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
              .size(34.dp)
              .clip(CircleShape)
              .background(Color.White)
              .clickable(enabled = !loading) { onDismiss() },
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = Color(0xFF1F2A3F))
          }
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              title,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFF1F2A3F),
              style = MaterialTheme.typography.titleLarge,
            )
          }
          Spacer(modifier = Modifier.size(34.dp))
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("选择绘图风格", color = Color(0xFF8996AA), style = MaterialTheme.typography.bodySmall)
                Text(
                  selectedPreset.title,
                  color = selectedPreset.accent,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.bodySmall,
                )
              }

              Text(
                "没有参考图就是文生图；上传参考图后自动切换成图生图。",
                color = Color(0xFF8E9AB0),
                style = MaterialTheme.typography.bodySmall,
              )

              LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                userScrollEnabled = false,
                modifier = Modifier.height(196.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                gridItems(presets) { preset ->
                  ImageStyleCard(
                    preset = preset,
                    selected = preset.key == selectedPreset.key,
                    onClick = { onStyleChange(preset.key) },
                  )
                }
              }
            }
          }

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("形象描述", color = Color(0xFF8996AA), style = MaterialTheme.typography.bodySmall)
                TextButton(
                  onClick = { onPromptChange(selectedPreset.promptHint) },
                  contentPadding = PaddingValues(0.dp),
                  enabled = !loading,
                ) {
                  Text("AI帮写", color = activeOrange, fontWeight = FontWeight.Bold)
                }
              }
              ScrollableOutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text("例如：黑框眼镜反光，格子衬衫塞进西装裤，手指总在裤缝模拟打字，背包插满电子设备线缆。") },
                enabled = !loading,
              )
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("还可输入 ${maxOf(0, 500 - prompt.length)} 字", color = Color(0xFF9AA8BC), style = MaterialTheme.typography.bodySmall)
                Text("${prompt.length}/500", color = Color(0xFF9AA8BC), style = MaterialTheme.typography.bodySmall)
              }
            }
          }

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("参考图（可选）", color = Color(0xFF8996AA), style = MaterialTheme.typography.bodySmall)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(92.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(Color(0xFFF6F8FC))
                  .border(1.dp, Color(0xFFE0E7F1), RoundedCornerShape(18.dp))
                  .clickable(enabled = !loading) { onAddReferenceImages() },
                contentAlignment = Alignment.Center,
              ) {
                if (referenceImageCount > 0) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("已选择 ${referenceImageCount} 张参考图", color = Color(0xFF60748E), fontWeight = FontWeight.SemiBold)
                    Text("继续添加会自动作为图生图参考", color = Color(0xFF93A4BC), style = MaterialTheme.typography.bodySmall)
                  }
                } else {
                  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("+ 点击上传", color = Color(0xFF94A4BA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("不上传就是文生图", color = Color(0xFFB1BECE), style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
              if (referenceImageCount > 0) {
                TextButton(onClick = onClearReferenceImages, enabled = !loading, contentPadding = PaddingValues(0.dp)) {
                  Text("清空参考图", color = Color(0xFF6C7E98))
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
          Button(
            onClick = onConfirm,
            enabled = !loading && prompt.trim().isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = warnYellow, contentColor = Color(0xFF171717)),
          ) {
            Text(if (loading) "生成中..." else "重新生成", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

@Composable
private fun ImageStyleCard(
  preset: ImageStylePreset,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val borderColor = if (selected) preset.accent else Color(0xFFE2E8F1)
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, borderColor, RoundedCornerShape(16.dp))
        .padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Brush.linearGradient(preset.colors)),
      ) {
        if (selected) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .size(16.dp)
              .clip(CircleShape)
              .background(preset.accent),
            contentAlignment = Alignment.Center,
          ) {
            Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
      Text(
        preset.title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF2D3C50),
      )
      Text(
        preset.subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF7D90AE),
      )
    }
  }
}

@Composable
private fun ProfileActionDialogRow(icon: ImageVector, text: String, onClick: () -> Unit, glyph: String? = null) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(38.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F7FF), contentColor = Color(0xFF2E466A)),
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (!glyph.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .defaultMinSize(minWidth = 30.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE8F0FF))
            .border(1.dp, Color(0xFFD5E1F6), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(glyph, style = MaterialTheme.typography.labelSmall, color = Color(0xFF3553A6), fontWeight = FontWeight.ExtraBold)
        }
      } else {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
      }
      Text(text, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun SettingsScene(vm: MainViewModel) {
  var showAccountDialog by remember { mutableStateOf(false) }
  var accountMode by remember { mutableStateOf("login") }
  var dialogUsername by remember { mutableStateOf(vm.loginUsername) }
  var dialogPassword by remember { mutableStateOf("") }
  var registerUsername by remember { mutableStateOf("") }
  var registerPassword by remember { mutableStateOf("") }
  var registerConfirmPassword by remember { mutableStateOf("") }
  var oldPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmNewPassword by remember { mutableStateOf("") }
  var revealLoginPassword by remember { mutableStateOf(false) }
  var revealRegisterPassword by remember { mutableStateOf(false) }
  var revealRegisterConfirmPassword by remember { mutableStateOf(false) }
  var revealOldPassword by remember { mutableStateOf(false) }
  var revealNewPassword by remember { mutableStateOf(false) }
  val promptDrafts = remember { mutableStateMapOf<String, String>() }
  var showModelManager by remember { mutableStateOf(false) }
  var activeModelSlot by remember { mutableStateOf<MainViewModel.SettingsModelSlot?>(null) }

  fun openAccountDialog(mode: String) {
    accountMode = mode
    dialogUsername = vm.loginUsername
    dialogPassword = ""
    registerUsername = ""
    registerPassword = ""
    registerConfirmPassword = ""
    oldPassword = ""
    newPassword = ""
    confirmNewPassword = ""
    showAccountDialog = true
  }

  fun openModelManager(slot: MainViewModel.SettingsModelSlot) {
    activeModelSlot = slot
    showModelManager = true
  }

  LaunchedEffect(vm.token, vm.activeTab) {
    if (vm.activeTab == "设置" && vm.token.isNotBlank()) {
      vm.ensureSettingsPanelData()
    }
  }
  LaunchedEffect(vm.storyPrompts.toList()) {
    vm.storyPrompts.forEach { prompt ->
      promptDrafts[prompt.code] = vm.currentStoryPromptValue(prompt.code)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageGray),
  ) {
    HeaderTitle(title = "设置", rightText = "返回") { vm.setTab("我的") }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SettingsSectionCard(title = "请求地址配置") {
        OutlinedTextField(
          value = vm.baseUrl,
          onValueChange = { vm.baseUrl = it },
          label = { Text("API 地址") },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MiniBtn(text = "保存连接", primary = true, onClick = { vm.saveConnection() })
        }
      }

      SettingsSectionCard(title = "账号设置") {
        Text(
          "登录状态：${if (vm.token.isBlank()) "未登录" else "已登录（${vm.userName.ifBlank { "未知账号" }}）"}",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF31445E),
        )
        if (vm.token.isNotBlank()) {
          Text(
            "当前账号的头像、模型配置、AI 故事提示词与 ai 漫剧资源完全隔离。",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF697A97),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (vm.token.isBlank()) {
            MiniBtn(text = "登录 / 注册", primary = true, onClick = { openAccountDialog("login") })
          } else {
            MiniBtn(text = "修改密码", onClick = { openAccountDialog("changePassword") })
            MiniBtn(text = "退出登录", onClick = { vm.clearToken() })
          }
        }
      }

      if (vm.token.isNotBlank()) {
        SettingsSectionCard(title = "模型配置") {
          vm.settingsModelSlots.forEach { slot ->
            val binding = vm.settingsModelBinding(slot.key)
            val recommendation = vm.settingsRecommendedModel(slot.key)
            val advisoryText = vm.settingsModelAdvisory(slot.key).orEmpty()
            val recommendationText = recommendation?.let {
              listOfNotNull(it.manufacturer.takeIf { value -> value.isNotBlank() }, it.model.takeIf { value -> value.isNotBlank() }).joinToString(" / ")
            }.orEmpty()
            SettingsModelPickerRow(
              title = slot.label,
              currentText = binding?.let {
                listOfNotNull(binding.manufacturer?.takeIf { it.isNotBlank() }, binding.model?.takeIf { it.isNotBlank() }).joinToString(" / ")
              }.orEmpty().ifBlank { "未绑定" },
              advisoryText = advisoryText,
              recommendationText = recommendationText,
              onUseRecommendation = if (recommendation != null && binding?.configId != recommendation.id) {
                { vm.bindRecommendedGameModel(slot.key) }
              } else {
                null
              },
              onManage = { openModelManager(slot) },
            )
          }
        }
      }

      if (vm.token.isNotBlank() && vm.isAdminAccount()) {
        SettingsSectionCard(title = "提示词配置") {
          vm.storyPrompts.forEach { prompt ->
            val isCustom = prompt.customValue?.isNotBlank() == true
            val meta = storyPromptUiMeta(prompt.code)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(prompt.name ?: prompt.code, fontWeight = FontWeight.Bold, color = Color(0xFF1F2430))
                  Text(
                    if (isCustom) "自定义" else "默认值",
                    color = if (isCustom) Color(0xFFBE7A16) else Color(0xFF61738E),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                      .clip(RoundedCornerShape(999.dp))
                      .background(if (isCustom) Color(0xFFFFF2D8) else Color(0xFFE4F6EA))
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  MiniBtn(text = "重置提示词", onClick = {
                    vm.resetStoryPrompt(prompt.code)
                    promptDrafts[prompt.code] = vm.currentStoryPromptValue(prompt.code)
                  })
                  MiniBtn(text = "保存", primary = true, onClick = {
                    vm.saveStoryPrompt(prompt.code, promptDrafts[prompt.code] ?: "")
                  })
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PromptMetaChip("Agent", label = true)
                PromptMetaChip(meta.agentLabel)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PromptMetaChip("TS", label = true)
                PromptMetaChip(meta.tsLabel, multiLine = true)
              }
              ScrollableOutlinedTextField(
                value = promptDrafts[prompt.code] ?: vm.currentStoryPromptValue(prompt.code),
                onValueChange = { promptDrafts[prompt.code] = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
              )
              Text(
                if (isCustom) "*当前使用自定义提示词，点击重置将恢复默认值" else "*当前使用默认提示词，编辑后将保存为自定义值",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7889A1),
              )
            }
          }
        }
      } else if (vm.token.isNotBlank()) {
        SettingsSectionCard(title = "提示词配置") {
          Text("只有 admin 账号可以编辑 AI 故事提示词。", style = MaterialTheme.typography.bodySmall, color = Color(0xFF697A97))
        }
      }

      SettingsSectionCard(title = "其他") {
        MiniBtn(text = "检查更新", onClick = { vm.notice = "当前为开发版，暂未接入在线更新" })
      }
    }
  }

  if (showAccountDialog) {
    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      title = {
        Text(if (accountMode == "changePassword") "修改密码" else "账号登录")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBtn(text = "登录", primary = accountMode == "login", onClick = { accountMode = "login" })
            MiniBtn(text = "注册", primary = accountMode == "register", onClick = { accountMode = "register" })
            if (vm.token.isNotBlank()) {
              MiniBtn(text = "改密", primary = accountMode == "changePassword", onClick = { accountMode = "changePassword" })
            }
          }
          OutlinedTextField(
            value = vm.baseUrl,
            onValueChange = { vm.baseUrl = it },
            label = { Text("API 地址") },
            modifier = Modifier.fillMaxWidth(),
          )
          if (accountMode == "login") {
            OutlinedTextField(
              value = dialogUsername,
              onValueChange = { dialogUsername = it },
              label = { Text("账号") },
              modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(
              value = dialogPassword,
              onValueChange = { dialogPassword = it },
              label = "密码",
              revealed = revealLoginPassword,
              onToggleReveal = { revealLoginPassword = !revealLoginPassword },
            )
          } else if (accountMode == "register") {
            OutlinedTextField(
              value = registerUsername,
              onValueChange = { registerUsername = it },
              label = { Text("账号") },
              modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(
              value = registerPassword,
              onValueChange = { registerPassword = it },
              label = "密码",
              revealed = revealRegisterPassword,
              onToggleReveal = { revealRegisterPassword = !revealRegisterPassword },
            )
            PasswordField(
              value = registerConfirmPassword,
              onValueChange = { registerConfirmPassword = it },
              label = "确认密码",
              revealed = revealRegisterConfirmPassword,
              onToggleReveal = { revealRegisterConfirmPassword = !revealRegisterConfirmPassword },
            )
          } else {
            PasswordField(
              value = oldPassword,
              onValueChange = { oldPassword = it },
              label = "原密码",
              revealed = revealOldPassword,
              onToggleReveal = { revealOldPassword = !revealOldPassword },
            )
            PasswordField(
              value = newPassword,
              onValueChange = { newPassword = it },
              label = "新密码",
              revealed = revealNewPassword,
              onToggleReveal = { revealNewPassword = !revealNewPassword },
            )
            OutlinedTextField(
              value = confirmNewPassword,
              onValueChange = { confirmNewPassword = it },
              label = { Text("确认新密码") },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          when (accountMode) {
            "login" -> {
              vm.loginUsername = dialogUsername.trim()
              vm.loginPassword = dialogPassword
              vm.loginAndSaveToken()
              showAccountDialog = false
            }
            "register" -> {
              if (registerPassword != registerConfirmPassword) {
                vm.notice = "两次输入的密码不一致"
              } else {
                vm.registerAndLogin(registerUsername.trim(), registerPassword)
                showAccountDialog = false
              }
            }
            else -> {
              if (newPassword != confirmNewPassword) {
                vm.notice = "两次输入的新密码不一致"
              } else {
                vm.changePassword(oldPassword, newPassword)
                showAccountDialog = false
              }
            }
          }
        }) {
          Text(if (accountMode == "login") "登录" else if (accountMode == "register") "注册" else "确认修改")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAccountDialog = false }) {
          Text("取消")
        }
      },
    )
  }

  val activeSlot = activeModelSlot
  if (showModelManager && activeSlot != null) {
    SettingsModelManagerDialog(
      vm = vm,
      slot = activeSlot,
      currentConfigId = vm.settingsModelBinding(activeSlot.key)?.configId,
      options = vm.settingsConfigOptions(activeSlot.configType),
      onDismiss = {
        showModelManager = false
        activeModelSlot = null
      },
    )
  }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      content = {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF203556))
        content()
      },
    )
  }
}

@Composable
private fun SettingsModelPickerRow(
  title: String,
  currentText: String,
  advisoryText: String = "",
  recommendationText: String = "",
  onUseRecommendation: (() -> Unit)? = null,
  onManage: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF31445E))
    Text(currentText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E819B))
    if (recommendationText.isNotBlank()) {
      Text("推荐：$recommendationText", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3559A6))
    }
    if (advisoryText.isNotBlank()) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFFFF8EA))
          .border(1.dp, Color(0xFFF0D7A6), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 8.dp),
      ) {
        Text(advisoryText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5B0B))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (onUseRecommendation != null) {
        MiniBtn(text = "用推荐", onClick = onUseRecommendation)
      }
      MiniBtn(text = "配置接口", primary = true, full = onUseRecommendation == null, onClick = onManage)
    }
  }
}

@Composable
private fun PromptMetaChip(text: String, label: Boolean = false, multiLine: Boolean = false) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (label) Color(0xFFF2EBFF) else Color.White)
      .border(1.dp, if (label) Color(0xFFD7C5FF) else Color(0xFFDDE6F3), RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = if (multiLine) 8.dp else 7.dp),
  ) {
    Text(
      text,
      color = if (label) Color(0xFF6F3EE6) else Color(0xFF29415F),
      style = MaterialTheme.typography.bodySmall,
      maxLines = if (multiLine) 3 else 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SettingsModelManagerDialog(
  vm: MainViewModel,
  slot: MainViewModel.SettingsModelSlot,
  currentConfigId: Long?,
  options: List<com.toonflow.game.data.ModelConfigItem>,
  onDismiss: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var keyword by remember(slot.key) { mutableStateOf("") }
  var selectedId by remember(slot.key, currentConfigId) { mutableStateOf(currentConfigId) }
  var showEditor by remember { mutableStateOf(false) }
  var editingModel by remember { mutableStateOf<com.toonflow.game.data.ModelConfigItem?>(null) }
  var pendingDelete by remember { mutableStateOf<com.toonflow.game.data.ModelConfigItem?>(null) }
  var testingId by remember { mutableStateOf<Long?>(null) }
  var showTestResult by remember { mutableStateOf(false) }
  var testResultTitle by remember { mutableStateOf("") }
  var testResultKind by remember { mutableStateOf("text") }
  var testResultContent by remember { mutableStateOf("") }
  var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      mediaPlayer?.release()
    }
  }

  val slotOptions = remember(options, slot.key, slot.configType) {
    options
      .filter { row -> settingsRowMatchesSlot(slot, row) }
      .sortedWith(compareByDescending<com.toonflow.game.data.ModelConfigItem> { it.createTime }.thenByDescending { it.id })
  }
  val recommendation = vm.settingsRecommendedModel(slot.key)
  val advisoryText = vm.settingsModelAdvisory(slot.key).orEmpty()
  val recommendationText = recommendation?.let {
    listOfNotNull(
      settingsManufacturerLabel(it.manufacturer).takeIf { value -> value.isNotBlank() },
      it.model.takeIf { value -> value.isNotBlank() },
    ).joinToString(" / ")
  }.orEmpty()

  val filteredOptions = remember(slotOptions, keyword) {
    val query = keyword.trim().lowercase(Locale.getDefault())
    slotOptions
      .filter { row ->
        if (query.isBlank()) return@filter true
        listOf(row.manufacturer, row.model, row.baseUrl, row.modelType)
          .any { it.lowercase(Locale.getDefault()).contains(query) }
      }
  }

  LaunchedEffect(slotOptions, selectedId) {
    if (selectedId != null && slotOptions.none { it.id == selectedId }) {
      selectedId = null
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("模型数据管理", fontWeight = FontWeight.ExtraBold, color = Color(0xFF213958))
            Text("${slot.label} · ${settingsModelKindLabel(slot.configType, slot.key)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6C7E98))
          }
          TextButton(onClick = onDismiss) {
            Text("关闭")
          }
        }

        if (advisoryText.isNotBlank() || recommendationText.isNotBlank()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFFFFF8EA))
              .border(1.dp, Color(0xFFF0D7A6), RoundedCornerShape(14.dp))
              .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("当前建议", fontWeight = FontWeight.Bold, color = Color(0xFF7C5410))
              if (advisoryText.isNotBlank()) {
                Text(advisoryText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5B0B))
              }
              if (recommendationText.isNotBlank()) {
                Text("推荐模型：$recommendationText", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5B0B))
              }
            }
            if (recommendation != null && selectedId != recommendation.id) {
              MiniBtn(text = "选中推荐", onClick = { selectedId = recommendation.id })
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MiniBtn(
            text = "新增模型",
            primary = true,
            onClick = {
              editingModel = null
              showEditor = true
            },
          )
          OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("搜索模型名称...") },
          )
          Text(
            text = "共 ${filteredOptions.size} 个模型",
            color = Color(0xFF4C67B2),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(Color(0xFFEEF3FF))
              .padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(filteredOptions, key = { it.id }) { row ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = if (selectedId == row.id) Color(0xFFF5F8FF) else Color.White),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedId = row.id }
                .border(1.dp, if (selectedId == row.id) Color(0xFFBCD0F7) else Color(0xFFEAEFF6), RoundedCornerShape(14.dp)),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  androidx.compose.material3.RadioButton(
                    selected = selectedId == row.id,
                    onClick = { selectedId = row.id },
                  )
                  Text(settingsManufacturerLabel(row.manufacturer), fontWeight = FontWeight.Bold, color = Color(0xFF3559A6))
                  Text(settingsModelKindLabel(row.type.ifBlank { slot.configType }, slot.key), color = Color(0xFFC57A16), style = MaterialTheme.typography.bodySmall)
                  if (settingsShouldShowModelType(slot)) {
                    Text(settingsModelTypeLabel(row.modelType.ifBlank { defaultSettingsModelTypeForSlot(slot) }, slot.key), color = Color(0xFF6E819B), style = MaterialTheme.typography.bodySmall)
                  }
                }
                Text(row.model.ifBlank { "配置${row.id}" }, fontWeight = FontWeight.ExtraBold, color = Color(0xFF213958))
                Text(row.baseUrl.ifBlank { "默认 Base URL" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E819B))
                Text(if (row.apiKey.isBlank()) "API Key：未填写" else "API Key：••••••••", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E819B))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  MiniBtn(
                    text = if (testingId == row.id) "测试中" else "测试",
                    primary = true,
                    onClick = {
                      testingId = row.id
                      scope.launch {
                        runCatching { vm.testManagedModelConfig(row) }
                          .onSuccess { result ->
                            testResultTitle = "${settingsManufacturerLabel(row.manufacturer)} / ${row.model.ifBlank { "配置${row.id}" }}"
                            testResultKind = result.kind
                            testResultContent = result.content
                            showTestResult = true
                          }
                          .onFailure {
                            vm.notice = "测试失败: ${it.message ?: "未知错误"}"
                          }
                        testingId = null
                      }
                    },
                  )
                  MiniBtn(
                    text = "编辑",
                    onClick = {
                      editingModel = row
                      showEditor = true
                    },
                  )
                  MiniBtn(
                    text = "删除",
                    onClick = { pendingDelete = row },
                  )
                }
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          MiniBtn(text = "取消", onClick = onDismiss)
          MiniBtn(
            text = "确认配置",
            primary = true,
            full = true,
            onClick = {
              val configId = selectedId
              val selectedRow = slotOptions.firstOrNull { it.id == configId }
              if (configId == null || selectedRow == null) {
                vm.notice = "请先选择模型配置"
              } else {
                scope.launch {
                  runCatching {
                    if (slot.key == "storyAvatarMattingModel" && selectedRow.manufacturer == "local_birefnet") {
                      val status = vm.getLocalAvatarMattingStatus(selectedRow.manufacturer, selectedRow.model)
                      if (!status.installed) {
                        if (!status.canInstall) {
                          error(status.message.ifBlank { "当前环境无法安装本地 BiRefNet" })
                        }
                        val installed = vm.installLocalAvatarMattingModel(selectedRow.manufacturer, selectedRow.model)
                        if (!installed.installed) {
                          error(installed.message.ifBlank { "本地 BiRefNet 尚未安装完成" })
                        }
                      }
                    }
                    vm.bindGameModel(slot.key, configId)
                  }.onSuccess {
                    onDismiss()
                  }.onFailure {
                    vm.notice = "保存模型配置失败: ${it.message ?: "未知错误"}"
                  }
                }
              }
            },
          )
        }
      }
    }
  }

  if (showEditor) {
    SettingsModelEditorDialog(
      vm = vm,
      slot = slot,
      initial = editingModel,
      onDismiss = { showEditor = false },
      onSubmit = { id, manufacturer, modelType, model, baseUrl, apiKey ->
        scope.launch {
          runCatching {
            if (id == null) {
              vm.addManagedModelConfig(slot.configType, model, baseUrl, apiKey, modelType, manufacturer)
            } else {
              vm.updateManagedModelConfig(id, slot.configType, model, baseUrl, apiKey, modelType, manufacturer)
            }
          }.onSuccess {
            showEditor = false
          }.onFailure {
            vm.notice = "保存模型配置失败: ${it.message ?: "未知错误"}"
          }
        }
      },
    )
  }

  val deletingRow = pendingDelete
  if (deletingRow != null) {
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("删除模型配置") },
      text = { Text("确定删除「${deletingRow.model.ifBlank { "配置${deletingRow.id}" }}」吗？") },
      confirmButton = {
        TextButton(onClick = {
          scope.launch {
            runCatching { vm.deleteManagedModelConfig(deletingRow.id) }
              .onSuccess {
                if (selectedId == deletingRow.id) selectedId = null
              }
              .onFailure {
                vm.notice = "删除模型配置失败: ${it.message ?: "未知错误"}"
              }
            pendingDelete = null
          }
        }) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) {
          Text("取消")
        }
      },
    )
  }

  if (showTestResult) {
    AlertDialog(
      onDismissRequest = {
        showTestResult = false
        mediaPlayer?.release()
        mediaPlayer = null
      },
      title = { Text("测试结果") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(testResultTitle, fontWeight = FontWeight.Bold, color = Color(0xFF213958))
          when (testResultKind) {
            "image" -> AsyncImage(
              model = testResultContent,
              contentDescription = null,
              modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp)),
              contentScale = ContentScale.Crop,
            )
            "audio" -> {
              Text("语音模型测试成功。", color = Color(0xFF596E8C), style = MaterialTheme.typography.bodySmall)
              MiniBtn(
                text = "播放测试音频",
                primary = true,
                onClick = {
                  mediaPlayer?.release()
                  mediaPlayer = MediaPlayer().apply {
                    setDataSource(testResultContent)
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                      release()
                      mediaPlayer = null
                    }
                    prepareAsync()
                  }
                },
              )
            }
            else -> Text(testResultContent, color = Color(0xFF31445E))
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          showTestResult = false
          mediaPlayer?.release()
          mediaPlayer = null
        }) {
          Text("关闭")
        }
      },
    )
  }
}

@Composable
private fun SettingsModelEditorDialog(
  vm: MainViewModel,
  slot: MainViewModel.SettingsModelSlot,
  initial: com.toonflow.game.data.ModelConfigItem?,
  onDismiss: () -> Unit,
  onSubmit: (Long?, String, String, String, String, String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val normalizedInitialManufacturer = remember(initial?.id, slot.key) {
    val raw = initial?.manufacturer?.ifBlank { defaultSettingsManufacturerForSlot(slot) } ?: defaultSettingsManufacturerForSlot(slot)
    if (isVoiceDesignSlot(slot) && !isVoiceDesignManufacturer(raw)) defaultSettingsManufacturerForSlot(slot) else raw
  }
  val normalizedInitialModelType = remember(initial?.id, slot.key) {
    if (isVoiceDesignSlot(slot)) defaultSettingsModelTypeForSlot(slot)
    else initial?.modelType?.ifBlank { defaultSettingsModelTypeForSlot(slot) } ?: defaultSettingsModelTypeForSlot(slot)
  }
  val normalizedInitialModel = remember(initial?.id, slot.key, normalizedInitialManufacturer, normalizedInitialModelType) {
    val raw = initial?.model?.ifBlank { defaultSettingsModelNameForSlot(slot, normalizedInitialManufacturer, normalizedInitialModelType) }
      ?: defaultSettingsModelNameForSlot(slot, normalizedInitialManufacturer, normalizedInitialModelType)
    if (isVoiceDesignSlot(slot) && !isVoiceDesignModelName(raw)) {
      defaultSettingsModelNameForSlot(slot, normalizedInitialManufacturer, normalizedInitialModelType)
    } else {
      raw
    }
  }
  var manufacturer by remember(initial?.id, slot.key) { mutableStateOf(normalizedInitialManufacturer) }
  var modelType by remember(initial?.id, slot.key) { mutableStateOf(normalizedInitialModelType) }
  var model by remember(initial?.id, slot.key) {
    mutableStateOf(normalizedInitialModel)
  }
  var baseUrl by remember(initial?.id, slot.key) {
    mutableStateOf(initial?.baseUrl?.ifBlank { defaultSettingsBaseUrl(manufacturer, slot.configType, modelType) } ?: defaultSettingsBaseUrl(manufacturer, slot.configType, modelType))
  }
  var apiKey by remember(initial?.id) { mutableStateOf(initial?.apiKey.orEmpty()) }
  var manufacturerExpanded by remember { mutableStateOf(false) }
  var modelTypeExpanded by remember { mutableStateOf(false) }
  var previousManufacturer by remember(initial?.id, slot.key) { mutableStateOf(normalizedInitialManufacturer) }
  var previousModel by remember(initial?.id, slot.key) { mutableStateOf(normalizedInitialModel) }
  var previousBaseUrl by remember(initial?.id, slot.key) { mutableStateOf(initial?.baseUrl.orEmpty()) }
  var previousApiKey by remember(initial?.id) { mutableStateOf(initial?.apiKey.orEmpty()) }
  var localAvatarMattingStatus by remember { mutableStateOf<com.toonflow.game.data.LocalAvatarMattingStatus?>(null) }
  var localAvatarMattingInstalling by remember { mutableStateOf(false) }
  var showLocalInstallConfirm by remember { mutableStateOf(false) }
  val modelTypeOptions = remember(slot.key, slot.configType) { settingsModelTypeOptionsForSlot(slot) }
  val usesLocalAvatarMatting = remember(slot.key, manufacturer) {
    slot.key == "storyAvatarMattingModel" && manufacturer == "local_birefnet"
  }

  fun applyManufacturerDefaults(nextManufacturer: String, nextModelType: String) {
    if (baseUrl.isBlank() || initial == null) {
      baseUrl = defaultSettingsBaseUrl(nextManufacturer, slot.configType, nextModelType)
    }
    if (initial == null || model.isBlank()) {
      model = defaultSettingsModelNameForSlot(slot, nextManufacturer, nextModelType)
    }
  }

  suspend fun refreshLocalAvatarMattingStatus(): com.toonflow.game.data.LocalAvatarMattingStatus? {
    if (!usesLocalAvatarMatting) {
      localAvatarMattingStatus = null
      return null
    }
    val status = vm.getLocalAvatarMattingStatus(manufacturer, model.trim())
    localAvatarMattingStatus = status
    return status
  }

  suspend fun ensureLocalAvatarMattingInstalled(interactive: Boolean): Boolean {
    if (!usesLocalAvatarMatting) return true
    val status = refreshLocalAvatarMattingStatus() ?: return false
    if (status.installed) return true
    if (status.status.equals("installing", ignoreCase = true)) {
      vm.notice = status.message.ifBlank { "本地 BiRefNet 安装中，请稍候" }
      return false
    }
    if (!status.canInstall) {
      error(status.message.ifBlank { "当前环境无法安装本地 BiRefNet" })
    }
    if (!interactive) return false
    localAvatarMattingInstalling = true
    vm.notice = "正在安装本地 BiRefNet，请稍候..."
    try {
      val installed = vm.installLocalAvatarMattingModel(manufacturer, model.trim())
      localAvatarMattingStatus = installed
      vm.notice = installed.message.ifBlank { "本地 BiRefNet 已安装" }
      return installed.installed
    } finally {
      localAvatarMattingInstalling = false
    }
  }

  LaunchedEffect(usesLocalAvatarMatting, model, initial?.id) {
    if (!usesLocalAvatarMatting) {
      localAvatarMattingStatus = null
      localAvatarMattingInstalling = false
      showLocalInstallConfirm = false
      return@LaunchedEffect
    }
    runCatching {
      refreshLocalAvatarMattingStatus()
    }.onFailure {
      vm.notice = "读取本地 BiRefNet 状态失败: ${it.message ?: "未知错误"}"
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "新增模型" else "编辑模型") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("厂商", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
        Box {
          MiniBtn(text = settingsManufacturerLabel(manufacturer), primary = true, full = true, onClick = { manufacturerExpanded = true })
          DropdownMenu(expanded = manufacturerExpanded, onDismissRequest = { manufacturerExpanded = false }) {
            settingsManufacturersForSlot(slot).forEach { item ->
              DropdownMenuItem(
                text = { Text(item.label) },
                onClick = {
                  previousManufacturer = manufacturer
                  previousModel = model
                  previousBaseUrl = baseUrl
                  previousApiKey = apiKey
                  manufacturer = item.value
                  if (item.value == "local_birefnet") {
                    model = defaultSettingsModelNameForSlot(slot, item.value, modelType)
                    baseUrl = ""
                    apiKey = ""
                  } else {
                    applyManufacturerDefaults(item.value, modelType)
                  }
                  manufacturerExpanded = false
                  if (slot.key == "storyAvatarMattingModel" && item.value == "local_birefnet") {
                    scope.launch {
                      runCatching {
                        val status = vm.getLocalAvatarMattingStatus(item.value, model.trim())
                        localAvatarMattingStatus = status
                        if (!status.installed) {
                          showLocalInstallConfirm = true
                        }
                      }.onFailure {
                        manufacturer = previousManufacturer
                        applyManufacturerDefaults(manufacturer, modelType)
                        vm.notice = "读取本地 BiRefNet 状态失败: ${it.message ?: "未知错误"}"
                      }
                    }
                  } else {
                    localAvatarMattingStatus = null
                    showLocalInstallConfirm = false
                  }
                },
              )
            }
          }
        }
        settingsManufacturerWebsite(manufacturer).takeIf { it.isNotBlank() }?.let { website ->
          TextButton(
            onClick = {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website)))
              }
            },
            contentPadding = PaddingValues(0.dp),
          ) {
            Text("点击获取厂商 API", color = Color(0xFF4C67B2))
          }
        }

        if (settingsShouldShowModelType(slot)) {
          Text("类型", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
          Box {
            MiniBtn(
              text = settingsModelTypeOptionLabel(slot, modelType),
              full = true,
              onClick = { modelTypeExpanded = true },
            )
            DropdownMenu(expanded = modelTypeExpanded, onDismissRequest = { modelTypeExpanded = false }) {
              modelTypeOptions.forEach { item ->
                DropdownMenuItem(
                  text = { Text(item.second) },
                  onClick = {
                    modelType = item.first
                    applyManufacturerDefaults(manufacturer, item.first)
                    modelTypeExpanded = false
                  },
                )
              }
            }
          }
        }

        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型") }, modifier = Modifier.fillMaxWidth())
        if (usesLocalAvatarMatting) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFFF6FBEF))
              .border(1.dp, Color(0xFFD9E5CF), RoundedCornerShape(14.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text("BiRefNet 本地模型", fontWeight = FontWeight.Bold, color = Color(0xFF31562D))
            Text(
              localAvatarMattingStatus?.message?.ifBlank { "首次使用需要安装 Python 依赖和模型文件。" }
                ?: "首次使用需要安装 Python 依赖和模型文件。",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF567052),
            )
            if (localAvatarMattingStatus?.installed != true) {
              MiniBtn(
                text = if (localAvatarMattingInstalling) "安装中" else if (localAvatarMattingStatus?.status == "failed") "重新安装" else "立即安装",
                onClick = {
                  scope.launch {
                    runCatching { ensureLocalAvatarMattingInstalled(true) }
                      .onFailure {
                        vm.notice = "本地 BiRefNet 安装失败: ${it.message ?: "未知错误"}"
                      }
                  }
                },
              )
            }
          }
        } else {
          OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(settingsApiKeyPlaceholder(slot, manufacturer)) },
            visualTransformation = PasswordVisualTransformation(),
          )
        }
        settingsApiKeyHint(slot, manufacturer).takeIf { it.isNotBlank() }?.let { hint ->
          Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A5B0B),
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        val submitManufacturer = if (isVoiceDesignSlot(slot)) defaultSettingsManufacturerForSlot(slot) else manufacturer
        val submitModelType = if (isVoiceDesignSlot(slot)) defaultSettingsModelTypeForSlot(slot) else modelType
        if (model.trim().isBlank()) return@TextButton
        if (settingsApiKeyRequired(submitManufacturer, slot.configType) && apiKey.trim().isBlank()) return@TextButton
        scope.launch {
          runCatching {
            if (slot.key == "storyAvatarMattingModel" && submitManufacturer == "local_birefnet") {
              val ready = ensureLocalAvatarMattingInstalled(true)
              if (!ready) return@runCatching
            }
            onSubmit(
              initial?.id,
              submitManufacturer,
              submitModelType,
              model.trim(),
              if (submitManufacturer == "local_birefnet") "" else baseUrl.trim(),
              if (submitManufacturer == "local_birefnet") "" else apiKey.trim(),
            )
          }.onFailure {
            vm.notice = "本地 BiRefNet 安装失败: ${it.message ?: "未知错误"}"
          }
        }
      }) {
        Text(if (localAvatarMattingInstalling) "安装中" else "保存")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    },
  )

  if (showLocalInstallConfirm) {
    AlertDialog(
      onDismissRequest = {
        showLocalInstallConfirm = false
        manufacturer = previousManufacturer
        model = previousModel
        baseUrl = previousBaseUrl
        apiKey = previousApiKey
      },
      title = { Text("安装本地 BiRefNet") },
      text = {
        Text(localAvatarMattingStatus?.message?.ifBlank { "首次使用需要安装 Python 依赖和模型文件。" }
          ?: "首次使用需要安装 Python 依赖和模型文件。")
      },
      confirmButton = {
        TextButton(onClick = {
          showLocalInstallConfirm = false
          scope.launch {
            runCatching {
              ensureLocalAvatarMattingInstalled(true)
            }.onFailure {
              vm.notice = "本地 BiRefNet 安装失败: ${it.message ?: "未知错误"}"
              manufacturer = previousManufacturer
              model = previousModel
              baseUrl = previousBaseUrl
              apiKey = previousApiKey
            }
          }
        }) {
          Text("确认安装")
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showLocalInstallConfirm = false
          manufacturer = previousManufacturer
          model = previousModel
          baseUrl = previousBaseUrl
          apiKey = previousApiKey
        }) {
          Text("取消")
        }
      },
    )
  }
}

@Composable
private fun PasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  revealed: Boolean,
  onToggleReveal: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth(),
      visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MiniBtn(text = if (revealed) "隐藏" else "显示", onClick = onToggleReveal)
    }
  }
}

@Composable
private fun ProjectSwitcherBar(vm: MainViewModel) {
  if (vm.projects.isEmpty()) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2D9)),
      shape = RoundedCornerShape(10.dp),
    ) {
      Text(
        "暂无项目，请先登录并检查账号是否有项目。",
        color = Color(0xFF5A512E),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(8.dp),
      )
    }
    return
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    vm.projects.forEach { project ->
      val active = project.id == vm.selectedProjectId
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(if (active) Color(0xFF314F7E) else Color(0xFFF2F7FF))
          .border(1.dp, if (active) Color(0xFF2A426A) else Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
          .clickable { vm.onProjectChanged(project.id) }
          .padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        Text(
          project.name.ifBlank { "未命名项目" },
          color = if (active) Color.White else Color(0xFF2E466A),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFFF8FBFF))
      .border(1.dp, Color(0xFFD4DEEE), RoundedCornerShape(10.dp))
      .clickable { onToggle() }
      .padding(horizontal = 10.dp, vertical = 9.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E5170))
    Box(
      modifier = Modifier
        .width(34.dp)
        .height(20.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(if (checked) Color(0xFF4AD06B) else Color(0xFFD3DEEF))
        .padding(2.dp),
      contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
      Box(
        modifier = Modifier
          .size(16.dp)
          .clip(CircleShape)
          .background(Color.White),
      )
    }
  }
}

@Composable
private fun InitialAvatar(name: String) {
  val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
  Box(
    modifier = Modifier
      .size(56.dp)
      .clip(CircleShape)
      .background(Color(0xFFE8EEF8))
      .border(1.dp, Color(0xFFD3DEEF), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(initial, color = Color(0xFF3A5276), fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun StoryCoverImage(title: String, coverPath: String? = null, modifier: Modifier = Modifier, emptyText: String = "无封面") {
  val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "故"
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (!coverPath.isNullOrBlank()) {
      AsyncImage(
        model = coverPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(modifier = Modifier.fillMaxSize().background(Color(0x22081424)))
      Text(
        title.ifBlank { "故事" },
        color = Color.White,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp),
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            brush = Brush.linearGradient(
              colors = listOf(Color(0xFFEAF1FD), Color(0xFFDCE7F8)),
            ),
          )
          .border(1.dp, Color(0xFFCFDAED)),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(initial, color = Color(0xFF445D82), fontWeight = FontWeight.ExtraBold)
          Text(emptyText, color = Color(0xFF6C82A3), style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun HeaderTitle(title: String, rightText: String, onRightClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2B40))
    Text(
      rightText,
      color = Color(0xFF6A59A8),
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.clickable { onRightClick() },
    )
  }
}

@Composable
private fun AvatarItem(
  label: String,
  icon: ImageVector,
  avatarPath: String? = null,
  avatarBgPath: String? = null,
  onClick: () -> Unit,
  showEditBadge: Boolean = false,
  iconTint: Color = Color(0xFF8AA0C6),
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    LayeredAvatarFrame(
      foregroundPath = avatarPath,
      backgroundPath = avatarBgPath,
      modifier = Modifier
        .size(54.dp)
        .clickable { onClick() },
      backgroundColor = Color.White,
      borderColor = Color(0xFFD3DEEF),
      fallback = {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(16.dp),
        )
      },
      overlay = {
        if (showEditBadge) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .offset(x = (-9).dp, y = (-2).dp)
              .size(17.dp)
              .clip(CircleShape)
              .background(Color.White)
              .border(1.dp, Color(0xFFC7D4E8), CircleShape)
              .shadow(elevation = 2.dp, shape = CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = "编辑",
              tint = Color(0xFF6B7F9F),
              modifier = Modifier.size(9.dp),
            )
          }
        }
      },
    )
    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF50617C), modifier = Modifier.padding(top = 5.dp))
  }
}

@Composable
private fun MediumEditableAvatar(path: String?, backgroundPath: String?, fallbackName: String, loading: Boolean = false, onClick: () -> Unit) {
  LayeredAvatarFrame(
    foregroundPath = path,
    backgroundPath = backgroundPath,
    modifier = Modifier
      .size(82.dp)
      .clickable(enabled = !loading) { onClick() },
    backgroundColor = Color(0xFFE5EAF3),
    borderColor = Color(0xFFCCD8EA),
    fallback = {
      Text(
        text = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        color = Color(0xFF7D8CA5),
        fontWeight = FontWeight.Bold,
      )
    },
    overlay = {
      if (loading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66132134)),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color(0xFFFFD071),
            strokeWidth = 2.2.dp,
          )
        }
      }

      if (!loading) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFC7D4E8), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "更换头像",
            tint = Color(0xFF6B7F9F),
            modifier = Modifier.size(14.dp),
          )
        }
      }
    },
  )
}

@Composable
private fun CircleGhostBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(Color(0xAD071425))
      .border(1.dp, Color(0xE1F1F7FF), CircleShape)
      .clickable { onClick() },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier = Modifier.size(18.dp),
    )
  }
}

@Composable
private fun BottomNav(active: String, onClick: (String) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp)
      .background(Color(0xF5FFFFFF))
      .border(1.dp, Color(0xFFDbe5F5)),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NavItem("home", "主页", active, onClick)
    NavItem("create", "创建故事", active, onClick)
    NavItem("chat", "聊过", active, onClick)
    NavItem("my", "我的", active, onClick)
  }
}

@Composable
private fun NavItem(key: String, label: String, active: String, onClick: (String) -> Unit) {
  val isActive = key == active
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick(key) }) {
    Text(label, color = if (isActive) activeOrange else Color(0xFF51627F), style = MaterialTheme.typography.labelMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
  }
}

@Composable
private fun ProtoInputCard(label: String, top: androidx.compose.ui.unit.Dp = 0.dp, content: @Composable () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(top = top),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6A7F9F), fontWeight = FontWeight.Bold)
      content()
    }
  }
}

@Composable
private fun ScrollableOutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  minLines: Int = 2,
  maxLines: Int = Int.MAX_VALUE,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  placeholder: (@Composable () -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
) {
  val scrollState = rememberScrollState()
  val visibleLines = minLines.coerceAtLeast(2)
  val explicitLineCount = value.lineSequence().count().coerceAtLeast(1)
  val wrappedLineCount = value
    .split('\n')
    .sumOf { line -> maxOf(1, kotlin.math.ceil(line.length / 18.0).toInt()) }
    .coerceAtLeast(1)
  val estimatedLineCount = maxOf(explicitLineCount, wrappedLineCount, visibleLines)
  val fieldHeight = (visibleLines * 24 + 24).dp
  val trackHeight = (visibleLines * 24).dp
  val thumbFraction = (visibleLines.toFloat() / estimatedLineCount.toFloat()).coerceIn(0.2f, 1f)
  val thumbHeight = trackHeight * thumbFraction
  val thumbTravel = trackHeight - thumbHeight
  val scrollProgress = if (scrollState.maxValue <= 0) 0f else scrollState.value.toFloat() / scrollState.maxValue.toFloat()

  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(fieldHeight)
        .clip(RoundedCornerShape(10.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFBCC9DA), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1F2A3F)),
        modifier = Modifier
          .fillMaxWidth()
          .height(trackHeight)
          .padding(end = 10.dp)
          .verticalScroll(scrollState),
        decorationBox = { innerTextField ->
          Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isBlank() && placeholder != null) {
              Box(modifier = Modifier.alpha(0.6f)) {
                placeholder()
              }
            }
            innerTextField()
          }
        },
      )
    }
    if (maxLines > minLines && (estimatedLineCount > visibleLines || value.isNotBlank() || scrollState.maxValue > 0)) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 10.dp)
          .height(trackHeight)
          .width(3.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(Color(0xFFE4EAF3)),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(thumbHeight)
            .offset(y = thumbTravel * scrollProgress)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF9BAAC0)),
        )
      }
    }
  }
}

@Composable
private fun MiniBtn(text: String, primary: Boolean = false, full: Boolean = false, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = (if (full) Modifier.fillMaxWidth() else Modifier).height(32.dp),
    shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (primary) Color(0xFF314F7E) else Color(0xFFF2F7FF),
      contentColor = if (primary) Color.White else Color(0xFF2E466A),
    ),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
  ) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun MiniIconBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.size(32.dp),
    shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFFF2F7FF),
      contentColor = Color(0xFF2E466A),
    ),
    contentPadding = PaddingValues(0.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier = Modifier.size(16.dp),
    )
  }
}
