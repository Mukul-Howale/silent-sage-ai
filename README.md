# 🔮 SilentSage AI
**Wise and silent helper during interviews.**  
Stay confident, stay stealthy. 🧘‍♂️✨

---

## 🚀 Features

- 🎤 Real-time voice transcription (only interviewer audio)
- 🧠 GPT-powered automatic answer suggestions
- 🕵️‍♂️ Stealth floating window with Listening indicator 🔴
- 🔄 Manual "Refresh" mini-button to request new GPT response
- 🎯 Smartly handles coding questions vs chit-chat

---

## ⚙️ Technologies Used

- Java 21
- Deepgram API (Speech-to-Text)
- Google Gemini AI (Answer generation)
- Java Swing (Minimal stealthy UI)
- Maven for build automation

---

## 🏗 Project Structure
```angular2html
SilentSageAI/
├── README.md
├── .env.example
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── silentsageai/
│       │           └── stealth/
│       │               ├── StealthAppLauncher.java
│       │               ├── StealthChatWindow.java
│       │               ├── TranscriptListener.java
│       │               └── GPTService.java
│       └── resources/
```
---

## 🔧 Setup Instructions

1. Clone the repository:
   ```bash
   https://github.com/Mukul-Howale/silent-sage-ai
2. Open in your IDE
3. Set your environment variables (create a root file .env)
   ```env
   OPENAI_API_KEY=your_openai_key_here
   DEEPGRAM_API_KEY=your_deepgram_key_here
4. Build:
   ```bash
   mvn clean install
5. Run:
   ```bash
   java -jar target/silentsageai-1.0.jar

## 🤖 Future Upgrades
- Auto-copy answers to clipboard 📋
- GPT mode switch ⚡🐢
- Profile Modes 🔥