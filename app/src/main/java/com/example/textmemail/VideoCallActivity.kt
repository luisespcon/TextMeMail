// app/src/main/java/com/example/textmemail/VideoCallActivity.kt
package com.example.textmemail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : AppCompatActivity() {

    // Tu App ID de Agora
    private val appId = "4d4ecc5bd1674f59bd295482c9546750"
    
    // Views
    private lateinit var localContainer: FrameLayout
    private lateinit var remoteContainer: FrameLayout
    private lateinit var endCallButton: ImageButton
    private lateinit var muteButton: ImageButton
    private lateinit var videoButton: ImageButton
    
    // Agora
    private var mRtcEngine: RtcEngine? = null
    private var isJoined = false
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    
    // Estado
    private var isMuted = false
    private var isVideoEnabled = true
    
    // Channel info
    private lateinit var channelName: String
    private lateinit var token: String
    private var uid = 0

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener parámetros del intent ANTES de crear layout
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: run {
            Toast.makeText(this, "Nombre de canal faltante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        token = intent.getStringExtra("TOKEN") ?: ""
        
        // Crear layout programáticamente
        setContentView(createVideoCallLayout())
        
        println("📱 VideoCallActivity iniciada con canal: $channelName")
        
        // Verificar permisos DESPUÉS de configurar la UI
        if (checkSelfPermission()) {
            // Delay pequeño para asegurar que la UI esté lista
            window.decorView.post {
                initializeEngine()
            }
        }
    }

    private fun createVideoCallLayout(): View {
        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Container remoto (pantalla completa)
        remoteContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF000000.toInt())
        }

        // Container local (esquina superior derecha)
        localContainer = FrameLayout(this).apply {
            val params = FrameLayout.LayoutParams(200, 300)
            params.topMargin = 100
            params.rightMargin = 50
            params.gravity = android.view.Gravity.TOP or android.view.Gravity.RIGHT
            layoutParams = params
            setBackgroundColor(0xFF333333.toInt())
        }

        // Botones de control
        val controlsLayout = createControlsLayout()

        rootLayout.addView(remoteContainer)
        rootLayout.addView(localContainer)
        rootLayout.addView(controlsLayout)

        return rootLayout
    }

    private fun createControlsLayout(): View {
        val layout = android.widget.LinearLayout(this).apply {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            params.bottomMargin = 100
            layoutParams = params
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        muteButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setOnClickListener { toggleMute() }
        }

        videoButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.presence_video_online)
            setOnClickListener { toggleVideo() }
        }

        endCallButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_call)
            setOnClickListener { leaveChannel() }
        }

        layout.addView(muteButton)
        layout.addView(videoButton)
        layout.addView(endCallButton)

        return layout
    }

    private fun checkSelfPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeEngine()
        } else {
            Toast.makeText(this, "Permisos de cámara y micrófono requeridos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeEngine() {
        try {
            println("🚀 INICIANDO VIDEOLLAMADA:")
            println("   - App ID: $appId")
            println("   - Channel: $channelName")
            println("   - Token: ${if (token.isEmpty()) "SIN TOKEN" else "CON TOKEN"}")
            
            // Verificar que el App ID no esté vacío
            if (appId.isBlank()) {
                throw Exception("App ID está vacío")
            }
            
            // Crear configuración con try-catch específico
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            
            println("✅ Config creado, inicializando RtcEngine...")
            
            // Inicializar RtcEngine con manejo específico
            try {
                mRtcEngine = RtcEngine.create(config)
                println("✅ RtcEngine creado exitosamente")
            } catch (e: Exception) {
                println("❌ Error específico creando RtcEngine: ${e.message}")
                throw Exception("No se pudo crear RtcEngine: ${e.message}")
            }
            
            // Configurar video
            mRtcEngine?.enableVideo()
            println("✅ Video habilitado")
            
            setupLocalVideo()
            println("✅ Video local configurado")
            
            joinChannel()
            println("✅ Intentando unirse al canal...")
            
        } catch (e: Exception) {
            println("❌ ERROR en initializeEngine: ${e.message}")
            e.printStackTrace()
            
            val errorMessage = when {
                e.message?.contains("101") == true -> "Error 101: Verifica tu App ID de Agora y conexión a internet"
                e.message?.contains("110") == true -> "Token inválido o expirado"
                e.message?.contains("2") == true -> "No tienes permisos de cámara/micrófono"
                else -> "Error inicializando videollamada: ${e.message}"
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            println("👥 Usuario se unió: $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            println("👋 Usuario se desconectó: $uid (razón: $reason)")
            runOnUiThread { 
                remoteSurfaceView?.visibility = View.GONE
                Toast.makeText(this@VideoCallActivity, "El usuario se desconectó", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isJoined = true
            println("🎉 ¡CONECTADO! Canal: $channel, UID: $uid")
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Conectado a la videollamada", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(err: Int) {
            println("❌ ERROR RTC: $err")
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Error en videollamada: $err", Toast.LENGTH_LONG).show()
            }
        }

        override fun onConnectionLost() {
            println("📡 Conexión perdida")
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Conexión perdida", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLocalVideo() {
        localSurfaceView = RtcEngine.CreateRendererView(baseContext)
        localSurfaceView?.setZOrderMediaOverlay(true)
        localContainer.addView(localSurfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = RtcEngine.CreateRendererView(baseContext)
        remoteContainer.addView(remoteSurfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        remoteSurfaceView?.visibility = View.VISIBLE
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        }
        mRtcEngine?.joinChannel(token, channelName, uid, options)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        mRtcEngine?.muteLocalAudioStream(isMuted)
        muteButton.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_btn_speak_now
        )
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        mRtcEngine?.muteLocalVideoStream(!isVideoEnabled)
        localSurfaceView?.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
        videoButton.setImageResource(
            if (isVideoEnabled) android.R.drawable.presence_video_online
            else android.R.drawable.presence_video_away
        )
    }

    private fun leaveChannel() {
        if (isJoined) {
            mRtcEngine?.leaveChannel()
            isJoined = false
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }
}
