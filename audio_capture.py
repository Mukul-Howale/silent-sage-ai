import asyncio
import json
import soundcard as sc
import sounddevice as sd
import numpy as np
import websockets
import wave
import io
import threading
import queue

class AudioCapture:
    def __init__(self, host='localhost', port=8080):
        self.host = host
        self.port = port
        self.audio_queue = queue.Queue()
        self.is_running = False
        self.sample_rate = 44100
        self.channels = 2
        self.chunk_size = 1024

    def capture_audio(self):
        """Capture system audio using soundcard"""
        self.is_running = True
        default_speaker = sc.default_speaker()
        
        with default_speaker.recorder(samplerate=self.sample_rate, channels=self.channels) as mic:
            while self.is_running:
                data = mic.record(numframes=self.chunk_size)
                self.audio_queue.put(data)

    def convert_to_wav(self, audio_data):
        """Convert numpy array to WAV format"""
        buffer = io.BytesIO()
        with wave.open(buffer, 'wb') as wav_file:
            wav_file.setnchannels(self.channels)
            wav_file.setsampwidth(2)  # 16-bit audio
            wav_file.setframerate(self.sample_rate)
            wav_file.writeframes((audio_data * 32767).astype(np.int16).tobytes())
        return buffer.getvalue()

    async def send_audio(self, websocket):
        """Send audio data through WebSocket"""
        while self.is_running:
            try:
                if not self.audio_queue.empty():
                    audio_data = self.audio_queue.get()
                    wav_data = self.convert_to_wav(audio_data)
                    await websocket.send(wav_data)
            except Exception as e:
                print(f"Error sending audio: {e}")
                break

    async def start_server(self):
        """Start WebSocket server"""
        async def handle_client(websocket, path):
            try:
                await self.send_audio(websocket)
            except websockets.exceptions.ConnectionClosed:
                pass

        server = await websockets.serve(
            handle_client,
            self.host,
            self.port
        )
        print(f"WebSocket server started at ws://{self.host}:{self.port}")
        await server.wait_closed()

    def start(self):
        """Start audio capture and WebSocket server"""
        # Start audio capture in a separate thread
        capture_thread = threading.Thread(target=self.capture_audio)
        capture_thread.start()

        # Start WebSocket server
        asyncio.run(self.start_server())

    def stop(self):
        """Stop audio capture and WebSocket server"""
        self.is_running = False

if __name__ == "__main__":
    audio_capture = AudioCapture()
    try:
        audio_capture.start()
    except KeyboardInterrupt:
        audio_capture.stop()
        print("Audio capture stopped")