import fi.iki.elonen.NanoHTTPD
import java.io.FileInputStream
import java.io.IOException

object ResponseHelper
{
    fun buildFileResponse(file: File2): NanoHTTPD.Response
    {
        return try {
            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/octet-stream",
                FileInputStream(file._file),
                file.length.toInt().toLong()
            ).also {
                it.addHeader("Content-Length", file.length.toString())
            }
        } catch (e: IOException) {
            buildForbiddenResponse("Reading file failed.")
        }
    }

    fun buildPlainTextResponse(text: String): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            NanoHTTPD.MIME_PLAINTEXT,
            text
        )
    }

    fun buildJsonTextResponse(jsonInText: String): NanoHTTPD.Response
    {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/json",
            jsonInText
        )
    }

    fun buildForbiddenResponse(s: String): NanoHTTPD.Response
    {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.FORBIDDEN,
            NanoHTTPD.MIME_PLAINTEXT,
            "FORBIDDEN: $s"
        )
    }

    fun buildInternalErrorResponse(s: String): NanoHTTPD.Response
    {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.INTERNAL_ERROR,
            NanoHTTPD.MIME_PLAINTEXT,
            "INTERNAL ERROR: $s"
        )
    }

    fun buildNotFoundResponse(path: String): NanoHTTPD.Response
    {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.NOT_FOUND,
            NanoHTTPD.MIME_PLAINTEXT,
            "Error 404, file not found: $path"
        )
    }
}