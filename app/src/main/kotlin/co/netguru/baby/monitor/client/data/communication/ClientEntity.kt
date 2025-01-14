package co.netguru.baby.monitor.client.data.communication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import co.netguru.baby.monitor.client.feature.noisedetection.NoiseDetector
import co.netguru.baby.monitor.client.feature.voiceAnalysis.VoiceAnalysisOption

@Entity(
    tableName = "CLIENT_DATA",
    indices = [Index(value = ["firebase_key"], unique = true)],
)
data class ClientEntity(
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "firebase_key") var firebaseKey: String,
    val voiceAnalysisOption: VoiceAnalysisOption = VoiceAnalysisOption.MACHINE_LEARNING,
    val noiseLevel: Int = NoiseDetector.DEFAULT_NOISE_LEVEL,
    // There is only one parent handled right now
    @PrimaryKey val id: Int? = 0,
)
