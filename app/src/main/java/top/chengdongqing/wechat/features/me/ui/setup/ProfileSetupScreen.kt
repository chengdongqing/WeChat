package top.chengdongqing.wechat.features.me.ui.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.cropper.rememberImageCropperLauncher
import top.chengdongqing.wechat.core.designsystem.components.informationbar.InformationBarType
import top.chengdongqing.wechat.core.designsystem.components.informationbar.WeInformationBar
import top.chengdongqing.wechat.core.designsystem.components.input.WeInput
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.util.createMediaUri

/**
 * 个人资料首次设置页面
 *
 * 用于用户首次使用应用时配置个人信息（昵称和头像）
 * 这是无中心化架构的身份创建页面，不涉及账号注册
 *
 * @param onBack 返回按钮点击回调
 * @param onSetupComplete 设置完成回调，传递用户名和头像URI
 */
@Composable
fun ProfileSetupScreen(
    onBack: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 用户输入状态
    var userName by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    Scaffold(
        topBar = {
            WeTopBar(
                title = "设置个人资料",
                containerColor = White,
                onBack = onBack,
                backIconResId = R.drawable.ic_close_outlined
            )
        },
        containerColor = White
    ) { paddingValues ->
        ProfileSetupContent(
            modifier = Modifier.padding(paddingValues),
            userName = userName,
            avatarUri = avatarUri,
            onUserNameChange = { userName = it },
            onAvatarChange = { avatarUri = it },
            onCompleteClick = {
                scope.launch {
                    keyboardController?.hide()
                    delay(300)
                    onSetupComplete(/*userName.trim(), avatarUri*/)
                }
            }
        )
    }
}

/**
 * 个人资料设置页面内容
 *
 * 包含头像选择、昵称输入和提交按钮
 * 键盘弹出时自动调整底部按钮位置
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSetupContent(
    modifier: Modifier = Modifier,
    userName: String,
    avatarUri: Uri?,
    onUserNameChange: (String) -> Unit,
    onAvatarChange: (Uri?) -> Unit,
    onCompleteClick: () -> Unit
) {
    // 键盘可见性检测
    val isKeyboardVisible = WindowInsets.isImeVisible

    // 键盘弹出时动画调整底部间距
    val bottomPadding by animateDpAsState(
        targetValue = if (isKeyboardVisible) 0.dp else 40.dp,
        label = "ButtonBottomPadding"
    )

    var errorInfo by remember { mutableStateOf("") }

    val handleOk = {
        when {
            userName.isBlank() -> errorInfo = "名字不能为空"
            avatarUri == null -> errorInfo = "请设置头像"
            else -> onCompleteClick()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 头像选择区域
            AvatarSelector(
                avatarUri = avatarUri,
                onAvatarChange = onAvatarChange
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 昵称输入框
            WeInput(
                value = userName,
                label = "名字",
                placeholder = "请填写名字",
                activeColor = Color(0xFFE5E5E5),
                maxLength = 17,
                onValueChange = onUserNameChange
            )

            // 昵称提示文本
            Text(
                text = "好名字可以让你的朋友更容易记住你。",
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, top = 12.dp)
            )

            // 弹性空白区域，将按钮推到底部
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 24.dp)
            )

            // 底部确定按钮
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = bottomPadding)
            ) {
                WeButton("确定") {
                    handleOk()
                }
            }
        }

        WeInformationBar(
            visible = errorInfo.isNotEmpty(),
            message = errorInfo,
            type = InformationBarType.WarnStrong,
            autoClose = true,
            onClose = { errorInfo = "" }
        )
    }
}

/**
 * 头像选择器组件
 *
 * 支持两种方式选择头像：
 * 1. 拍照
 * 2. 从相册选择
 *
 * @param avatarUri 当前头像URI，null表示未设置
 * @param onAvatarChange 头像变更回调
 */
@Composable
private fun AvatarSelector(
    avatarUri: Uri?,
    onAvatarChange: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 临时URI，用于拍照时存储图片
    val tempUri = remember { mutableStateOf<Uri?>(null) }

    // ActionSheet状态和选项
    val actionSheet = rememberActionSheetState()
    val options = remember {
        listOf(
            ActionSheetItem("拍照"),
            ActionSheetItem("从相册选择")
        )
    }

    val launchCropper = rememberImageCropperLauncher {
        onAvatarChange(it)
    }

    // 拍照启动器
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempUri.value?.let { launchCropper(it) }
        }
    }

    // 相册选择启动器
    val pickPicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { launchCropper(it) }
    }

    // 显示ActionSheet
    val showActionSheet = {
        actionSheet.show(options) { selectedIndex ->
            when (selectedIndex) {
                0 -> {
                    // 拍照
                    scope.launch {
                        val uri = context.createMediaUri()
                        tempUri.value = uri
                        takePicture.launch(uri)
                    }
                }

                1 -> {
                    // 从相册选择
                    pickPicture.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像展示/占位图
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .weClickable { showActionSheet() }
        ) {
            if (avatarUri != null) {
                // 显示已选择的头像
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 显示占位图
                Image(
                    painter = painterResource(R.drawable.img_avatar_placeholder),
                    contentDescription = "选择头像",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        WeButton(
            text = if (avatarUri != null) "更换头像" else "设置头像",
            type = ButtonType.Plain,
            size = ButtonSize.Small,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            prefix = {
                Icon(
                    painter = painterResource(R.drawable.ic_camera_filled),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        ) {
            showActionSheet()
        }
    }
}