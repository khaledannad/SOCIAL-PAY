package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write")
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val currentUser = try {
    FirebaseAuth.getInstance().currentUser
  } catch (_: Exception) {
    null
  }

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

fun extractUserFriendlyFirestoreMessage(error: Throwable, isRtl: Boolean): String {
  val root = generateSequence(error) { it.cause }
    .filterIsInstance<FirebaseFirestoreException>()
    .firstOrNull() ?: error

  val message = root.message.orEmpty()
  return when {
    message.contains("PERMISSION_DENIED", ignoreCase = true) -> {
      if (isRtl) "تعذر الحفظ: لا تملك صلاحية الوصول أو البيانات غير مكتملة." else "Permission denied or invalid input format."
    }
    message.contains("Quota exceeded", ignoreCase = true) -> {
      if (isRtl) "تم تجاوز الحصة اليومية لقاعدة البيانات السحابية. يرجى المحاولة لاحقاً." else "Daily Firestore quota exceeded. Please try again tomorrow."
    }
    message.contains("UNAVAILABLE", ignoreCase = true) || message.contains("offline", ignoreCase = true) -> {
      if (isRtl) "الاتصال بالإنترنت غير مستقر حالياً. يرجى التحقق والمحاولة مجدداً." else "Network unavailable. Please check your connection and retry."
    }
    else -> root.localizedMessage ?: (if (isRtl) "حدث خطأ أثناء المزامنة مع السحابة" else "Cloud sync error occurred")
  }
}
