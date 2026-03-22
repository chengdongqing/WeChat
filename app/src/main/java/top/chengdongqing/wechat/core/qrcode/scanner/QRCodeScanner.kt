package top.chengdongqing.wechat.core.qrcode.scanner

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.common.Barcode
import top.chengdongqing.wechat.core.designsystem.util.RequestAddFriendPermission

@Composable
fun WeQRCodeScanner(onRevoked: () -> Unit, onChange: (List<Barcode>) -> Unit) {
    val state = rememberScannerState(onChange)

    RequestAddFriendPermission(
        extraPermissions = remember { listOf(Manifest.permission.CAMERA) },
        onRevoked = onRevoked
    ) {
        CameraView(state)
        ScannerDecoration()
        ScannerTools(state)
    }
}

@Composable
private fun CameraView(state: ScannerState) {
    AndroidView(
        factory = { state.previewView },
        modifier = Modifier.fillMaxSize(),
        update = { state.updateCamera() }
    )
}