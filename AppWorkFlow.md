Best Android Flow
Step 1 — User pastes Instagram URL

Jetpack Compose:

OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    label = { Text("Instagram URL") }
)
Step 2 — Call Your Backend

Using Retrofit.

Retrofit API
interface ApiService {

    @POST("download")
    suspend fun getVideo(
        @Body request: DownloadRequest
    ): DownloadResponse
}

data class DownloadRequest(
    val url: String
)

data class DownloadResponse(
    val video: String
)
Step 3 — Download Video to Phone

Use Android DownloadManager.

This is the BEST approach.

Download Function
fun downloadVideo(context: Context, url: String) {

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Instagram Video")
        .setDescription("Downloading...")
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "instagram_video_${System.currentTimeMillis()}.mp4"
        )

    val manager =
        context.getSystemService(Context.DOWNLOAD_SERVICE)
                as DownloadManager

    manager.enqueue(request)
}
Required Permissions
AndroidManifest.xml

For Android 9 and below:

<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

For Android 10+:

usually not needed with DownloadManager.
Jetpack Compose Button
Button(
    onClick = {

        scope.launch {

            val response = api.getVideo(
                DownloadRequest(url)
            )

            downloadVideo(context, response.video)
        }
    }
) {
    Text("Download")
}
Backend Example (Node.js)
Express + yt-dlp

The MOST powerful solution.

Install:

pip install yt-dlp

Server:

const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use(express.json());

app.post("/download", (req, res) => {

    const url = req.body.url;

    exec(`yt-dlp -g ${url}`, (err, stdout) => {

        if (err) {
            return res.status(500).send(err);
        }

        res.json({
            video: stdout.trim()
        });
    });
});

app.listen(3000);

yt-dlp supports Instagram Reels/posts/videos.

Project:
yt-dlp GitHub

Production Recommendation
BEST STACK
Android
Jetpack Compose
Retrofit
Coroutines
DownloadManager
Backend
Node.js or Python
yt-dlp

This is how most downloader apps work.

Important Legal Note

Instagram content belongs to creators.

You should:

download only public content,
avoid bypassing authentication,
include Terms/Privacy Policy,
respect copyright laws.

Avoid:

downloading private account content,
scraping logged-in user data,
storing Instagram cookies insecurely.
Alternative: Share Intent Method

Many apps avoid scraping entirely.

User:

opens Instagram,
taps Share,
shares Reel URL to your app.

Your app receives URL:

<intent-filter>
    <action android:name="android.intent.action.SEND"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <data android:mimeType="text/plain"/>
</intent-filter>

Then extract URL from intent text.

This creates a smoother UX.

Suggested App Flow
Share Instagram Reel
        ↓
Your App Opens
        ↓
Extract URL
        ↓
Call Backend
        ↓
Get MP4 URL
        ↓
DownloadManager
        ↓
Saved to Downloads
Recommendation for You

Since you are using:

Kotlin
Jetpack Compose
Utility app

I strongly recommend:

Compose UI
+ Retrofit
+ Node.js backend
+ yt-dlp
+ DownloadManager

because:

stable,
scalable,
easier maintenance,
fewer Instagram blocks.

If you want, I can also help you with:

Complete Jetpack Compose downloader app
Full backend source code
MVVM architecture
Hilt dependency injection
Share intent handling
Reels thumbnail extraction
Progress bar while downloading
Android 14 compatible storage handling
FFmpeg integration
Play Store policy-safe implementation