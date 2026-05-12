// infra-stt 모듈 — 음성 인식(STT) 어댑터.
// 도메인이 선언한 SpeechToTextPort 인터페이스의 구현체가 산다.
//
// 외부 SDK (Azure Speech SDK 등)는 여기에만 가둔다.
// 도메인은 SDK 클래스를 알지 못하며, SDK 업데이트가 도메인을 흔들지 않는다.

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

// Azure Speech SDK는 Maven Central에 .aar 패키징으로만 배포된다 (Android 우선 정책).
// Java SE 컴파일러는 .aar를 직접 못 읽으므로 빌드 시점에 classes.jar를 추출해서 libs/로 떨어뜨린다.
// 이 모듈 안에서만 처리되므로 도메인/다른 인프라는 이 사정을 알 필요가 없다.

val azureSpeechVersion = "1.40.0"
val azureSpeechAar by configurations.creating
val extractedAzureSpeechJar = layout.buildDirectory.file("libs/azure-speech-$azureSpeechVersion.jar")

dependencies {
    azureSpeechAar("com.microsoft.cognitiveservices.speech:client-sdk:$azureSpeechVersion@aar")
}

val extractAzureSpeechSdk by tasks.registering {
    val aar = azureSpeechAar
    val output = extractedAzureSpeechJar
    inputs.files(aar)
    outputs.file(output)
    doLast {
        val aarFile: File = aar.singleFile
        val jarFile: File = output.get().asFile
        jarFile.parentFile.mkdirs()
        val zip = ZipFile(aarFile)
        try {
            val entry: ZipEntry = zip.getEntry("classes.jar")
                ?: error("classes.jar not found in ${aarFile.name}")
            zip.getInputStream(entry).use { src: InputStream ->
                jarFile.outputStream().use { dst: OutputStream -> src.copyTo(dst) }
            }
        } finally {
            zip.close()
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))

    // Spring 자동 설정 + @ConfigurationProperties 바인딩을 위한 starter.
    // web 스타터는 끌어오지 않는다 — STT 모듈은 HTTP 서버가 아니다.
    implementation("org.springframework.boot:spring-boot-starter")

    // 추출된 Azure Speech SDK classes.jar.
    // 운영 배포 시 OS별 네이티브 라이브러리(JNI .so/.dll/.dylib)도 필요하다 —
    // Microsoft 공식 배포본(https://aka.ms/csspeech)에서 받아 java.library.path에 둔다.
    implementation(files(extractedAzureSpeechJar).builtBy(extractAzureSpeechSdk))
}
