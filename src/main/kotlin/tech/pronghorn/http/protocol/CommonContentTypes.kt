package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.ByteBacked

interface ContentType: ByteBacked {
    fun getTypeName(): String
}

enum class CommonContentTypes(val _typeName: String,
                              val extensions: Array<String>,
                              override val bytes: ByteArray = _typeName.toByteArray(Charsets.US_ASCII)): ContentType {
    AudioAac("audio/aac", arrayOf(".aac")),
    AudioOgg("audio/ogg", arrayOf(".oga")),
    AudioWav("audio/x-wav", arrayOf(".wav")),
    AudioWebm("audio/webm", arrayOf(".webm")),
    ApplicationOctetStream("application/octet-stream", arrayOf(".bin")),
    ApplicationBzip("application/x-bzip", arrayOf(".bz")),
    ApplicationBzip2("application/x-bzip2", arrayOf(".bz2")),
    ApplicationJar("application/java-archive", arrayOf(".jar")),
    ApplicationJavascript("application/javascript", arrayOf(".js")),
    ApplicationJson("application/json", arrayOf(".json")),
    ApplicationPdf("application/pdf", arrayOf(".pdf")),
    ApplicationRar("application/x-rar-compressed", arrayOf(".rar")),
    ApplicationTar("application/x-tar", arrayOf(".tar")),
    ApplicationXhtml("application/xhtml+xml", arrayOf(".xhtml")),
    ApplicationXml("application/xml", arrayOf(".xml")),
    ApplicationZip("application/zip", arrayOf(".zip")),
    ApplicationSevenZip("application/x-7z-compressed", arrayOf(".7z")),
    FontTrueType("font/ttf", arrayOf(".ttf")),
    FontWoff("font/woff", arrayOf(".woff")),
    FontWoff2("font/woff2", arrayOf(".woff2")),
    ImageGif("image/gif", arrayOf(".gif")),
    ImageIco("image/x-icon", arrayOf(".ico")),
    ImagePng("image/png", arrayOf(".png")),
    ImageSvg("image/svg+xml", arrayOf(".svg")),
    ImageTiff("image/tiff", arrayOf(".tiff", ".tif")),
    ImageWebm("image/webp", arrayOf(".webp")),
    ImageJpeg("image/jpeg", arrayOf(".jpg", ".jpeg")),
    TextCss("text/css", arrayOf(".css")),
    TextCsv("text/csv", arrayOf(".csv")),
    TextHtml("text/html", arrayOf(".html", ".htm")),
    TextPlain("text/plain", arrayOf(".txt")),
    VideoAvi("video/x-msvideo", arrayOf(".avi")),
    VideoMpeg("video/mpeg", arrayOf(".mpeg")),
    VideoOgg("video/ogg", arrayOf(".ogv")),
    VideoWebm("video/webm", arrayOf(".webm"));

    override fun getTypeName(): String = _typeName
}
