### 整体架构分层

```
app/
├── src/main/java/com/yourapp/
│   ├── data/                    # 数据层
│   │   ├── call/
│   │   │   ├── repository/
│   │   │   │   └── CallRepositoryImpl.kt
│   │   │   ├── datasource/
│   │   │   │   ├── local/
│   │   │   │   │   └── CallHistoryDao.kt
│   │   │   │   └── remote/
│   │   │   │       └── CallApiService.kt
│   │   │   └── model/
│   │   │       └── CallDto.kt
│   │
│   ├── domain/                  # 业务逻辑层
│   │   ├── call/
│   │   │   ├── model/
│   │   │   │   ├── Call.kt
│   │   │   │   ├── CallType.kt (AUDIO/VIDEO)
│   │   │   │   └── CallState.kt
│   │   │   ├── repository/
│   │   │   │   └── CallRepository.kt (interface)
│   │   │   └── usecase/
│   │   │       ├── StartCallUseCase.kt
│   │   │       ├── EndCallUseCase.kt
│   │   │       ├── ToggleMuteUseCase.kt
│   │   │       ├── ToggleVideoUseCase.kt
│   │   │       └── SwitchCameraUseCase.kt
│   │
│   ├── presentation/            # UI层
│   │   ├── call/
│   │   │   ├── screens/
│   │   │   │   ├── incoming/
│   │   │   │   │   ├── IncomingCallScreen.kt
│   │   │   │   │   └── IncomingCallViewModel.kt
│   │   │   │   ├── outgoing/
│   │   │   │   │   ├── OutgoingCallScreen.kt
│   │   │   │   │   └── OutgoingCallViewModel.kt
│   │   │   │   └── active/
│   │   │   │       ├── ActiveCallScreen.kt
│   │   │   │       └── ActiveCallViewModel.kt
│   │   │   ├── components/
│   │   │   │   ├── CallControlButtons.kt
│   │   │   │   ├── VideoRenderer.kt
│   │   │   │   ├── AudioWaveform.kt
│   │   │   │   └── CallTimer.kt
│   │   │   └── navigation/
│   │   │       └── CallNavigation.kt
│   │
│   ├── core/                    # 核心模块
│   │   ├── call/
│   │   │   ├── manager/
│   │   │   │   └── CallManager.kt
│   │   │   ├── engine/
│   │   │   │   ├── CallEngine.kt (interface)
│   │   │   │   └── WebRTCEngine.kt (或 AgoraEngine.kt)
│   │   │   ├── connection/
│   │   │   │   ├── SignalingClient.kt
│   │   │   │   └── PeerConnectionManager.kt
│   │   │   └── service/
│   │   │       └── CallService.kt (Foreground Service)
│   │   └── permission/
│   │       └── PermissionManager.kt
│   │
│   └── di/                      # 依赖注入
│       └── CallModule.kt
```

### 技术选型建议

1. WebRTC引擎: WebRTC (原生), Agora SDK, 或 100ms SDK
2. 信令服务: Socket.IO, WebSocket, 或 Firebase
3. 依赖注入: Hilt
4. 异步处理: Kotlin Coroutines + Flow
5. 权限管理: Accompanist Permissions

### 关键注意事项

1. 权限处理: CAMERA, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS
2. 后台保活: 使用 Foreground Service
3. 网络状态: 监听网络变化并处理重连
4. 生命周期: 正确处理 Activity/Screen 的生命周期
5. 资源释放: 确保通话结束后释放相机和麦克风资源