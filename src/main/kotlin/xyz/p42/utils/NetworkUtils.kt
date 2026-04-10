package xyz.p42.utils


import kotlinx.serialization.encodeToString
import xyz.p42.graphQlEndpoint
import xyz.p42.json
import xyz.p42.walletStorePath
import xyz.p42.model.Account
import xyz.p42.model.GraphQlPayload
import xyz.p42.model.ImportAccountGraphQlResponse
import xyz.p42.model.LockAccountGraphQlResponse
import xyz.p42.model.UnlockAccountGraphQlResponse
import xyz.p42.model.VkGraphQlResponse
import xyz.p42.properties.ENDPOINT_AVAILABILITY_CHECK_TIMEOUT
import xyz.p42.properties.RESPONSE_STRING_LIMIT_CHARS
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val logger = LoggingUtils.logger

fun isEndpointAvailable(url: String): Boolean =
  try {
    logger.info("Checking the '$url' availability...")

    val connection = URI(url).toURL().openConnection()
    connection.connectTimeout = ENDPOINT_AVAILABILITY_CHECK_TIMEOUT
    connection.connect()
    true
  } catch (e: Exception) {
    logger.info(e.message!!)
    false
  }

fun getAccountVerificationKey(account: Account): String? =
  sendGraphQlQuery(getAccountVkGraphQlQuery(account.pk)).let {
    if (it == null) {
      logger.info("Verification key for the account '${account.pk}' is not available!")
      return null
    }
    return json.decodeFromString<VkGraphQlResponse>(it).data.account.verificationKey?.verificationKey
  }

fun lockAccount(account: Account): String =
  sendGraphQlQuery(getLockAccountGraphQlQuery(account.pk)).let {
    checkNotNull(it) { "Account '${account.pk}' cannot be locked!" }
    return json.decodeFromString<LockAccountGraphQlResponse>(it).data.lockAccount.account.publicKey
  }

fun importAccountIfNeeded(account: Account) {
  val keysDir = walletStorePath ?: return
  val keyFile = File(keysDir, account.pk)
  if (!keyFile.exists()) {
    logger.info("Key file not found for account '${account.pk}' at ${keyFile.absolutePath}, skipping import")
    return
  }
  logger.info("Importing account '${account.pk}' from ${keyFile.absolutePath}...")
  val response = sendGraphQlQuery(getImportAccountGraphQlQuery(keyFile.absolutePath))
  if (response == null) {
    logger.info("WARNING: importAccount returned null for '${account.pk}'")
    return
  }
  val result = json.decodeFromString<ImportAccountGraphQlResponse>(response).data.importAccount
  if (result.alreadyImported) {
    logger.info("Account '${account.pk}' was already imported")
  } else {
    logger.info("Account '${account.pk}' imported successfully")
  }
}

fun unlockAccount(account: Account): String {
  importAccountIfNeeded(account)
  return sendGraphQlQuery(getUnlockAccountGraphQlQuery(account.pk)).let {
    checkNotNull(it) { "Account '${account.pk}' cannot be unlocked!" }
    json.decodeFromString<UnlockAccountGraphQlResponse>(it).data.unlockAccount.account.publicKey
  }
}

fun sendGraphQlQuery(query: String): String? =
  try {
    val client = HttpClient.newBuilder().build()
    val request =
      HttpRequest.newBuilder()
        .uri(URI.create(graphQlEndpoint))
        .POST(
          HttpRequest.BodyPublishers.ofString(
            json.encodeToString(
              value = GraphQlPayload(
                query
              )
            )
          )
        )
        .header("Content-Type", "application/json")
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    logger.info("GraphQL response: ${response.body().take(RESPONSE_STRING_LIMIT_CHARS)} ...")
    if (response.statusCode() != 200) {
      null
    } else {
      response.body()
    }
  } catch (e: Exception) {
    null
  }
