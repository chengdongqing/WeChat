package top.chengdongqing.wechat.feature.chat.ai

import android.content.Context
import androidx.annotation.StringRes
import top.chengdongqing.wechat.feature.chat.R

@get:StringRes
val LocalAiError.messageRes: Int
    get() = when (this) {
        LocalAiError.MODEL_LOAD_FAILED -> R.string.local_ai_error_model_load_failed
        LocalAiError.MODEL_IMPORT_FAILED -> R.string.local_ai_error_model_import_failed
        LocalAiError.CANNOT_READ_MODEL -> R.string.local_ai_error_cannot_read_model
        LocalAiError.LOADING_CANCELLED -> R.string.local_ai_error_loading_cancelled
        LocalAiError.FILE_TOO_SMALL -> R.string.local_ai_error_file_too_small
        LocalAiError.INVALID_GGUF_FILE -> R.string.local_ai_error_invalid_gguf_file
        LocalAiError.MODEL_BACKUP_FAILED -> R.string.local_ai_error_model_backup_failed
        LocalAiError.MODEL_SAVE_FAILED -> R.string.local_ai_error_model_save_failed
        LocalAiError.MODEL_IS_CANCELLING -> R.string.local_ai_error_model_is_cancelling
        LocalAiError.MODEL_NOT_SELECTED -> R.string.local_ai_error_model_not_selected
        LocalAiError.INFERENCE_FAILED -> R.string.local_ai_error_inference_failed
    }

fun Context.getLocalAiErrorMessage(
    throwable: Throwable,
    fallback: LocalAiError
): String = getString(throwable.localAiError(fallback).messageRes)
