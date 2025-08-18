# TextMeMail 📱

TextMeMail is an **Android messaging application** built with **Kotlin**, **Jetpack Compose**, and **Firebase**.  
It provides user authentication, account management, instant messaging, and video chat features.  

---

## 🚀 Features

- **User Authentication**
  - Sign up with email & password
  - Password confirmation
  - Email verification
  - User profile stored in **Firestore** (name, preferred language, etc.)

- **Account Management**
  - Change email
  - Change password
  - Switch language preference (English / Spanish)

- **Messaging**
  - Real-time instant messaging between users  
  - Secure data storage with **Cloud Firestore**

- **Video Chat**
  - Peer-to-peer video call functionality (planned)

- **UI**
  - Built entirely with **Jetpack Compose**
  - Modern Material 3 design
  - Dark/light theme support

---

## 🛠 Tech Stack

- **Android** (Kotlin, Jetpack Compose, Material 3)
- **Firebase Authentication**
- **Firebase Firestore** (NoSQL database)
- **Firebase Realtime features** (for chat)
- **WebRTC** (planned for video calls)

---

## 📂 Project Structure

app/
├── src/main/java/com/example/textmemail
│   ├── MainActivity.kt
│   ├── ui_auth/ (Authentication UI screens)
│   ├── auth/ (Firebase Auth manager)
│   └── ui/theme/ (App theming)
└── res/ (Resources: XML, icons, strings)

---

## 📖 How It Works

1. **User Registration**  
   - New users sign up with email + password + name + language preference.  
   - User data is stored in Firestore under the `users` collection.  
   - Email verification ensures account security.  

2. **Login**  
   - Existing users can sign in with email and password.  

3. **Messaging**  
   - Users can send and receive instant messages in real time.  

4. **Video Calls**  
   - Planned feature using WebRTC for peer-to-peer video chat.  

---

## 🔒 Security

- Firebase Authentication manages user credentials securely.  
- Firestore security rules restrict access to authenticated users only.  

---

## 📌 Roadmap

- [x] Firebase Auth setup  
- [x] User registration with Firestore integration  
- [ ] Real-time messaging with Firestore  
- [ ] Video chat integration (WebRTC)  
- [ ] Push notifications for new messages  

---

## 📝 License
 
Feel free to use, modify, and distribute for personal or academic projects.  
