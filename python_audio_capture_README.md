# Python Audio Capture Component

This component captures system audio and streams it via WebSocket to the Java application.

## Setup

1. Install Python 3.8 or higher
2. Install required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running the Audio Capture

1. Start the audio capture server:
   ```bash
   python audio_capture.py
   ```

The server will start capturing system audio and make it available via WebSocket at `ws://localhost:8080`.

## Configuration

You can modify the following parameters in `audio_capture.py`:
- `host`: WebSocket server host (default: 'localhost')
- `port`: WebSocket server port (default: 8080)
- `sample_rate`: Audio sample rate (default: 44100)
- `channels`: Number of audio channels (default: 2)
- `chunk_size`: Audio chunk size (default: 1024)

## Integration with Java Application

The Java application can connect to the WebSocket server to receive the audio stream. The audio is sent in WAV format, which can be easily processed by the Java application.

## Stopping the Server

Press Ctrl+C to stop the audio capture server.