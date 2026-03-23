package com.toonflow.game

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import coil.compose.AsyncImage
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.VoiceBindingDraft
import com.toonflow.game.data.VoiceMixItem
import com.toonflow.game.data.WorldItem
import com.toonflow.game.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private val bgDark = Color(0xFF081424)
private val textSoft = Color(0xFFEAF3FF)
private val pageGray = Color(0xFFF1F3F7)
private val lightLine = Color(0xFFD3DEEF)
private val warnYellow = Color(0xFFFFE600)
private val activeOrange = Color(0xFFFF8E2B)

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
  var autoVoice by remember { mutableStateOf(true) }
  var showDialogMenu by remember { mutableStateOf(false) }
  var showStorySettingDetail by remember { mutableStateOf(false) }
  val storyAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateStoryPlayerAvatarFromUri(uri.toString())
    }
  }
  val accountAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      vm.updateAccountAvatarFromUri(uri.toString())
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

  Column(modifier = Modifier.fillMaxSize().background(pageGray)) {
    if (vm.notice.isNotBlank()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8E0F8)),
        shape = RoundedCornerShape(10.dp),
      ) {
        Text(
          text = vm.notice,
          color = Color(0xFF4B4470),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(8.dp),
        )
      }
    }

    val showProjectBar = vm.activeTab == "主页" || vm.activeTab == "故事大厅"
    if (showProjectBar) {
      ProjectSwitcherBar(vm = vm)
    }

    Box(modifier = Modifier.weight(1f)) {
      when (vm.activeTab) {
        "主页" -> HomeScene(vm = vm, autoVoice = autoVoice, onToggleVoice = { autoVoice = !autoVoice })
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
        )
        "聊过" -> HistoryScene(vm = vm)
        "游玩" -> PlayScene(
          vm = vm,
          mode = playMode,
          onModeChange = { playMode = it },
          autoVoice = autoVoice,
          onToggleVoice = { autoVoice = !autoVoice },
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
        "我的" -> ProfileScene(vm = vm, onPickAvatar = { accountAvatarPicker.launch("image/*") })
        "设置" -> SettingsScene(vm = vm)
        else -> HomeScene(vm = vm, autoVoice = autoVoice, onToggleVoice = { autoVoice = !autoVoice })
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
}

@Composable
private fun HomeScene(vm: MainViewModel, autoVoice: Boolean, onToggleVoice: () -> Unit) {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0xFF0F1F36), Color(0xFF1C3659), Color(0xFF0B1526)),
          ),
        ),
    )

    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 8.dp, end = 10.dp),
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
        .padding(top = 12.dp, start = 10.dp),
    ) {
      Text("主页", color = Color(0xFFE7F1FF), fontWeight = FontWeight.Bold)
      Text("项目：${vm.selectedProjectName()}", color = Color(0xFFBFD3F1), style = MaterialTheme.typography.labelSmall)
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val rec = vm.recommendedWorld()
      if (rec != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x6620344B)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              vm.startFromWorld(rec, vm.quickInput.trim())
              vm.quickInput = ""
            },
        ) {
          Box(modifier = Modifier.fillMaxWidth()) {
            val coverPath = vm.worldCoverPath(rec).trim().ifBlank { null }
            if (coverPath != null) {
              AsyncImage(
                model = coverPath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(138.dp),
                contentScale = ContentScale.Crop,
              )
              Box(modifier = Modifier.fillMaxWidth().height(138.dp).background(Color(0x7A14263A)))
            }
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "随机推荐：${rec.name}",
                color = textSoft,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = rec.intro.ifBlank { "点击进入故事，或按住说话。" },
                color = textSoft,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
              )
              Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text("章节 ${rec.chapterCount ?: 0}", color = Color(0xFFE7EDF9), style = MaterialTheme.typography.labelSmall)
                Text("会话 ${rec.sessionCount ?: 0}", color = Color(0xFFE7EDF9), style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      } else {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x6620344B)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("暂无可游玩故事", color = textSoft, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("当前项目还没有“已发布且有章节”的故事。", color = textSoft, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
          }
        }
      }

      OutlinedTextField(
        value = vm.quickInput,
        onValueChange = { vm.quickInput = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("输入一句话开始故事") },
        singleLine = false,
      )

      Button(
        onClick = { vm.quickStart() },
        enabled = rec != null,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF41516C), contentColor = Color.White),
      ) {
        Text("按住说话")
      }
    }
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
            onPlay = { vm.startFromWorld(item) },
            onEdit = {
              vm.openWorldForEdit(item)
            },
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
private fun HallStoryCard(world: WorldItem, onPlay: () -> Unit, onEdit: () -> Unit) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    modifier = Modifier.fillMaxWidth(),
  ) {
    StoryCoverImage(
      title = world.name,
      coverPath = world.settings?.coverPath?.ifBlank { null },
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
        MiniBtn(text = "编辑", onClick = onEdit)
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
) {
  var npcName by remember { mutableStateOf("") }
  var npcDesc by remember { mutableStateOf("") }
  var npcVoice by remember { mutableStateOf("") }
  var npcVoiceMode by remember { mutableStateOf("text") }
  var npcVoiceConfigId by remember { mutableStateOf<Long?>(null) }
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
  var showCoverActionDialog by remember { mutableStateOf(false) }
  var showChapterBgActionDialog by remember { mutableStateOf(false) }
  var showUserVoiceDialog by remember { mutableStateOf(false) }
  var showNpcVoiceDialog by remember { mutableStateOf(false) }
  var showNarratorVoiceDialog by remember { mutableStateOf(false) }
  var showImageGenerateDialog by remember { mutableStateOf(false) }
  var imageGenerateTarget by remember { mutableStateOf("") }
  var imageGeneratePrompt by remember { mutableStateOf("") }
  var imageGenerateStyleKey by remember { mutableStateOf("general_3") }
  val imageGenerateReferenceUris = remember { mutableStateListOf<String>() }
  val mentionRoles = vm.mentionRoleNames().distinct().filter { it.isNotBlank() }.ifEmpty { listOf("用户", "旁白") }
  val resolvedOpeningRole = if (vm.chapterOpeningRole in mentionRoles) vm.chapterOpeningRole else mentionRoles.first()
  val chapterRemain = (1500 - vm.chapterContent.length).coerceAtLeast(0)
  val showGlobalBackground = vm.chapters.size > 1 || vm.globalBackground.isNotBlank()
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
              fallbackName = vm.playerName.ifBlank { vm.userName.ifBlank { "用户" } },
              onClick = { showAvatarActionDialog = true },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("点击头像更换", color = Color(0xFF3F5476), fontWeight = FontWeight.SemiBold)
              Text("支持 PNG / GIF，保存时会自动标准化。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
              Text("可选：上传、AI 文生图、AI 图生图。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
            }
          }
          if (showAvatarActionDialog) {
            AvatarActionDialog(
              onDismiss = { showAvatarActionDialog = false },
              onUpload = {
                showAvatarActionDialog = false
                onPickAvatar()
              },
              onAiGenerate = {
                openImageGenerate("user", vm.playerDesc)
                showAvatarActionDialog = false
              },
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
          OutlinedTextField(
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
            initialConfigId = vm.playerVoiceConfigId,
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
                  configId = npcVoiceConfigId,
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
                    configId = npcVoiceConfigId,
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
          npcVoiceConfigId = null
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
              fallbackName = npcName.ifBlank { "角色" },
              onClick = { showNpcAvatarActionDialog = true },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("点击头像更换", color = Color(0xFF3F5476), fontWeight = FontWeight.SemiBold)
              Text("支持 PNG / GIF，保存时会自动标准化。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
              Text("可选：上传、AI 文生图、AI 图生图。", color = Color(0xFF7F95B5), style = MaterialTheme.typography.bodySmall)
            }
          }
          if (showNpcAvatarActionDialog) {
            AvatarActionDialog(
              onDismiss = { showNpcAvatarActionDialog = false },
              onUpload = {
                showNpcAvatarActionDialog = false
                npcAvatarPicker.launch("image/*")
              },
              onAiGenerate = {
                openImageGenerate("npc", npcDesc)
                showNpcAvatarActionDialog = false
              },
            )
          }
        }
        ProtoInputCard(label = "角色名") {
          OutlinedTextField(value = npcName, onValueChange = { npcName = it }, modifier = Modifier.fillMaxWidth())
        }
        ProtoInputCard(label = "角色设定", top = 10.dp) {
          OutlinedTextField(
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
            onClick = {
              vm.removeNpcRole(editingNpcIndex)
              npcName = ""
              npcDesc = ""
              npcVoice = ""
              npcVoiceMode = "text"
              npcVoiceConfigId = null
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
            Text("删除当前角色", color = Color(0xFFC64242), fontWeight = FontWeight.SemiBold)
          }
        }
        if (showNpcVoiceDialog) {
          VoicePickerDialog(
            vm = vm,
            title = "选择角色音色",
            initialLabel = npcVoice,
            initialConfigId = npcVoiceConfigId,
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
              npcVoiceConfigId = binding.configId
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
        OutlinedTextField(
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
      vm.saveStoryEditor(publish = false, successNotice = "故事设定已保存")
      onNext()
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
            onClick = onOpenUserEditor,
            showEditBadge = true,
            iconTint = Color(0xFF6B55A2),
          )
          vm.npcRoles.forEachIndexed { index, role ->
            AvatarItem(
              label = role.name.ifBlank { "新角色" },
              icon = Icons.Outlined.AccountCircle,
              avatarPath = role.avatarPath.trim().ifBlank { null },
              onClick = {
                npcName = role.name
                npcAvatarPath = role.avatarPath
                npcAvatarBgPath = role.avatarBgPath
                npcAvatarDraftKey = role.id.ifBlank { "draft_npc_${System.currentTimeMillis()}" }
                npcDesc = role.description
                npcVoice = role.voice
                npcVoiceMode = role.voiceMode
                npcVoiceConfigId = role.voiceConfigId
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
              npcVoiceConfigId = null
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

    if (showGlobalBackground) {
      Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("全局背景（选填）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
          OutlinedTextField(
            value = vm.globalBackground,
            onValueChange = { vm.globalBackground = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = { Text("多章节故事时可填写世界背景，提及时请使用角色名或“用户”。") },
          )
          MentionRow(vm = vm, onClick = { vm.appendGlobalMention(it) })
        }
      }
    }

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
        OutlinedTextField(
          value = vm.chapterOpeningLine,
          onValueChange = { vm.chapterOpeningLine = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          placeholder = { Text("作为选定角色/旁白的第一句话开启整个故事") },
        )
      }
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
        OutlinedTextField(
          value = vm.chapterContent,
          onValueChange = { if (it.length <= 1500) vm.chapterContent = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 4,
          placeholder = { Text("描述主要情节，包括用户在故事中和其他角色的互动。提及时请使用角色原名或用户，不要使用“你”来代称。") },
        )
        MentionRow(vm = vm, onClick = { vm.appendChapterMention(it) })
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("还可输入1500字", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A8C0))
          Text("${1500 - chapterRemain}/1500", style = MaterialTheme.typography.bodySmall, color = Color(0xFF98A8C0))
        }
      }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("成功条件（章节结局）", fontWeight = FontWeight.Bold, color = Color(0xFF232F43))
        OutlinedTextField(
          value = vm.chapterCondition,
          onValueChange = { vm.chapterCondition = it },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          placeholder = { Text("只有用户达成该条件才进入下一章节。为空代表无结束，AI 持续编排。") },
        )
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

    if (vm.chapters.isNotEmpty() || vm.chapterTitle.isNotBlank() || vm.chapterContent.isNotBlank()) {
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        vm.chapters.forEach { chapter ->
          val active = chapter.id == vm.selectedChapterId
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(if (active) Color(0xFF314F7E) else Color(0xFFF2F7FF))
              .border(1.dp, if (active) Color(0xFF2A426A) else Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
              .clickable { vm.saveCurrentChapterAndSelect(chapter.id) }
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              chapter.title.ifBlank { "章节${chapter.sort}" },
              color = if (active) Color.White else Color(0xFF2E466A),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
        val draftActive = vm.selectedChapterId == null && (vm.chapterTitle.isNotBlank() || vm.chapterContent.isNotBlank() || vm.chapterOpeningLine.isNotBlank())
        if (draftActive) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(Color(0xFF314F7E))
              .border(1.dp, Color(0xFF2A426A), RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            Text(
              vm.chapterTitle.ifBlank { "新章节草稿" },
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF2F7FF))
            .border(1.dp, Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
            .clickable { vm.saveCurrentChapterAndSelect(null) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
          Text("新章节", color = Color(0xFF2E466A), style = MaterialTheme.typography.labelSmall)
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
            initialConfigId = vm.narratorVoiceConfigId,
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
          Text("章节背景图", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5E7395))
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
        if (showChapterBgActionDialog) {
          AvatarActionDialog(
            onDismiss = { showChapterBgActionDialog = false },
            onUpload = {
              showChapterBgActionDialog = false
              chapterBgPicker.launch("image/*")
            },
            onAiGenerate = {
              openImageGenerate("chapter", vm.chapterContent)
              showChapterBgActionDialog = false
            },
          )
        }
          OutlinedTextField(
            value = vm.chapterMusic,
            onValueChange = { vm.chapterMusic = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("背景音乐（可选）") },
          )
          SettingSwitchRow(
            label = "结局条件对用户可见",
            checked = vm.chapterConditionVisible,
            onToggle = { vm.chapterConditionVisible = !vm.chapterConditionVisible },
          )
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
        vm.saveStoryEditor(publish = false, successNotice = "故事设定已保存")
        onNext()
      },
      modifier = Modifier.fillMaxWidth().height(44.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = warnYellow, contentColor = Color(0xFF1F2128)),
    ) {
      Text("下一步", fontWeight = FontWeight.ExtraBold)
    }

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
            coverPath = item.worldCoverPath.ifBlank { vm.worldCoverPath(world) }.ifBlank { null },
            onClick = { vm.openSession(item.sessionId) },
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryCard(item: SessionItem, coverPath: String?, onClick: () -> Unit) {
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
        if (item.projectName.isNotBlank()) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(Color(0xFFEEF3FC))
              .border(1.dp, Color(0xFFD7E2F2), RoundedCornerShape(999.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp),
          ) {
            Text(
              text = item.projectName,
              color = Color(0xFF4F6281),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
        Text(
          text = if (item.title.isBlank()) item.worldName else item.title,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = if (item.projectName.isNotBlank()) 6.dp else 0.dp),
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
  var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
  var ttsReady by remember { mutableStateOf(false) }
  DisposableEffect(context) {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context) { status ->
      val ready = status == TextToSpeech.SUCCESS
      ttsReady = ready
      if (ready) {
        engine?.language = Locale.SIMPLIFIED_CHINESE
      }
    }
    textToSpeech = engine
    onDispose {
      ttsReady = false
      textToSpeech?.stop()
      textToSpeech?.shutdown()
      textToSpeech = null
    }
  }

  val sessionTitle = vm.playSessionTitle()
  val currentChapter = vm.playCurrentChapter()
  val displayMessages = if (mode == "history") vm.messages.toList() else vm.messages.takeLast(1)
  val listState = rememberLazyListState()
  LaunchedEffect(mode, displayMessages.size) {
    if (displayMessages.isNotEmpty()) {
      listState.scrollToItem(displayMessages.lastIndex)
    }
  }
  LaunchedEffect(mode) {
    if (mode == "tips" || mode == "setting") {
      selectedMessage = null
      onCloseDialogMenu()
    }
  }

  val tipOptions = vm.buildAiTipOptions()
  val statePreview = vm.playStatePreview()
  val chapterTitle = vm.playChapterTitle()
  val playerAvatarPath = vm.userAvatarPath.trim().ifBlank { null }
  val playerAvatarBgPath = vm.userAvatarBgPath.trim().ifBlank { null }
  val chapterBackgroundPath = vm.playChapterBackgroundPath().trim().ifBlank { null }
  val closeDialogMenu = {
    selectedMessage = null
    onCloseDialogMenu()
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15283F))) {
    if (chapterBackgroundPath != null) {
      AsyncImage(
        model = chapterBackgroundPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Box(modifier = Modifier.fillMaxSize().background(Color(0xAA0F1D31)))
    }
    if (playerAvatarBgPath != null) {
      AsyncImage(
        model = playerAvatarBgPath,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().alpha(0.16f),
        contentScale = ContentScale.Crop,
      )
    }
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(sessionTitle, color = textSoft, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            text = if (vm.debugMode) "章节：$chapterTitle（调试）" else "章节：$chapterTitle",
            color = Color(0xFFBED2F0),
            style = MaterialTheme.typography.labelSmall,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CircleGhostBtn(icon = Icons.Outlined.Search, contentDescription = "进入故事大厅") { vm.setTab("故事大厅") }
          CircleGhostBtn(
            icon = if (autoVoice) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            contentDescription = "切换语音",
          ) { onToggleVoice() }
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (displayMessages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("当前会话暂无消息，发送一句话开始。", color = Color(0xFFD5E6FF))
          }
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(displayMessages, key = { "${it.id}_${it.createTime}" }) { msg ->
              Bubble(
                title = vm.displayNameForMessage(msg),
                content = msg.content.ifBlank { "（空消息）" },
                roleType = msg.roleType,
                avatarPath = vm.avatarPathForMessage(msg).trim().ifBlank { if (msg.roleType == "player") playerAvatarPath else null },
                reaction = vm.reactionForMessage(msg),
                onOpenMenu = {
                  selectedMessage = msg
                  onOpenDialogMenu()
                },
              )
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
            chapterContent = currentChapter?.content?.ifBlank { "暂无章节内容" } ?: "暂无章节内容",
            chapterCondition = vm.playChapterConditionText(),
            roles = vm.playStoryRoles(),
            allowRoleView = vm.playAllowRoleView(),
            statePreview = statePreview,
            miniGameSummary = "类型:${vm.miniGameType.ifBlank { "-" }} 状态:${vm.miniGameStatus} 轮次:${vm.miniGameRound}",
            showStorySettingDetail = showStorySettingDetail,
            onToggleStorySettingDetail = onToggleStorySettingDetail,
            onClose = onCloseSetting,
          )
        }

        if (showDialogMenu && mode != "tips" && mode != "setting" && selectedMessage != null) {
          DialogMenu(
            message = selectedMessage!!,
            reaction = vm.reactionForMessage(selectedMessage!!),
            onCopy = {
              clipboardManager.setText(AnnotatedString(selectedMessage!!.content))
              vm.notice = "已复制对话内容"
              closeDialogMenu()
            },
            onReplay = {
              val content = selectedMessage!!.content.trim()
              if (content.isBlank()) {
                vm.notice = "这条对话没有可重听内容"
              } else if (!ttsReady || textToSpeech == null) {
                vm.notice = "系统语音暂不可用"
              } else {
                textToSpeech?.speak(content, TextToSpeech.QUEUE_FLUSH, null, vm.messageUiKey(selectedMessage!!))
                vm.notice = "正在重听该条对话"
              }
              closeDialogMenu()
            },
            onLike = {
              vm.setReactionForMessage(selectedMessage!!, "like")
              closeDialogMenu()
            },
            onDislike = {
              vm.setReactionForMessage(selectedMessage!!, "dislike")
              closeDialogMenu()
            },
            onRewrite = {
              vm.applyRewritePrompt(selectedMessage!!)
              closeDialogMenu()
            },
            onClose = closeDialogMenu,
          )
        }
      }

      FooterBar(
        debugMode = vm.debugMode,
        mode = mode,
        onModeChange = onModeChange,
        onOpenSetting = { onModeChange("setting") },
        onExitDebug = onExitDebug,
        onRefresh = { vm.refreshPlaySession() },
        miniTags = vm.miniGameTags,
        onMiniTag = { vm.triggerMiniGame(it) },
        onSyncMiniGame = { vm.syncMiniGame() },
        sendText = vm.sendText,
        onSendTextChange = { vm.sendText = it },
        onSend = { vm.sendMessage() },
      )
    }
  }

  if (vm.debugEndDialog != null) {
    AlertDialog(
      onDismissRequest = { vm.closeDebugDialog(false) },
      title = { Text("章节调试结束") },
      text = { Text(vm.debugEndDialog ?: "") },
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
private fun Bubble(
  title: String,
  content: String,
  roleType: String = "npc",
  avatarPath: String? = null,
  reaction: String = "",
  onOpenMenu: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isPlayer = roleType == "player"
  val reactionLabel = when (reaction) {
    "like" -> "已点赞"
    "dislike" -> "已点踩"
    else -> ""
  }
  val reactionIcon = if (reaction == "dislike") Icons.Outlined.ThumbDown else Icons.Outlined.ThumbUp
  Column(
    modifier = modifier.fillMaxWidth(),
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
        SmallAvatar(path = avatarPath, title = title)
        Spacer(modifier = Modifier.width(6.dp))
      }
      Card(
        modifier = Modifier
          .fillMaxWidth(0.88f)
          .pointerInput(onOpenMenu) {
            detectTapGestures(
              onDoubleTap = { onOpenMenu?.invoke() },
              onLongPress = { onOpenMenu?.invoke() },
            )
          },
        colors = CardDefaults.cardColors(containerColor = if (isPlayer) Color(0xB3274568) else Color(0xE6FFFFFF)),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            content,
            color = if (isPlayer) Color(0xFFE8F2FF) else Color(0xFF1F2B40),
            style = MaterialTheme.typography.bodySmall,
          )
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
        SmallAvatar(path = avatarPath, title = title)
      }
    }
  }
}

@Composable
private fun SmallAvatar(path: String?, title: String) {
  Box(
    modifier = Modifier
      .size(24.dp)
      .clip(CircleShape)
      .background(Color(0xFFE5EAF3))
      .border(1.dp, Color(0xFFC7D4E8), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (!path.isNullOrBlank()) {
      AsyncImage(
        model = path,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Text(
        text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        color = Color(0xFF7D8CA5),
        style = MaterialTheme.typography.labelSmall,
      )
    }
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
  onClose: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 48.dp)
      .offset(y = (-160).dp),
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
      .offset(y = (-80).dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBFF)),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("AI 提示", color = Color(0xFF233450), fontWeight = FontWeight.Bold)
      options.take(3).forEach { option ->
        MiniBtn(text = option, onClick = { onPick(option) }, full = true)
      }
      MiniBtn(text = "返回", onClick = onBack)
    }
  }
}

@Composable
private fun DialogMenuAction(icon: ImageVector, text: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 9.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun StorySettingPanel(
  worldName: String,
  worldIntro: String,
  globalBackground: String,
  chapterTitle: String,
  chapterContent: String,
  chapterCondition: String,
  roles: List<com.toonflow.game.data.StoryRole>,
  allowRoleView: Boolean,
  statePreview: String,
  miniGameSummary: String,
  showStorySettingDetail: Boolean,
  onToggleStorySettingDetail: () -> Unit,
  onClose: () -> Unit,
) {
  var selectedRoleId by remember(worldName) { mutableStateOf<String?>(null) }
  var showModePicker by remember(worldName) { mutableStateOf(false) }
  val selectedRole = roles.firstOrNull { it.id == selectedRoleId } ?: roles.firstOrNull()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp)
      .offset(y = (-90).dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xDD0B1A2D)),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(worldName, color = Color.White, fontWeight = FontWeight.Bold)
      Text("简介：$worldIntro", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
      Text("角色列表", color = Color(0xFFD5EBFF), style = MaterialTheme.typography.bodySmall)
      if (roles.isNotEmpty()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          roles.forEach { role ->
            val active = role.id == selectedRole?.id
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (active) Color(0x333A7BFF) else Color(0x22122136))
                .border(1.dp, if (active) Color(0xFF80AAFF) else Color(0x334C6A93), RoundedCornerShape(999.dp))
                .clickable { selectedRoleId = role.id },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Spacer(modifier = Modifier.width(2.dp))
              SmallAvatar(path = role.avatarPath.trim().ifBlank { null }, title = role.name)
              Text(
                role.name.ifBlank { role.roleType },
                color = Color(0xFFDCEEFF),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 10.dp),
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
              Text("参数卡：${card}", color = Color(0xFFAFC6E9), style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
      Text("小游戏：$miniGameSummary", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
      if (showStorySettingDetail) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("故事设定", color = Color.White, fontWeight = FontWeight.Bold)
            Text("故事背景：${globalBackground.ifBlank { "暂无全局背景" }}", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text("章节：$chapterTitle", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall)
            Text("章节内容：$chapterContent", color = Color(0xFFDCEEFF), style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis)
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
      MiniBtn(text = "关闭", onClick = onClose)
    }
  }
}

@Composable
private fun FooterBar(
  debugMode: Boolean,
  mode: String,
  onModeChange: (String) -> Unit,
  onOpenSetting: () -> Unit,
  onExitDebug: () -> Unit,
  onRefresh: () -> Unit,
  miniTags: List<String>,
  onMiniTag: (String) -> Unit,
  onSyncMiniGame: () -> Unit,
  sendText: String,
  onSendTextChange: (String) -> Unit,
  onSend: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      if (debugMode) {
        MiniBtn(text = "返回编辑", onClick = onExitDebug)
      } else {
        MiniBtn(text = "故事设定 >", onClick = onOpenSetting)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniBtn(text = "历史", onClick = { onModeChange(if (mode == "history") "live" else "history") })
        MiniBtn(text = "提示", onClick = { onModeChange(if (mode == "tips") "live" else "tips") })
        MiniBtn(text = if (debugMode) "状态" else "刷新", onClick = onRefresh)
      }
    }
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      miniTags.forEach { tag ->
        MiniBtn(text = tag, onClick = { onMiniTag(tag) })
      }
      MiniBtn(text = "同步小游戏", onClick = onSyncMiniGame)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = sendText,
        onValueChange = onSendTextChange,
        modifier = Modifier.weight(1f),
        placeholder = { Text("按住说话 / 输入文字") },
        maxLines = 2,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Button(onClick = onSend, shape = RoundedCornerShape(12.dp)) {
        Text("发送")
      }
    }
  }
}

@Composable
private fun ProfileScene(vm: MainViewModel, onPickAvatar: () -> Unit) {
  var showAvatarActionDialog by remember { mutableStateOf(false) }
  var showImageGenerateDialog by remember { mutableStateOf(false) }
  var imageGeneratePrompt by remember { mutableStateOf("") }
  var imageGenerateStyleKey by remember { mutableStateOf("general_3") }
  val imageGenerateReferenceUris = remember { mutableStateListOf<String>() }
  val allWorlds = vm.worldsForSelectedProject()
  val publishedWorlds = allWorlds.filter { vm.isWorldPublished(it) }
  val draftWorlds = allWorlds.filter { !vm.isWorldPublished(it) }
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
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .border(1.dp, Color(0xFFCCD8EA), CircleShape)
          .background(Color(0xFFE5EAF3))
          .clickable { showAvatarActionDialog = true },
      ) {
        if (vm.accountAvatarPath.isNotBlank()) {
          AsyncImage(
            model = vm.accountAvatarPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFFE5EAF3)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = vm.userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
              color = Color(0xFF7D8CA5),
              fontWeight = FontWeight.Bold,
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
      }
      Text(
        vm.userName.ifBlank { "未登录" },
        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF1F2A3F),
      )
    }

    if (showAvatarActionDialog) {
      AvatarActionDialog(
        onDismiss = { showAvatarActionDialog = false },
        onUpload = {
          showAvatarActionDialog = false
          onPickAvatar()
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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ProfileStatCell(value = likeCount.toString(), label = "获赞", modifier = Modifier.weight(1f))
      ProfileStatCell(value = followCount.toString(), label = "关注", modifier = Modifier.weight(1f))
      ProfileStatCell(value = fanCount.toString(), label = "粉丝", modifier = Modifier.weight(1f))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ProfileActionBtn(text = "新建故事", modifier = Modifier.weight(1f)) { vm.startNewStoryDraft() }
      ProfileActionBtn(text = "编辑资料", modifier = Modifier.weight(1f)) { vm.setTab("设置") }
    }

    Text("${allWorlds.size} 作品", fontWeight = FontWeight.ExtraBold, color = Color(0xFF313B4C))
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

    val workCards = mutableListOf<@Composable () -> Unit>()
    if (draftWorlds.isNotEmpty()) {
      draftWorlds.take(7).forEach { world ->
        workCards.add {
          ProfileWorkCard(
            cover = {
              StoryCoverImage(
                title = world.name.ifBlank { "故事" },
                coverPath = vm.worldCoverPath(world).ifBlank { null },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(126.dp),
                emptyText = "草稿",
              )
            },
            meta = "${world.name.ifBlank { "未命名故事" }} · 草稿",
            onClick = { vm.openWorldForEdit(world) },
            actions = {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileActionBtn(text = "编辑", modifier = Modifier.weight(1f)) { vm.openWorldForEdit(world) }
                ProfileActionBtn(text = "新开", modifier = Modifier.weight(1f)) { vm.startNewStoryDraft() }
              }
            },
          )
        }
      }
    } else {
      workCards.add {
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
          onClick = { vm.startNewStoryDraft() },
        )
      }
    }

    if (publishedWorlds.isNotEmpty()) {
      publishedWorlds.take(7).forEach { world ->
        workCards.add {
          ProfileWorkCard(
            cover = {
              StoryCoverImage(
                title = world.name.ifBlank { "故事" },
                coverPath = vm.worldCoverPath(world).ifBlank { null },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(126.dp),
              )
            },
            meta = "${world.name.ifBlank { "未命名故事" }} · 浏览 ${world.sessionCount ?: 0}",
            onClick = { vm.startFromWorld(world) },
            actions = {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileActionBtn(text = "进入游戏", modifier = Modifier.weight(1f)) { vm.startFromWorld(world) }
                ProfileActionBtn(text = "编辑", modifier = Modifier.weight(1f)) { vm.reopenPublishedWorldAsDraft(world) }
              }
            },
          )
        }
      }
    } else {
      workCards.add {
        ProfileWorkCard(
          cover = {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .background(Color(0xFFF3F6FC)),
              contentAlignment = Alignment.Center,
            ) {
              Text("暂无已发布故事", color = Color(0xFF90A3C2))
            }
          },
          meta = "发布后会在这里展示",
          onClick = { vm.startNewStoryDraft() },
        )
      }
    }

    for (row in workCards.chunked(2)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { card ->
          Box(modifier = Modifier.weight(1f)) {
            card()
          }
        }
        if (row.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
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

@Composable
private fun ProfileWorkCard(
  cover: @Composable () -> Unit,
  meta: String,
  onClick: () -> Unit,
  actions: (@Composable () -> Unit)? = null,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    cover()
    Text(
      text = meta,
      color = Color(0xFF556B8A),
      style = MaterialTheme.typography.bodySmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(7.dp),
    )
    if (actions != null) {
      Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 0.dp)) {
        actions()
      }
    }
  }
}

@Composable
private fun AvatarActionDialog(
  onDismiss: () -> Unit,
  onUpload: () -> Unit,
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
  initialConfigId: Long?,
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
  var selectedConfigId by remember(initialConfigId, title) { mutableStateOf(initialConfigId) }
  var selectedPresetId by remember(initialPresetId, title) { mutableStateOf(initialPresetId) }
  var selectedMode by remember(initialMode, title) { mutableStateOf(initialMode.ifBlank { "text" }) }
  var referenceAudioPath by remember(initialReferenceAudioPath, title) { mutableStateOf(initialReferenceAudioPath) }
  var referenceAudioName by remember(initialReferenceAudioName, title) { mutableStateOf(initialReferenceAudioName) }
  var referenceText by remember(initialReferenceText, title) { mutableStateOf(initialReferenceText) }
  var promptText by remember(initialPromptText, title) { mutableStateOf(initialPromptText) }
  val selectedMixVoices = remember(initialMixVoices, title) {
    mutableStateListOf<VoiceMixItem>().apply {
      addAll(initialMixVoices.ifEmpty { listOf(VoiceMixItem(weight = 0.7)) })
    }
  }
  var previewText by remember(initialLabel) {
    mutableStateOf(
      if (initialLabel.isNotBlank()) "你好，我是$initialLabel" else "你好，很高兴见到你。",
    )
  }
  var previewLoading by remember { mutableStateOf(false) }
  var audioUploading by remember { mutableStateOf(false) }
  var previewStatus by remember { mutableStateOf("") }
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
    vm.ensureVoiceModels()
  }
  LaunchedEffect(vm.voiceModels.size) {
    if (selectedConfigId == null && vm.voiceModels.isNotEmpty()) {
      selectedConfigId = vm.voiceModels.first().id
    }
  }
  LaunchedEffect(selectedConfigId) {
    vm.ensureVoicePresets(selectedConfigId)
  }

  val presets = vm.voicePresets(selectedConfigId)

  LaunchedEffect(presets.size, selectedMode) {
    if (selectedMode == "text" && selectedPresetId.isBlank() && presets.isNotEmpty()) {
      selectedPresetId = presets.first().voiceId
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
    if (selectedConfigId == null) return "请先选择语音模型"
    return when (selectedMode) {
      "text" -> if (selectedPresetId.isBlank()) "请先选择音色预设" else null
      "clone" -> if (referenceAudioPath.isBlank()) "克隆模式需要上传参考音频" else null
      "mix" -> if (selectedMixVoices.none { it.voiceId.isNotBlank() }) "混合模式至少选择一个音色" else null
      "prompt_voice" -> if (promptText.trim().isBlank()) "提示词模式需要填写提示词" else null
      else -> null
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
        Text("语音模型", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
        if (vm.voiceModels.isEmpty()) {
          Text("未加载到语音模型，请先在设置中配置 voice 模型。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
        } else {
          vm.voiceModels.forEach { model ->
            SelectableRow(
              text = buildString {
                append(model.model.ifBlank { "未命名模型" })
                if (model.manufacturer.isNotBlank()) append(" · ${model.manufacturer}")
              },
              selected = model.id == selectedConfigId,
              onClick = {
                selectedConfigId = model.id
                selectedPresetId = ""
              },
            )
          }
        }

        Text("绑定模式", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
        listOf(
          "text" to "预设音色",
          "clone" to "克隆音色",
          "mix" to "混合音色",
          "prompt_voice" to "提示词音色",
        ).forEach { (modeKey, modeLabel) ->
          SelectableRow(
            text = modeLabel,
            selected = selectedMode == modeKey,
            onClick = { selectedMode = modeKey },
          )
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
            OutlinedTextField(
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
            Text("提示词", fontWeight = FontWeight.Bold, color = Color(0xFF25324A))
            OutlinedTextField(
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
              selectedConfigId == null -> {
                Text("请先选择语音模型。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
              }
              presets.isEmpty() -> {
                Text("当前模型还没有返回可用音色。", color = Color(0xFF6A7F9F), style = MaterialTheme.typography.bodySmall)
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
        OutlinedTextField(
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
                    configId = selectedConfigId,
                    text = previewText.trim(),
                    mode = selectedMode,
                    presetId = selectedPresetId,
                    referenceAudioPath = referenceAudioPath,
                    referenceText = referenceText.trim(),
                    promptText = promptText.trim(),
                    mixVoices = selectedMixVoices.toList(),
                  )
                  if (url.isBlank()) error("未返回试听音频")
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
            enabled = mediaPlayer != null,
          ) {
            Text("停止")
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
              configId = selectedConfigId,
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
private fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) Color(0xFFE8F0FF) else Color(0xFFF8FBFF))
      .border(1.dp, if (selected) Color(0xFF9EB5DE) else Color(0xFFD8E3F3), RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    Text(text, color = if (selected) Color(0xFF294A79) else Color(0xFF5E7395))
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
              OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text("例如：黑框眼镜反光，格子衬衫塞进西装裤，手指总在裤缝模拟打字，背包插满电子设备线缆。") },
                enabled = !loading,
              )
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("还可输入 460 字", color = Color(0xFF9AA8BC), style = MaterialTheme.typography.bodySmall)
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
private fun ProfileActionDialogRow(icon: ImageVector, text: String, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(38.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F7FF), contentColor = Color(0xFF2E466A)),
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
      Text(text, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun SettingsScene(vm: MainViewModel) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageGray)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    HeaderTitle(title = "设置", rightText = "返回") { vm.setTab("我的") }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = vm.baseUrl,
          onValueChange = { vm.baseUrl = it },
          label = { Text("API Base URL") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = vm.loginUsername,
          onValueChange = { vm.loginUsername = it },
          label = { Text("用户名") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = vm.loginPassword,
          onValueChange = { vm.loginPassword = it },
          label = { Text("密码") },
          modifier = Modifier.fillMaxWidth(),
        )
        Text(
          "token 会在账号登录后自动获取，不需要手动填写。",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF697A97),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MiniBtn(text = "账号登录", primary = true, onClick = { vm.loginAndSaveToken() })
          MiniBtn(text = "保存连接", onClick = { vm.saveConnection() })
        }
      }
    }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("登录状态：${if (vm.token.isBlank()) "未登录" else "已登录"}")
        MiniBtn(text = "退出登录", onClick = { vm.clearToken() })
      }
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
  onClick: () -> Unit,
  showEditBadge: Boolean = false,
  iconTint: Color = Color(0xFF8AA0C6),
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .border(1.dp, Color(0xFFD3DEEF), CircleShape)
        .background(Color.White)
        .clickable { onClick() },
      contentAlignment = Alignment.Center,
    ) {
      if (!avatarPath.isNullOrBlank()) {
        AsyncImage(
          model = avatarPath,
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      } else {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(16.dp),
        )
      }
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
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF50617C), modifier = Modifier.padding(top = 5.dp))
  }
}

@Composable
private fun MediumEditableAvatar(path: String?, fallbackName: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(82.dp)
      .clip(CircleShape)
      .border(1.dp, Color(0xFFCCD8EA), CircleShape)
      .background(Color(0xFFE5EAF3))
      .clickable { onClick() },
  ) {
    if (!path.isNullOrBlank()) {
      AsyncImage(
        model = path,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFE5EAF3)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
          color = Color(0xFF7D8CA5),
          fontWeight = FontWeight.Bold,
        )
      }
    }

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
}

@Composable
private fun CircleGhostBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
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
      imageVector = icon,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier = Modifier.size(16.dp),
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
