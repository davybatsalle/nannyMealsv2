# NannyMeals

An Android app designed to help childminders track the meals they provide to the children in their care.

## Features

### User Registration and Authentication
- Create an account using email and password
- Secure login mechanism
- Password reset functionality

### Child Profiles
- Create profiles for each child including:
  - Name and date of birth
  - Dietary restrictions
  - Allergies
  - Parent/guardian contact information

### Meal Tracking
- Log meals with date, time, and type (breakfast, lunch, dinner, snack)
- Add notes about each meal
- Associate meals with specific children
- Calendar view to visualize meal logs

### Reports and Analytics
- Generate reports on meal patterns
- View insights about meal frequency and types
- Export reports in CSV format
- Email reports to parents/guardians

### Notifications
- Configurable meal reminders for breakfast, lunch, and dinner
- Customizable reminder times

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room
- **Authentication**: Firebase Auth
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose
- **Background Work**: WorkManager
- **Preferences**: DataStore

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add an Android app with package name `com.nannymeals.app`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Email/Password authentication in Firebase Console

### Build and Run
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Add your `google-services.json` file
5. Run the app on an emulator or device

## Project Structure

```
app/
├── src/main/java/com/nannymeals/app/
│   ├── data/
│   │   ├── dao/          # Room DAOs
│   │   ├── database/     # Room database
│   │   ├── entity/       # Room entities
│   │   ├── mapper/       # Data mappers
│   │   └── repository/   # Repository implementations
│   ├── di/               # Hilt modules
│   ├── domain/
│   │   ├── model/        # Domain models
│   │   └── repository/   # Repository interfaces
│   ├── navigation/       # Navigation setup
│   ├── notifications/    # WorkManager workers
│   └── ui/
│       ├── auth/         # Login/Register screens
│       ├── children/     # Child management screens
│       ├── home/         # Home dashboard
│       ├── meals/        # Meal tracking screens
│       ├── reports/      # Reports screens
│       ├── settings/     # Settings screen
│       └── theme/        # Material theme
└── src/main/res/         # Resources
```

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection for authentication and sync
- Notification permissions for meal reminders

## License

This project is proprietary software. All rights reserved.

## Contributing

This project is not open for contributions at this time.
