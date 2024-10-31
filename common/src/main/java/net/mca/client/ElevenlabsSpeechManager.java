package net.mca.client;
import net.mca.Config;
import net.mca.MCA;
import net.mca.client.sound.CustomEntityBoundSoundInstance;
import net.mca.client.sound.SingleWeighedSoundEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;


public class ElevenlabsSpeechManager {
    private static final MessageDigest MESSAGEDIGEST;
    public static final ElevenlabsSpeechManager INSTANCE = new ElevenlabsSpeechManager();

    private final MinecraftClient client;
    private boolean warningIssued = false;

    private static final String XI_API_KEY = Config.getInstance().elevenlabsPrivteAPIkey;  // ElevenLabs API key
    private static final int CHUNK_SIZE = 1024;  // Chunk size for streaming
    private static final String MODEL_ID = "eleven_multilingual_v2";  // ElevenLabs TTS model ID

    static {
        try {
            MESSAGEDIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public ElevenlabsSpeechManager() {
        client = MinecraftClient.getInstance();
    }

    private final Random random = new Random();

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format(Locale.ROOT, "%0" + (bytes.length << 1) + "X", bi);
    }

    public String getHash(String text) {
        MESSAGEDIGEST.update(text.getBytes());
        return toHex(MESSAGEDIGEST.digest()).toLowerCase(Locale.ROOT);
    }

    public void play(String language, String voice, float pitch, String text, Entity entity) {
        String hash = getHash(text);
        CompletableFuture.runAsync(() -> {
            if (downloadAudio(language, voice, text, hash, false)) {
                play(language, voice, pitch, entity, hash);
            } else if (!warningIssued) {
                warningIssued = true;
                MinecraftClient.getInstance().getMessageHandler().onGameMessage(
                        Text.translatable("command.tts_busy").formatted(Formatting.ITALIC, Formatting.GRAY),
                        false
                );
            }
        });
    }

    public void play(String language, String voice, float pitch, Entity entity, String hash) {
        Identifier soundLocation = MCA.locate("tts_cache/" + language + "-" + voice + "/" + hash);
        Sound sound = new Sound(soundLocation.getPath(), ConstantFloatProvider.create(1.0f), ConstantFloatProvider.create(1.0f), 1, Sound.RegistrationType.FILE, true, false, 16);
        SingleWeighedSoundEvents weightedSoundEvents = new SingleWeighedSoundEvents(sound, soundLocation, "");
        EntityTrackingSoundInstance instance = new CustomEntityBoundSoundInstance(weightedSoundEvents, SoundEvent.of(soundLocation), SoundCategory.NEUTRAL, 1.0f, pitch, entity, random.nextLong());
        client.execute(() -> client.getSoundManager().play(instance));

    }

    public boolean downloadAudio(String language, String voice, String text, String hash, boolean dry) {
        File mp3File = new File("ElevenlabsTTS_cache/mp3s/" + language + "-" + voice + "/" + hash + ".mp3");
        File oggFile = new File("tts_cache/" + language + "-" + voice + "/" + hash + ".ogg");

        // Check if OGG file already exists and is valid
        if (oggFile.exists() && oggFile.length() > 0) {
            return true;
        }

        // Ensure the directory structure exists

        oggFile.getParentFile().mkdirs();
        mp3File.getParentFile().mkdirs();
        String VOICE_ID = getGenderVoiceModelID(voice);
        String ttsUrl = "https://api.elevenlabs.io/v1/text-to-speech/" + VOICE_ID + "/stream";

        try (FileOutputStream outputStream = new FileOutputStream(mp3File)) {
            HttpURLConnection connection = (HttpURLConnection) new URL(ttsUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("xi-api-key", XI_API_KEY);

            String payload = String.format(
                    "{\"text\": \"%s\", \"model_id\": \"%s\", \"voice_settings\": {\"stability\": 0.3, \"similarity_boost\": 0.5, \"style\": 0.05, \"use_speaker_boost\": true}}",
                    cleanPhrase(text), MODEL_ID
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        if (dry) {
                            break;
                        }
                    }
                }

                // Convert MP3 to OGG using ffmpeg
                if (convertMp3ToOgg(mp3File.getAbsolutePath(), oggFile.getAbsolutePath())) {
                    if(mp3File.delete()){
                        MCA.LOGGER.info("File deleted successfully");
                    }
                    else {

                        MCA.LOGGER.info("File Not deleted");
                    }
                    return true;
                } else {
                    MCA.LOGGER.warn("Failed to convert MP3 to Ogg format.");
                }
            } else {
                MCA.LOGGER.warn("Failed to get audio: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            MCA.LOGGER.warn("Failed to download or save the audio: " + e.getMessage());
        }

        return false;
    }
    // function to convert the downloaded file to OGG which supported in MineCraft
    private boolean convertMp3ToOgg(String mp3Path, String oggPath) {
        // Path to the ffmpeg executable located in the main Minecraft folder
        String ffmpegPath = "ffmpeg-7.1-essentials_build/bin/ffmpeg.exe" ; // Adjust this path as necessary
        // Use ProcessBuilder to set up the ffmpeg command with appropriate settings for Minecraft
        ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath, "-i", mp3Path,    // Input MP3 file
                "-ac", "1",                   // Set audio channels to 1 (mono)
                "-ar", "44100",               // Set sample rate to 44100 Hz
                "-ab", "128k",                // Set audio bitrate to 128 kbps
                "-f", "ogg",                  // Set output format to Ogg
                oggPath                       // Output Ogg file path
        );

        // Optional: Redirect error stream to the same output stream
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            process.waitFor(); // Wait for ffmpeg to complete
            return process.exitValue() == 0; // Return true if the conversion was successful
        } catch (IOException | InterruptedException e) {
            MCA.LOGGER.warn("Error during MP3 to OGG conversion: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static String getGenderVoiceModelID(String str) {

        if (str != null && !str.isEmpty()) {
            char firstLetter = Character.toLowerCase(str.charAt(0)); // Get first letter and convert to lowercase
            char lastChar = str.charAt(str.length() - 1);
            //add up to 9 Voices IDs for each Gender
            if (firstLetter == 'f') {
                //female Voice IDs
                return switch (lastChar) {
                    case '0', '1', '2' -> "MF3mGyEYCl7XYWbV9V6O";
                    case '3', '4', '5' -> "AZnzlk1XvdvUeBnXmlld";
                    case '6', '7', '8', '9' -> "pMsXgVXv3BLzUgSXRplE";
                    default -> "AZnzlk1XvdvUeBnXmlld";  // If the last character not from 0 to 9
                };
                // Array of hard-coded female texts
            } else if (firstLetter == 'm') {
                //male Voice IDs
                return switch (lastChar) {
                    case '0', '1', '2' -> "ErXwobaYiN019PkySvjV";
                    case '3', '4', '5' -> "VR6AewLTigWG4xSOukaG";
                    case '6', '7', '8', '9' -> "onwK4e9ZLuTAKqWW03F9";
                    default -> "onwK4e9ZLuTAKqWW03F9";  // If the last character not from 0 to 9
                };
            }
        }
        return "Unknown";
    }



    @SuppressWarnings("SpellCheckingInspection")
    public String cleanPhrase(String p) {
        p = p.replaceAll("\\*.*\\*", "");
        p = p.replace("%supporter%", "someone");
        p = p.replace("%Supporter%", "someone");
        p = p.replace("some %2$s", "something");
        p = p.replace("at %2$s", "somewhere here");
        p = p.replace("At %2$s", "Somewhere here");
        p = p.replace(" to %2$s", " to here");
        p = p.replace(", %1$s.", ".");
        p = p.replace(", %1$s!", "!");
        p = p.replace(" %1$s!", "!");
        p = p.replace(", %1$s.", ".");
        p = p.replace("%1$s!", " ");
        p = p.replace("%1$s, ", " ");
        p = p.replace("%1$s", " ");
        p = p.replace("avoid %2$s", "avoid that location");
        p = p.replace(" Should be around %2$s.", "");
        p = p.replace("  ", " ");
        p = p.replace(" ,", ",");
        p = p.trim();
        return p;

    }
}
