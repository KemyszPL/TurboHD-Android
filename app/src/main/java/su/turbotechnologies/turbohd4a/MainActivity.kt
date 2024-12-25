package su.turbotechnologies.turbohd4a

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val PICK_AV_FILE = 1
    private var selectedVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPickVideo: Button = findViewById(R.id.btnPickVideo)
        val btnTranscode: Button = findViewById(R.id.btnTranscode)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Handle "Pick Video" button click
        btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, PICK_AV_FILE)
        }

        // Handle "Transcode Video" button click
        btnTranscode.setOnClickListener {
            if (selectedVideoUri != null) {
                transcodeVideo(selectedVideoUri!!, progressBar)
            } else {
                Toast.makeText(this, "Please select a video first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == PICK_AV_FILE) {
            selectedVideoUri = data?.data // Save the selected video URI
            Log.d("Selected URI", "Uri: $selectedVideoUri")
        }
    }

    private fun copyFileToInternalStorage(uri: Uri): String? {
        val fileName = "temp_video.mp4"
        val tempFile = File(cacheDir, fileName)

        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun transcodeVideo(uri: Uri, progressBar: ProgressBar) {
        // Copy the file to a temporary path
        val tempFilePath = copyFileToInternalStorage(uri)

        if (tempFilePath == null) {
            Toast.makeText(this, "Failed to access video file", Toast.LENGTH_SHORT).show()
            return
        }

        // Define the output path
        val outputFilePath = File(filesDir, "output_video.mp4").absolutePath

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // FFmpeg command to transcode the video
        val command = "-i $tempFilePath -vcodec libx264 -crf 28 $outputFilePath"

        // Execute the FFmpeg command
        FFmpegKit.executeAsync(command, { session ->
            progressBar.visibility = View.GONE // Hide progress bar when done

            if (session.returnCode.isValueSuccess) {
                Log.d("FFmpeg", "Transcoding completed successfully.")
                Toast.makeText(this, "Transcoding completed!", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("FFmpeg", "Transcoding failed: ${session.returnCode}")
                Toast.makeText(this, "Transcoding failed", Toast.LENGTH_SHORT).show()
            }
        }, { log ->
            Log.d("FFmpeg Log", log.message) // Log FFmpeg output
        }, { statistics ->
            Log.d("FFmpeg Stats", "Frame: ${statistics.videoFrameNumber}")
        })
    }


}